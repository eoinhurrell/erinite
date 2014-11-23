(ns erinite.services.navigation
  (:require [quile.component :as component]
            [shodan.console :as console :include-macros true]
            [erinite.lib.navigation :as nav]
            [erinite.lib.events :as events]
            [goog.events :as gevents]))

(defn nav-state-watcher
  [component _ _ old-state new-state]
  (let [old-path (:path old-state)
        new-path (:path new-state)]
    (when (not= old-path new-path)
     (events/send! :Navigation/page-changed new-path (nav/page component)))))

(defrecord Navigation [nav-state]
  component/Lifecycle
  (start [component]
    ;; Watch state for changes so that page-changed events can be emitted
    (add-watch nav-state :nav-state (partial nav-state-watcher component))
    ;; Listen to events to manipulate the navigation state
    (events/listen!
      {:Navigation/set  #(nav/set-page! component %)
       :Navigation/push #(nav/push-page! component %)
       :Navigation/pop  #(nav/pop-page! component)
       :Erinite/start ;; Emit page-changed event so that the system knows about the starting page
          #(events/send! :Navigation/page-changed (:path @nav-state) (nav/page component))})
    component)

  (stop [component]
    (assoc component :nav-state nil)))

(defn navigation-srv [{:keys [pages start-page]}]
  (map->Navigation {:nav-state (atom {:path [start-page]
                                      :start start-page
                                      :state (nav/set-parents pages)})})) 

; (def history (Html5History.))
; (.setUseFragment history false)
; (.setPathPrefix history "")
; (.setEnabled history true)
;  
; (let [navigation (listen history EventType/NAVIGATE)]
;   (go 
;      (while true
;        (let [token (.-token (<! navigation))]
;          (secretary/dispatch! token)))))
;  
; (events/listen js/document "click" (fn [e]
;                                      (let [path (.getPath (.parse Uri (.-href (.-target e))))
;                                            title (.-title (.-target e))]
;                                        (when (secretary/any-matches? path)
;                                          (. history (setToken path title))
;                                          (.preventDefault e)))))
