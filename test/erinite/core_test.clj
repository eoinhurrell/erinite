(ns erinite.core-test
  (:use [erinite.macros]
        [clojure.core.async :only [>!!]])
  (:require [clojure.test :refer :all]
            [erinite.core :refer :all]))

(defn set-value [p v] v)
(defn inc-value [p v] (inc p))

(deftest configure-app-test
  (testing "creation of an app"
    (let [app (configure-app "test-app" {})]
      (is (and (:state app)
               (:channel app)
               (= (:name app)
                  "test-app")))))

  (testing "creation of an app using the macro"
      (configure test-app)
      (is (and (:state test-app)
               (:channel test-app)
               (= (:name test-app)
                  "test-app")))))

(deftest run-app-transform
  (testing "running an app with a single transform and a simple path"
    (configure test-app
               :transform [[:test [:Test] set-value]])
    (>!! (:channel test-app) [:test "test"])
    (Thread/sleep 10) ; Give the go blocks time to process the message
    (is (= (deref (:state test-app))
           {:Test "test"})))
  
  (testing "running an app with a single transform and a nested path"
    (configure test-app
               :transform [[:test [:Test :Path] set-value]])
    (>!! (:channel test-app) [:test "test"])
    (Thread/sleep 10) ; Give the go blocks time to process the message
    (is (= (deref (:state test-app))
           {:Test {:Path "test"}})))

  (testing "running an app with multiple transforms with a simple path"
    (configure test-app
               :transform [[:test1 [:Test] set-value]
                           [:test2 [:Test] inc-value]])
    ; First test setting the value
    (>!! (:channel test-app) [:test1 1])
    (Thread/sleep 10) ; Give the go blocks time to process the message
    (is (= (deref (:state test-app))
           {:Test 1})))
    ; Then test updating it
    (>!! (:channel test-app) [:test2 nil])
    (Thread/sleep 10) ; Give the go blocks time to process the message
    (is (= (deref (:state test-app))
           {:Test 2})))  


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


(deftest state-transform-test
  (testing "state gets left untouched given a message that is not configured"
    (is (= (state-transform
             {:a [1 2] :b :B}
             [:msg 3]
             {:some-msg [[[:a] conj]]})
           {:a [1 2] :b :B})))

  (testing "state gets transformed by message when one message is configured"
    (is (= (state-transform
             {:a [1 2] :b :B}
             [:msg 3]
             {:msg [[[:a] conj]]})
           {:a [1 2 3] :b :B})))

  (testing "state gets transformed by message when one message is configured"
    (is (= (state-transform
             {:a [1 2] :b :B}
             [:msg 3]
             {:msg [[[:a] conj]
                    [[:b] (fn [p v] v)]]})
           {:a [1 2 3] :b 3}))))

