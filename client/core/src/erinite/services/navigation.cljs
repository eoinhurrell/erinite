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
    (println "Page load" path)
    (events/send! :Navigation/load
                  (or path [(name (:default state))]))))


(defn pop-state [e]
  (handle-page-load
    (js->clj (.-state e) :keywordize-keys true)))


(defn nav-state-watcher
  [component _ _ old-state new-state]
  (let [old-path (:path old-state)
        new-path (:path new-state)]
    (when (not= old-path new-path)
      (let [page          (nav/details new-state)
            structure     (:structure new-state)
            append-params (fn [[doc-id page-id]]
                            (concat
                              [(name (if (= page-id :*) doc-id page-id))]
                              (map
                                #(get (:params page) % "")
                                (get-in structure [page-id :params]))))
            path          (->> new-path
                            (map append-params)
                            flatten
                            (clojure.string/join "/")
                            (str "/"))]
        (println "")
        (println old-path)
        (println new-path)
        (println path)
        (push-state! {:default (:default-document new-state)} (:title page) path)
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
  [navigation structure null page-id remaining]
  ;; Look up what parameters this page expects
  (let [page-id       (keyword page-id)
        page          (get structure page-id)
        param-list    (:params page)
        ;; Retrieve list of items for parameters actually present on path
        parameters    (take-while ; Keep taking from list while there
                        #(not= % "") ; If empty item, then there are no more
                        ;; Maximum number of params is number in param-list
                        (take (count param-list) remaining))]
    ;; Navigate forward one page
    (nav/forward!
      navigation
      page-id
      ;; Create map of parameters name to value
      (into {} (map vector param-list parameters))
      ;; Don't move from document root to default page
      true)
    ;; Strip parameters from remaining path 
    [nil (drop (count parameters) remaining)]))


(defn page-change-helper
  "Helper function to factor repitition from page change functions set and push"
  [component f]
  (fn [new-page & [params]]
    (f component new-page (if (map? params) params {}))))


(defrecord Navigation [nav-state]
  component/Lifecycle
  (start [component]
    (try
    ;; Watch state for changes so that page-changed events can be emitted
    (add-watch nav-state :nav-state (partial nav-state-watcher component))
    ;; Handle pop state browser events
    (set! (.-onpopstate js/window) pop-state)
    ;; Listen to events to manipulate the navigation state
    (events/listen!
      {;; Set the page to a specific page, named by a path
       :Navigation/load (fn [path]
                          (let [structure (:structure @nav-state)]
                            ;; Clear existing documents and set root
                            (println "LOADING" path)
                            (nav/set-document! component (keyword (first path)))
                            ;; Navigate along the path by consuming the path
                            ;; page by page an extracting the required
                            ;; parameters from each and then navigating forward
                            ;; for each page
                            (consume
                              (partial consume-path component structure)
                              nil
                              path)
                            (println "DONE" (:path @nav-state))))
       ;; Set te page to a specific subpage
       :Navigation/forward  (page-change-helper component nav/forward!)
       ;; Set the page to the parent page
       :Navigation/back     #(nav/backward! component)
       ;; Set the page to a specific page (relative to the open document)
       :Navigation/go-to    (page-change-helper component nav/go-to!)
       ;; Close the current document, setting the page to latest in previous doc
       :Navigation/close    #(nav/close! component)
       ;; Close all open documents and open a new document, set page to root
       :Navigation/set-document (page-change-helper component nav/set-document!)
       ;; Emit page-changed event so the system knows about the starting page
       :Erinite/start 
          #(events/send! :Navigation/page-changed
                         (:path @nav-state)
                         (nav/details @nav-state))})
    ;; Handle initial page load
    (handle-page-load {:default (:default-document @nav-state)})
    component
    (catch js/Error e
      (js/console.log e))))

  (stop [component]
    (assoc component :nav-state nil)))


(defn navigation-srv [{:keys [documents root-document]}]
  (map->Navigation {:nav-state (atom (nav/make-nav-state documents root-document))})) 

