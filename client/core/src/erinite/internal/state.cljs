(ns erinite.internal.state)

;; Application model state
(def app-model (atom {}))

;; Application model undo history
(def undo-history (atom []))

