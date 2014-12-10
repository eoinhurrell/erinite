(ns erinite.services.renderer
  (:require-macros [kioo.om])
  (:require [quile.component :as component]
            [shodan.console :as console :include-macros true]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [erinite.internal.state :as state]
            [erinite.lib.events :as events]))


(defn invalid-renderer [page data]
  (dom/div nil "INVALID RENDERER"))


(defn erinite-root-component
  "The Erinite management and Om root component.
   This component is the root of an Erinite application, which is used to manage
   application state (the app model) and the component hierarchy of an Erinite
   client application."
  [data owner {:keys [renderers views]}]
  (reify
    om/IDisplayName
    (display-name [_]
      "Erinite Application")

    om/IWillMount
    (will-mount [_]
      (events/listen!
        {:Navigation/page-changed
            (fn [path page]
              (let [[doc-id page-id] (peek path)]
                (om/set-state! owner
                  :current-page
                  (assoc page
                    :view-components views
                    :document-id     doc-id 
                    :page-id         page-id))))
         :Renderer/set
            (fn [new-renderer]
              (om/set-state! owner
                :renderer
                (get renderers new-renderer invalid-renderer)))
         :State/set
            (fn [where what]
              (om/update! data where what))}))

    om/IRenderState 
    (render-state [_ {:keys [current-page renderer]}]
      (renderer current-page data))))


(defrecord Renderer [om-config navigation]
  component/Lifecycle
  (start [component]
    ;; Set up Om root and management component
    (om/root erinite-root-component state/app-model om-config)
    component)

  (stop [component]
    component))

(defn renderer-srv [{:keys [target shared-data- renderer renderers views]}]
  (let [config  {:target (if (= target :body)
                           (. js/document -body)
                           (. js/document (getElementById target)))
                 :shared shared-data-
                 :init-state {:renderer (get renderers renderer)}
                 :opts {:renderers  renderers
                        :views      views}}]
    (map->Renderer {:om-config config})))

