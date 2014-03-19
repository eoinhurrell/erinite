(ns erinite.transform
  (:require [clojure.core.match :refer [match]]
            [clojure.core.async :refer [chan <! >! go go-loop alts! mult]]))

(defn make-cell
  "Create a new data cell.
   Data sent to :ch will set :state and, if it differs from the previous
   value of :state, forward the new value to :mult

   :mult is a mult which can be tap'd to receive a copy of data sent to it."
  ([] (make-cell nil 10))
  ([cell-name] (make-cell cell-name 10))
  ([cell-name buffer-size]
    (let [; The cells internal state, updated whenev it receives data
          state   (atom nil)
          ; The channel through which the cell receives updates to its state
          input   (chan buffer-size)
          ; If the state changes, the new value is sent out through this channel
          output  (chan)
          ; This is used to "fan out" the changed data to many dependents
          outputm (mult output)]
      (go-loop []
        (when-let [value (<! input)]
          (when-not (= @state value)
            ; The state has changed, update it and propogate the change
            (reset! state value)
            (>! output
                ; If the cell is named, use its name as the message topic
                (if cell-name
                  [cell-name value]
                  value)))
          (recur)))
      ; Return a function to encapsulate access, disallowing direct access to
      ; the state as change is only allowed through the :ch channel.
      (fn [key]
        (match key
          :state  (deref state)
          :ch     input
          :mult   outputm)))))


(defn make-transform
  "Create a new transform handler"
  [static-cells out-cell opts func]
  (let [; The channel through which this transform receives its input messages.
        in-ch       (chan)
        ; The function that is called to transform its input data. The result of
        ; this function call is sent to out-cell.
        ; This function takes the generic [topic value] message form and
        ; converts it to the format expected by func, as described by opts.
        ; This includes reading the current data from dependency cells and the
        ; previous value from out-cell, if required.
        handler-fn  (match opts
                      :const    (fn [_ _] (func))
                      :prev     (fn [_ _] (func (out-cell :state)))
                      :value    (fn [_ v] (func v))
                      :topic    (fn [t _] (func t))
                      :msg      func
                      :valdeps  (fn [_ v]
                                  (let [statics (map #(% :state) static-cells)]
                                    (apply func v statics))) 
                      :depends  (fn [_ _]
                                  (let [statics (map #(% :state) static-cells)]
                                    (apply func statics))) 
                      :else     (fn [t v]
                                  (let [[prev & statics] (map #(% :state)
                                                           (concat
                                                             [out-cell]
                                                             static-cells))]
                                    (apply func prev t v statics))))]
    ; Receive a new message, convert it into the standard [topic value] format,
    ; send the message to handler-fn to be transformed and send the return value
    ; to out-cell
    (go-loop []
      (when-let [message        (<! in-ch)]
        (let [[topic value] (match message
                              [topic]           [topic nil]
                              [topic value]     [topic value]
                              [topic & values]  [topic values]
                              :else
                                (cond
                                  (keyword? message)  [message nil]
                                  (number? message)   [:Number message]
                                  (string? message)   [:Text message]))]
          (>! (out-cell :ch) (handler-fn topic value))
          (recur))))
    in-ch))

