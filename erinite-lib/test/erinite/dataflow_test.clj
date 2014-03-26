(ns erinite.dataflow-test
  (:require [clojure.test :refer :all]
            [clojure.core.match :refer [match]]
            [clojure.core.async :refer [go >!! <!! tap chan alts!! timeout close!]]
            [erinite.dataflow :refer :all]))

(def config  {:inputs  #{:init :test}
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
                                     (+ p v d))}]})

(def config  {:inputs  #{:init :test}
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
                                     (+ p v d))}]})

(deftest test-make-dataflow
  (testing "create a dataflow network"
    (let [router (make-dataflow config)
          ch     ((:sub router) :LATEST :out)]
      (>!! (:in router) [:init 5])
      (>!! (:in router) [:test 10])
      (>!! (:in router) [:test 99])
      (Thread/sleep 10)
      (let [[[topic value] c] (alts!! [ch (timeout 50)])]
        (is (= topic :out))
        (is (= value 124)))))
  
  (testing "create a dataflow network"
    (let [router (make-dataflow config)
          ch     ((:sub router) :QUEUED :out)]
      (>!! (:in router) [:init 5])
      (>!! (:in router) [:test 10])
      (>!! (:in router) [:test 99])
      (Thread/sleep 10)
      (doseq [expected-value [5 20 124]]
        (let [[[topic value] c] (alts!! [ch (timeout 50)])]
          (is (= topic :out))
          (is (= value expected-value)))))))

