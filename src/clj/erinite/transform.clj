(ns erinite.transform
  (:require [clojure.core.match :refer [match]]
            [clojure.core.async :refer [chan <! >! >!! go go-loop alts! mult close!]]))

(defn make-cell
  [cell-name]
  (let [state       (atom :_EMPTY_)
        recv        (chan)
        out         (chan)
        out-mult    (mult out)
        transforms  (atom [])]
    (go-loop []
      (let [msg (<! recv)]
        (if (not (nil? msg))
          (do
            (println "Cell" cell-name "got message" msg)
            (doseq [transform @transforms]
              (let [old-state @state
                    new-state (transform old-state msg)]
                (when-not (= new-state old-state)
                  (println "STATE CHANGED FOR" cell-name old-state "->" new-state)
                  (reset! state new-state)
                  (>! out [cell-name new-state]))))
            (recur))
          (close! out))))
    (fn [what]
      (cond
        (= what :in)          recv
        (= what :state)       @state
        (= what :out)         out-mult
        (= what :transforms)  transforms
        (= what :name)        cell-name))))

(defn make-transform
  [depends output opts func]
  (let [handler-fn  (match opts
                      :const    (fn [_ _] (func))
                      :prev     (fn [p _] (func p))
                      :value    (fn [_ [_ v]] (func v))
                      :topic    (fn [_ [t _]] (func t))
                      :msg      (fn [_ [t v]] (func t v))
                      :valdeps  (fn [_ [_ v]]
                                  (let [statics (map #(% :state) depends)]
                                    (apply func v statics))) 
                      :depends  (fn [_ _]
                                  (let [statics (map #(% :state) depends)]
                                    (apply func statics))) 
                      :else     (fn [p [t v]]
                                  (let [statics (map #(% :state) depends)]
                                    (apply func p t v statics))))]
    (swap! (output :transforms) conj handler-fn)
    (output :in)))


