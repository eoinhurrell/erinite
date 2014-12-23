(ns erinite.internal.template-transform
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [shodan.console :as console :include-macros true]
            [bootstrap-cljs :as bs :include-macros true]
            [erinite.lib.animation :refer [transition-group]]
            [erinite.lib.events :as events]))

(defn replace-placeholder
  [{:keys [view view-components] :as current-page} data node]
  (let [view-element  (get-in node [:attrs :id])
        component-id  (->> view-element keyword (get view))]
    (if-let [component-ctor (get view-components component-id)]
      (let [component (component-ctor
                        data
                        {:state {:current-page (dissoc current-page
                                                       :view-components)}
                         :react-key (or (get-in node [:attrs :key])
                                        (name component-id))})
            enter-anim?   (if (get-in node [:attrs :enter-anim]) true false)
            leave-anim?   (if (get-in node [:attrs :leave-anim]) true false)]
        (if (or enter-anim? leave-anim?)
          (transition-group
            {:name (name view-element)
             :enter enter-anim?
             :leave leave-anim?}
            component)
          component))
      (:content node))))


(defn replace-control
  [pre node]
  (let [node    (if pre
                  (let [xclass  (->> node :attrs :className (str ".") keyword)
                        xid     (->> node :attrs :id        (str "#") keyword)]
                    (-> node
                        ((get pre xclass identity))
                        ((get pre xid identity))))
                  node)
        attrs   (:attrs node)
        attrs   (into {}
                  (map
                    (fn [[k v]]
                      (let [a (name k)
                            c (count a)]
                        (cond
                          ;; If attr ends with -param, remove it from the attrs
                          (and (> c 9) (= (subs a (- c 6) "-param")))
                              [::_ nil] ; This will be removed
                          ;; If attr starts with on- then convert it into an
                          ;; event handler function
                          (and (> c 3) (= (subs a 0 3) "on-"))
                              [k  (events/send
                                    (keyword v)
                                    ;; If an attr exists with name <k>-param
                                    ;; then use its value as the parameter to
                                    ;; the event sent by this handler function
                                    (get attrs (keyword (str a "-param"))))]
                          ;; Otherwise leave attr as is
                          :otherwise [k v])))
                    attrs))
        c-type  (keyword (:type attrs))
        config  (dissoc attrs :type ::_)
        content (:content node)]
  (case c-type
    :accordion       (bs/accordion config content)
    :affix           (bs/affix config content)
    :alert           (bs/alert config content)
    :badge           (bs/badge config content)
    :button          (bs/button config content)
    :button-group    (bs/button-group config content)
    :button-toolbar  (bs/button-toolbar config content)
    :carousel        (bs/carousel config content)
    :carousel-item   (bs/carousel-item config content)
    :col             (bs/col config content)
    :dropdown-button (bs/dropdown-button config content)
    :dropdown-menu   (bs/dropdown-menu config content)
    :glyphicon       (bs/glyphicon config content)
    :grid            (bs/grid config content)
    :input           (bs/input config content)
    :jumbotron       (bs/jumbotron config content)
    :label           (bs/label config content)
    :menu-item       (bs/menu-item config content)
    :modal           (bs/modal config content)
    :modal-trigger   (bs/modal-trigger config content)
    :nav             (bs/nav config content)
    :navbar          (bs/navbar config content)
    :nav-item        (bs/nav-item config content)
    :overlay-trigger (bs/overlay-trigger config content)
    :page-header     (bs/page-header config content)
    :pager           (bs/pager config content)
    :page-item       (bs/page-item config content)
    :panel           (bs/panel config content)
    :panel-group     (bs/panel-group config content)
    :popover         (bs/popover config content)
    :progress-bar    (bs/progress-bar config content)
    :row             (bs/row config content)
    :split-button    (bs/split-button config content)
    :subNav          (bs/subNav config content)
    :tabbed-area     (bs/tabbed-area config content)
    :table           (bs/table config content)
    :tab-pane        (bs/tab-pane config content)
    :tooltip         (bs/tooltip config content)
    :well            (bs/well config content)
    content)))

