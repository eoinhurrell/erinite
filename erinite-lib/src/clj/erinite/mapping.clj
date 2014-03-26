(ns erinite.mapping
  (:require [clojure.core.match :refer [match defpred]]
            [clojure.core.async :refer [chan <! >! go go-loop alts! mult]]
            [clojure.set :refer [intersection difference union]]
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
  "Take a list of mappings, normalise each one and pack it into a data structure
   with the following fields:
      :handlers   list of normalised mappings for each transformation handler
      :inputs     set of keywords representing message types that are only used 
                  as inputs (and not depends)
      :depends    set of keywords representing cells that are used as depends
      :outputs    set of keywords representing cells that are written to by
                  at least one transformation
   
   It is an error to have :depends which are not also :outputs"
  [mappings]
  (let [maps  (map normalise-mapping mappings)
        ; Remove :handler and :op fields before converting format
        cells (merge-maps (map #(dissoc % :handler :opts) maps))
        ; Convert lists to sets
        cells (update-in cells [:output] set)
        cells (update-in cells [:depends] #(set (apply concat %)))
        cells (update-in cells [:inputs] #(difference
                                            (apply union %)
                                            (:output cells)))
        ; Get a set of dependencies that don't exist in the outputs lists too
        unknown-deps (difference (:depends cells)
                                 (:output cells))]
    ; If there are any unknown dependencies, that's an error
    (if (not-empty unknown-deps)
      (throw (Exception.
               (str
                 "Cells marked as dependencies, but do not exist as outputs:\n"
                 (vec unknown-deps))))
      ; Rename :output to :outputs and add :handlers
      (assoc (dissoc cells :output)
             :outputs (:output cells)
             :handlers maps))))

