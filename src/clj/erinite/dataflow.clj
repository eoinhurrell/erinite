(ns erinite.dataflow
  (:require [clojure.core.async :refer [chan]]
            [clojure.set :refer [intersection]]
            [erinite.transform :refer [make-cell make-transform]]))

(defn make-dataflow
  "Build a dataflow network from a preprocessed configuration mapping."
  [{:keys [inputs depends output handlers]}]
  (let [; Create a cell for each output (but only if its also an input or
        ; depend as otherwise, its a message for a service and does not need
        ; the overhead of a cell).
        cells (into {} (map
                         (fn [cell] [cell (make-cell)])
                         (filter
                           #(not-empty (intersection % inputs depends))
                           output)))
        ; Create a transform for each handler, store the inputs and channels
        chs   (map
                (fn [[input depends output opt func]]
                  [input
                   (make-transform
                    (map output depends)
                    (get cells output)
                    opt
                    func)])
                handlers)
        message-router (chan)]
    message-router))

