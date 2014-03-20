(ns catalyst-web.core.core
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

(enable-console-print!)

(def app-state (atom {:text "Hello world!"}))

(om/root
  (fn [app owner]
    (dom/div nil
      (dom/div nil "Hello")
      (dom/div nil (:text app))))
  app-state
  {:target (. js/document (getElementById "app"))})

(reset! app-state {:text "Bawww"})

(defmacro configure [name & args]
  `(def ~name (atom)))

(configure
  catalyst-app-main
  {:messages []})
