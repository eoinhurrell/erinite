(ns erinite.internal.template-transform
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [shodan.console :as console :include-macros true]
            [bootstrap-cljs :as bs :include-macros true]
            [erinite.lib.events :as events]))

(defn replace-placeholder
  [{:keys [view view-components] :as current-page} data node]
  (let [component-id (->> [:attrs :id] (get-in node) keyword (get view))]
    (if-let [component (get view-components component-id)]
      (component data {:state {:current-page (dissoc current-page
                                                     :view-components)}})
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
                        (if (and (> c 3)
                                 (= (subs a 0 3) "on-")
                                 (not (and (> c 9)
                                           (= (subs a (- c 6) "-param")))))
                          [k (events/send
                               (keyword v)
                               (get attrs (keyword (str a "-param"))))]
                          [k v])))
                    attrs))
        c-type  (keyword (:type attrs))
        config  (assoc
                  (dissoc attrs :type :event :class)
                  :className (:class attrs))
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

