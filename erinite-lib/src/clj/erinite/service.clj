(ns erinite.service)

(defn ajax-api [conf cmd params]
  (println "AJAX [" conf "]:" cmd params))

(defn comm [dataflow]
  (let [state (init {})
        check-snapshot (chan)
        pull!  (fn [which] (go (>! check-snapshot [:+CHECK-SNAPSHOT+ which])))
        queued [((dataflow :sub) :LATEST :save)
                ((dataflow) :sub :undo)
                check-snapshot
                ]]
    (go-loop []
      (let [[msg ch] (alts!! queued)
            state @state]
        (match msg
          [:+CHECK-SNAPSHOT+ which]
            (let [msg (<! which)]
              
              )
          [:save [id value]]
            (do
              (ajax-api (:ajax-data state) :save [id value]))
          [:undo nil]
            (do
              (ajax-api (:ajax-data state) :save [])))))))

(comment

  Three types of message handlers:
    QUEUED    - Always run in order, in a loop
    SNAPSHOT  - Latest state, run on-demand when desired
    LATEST    - Like queued, but max queue length is 1

(service comm
  [state]
  (init 
    {})
  (on :save :LATEST [[id value]]
    (ajax-api (:ajax-data state) :save [id value]))
  (on :undo [nil]
    (ajax-api (:ajax-data state) :undo [])))
  



  )

