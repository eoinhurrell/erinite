(ns erinite.services.navigation
  (:require [quile.component :as component]
            [shodan.console :as console :include-macros true]
            [erinite.lib.navigation :as nav]
            [erinite.lib.events :as events]
            [goog.events :as gevents]))


(defn push-state!
  ([state title]
    (.pushState js/history (clj->js state) title))
  ([state title path]
    (.pushState js/history (clj->js state) title path)))


(defn handle-page-load
  [state]
  (let [pathname  (.. js/window -location -pathname)
        path      (next (clojure.string/split pathname #"/"))]
    (events/send! :Navigation/load
                  (or path [(name (:root state))]))))


(defn pop-state [e]
  (handle-page-load
    (js->clj (.-state e) :keywordize-keys true)))


(defn nav-state-watcher
  [component _ _ old-state new-state]
  (let [old-path (:path old-state)
        new-path (:path new-state)]
    (when (not= old-path new-path)
      (let [page          (nav/page new-state)
            state         (:state new-state)
            append-params (fn [page-id]
                            (concat
                              [(name page-id)]
                              (map
                                #(get (:params page) %)
                                (get-in state [page-id :params]))))
            path          (->> new-path
                            (map append-params)
                            flatten
                            (clojure.string/join "/")
                            (str "/"))]
        (push-state! {:root (:start new-state)} (:title page) path)
        (events/send! :Navigation/page-changed new-path page)))))


(defn consume
  "Similar to reduce, except that each reduction can consume multiple items
   f => (fn [accum item s] ...) -> [accum items]
   At each step, item is (first items) and seq is (rest items) until no more
   items are left.
   init => starting value of accum, nil if omitted
   coll => collection to consume"
  ([f coll] (consume f nil coll))
  ([f init coll]
    (loop [s     coll
           state init]
      (let [[state items] (f state (first s) (rest s))]
        (if (seq items)
          (recur items state)
          state)))))


(defn consume-path
  [state [_ params] page-id remaining]
  ;; Look up what parameters this page
  ;; expects
  (let [page        (get state (keyword page-id))
        param-list  (:params page)]
    (vector
      ;; Keep the page-id and one
      ;; remaining item from the path
      ;; for each parameter
      (vector
        page-id
        (into params (map
                       vector
                       param-list
                       remaining)))
      ;; Drop the parameters from the
      ;; remaining items in the path
      (drop (count param-list)
            remaining))))


(defn page-change-helper
  "Helper function to factor repitition from page change functions set and push"
  [component f]
  (fn [new-page & [params]]
    (f component new-page (if (map? params) params {}))))


(defrecord Navigation [nav-state]
  component/Lifecycle
  (start [component]
    ;; Watch state for changes so that page-changed events can be emitted
    (add-watch nav-state :nav-state (partial nav-state-watcher component))
    ;; Handle pop state browser events
    (set! (.-onpopstate js/window) pop-state)
    ;; Listen to events to manipulate the navigation state
    (events/listen!
      {;; Set the page to a specific page
       :Navigation/set  (page-change-helper component nav/set-page!) 
       ;; Set te page to a specific subpage
       :Navigation/push (page-change-helper component nav/push-page!) 
       ;; Set the page to the parent page
       :Navigation/pop  #(nav/pop-page! component)
       ;; Set the page to a specific page, named by a path
       :Navigation/load (fn [path]
                          (let [state (:state @nav-state)
                                ;; Get the page and parameters by consuming the
                                ;; path page by page and extracting the required
                                ;; parameters from each
                                [new-page params] (consume
                                                    (partial consume-path state)
                                                    [nil {}]
                                                    path)]
                            ;; Change to the new page, passing the params along
                            (nav/set-page! component
                                           (keyword new-page)
                                           params)))
       ;; Emit page-changed event so the system knows about the starting page
       :Erinite/start 
          #(events/send! :Navigation/page-changed
                         (:path @nav-state)
                         (nav/page @nav-state))})
    ;; Handle initial page load
    (handle-page-load {:root (:start @nav-state)})
    component)

  (stop [component]
    (assoc component :nav-state nil)))


(defn navigation-srv [{:keys [pages start-page]}]
  (map->Navigation {:nav-state (atom {:path [start-page]
                                      :start start-page
                                      :params {}
                                      :state (nav/set-parents pages)})})) 

