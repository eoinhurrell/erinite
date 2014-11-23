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
            (fn [_ state]
              (om/set-state! owner :current-page (assoc state :view-components views)))
         :Renderer/set
            (fn [new-renderer]
              (om/set-state! owner :renderer new-renderer))}))

    om/IRenderState 
    (render-state [_ {:keys [current-page renderer]}]
      ((get renderers renderer invalid-renderer) current-page data))))

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
                 :init-state {:renderer renderer}
                 :opts {:renderers  renderers
                        :views      views}}]
    (map->Renderer {:om-config config})))

