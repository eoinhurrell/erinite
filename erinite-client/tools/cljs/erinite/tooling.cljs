(ns erinite.tooling
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [erinite.utility :as util]
            [cljs.core.async :refer [put! chan <!]]))

(def tools-data (atom {:expanded false
                       :tools []
                       :current-tab 0
                       :roots {}}))

(defn root [root-name component state target]
  (om/root component state target)
  (let [func (fn [_ _ _ v] (swap! tools-data assoc-in [:roots root-name] v))]
    (add-watch state ::ankha func)
    (func nil nil nil @state)))

(set! util/root root)

(defn uninstall-all []
  (swap! tools-data assoc-in [:tools] []))

(defn install [label component]
  (let [id (count (:tools @tools-data :tools))]
    (swap! tools-data update-in [:tools] conj {:id id
                                               :label label
                                               :component component})))

(defn tool-tabs [tool owner]
  (reify
    om/IRenderState
    (render-state [this {:keys [selected select]}]
      (dom/div #js {:className (str "tab"
                                    (when (= selected (:id tool))
                                      " selected"))
                    :onClick (fn [e]
                               (put! select (:id @tool)))}
               (:label tool)))))


(defn erinite-tooling [app owner]
  (reify
    om/IInitState
    (init-state [_]
      {:select (chan)})
    om/IWillMount
    (will-mount [_]
      (let [select (om/get-state owner :select)]
        (go-loop []
          (let [tool (<! select)]
            (om/transact! app :current-tab
              (fn [_] tool))
            (recur)))))
    om/IRenderState
    (render-state [this {:keys [select]}]
      (if (:expanded app)
        (let [current-id    (:current-tab app)
              current-tool  (-> (filter (fn [t] (= current-id (:id t)))
                                        (:tools app))
                                first
                                :component)]
          (dom/div #js {:className "container"}
            (dom/div #js {:className "content-area"}
              (dom/div #js {:className "content"}
                (om/build current-tool (:roots app))))
            (apply dom/div #js {:className "tabs"}
              (conj (om/build-all tool-tabs (:tools app)
                            {:state       {:selected current-id}
                             :init-state  {:select select}})
              (dom/div #js {:id "tooling-toggle"
                            :className "tab"
                            :style {:float "right !important"}
                            :onClick #(om/transact! app :expanded (fn [_] false))}
                      ">>")))))
        (dom/div #js {:id "tooling-toggle"
                      :className "tab"
                      :onClick #(om/transact! app :expanded (fn [_] true))}
                    "<<")))))

(om/root
  erinite-tooling
  tools-data
  {:target (js/document.getElementById "erinite-tooling")})
