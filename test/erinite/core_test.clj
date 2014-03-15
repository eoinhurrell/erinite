(ns erinite.core-test
  (:use [erinite.macros]
        [clojure.core.async :only [>!!]])
  (:require [clojure.test :refer :all]
            [erinite.core :refer :all]))

(defn set-value [p v] v)
(defn inc-value [p v] (inc p))


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


(deftest parse-derive-depedns-test
  (testing "only triggers"
    (is (= (parse-derive-depends [[[[:A] [:B] [:C]] [:X] nil]
                                  [[[:A] [:B]] [:X] nil]
                                  [[[:A]] [:X] nil]])
           [[#{[:A] [:B] [:C]} [[:A] [:B] [:C]] [:X] nil]
            [#{[:A] [:B]} [[:A] [:B]] [:X] nil]
            [#{[:A]} [[:A]] [:X] nil]])))

  (testing "only static"
    (is (= (parse-derive-depends [[[:static= [:A] [:B] [:C]] [:X] nil]
                                  [[:static= [:A] [:B]] [:X] nil]
                                  [[:static= [:A]] [:X] nil]])
           [[#{} [[:A] [:B] [:C]] [:X] nil]
            [#{} [[:A] [:B]] [:X] nil]
            [#{} [[:A]] [:X] nil]])))

  (testing "triggers and static"
    (is (= (parse-derive-depends [[[[:A] [:B] :static= [:C] [:D]] [:X] nil]
                                  [[[:A] :static= [:B]] [:X] nil]])
           [[#{[:A] [:B]} [[:A] [:B] [:C] [:D]] [:X] nil]
            [#{[:A]} [[:A] [:B]] [:X] nil]]))))


(deftest state-transform-test
  (testing "state gets left untouched given a message that is not configured"
    (is (= (state-transform
             {:a [1 2] :b :B}
             [:msg 3]
             {:some-msg [[[:a] conj]]})
           [{:a [1 2] :b :B} #{}])))

  (testing "state gets transformed by message when one message is configured"
    (is (= (state-transform
             {:a [1 2] :b :B}
             [:msg 3]
             {:msg [[[:a] conj]]})
           [{:a [1 2 3] :b :B} #{[:a]}])))

  (testing "state gets transformed by message when one message is configured"
    (is (= (state-transform
             {:a [1 2] :b :B}
             [:msg 3]
             {:msg [[[:a] conj]
                    [[:b] (fn [p v] v)]]})
           [{:a [1 2 3] :b 3} #{[:a] [:b]}]))))

(deftest state-derive-test
  (testing "state gets left untouched when no changes are reported"
    (is (= (state-derive
             {:A 10 :B 20}
             #{}
             [[#{[:A] [:B]} [[:A] [:B]] [:Result] (fn [p a b] (+ a b))]])
           {:A 10 :B 20})))
  
  (testing "state gets updated when a change is reported"
    (is (= (state-derive
             {:A 10 :B 20}
             #{[:A]}
             [[#{[:A] [:B]} [[:A] [:B]] [:Result] (fn [p a b] (+ a b))]])
           {:A 10 :B 20 :Result 30})))
  
  (testing "state gets updated when multiple changes are reported"
    (is (= (state-derive
             {:A 10 :B 20}
             #{[:B]}
             [[#{[:A] [:B]} [[:A] [:B]] [:Result] (fn [p a b] (+ a b))]
              [#{[:B]} [[:B]] [:Second] (fn [p b] (inc b))]])
           {:A 10 :B 20 :Result 30 :Second 21})))
  
  (testing "derives update state when relying on non-triggering dependencies"
    (is (= (state-derive
             {:A 0 :B 10 :C 20}
             #{[:A]}
             [[#{[:A]} [[:A] [:B] [:C]] [:Result] (fn [p _ b c] (+ b c))]])
           {:A 0 :B 10 :C 20 :Result 30}))))


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


(deftest run-app-derive
  (testing "running an app with a single transform and a single derive"
    (configure test-app
               :transform [[:test   [:Test] set-value]]
               :derive    [[[[:Test]] [:Result] (fn [p a] (* 2 a))]])
    (>!! (:channel test-app) [:test 5])
    (Thread/sleep 10) ; Give the go blocks time to process the message
    (is (= (deref (:state test-app))
           {:Test   5
            :Result 10})))

  (testing "running an app with multiple transforms and derives"
    (configure test-app
               :transform [[:init [:A] (fn [p v] (:A v))]
                           [:init [:B] (fn [p v] (:B v))]
                           [:set-a [:A] set-value]
                           [:set-b [:B] set-value]]
               :derive    [[[[:A] [:B]] [:Result] (fn [p a b] (+ a b))]])
    ; First test that initialising the state correctly updates the derive
    (>!! (:channel test-app) [:init {:A 1 :B 2}])
    (Thread/sleep 10) ; Give the go blocks time to process the message
    (is (= (deref (:state test-app))
           {:A 1
            :B 2
            :Result 3}))
    ; Next test that updating part of the state will update the derive
    (>!! (:channel test-app) [:set-a 10])
    (Thread/sleep 10) ; Give the go blocks time to process the message
    (is (= (deref (:state test-app))
           {:A 10
            :B 2
            :Result 12}))
    ; Finally, test that updating the other part of the state will update the
    ; derive as expected
    (>!! (:channel test-app) [:set-b 20])
    (Thread/sleep 10) ; Give the go blocks time to process the message
    (is (= (deref (:state test-app))
           {:A 10
            :B 20
            :Result 30})))
  
  (testing "running an app with multiple transforms, derives and messages"
    (configure test-app
               :transform [[:init [:Inputs] (fn [_ _] {:A 0 :B 0 :C 0})]
                           [:set  [:Inputs] (fn [p {:keys [a b c]}]
                                               (assoc p
                                                      :A (or a (:A p))
                                                      :B (or b (:B p))
                                                      :C (or c (:C p))))]]
               :derive    [[[[:Inputs] :static= [:Inputs :A] [:Inputs :B] [:Inputs :C]]
                            [:Sum]
                            (fn [p _ a b c] (+ a b c))]])
    (>!! (:channel test-app) [:init nil])
    (Thread/sleep 10) ; Give the go blocks time to process the message
    (is  (= (:Sum (deref (:state test-app))) 0))

    (>!! (:channel test-app) [:set {:a 5}])
    (>!! (:channel test-app) [:set {:b 2}])
    (Thread/sleep 10) ; Give the go blocks time to process the message
    (is  (= (:Sum (deref (:state test-app))) 7))
    
    (>!! (:channel test-app) [:set {:c 3}])
    (>!! (:channel test-app) [:set {:a 9}])
    (>!! (:channel test-app) [:set {:a 4}])
    (>!! (:channel test-app) [:set {:b 1}])
    (Thread/sleep 10) ; Give the go blocks time to process the message
    (is  (= (:Sum (deref (:state test-app))) 8))))

