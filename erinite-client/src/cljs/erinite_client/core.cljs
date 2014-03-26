(ns erinite-client.core
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [erinite.utility :as util]))

(enable-console-print!)


(def app-state (atom {:text "Hello kello you douche"}))

(util/root :my-root
  (fn [app owner]
    (dom/div nil
      (dom/h1 nil (:text app))))
  app-state
  {:target (. js/document (getElementById "app1"))})

(def other-state (atom {:text "Hello doggy"}))


(util/root :another-root
  (fn [app owner]
    (dom/h1 nil (:text app)))
  other-state
  {:target (. js/document (getElementById "app2"))})

(reset! app-state {:text "Hello"})
(reset! other-state {:text "Hi"})


