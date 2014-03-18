(ns erinite.mapping
  (:require [clojure.core.match :refer [match defpred]]
            [clojure.core.async :refer [chan <! >! go go-loop alts! mult]]
            [clojure.set :refer [intersection union]]
            [erinite.transform :as t]))

(defn merge-maps
  "Merge multiple maps, putting their values into sets under each key"
  [maps]
  (reduce
    (fn [merged kv]
      (reduce
        (fn [merged [k v]]
          (update-in merged [k] (fn [old] (conj (if (nil? old) [] old) v))))
        merged
        kv))
    {}
    maps))

(defpred set?)
(defpred keyword?)
(defpred fn?)
(defpred vector?)

(defn normalise-mapping
  "Take in a mapping in the end-user format (ie optional fields) and create a
   normalised map for consumption."
  [mapping]
  (match mapping
    [inputs :guard set?
     output :guard keyword?
     handler :guard fn?]     {:inputs inputs
                              :output output
                              :handler handler}
    [inputs :guard set?
     depends :guard vector?
     output :guard keyword?
     handler :guard fn?]     {:inputs inputs
                              :depends depends
                              :output output
                              :handler handler}
    [inputs :guard set?
     output :guard keyword?
     option :guard keyword?
     handler :guard fn?]     {:inputs inputs
                              :output output
                              :opts option
                              :handler handler}
    [inputs :guard set?
     depends :guard vector?
     output :guard keyword?
     option :guard keyword?
     handler :guard fn?]     {:inputs inputs
                              :depends depends
                              :output output
                              :opts option
                              :handler handler}
    :else (throw
            (Exception.
              (str "Error parsing mapping (invalid format):\n"
                   (vec (map (fn [x] (if (fn? x) 'Fn x)) mapping)))))))


(defn parse-mappings
  ""
  [mappings]
  (let [maps  (map normalise-mapping mappings)
        cells (merge-maps (map #(dissoc % :handler :opts) maps))
        cells (update-in cells [:output] set)
        cells (update-in cells [:inputs] #(apply union %))
        cells (update-in cells [:depends] #(set (apply concat %)))
        ]
    cells))
