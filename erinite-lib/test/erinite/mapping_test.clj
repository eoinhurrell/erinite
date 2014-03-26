(ns erinite.mapping-test
  (:require [clojure.test :refer :all]
            [clojure.core.match :refer [match]]
            [clojure.core.async :refer [go >!! tap chan alt!! timeout close!]]
            [erinite.mapping :refer :all]))

(deftest test-merge-maps
  (testing "merging a single map"
    (is (= (merge-maps [{:a 1}])
           {:a [1]})))

  (testing "merging two one-element maps"
    (is (= (merge-maps [{:a 1} {:a 2}])
           {:a [1 2]})))

  (testing "merging three two element maps"
    (is (= (merge-maps [{:a 1 :b 2}
                        {:a 2 :b 4}
                        {:a 7 :c 9}])
           {:a [1 2 7]
            :b [2 4]
            :c [9]}))))


(deftest test-normalise-mapping
  (testing "simple mapping containing only mandatory fields"
    (let [mapping (normalise-mapping [#{:topic} :output (fn [& args] args)])]
      (is (= (:depends mapping) nil))
      (is (= (:output  mapping) :output))
      (is (= (:inputs  mapping) #{:topic}))
      (is (= (:opts mapping) nil))
      (is (fn? (:handler mapping)))))

  (testing "mapping containing all depends optional field"
    (let [mapping (normalise-mapping [#{:topic} [:depend] :output (fn [& args] args)])]
      (is (= (:depends mapping) [:depend]))
      (is (= (:output  mapping) :output))
      (is (= (:inputs  mapping) #{:topic}))
      (is (= (:opts mapping) nil))
      (is (fn? (:handler mapping)))))

  (testing "mapping containing option optional field"
    (let [mapping (normalise-mapping [#{:topic} :output :opt (fn [& args] args)])]
      (is (= (:depends mapping) nil))
      (is (= (:output  mapping) :output))
      (is (= (:inputs  mapping) #{:topic}))
      (is (= (:opts mapping) :opt))
      (is (fn? (:handler mapping)))))

  (testing "mapping containing all optional fields"
    (let [mapping (normalise-mapping [#{:topic} [:depend] :output :opt (fn [& args] args)])]
      (is (= (:depends mapping) [:depend]))
      (is (= (:output  mapping) :output))
      (is (= (:inputs  mapping) #{:topic}))
      (is (= (:opts mapping) :opt))
      (is (fn? (:handler mapping))))))

(deftest test-parse-mappings
  (testing "parse list of mappings"
    (is (= (parse-mappings
              [[#{:init}                    :alarms     :const  identity]
               [#{:init}                    :selection  :const  identity]
               [#{:prev :next} [:alarms]    :selection          identity]
               [#{:toggle}     [:selection] :alarms             identity]
               [#{:create}                  :alarms             identity]
               [#{:tick}                    :ticks      :prev   identity]
               [#{:ticks}      [:alarms]    :notices            identity]])
           {:inputs  #{:init :prev :next :toggle :create :tick }
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
                        :handler identity}]}))))


