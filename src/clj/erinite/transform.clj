(ns erinite.transform
  (:require [clojure.core.match :refer [match]]
            [clojure.core.async :refer [chan <! >! go go-loop alts! mult]]
            [clojure.set :refer [intersection]]))

(defn make-cell
  "Create a new data cell.
   Data sent to :ch will set :state and, if it differs from the previous
   value of :state, forward the new value to :mult

   :mult is a mult which can be tap'd to receive a copy of data sent to it."
  ([] (make-cell 10))
  ([buffer-size]
    (let [state   (atom nil)
          input   (chan buffer-size)
          output  (chan)
          outputm (mult output)]
      (go-loop []
        (when-let [value (<! input)]
          (when-not (= @state (reset! state value))
            (>! output value))
          (recur)))
      (fn [key]
        (match key
          :state  (deref state)
          :ch     input
          :mult   outputm)))))


(defn make-transform
  "Create a new transform handler"
  [static-cells out-cell opts func]
  (let [in-ch       (chan)
        handler-fn  (match opts
                      :const    (fn [_ _] (func))
                      :prev     (fn [_ _] (func (out-cell :state)))
                      :value    (fn [_ v] (func v))
                      :topic    (fn [t _] (func t))
                      :msg      (fn [t v] (func t v))
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

