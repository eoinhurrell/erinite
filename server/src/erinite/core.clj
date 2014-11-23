(ns erinite.core)


(defprotocol Service
  (id [service])
  (routes [service])
  (api-version [service])
  (call [service resource session]))


(defmulti service-query (fn [r _ _] (:route/name r)))
(defmethod service-query :metrics
  [service query session]
  (let [{:keys [user-id account-id]}  session
        {:keys [id child child-id
                start count order]}   resource]
    (println "User" user-id "accessing metric" id "and" child child-id)
    (println "Taking" count "items, starting from" start "in"
             (get {"asc" "ascending" "desc" "descending"} order "descending")
             "order.")
    {:foo id :value 10}))

(defrecord DataService []
  component/Lifecycle
  (start [service])
  (stop [service])

  Service
  (config [_]
    {:id :data-service
     :api-version [1 2]
     :routes [[:metrics [["metrics" (silk/int :id) :child (silk/int :child-id)]
                         {"start" (silk/? (silk/int :start) {:start 0})
                          "count" (silk/? (silk/int :count) {:count 10})
                          "order" (silk/? :order {:order "asc"})}]]]
     :query service-query         ;(fn [service query session] ...)
     :transform sercive-xform     ;(fn [resource session] ...)
     :validate service-validator  ;(fn [resource session] ...)
     }))


(defn parse-uri
  "Take in a uri and return a map containing API version, service name and
   resource uri.
   Version is either 'v' followed by version number, either an integer or a pair
   of integers seperated by a dot: v12 or v4.2
   
   (parse-uri \"/v1.2/metrics/metrics/3/values?start=12345&count=100&order=asc\")
   => {:version [1 2]
       :service :metrics
       :uri \"/metrics/3/values?start=12345&count=100&order=asc\"}

   (parse-uri \"/v12/metrics/metrics/3/values?start=12345&count=100&order=asc\")
   => {:version 12
       :service :metrics
       :uri \"/metrics/3/values?start=12345&count=100&order=asc\"}"
  [uri]
  (let [service   (re-find #"^/v[0-9]+(?:\.[0-9]+)?/[-_,.\w$]+" uri)
        resource  (subs uri (count service))
        routes    (silk/routes
                    [[:a [[(silk/cat "v" (silk/int :major)
                                     "." (silk/int :minor)) :service]]]
                     [:b [[(silk/cat "v" (silk/int :version)) :service]]]])
        {:keys [version major minor service] :as res} (silk/arrive routes service)]
  {:version (or version [major minor])
   :service (keyword service)
   :uri     resource}))


(def service
  {:query (fn [action uri params session])
   :transform (fn [resource params])
   :validate (fn [action resource params session])
   }

  )


(defn stream-changes
  [base-url key]
  (let [ch (async/chan)]
    (go-loop []
      (let [f (etcd/await base-url key)]
        (>! ch @f)
        (recur)))
    ch))

(defn simple-loop
  [callback & args]
  (let [{in-ch :in out-ch :out} (into {} args)]
    (go-loop []
      (when-let [input (if in-ch (<! in-ch) true)]
        (let [output (if in-ch (callback) (callback input))]
          (when out-ch (>! out-ch output))
          (recur))))
    out-ch))

(let [base-url  "http://localhost:4001"
      changes   (stream-changes listen-to base-url "foo/bar")]
  (simple-loop println :in changes))

(defprotocol Service
  (start [service config])
  (stop [service config])
  (configure [service config reconfigure?]))

(reify
  Service
  (start [service config]
    (events/listen! :)
    )
  )


