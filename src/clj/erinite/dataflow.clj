(ns erinite.dataflow
  (:require [clojure.core.async :refer [chan pub sub tap]]
            [clojure.set :refer [intersection]]
            [erinite.transform :refer [make-cell make-transform]]))

(defn make-dataflow
  "Build a dataflow network from a preprocessed configuration mapping."
  [{:keys [inputs depends outputs handlers]}]
  (let [; Create a cell for each output
        cells (into {}
                    (map
                      (fn [cell] [cell (make-cell cell)])
                      outputs))
        ; Create a transform for each handler, store the inputs and channels
        chs   (map
                (fn [{:keys [inputs depends output opts handler]}]
                  [inputs
                   (make-transform
                    (map cells depends)
                    (get cells output)
                    opts
                    handler)])
                handlers)
        input-router      (chan)
        input-publisher   (pub input-router first)]
    ; Connect transform inputs to the cells that they are mapped to
    ; Publish input messages to the correct transforms
    (doseq [[inputs ch] chs
            input       inputs]
      ; Inputs which are also outputs are cells, not pure messages to be routed
      ; so these are connected to the cell they refer to.
      ; Inputs which are not also outputs are pure messages and must be routed
      ; to the correct transforms
      (if (outputs input) ; True if input is also a transform output
        (tap ((cells input) :out) ch)
        (sub input-publisher input ch)))
    ; Return the message router channel and the publisher
    {:in  input-router
     ; Function to subscribe to output topics by routing them to a channel
     :sub (fn [topic ch]
            (tap ((cells topic) :out) ch))}))

