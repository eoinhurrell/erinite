(ns erinite.utility
  (:require [om.core :as om :include-macros true]))

(defn root [root-name component state target]
  (om/root component state target))

