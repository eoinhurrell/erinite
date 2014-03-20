(ns erinite.dataflow-test
  (:require [clojure.test :refer :all]
            [clojure.core.match :refer [match]]
            [clojure.core.async :refer [go >!! <!! tap chan alts!! timeout close!]]
            [erinite.dataflow :refer :all]))

(def config {:inputs  #{:init :prev :next :toggle :create :tick }
             :depends #{:alarms :selection }
             :outputs #{:alarms :selection :ticks :notices}
             :handlers [{:inputs #{:init}
                         :output :alarms
                         :opts :const
                         :handler identity}
                        {:inputs #{:init}
                         :output :selection
                         :opts :const
                         :handler identity}
                        {:inputs #{:prev :next}
                         :depends [:alarms]
                         :output :selection
                         :handler identity}
                        {:inputs #{:toggle}
                         :depends [:selection]
                         :output :alarms
                         :handler identity}
                        {:inputs #{:create}
                         :output :alarms
                         :handler identity}
                        {:inputs #{:tick}
                         :output :ticks
                         :opts :prev
                         :handler identity}
                        {:inputs #{:ticks}
                         :depends [:alarms]
                         :output :notices
                         :handler identity}]})

(def conf2  {:inputs  #{:init :test}
             :depends #{:val}
             :outputs #{:val :out}
             :handlers [{:inputs #{:init}
                         :output :val
                         :opts :value
                         :handler identity}
                        {:inputs #{:init}
                         :output :out
                         :opts :value
                         :handler identity}
                        {:inputs #{:test}
                         :depends [:val]
                         :output :out
                         :handler (fn [p t v d]
                                    (println "IN H" v d)
                                    (+ v d))}]})

(deftest test-make-dataflow
  (testing "create a dataflow network"
    (let [router (make-dataflow conf2)
          ch     (chan)]
      ((:sub router) :out ch)
      (>!! (:in router) [:init 5])
      (>!! (:in router) [:test])
      (Thread/sleep 500)
      (let [[[topic value] c] (alts!! [ch (timeout 50)])]
        (is (= topic :out))
        (is (= value 10))))))
