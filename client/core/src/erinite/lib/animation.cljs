(ns erinite.lib.animation)

(def css-trans-group (-> js/React (aget "addons") (aget "CSSTransitionGroup"))) 
(defn transition-group
  [{:keys [name enter leave]} components]
  (let [opts #js {:transitionName  name
                  :transitionEnter enter
                  :transitionLeave leave}]
    (if (coll? components)
      (apply css-trans-group opts components)
      (css-trans-group opts components))))
