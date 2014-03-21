(ns erinite.dataflow
  (:require [clojure.core.async :as async]
            [clojure.set :refer [intersection]]
            [clojure.core.match :refer [match]]
            [erinite.mapping :refer [merge-maps]]
            [erinite.transform :refer [make-cell make-sink make-transform]]))

(defn make-dataflow
  "Build a dataflow network from a preprocessed configuration mapping."
  [{:keys [inputs depends outputs handlers]}]
  (let [; Create a cell for each output
        cells (into {}
                    (map
                      (fn [cell] [cell (make-cell cell)])
                      outputs))
        ; Create a transform for each handler, store the inputs and channels
        xforms  (map
                  (fn [{:keys [inputs depends output opts handler]}]
                    (let [transform (make-transform
                                      inputs
                                      (map cells depends)
                                      (get cells output)
                                      opts
                                      handler)]
                      (into {} (map (fn [input] [input transform]) inputs))))
                  handlers)
        cell-map      (merge-maps xforms)
        input-router  (async/chan 10)
        sinks         (atom {})]
    ; Connect transform inputs to the cells that they are mapped to
    ; Publish input messages to the correct transforms
    (async/go-loop []
      (let [msg (async/<! input-router)]
        (if (not (nil? msg))
          (do
            (let [[topic value] msg]
              (doseq [cell (cell-map topic)]
                (cell :in msg))
              (recur)))
          (async/close! input-router))))
    ; Return the message router channel and the publisher
    {:in  input-router
     ; Function to subscribe to output topics by routing them to a channel
     :sub (fn this
            ([topic] (this :QUEUED topic))
            ([option topic]
              (let [sink (get @sinks topic)]
                (if sink
                  (let [ch (match option
                              :QUEUED (async/chan 10)
                              :LATEST (async/chan (async/sliding-buffer 1)))]
                    (sink :out ch)
                    ch)
                  (let [new-sink (make-sink)]
                    ((get cells topic) :out new-sink)
                    (swap! sinks assoc topic new-sink)
                    (this option topic))))))}))

