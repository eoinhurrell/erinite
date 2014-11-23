(ns erinite.core
  "Erinite Client entry point.

   To create an Erintie Client application, call (app-main ...) from your client
   applications core.cljs"
  (:require [quile.component :as component]
            [shodan.console :as console :include-macros true]
            [erinite.internal.template-transform]
            [erinite.lib.events :as events]
            [erinite.services.navigation :refer [navigation-srv]]  
            [erinite.services.renderer :refer [renderer-srv]]))


(defn ^:export sendEvent
  "Function exported so that javascript code can send Erinite events."
  [event-name & event-data]
  (apply events/send!
         (keyword event-name)
         (js->clj event-data)))


(defn ^:private gen-system-map
  "Take in a spec and a config map and return a component system map.
   spec:  {:component-name component-ctor
           :component-name [component-ctor :dependency-name]}
   component constructors should take a single argument: the config map."
  [spec config]
  (component/map->SystemMap
    (into {}
      (map
        (fn [[k v]]
          (vector k
            (if (fn? v)
              (v config)
              (component/using
                ((first v) config)
                (vec (rest v))))))
        spec))))


(defn app-main
  "Entry point to an erinite application client"
  [& args]
  (let [;; Initialisation data
        system            (atom nil)
        builtin-attrs     {:shared-data- {:system system}}
        ;; Get attributes and event map from arguments
        attrs             (apply hash-map (butlast args))
        attrs             (merge builtin-attrs attrs)
        ;; Create system from service components
        builtin-services  {:navigation  navigation-srv 
                           :renderer    [renderer-srv :navigation]}
        system-spec       (merge (or (:services attrs) {}) builtin-services)
        system-map        (gen-system-map system-spec attrs)
        ;; Get mandatory attributes
        {:keys [target renderers renderer views start-page]} attrs]
    ;; Make sure mandatory attributes are set
    (assert (not (some nil? [target renderers renderer views start-page]))
            "Mandatory attributes must not be nil")
    ;; Connect all event handlers
    (events/listen! (last args))
    ;; Start up the systems
    (reset! system (component/start system-map))
    ;; Kick off the application
    (events/send! :Erinite/init)
    (events/send! :Erinite/start)))

