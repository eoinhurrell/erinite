(ns erinite.core-test
  (:require [clojure.test :refer :all]
            [erinite.core :refer :all]
            [erinite.macros :refer :all]))

(deftest list->map-test
  (testing "convert empty list"
    (is (= (list->map [])
           {})))

  (testing "convert list with one instance of each key"
    (is (= (list->map [[:a 1]
                       [:b 2]
                       [:c 3]])
           {:a [[1]]
            :b [[2]]
            :c [[3]]})))

  (testing "convert list with multiple instances of some keys"
    (is (= (list->map [[:a 1]
                       [:b 2]
                       [:c 3]
                       [:a 4]
                       [:b 5]])
           {:a [[1] [4]]
            :b [[2] [5]]
            :c [[3]]}))))

