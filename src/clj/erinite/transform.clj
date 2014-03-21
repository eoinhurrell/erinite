(ns erinite.transform
  (:require [clojure.core.match :refer [match]]
            [clojure.core.async :as async]))


(defn make-cell
  [cell-name]
  (let [state       (atom :_EMPTY_)
        outputs     (atom [])
        transforms  (atom {})]
    (fn [what & args]
      (cond
        (= what :in)
          (let [[topic value] (first args)]
            (doseq [transform (@transforms topic)]
              (let [old-state @state
                    new-state (transform old-state topic value)]
                (when-not (= new-state old-state)
                  (reset! state new-state)
                  (doseq [output @outputs]
                    (output :in [cell-name new-state]))))))
        (= what :state)
          @state
        (= what :out)
          (swap! outputs conj (first args))
        (= what :add-transform) 
          (let [transform (second args)]
          (doseq [input (first args)]
            (swap! transforms update-in [input] conj transform)))
        (= what :name)
          cell-name))))


(defn make-sink
  "Similar API as a cell, but puts the messages on a channel"
  []
  (let [ch (async/chan)
        out-router (async/mult ch)]
    (fn [what & args]
      (cond
        (= what :in)  (async/>!! ch (first args))
                        
        (= what :out) (async/tap out-router (first args))
        (= what :name) :_SINK_))))


(defn make-transform
  [inputs depends output opts func]
  (let [handler-fn  (match opts
                      :const    (fn [_ _ _] (func))
                      :prev     (fn [p _ _] (func p))
                      :value    (fn [_ _ v] (func v))
                      :topic    (fn [_ t _] (func t))
                      :msg      (fn [_ t v] (func t v))
                      :valdeps  (fn [_ _ v]
                                  (let [statics (map #(% :state) depends)]
                                    (apply func v statics))) 
                      :depends  (fn [_ _ _]
                                  (let [statics (map #(% :state) depends)]
                                    (apply func statics))) 
                      :else     (fn [p t v]
                                  (let [statics (map #(% :state) depends)]
                                    (apply func p t v statics))))]
    (output :add-transform inputs handler-fn)
    output))


