(ns erinite.core
  (:use [clojure.core.async :only [chan <! >! go go-loop]]))


(defn -route-messages
  "Route messages from the message channel to one or more target channels"
  [message-ch targets]
  (go-loop []
    (when-let [message (<! message-ch)]
      ; For each potential target, check if the message matches the
      ; configuration and if so, send the message to that targets channel.
      (let [topic (first message)]
        (doseq [[ch conf] targets] 
          (when (contains? conf topic)
            (>! ch message))))
      (recur))))


(defn state-transform
  "Update the state by applying the message"
  [state message config]
  (let [[topic params] message]
    (reduce
      ; For each path and update-fn pair, apply the update-fn to state
      (fn [[state changes] [path update-fn]]
        [(update-in state path update-fn params)
         (conj changes path)])
      [state #{}]
      (get config topic []))))


(defn -dispatch-transforms
  "Dispatch messages to transform functions"
  [ch config-transform config-derive state]
  (go-loop []
    (when-let [message (<! ch)]
      (swap! state
             (fn [s]
               (let [[new-state derives] (state-transform
                                           s
                                           message
                                           config-transform)]
                 new-state)))
      (recur))))


(defn list->map
  "Takes a list of configuration data and transforms it into a map so that
   checking if messages match the configured mapping can be done through a
   map lookup (get or contains?)."
  [conf-list]
  (->> conf-list
       (map (fn [[topic & other]] [topic other]))
       (reduce
         (fn [accum [key value]]
           (assoc accum
                  key
                  (conj
                    (get accum key [])
                    value)))
         {})))


(defn configure-app
  "Create, configure and start an application"
  [name {:keys [transform derive service]}]
  (let [message-ch      (chan)
        transforms-ch   (chan)
        service-ch      (chan)
        state           (atom {})
        transforms-map  (list->map transform)
        services-map    (list->map service)
        derives-conf    (map (fn [[deps path func]]
                               [(set deps) deps path func])
                             derive)]
    (-route-messages message-ch [[transforms-ch transforms-map]
                                 [service-ch    services-map]])
    (-dispatch-transforms transforms-ch
                          transforms-map
                          derives-conf
                          state)
    {:state state
     :channel message-ch
     :name name}))


