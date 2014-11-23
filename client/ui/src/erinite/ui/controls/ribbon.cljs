(ns erinite.ui.controls.ribbon
  (:require [om-tools.dom :as dom :include-macros true]))

(defn ribbon
  "Corner ribbon element."
  [label color position & [size]]
  (dom/div
    {:class (str "ribbon-wrapper ribbon-" (name position)
                 (when size (str " " (name size))))}
    (dom/div
      {:class (str "ribbon ribbon-" (name color))}
      label)))

