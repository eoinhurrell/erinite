(ns erinite.transform-test
  (:require [clojure.test :refer :all]
            [clojure.core.async :refer [go >!! tap chan alt!! timeout close!]]
            [erinite.transform :refer :all]))

(deftest test-make-cell
  (testing "create a cell"
    (let [cell  (make-cell)]
      ; Check that the cell state is empty
      (is (= (cell :state) nil))))

  (testing "connect a channel to a cell"
    (let [cell  (make-cell)
          ch    (chan)]
      ; Tap the mult and test that it receives data sent to :ch
      ; and that the cell state is set to contain the data
      (tap (cell :mult) ch)
      (>!! (cell :ch) "Test")
      (let [result (alt!! [ch (timeout 25)] ([v _] v))]
        (is (= result "Test"))
        (is (= (cell :state) "Test")))))

  (testing "changing a cell will set its state to the latest change"
    (let [cell  (make-cell)
          ch    (chan)]
      (tap (cell :mult) ch)
      (>!! (cell :ch) "Test1")
      (>!! (cell :ch) "Test2")
      (let [result1 (alt!! [ch (timeout 25)] ([v _] v))
            result2 (alt!! [ch (timeout 25)] ([v _] v))]
        ; Get the first change
        (is (= result1 "Test1"))
        ; Get the second change
        (is (= result2 "Test2"))
        ; View the current state - which should be the second change
        (is (= (cell :state) "Test2")))))

  (comment Need to figure out a way of testing this properly...
           Currently no guarantee it properly works as test deadlocks...
  (testing "cells can have custom buffer sizes"
    (let [cell  (make-cell 3)
          ch    (chan)]
      (tap (cell :mult) ch)
      (go (println "Not timed out yet")
          (<! (timeout 25))
          (println "Closing")
          (close! (cell :ch)))
      (>!! (cell :ch) "a")
      (>!! (cell :ch) "b")
      (>!! (cell :ch) "c")
      (>!! (cell :ch) "d")
      (>!! (cell :ch) "e")
      (>!! (cell :ch) "f")
      (let [results (map (fn [_] (alt!! [ch (timeout 25)] ([v _] v))) (range 3))]
        ; Get the changes
        (is (= results ["a" "b" "c" nil nil nil]))
        ; View the current state - which should be the second change
        (is (= (cell :state) "c"))))))

  (testing "cells do not propogate values if their state is unchanged"
    (let [cell  (make-cell)
          ch    (chan)]
      (tap (cell :mult) ch)
      ; Set the initial value
      (>!! (cell :ch) "Test")
      (let [result (alt!! [ch (timeout 25)] ([v _] v))]
        (is (= result "Test"))
        (is (= (cell :state) "Test")))
      ; Now set it again, this time the channel should timeout
      (>!! (cell :ch) "Test")
      (let [result (alt!! [ch (timeout 25)] ([v _] v))]
        (is (= result nil))
        (is (= (cell :state) "Test"))))))


(deftest test-make-transform
  (testing "create a transform that outputs a constant value"
    (let [cell  (make-cell)
          ch    (make-transform nil cell :const (fn [] 5))]
      (>!! ch :Ignored)
      (Thread/sleep 10)
      (is (= (cell :state) 5))
      (>!! ch 10)
      (Thread/sleep 10)
      (is (= (cell :state) 5))))

  (testing "create a transform that takes in only the previous value"
    (let [cell  (make-cell)
          ch    (make-transform nil cell :prev (fn [p] (if p (inc p) 0)))]
      (>!! ch :Ignored)
      (Thread/sleep 10)
      (is (= (cell :state) 0))
      (>!! ch :Ignored)
      (Thread/sleep 10)
      (is (= (cell :state) 1))))
  
  (testing "create transform that takes only the new value"
    (let [cell  (make-cell)
          ch    (make-transform nil cell :value inc)]
      (>!! ch [:Ignored 1])
      (Thread/sleep 10)
      (is (= (cell :state) 2))
      (>!! ch [:Ignored 3])
      (Thread/sleep 10)
      (is (= (cell :state) 4))))

  (testing "create transform that takes only the topic"
    (let [cell  (make-cell)
          ch    (make-transform nil cell :topic identity)]
      (>!! ch [:TopicA 1])
      (Thread/sleep 10)
      (is (= (cell :state) :TopicA))
      (>!! ch [:TopicB 3])
      (Thread/sleep 10)
      (is (= (cell :state) :TopicB))))

  (testing "create transform that takes the message topic and value"
    (let [cell  (make-cell)
          ch    (make-transform nil cell :msg (fn [t v] (str t v)))]
      (>!! ch [:TopicA 1])
      (Thread/sleep 10)
      (is (= (cell :state) ":TopicA1"))
      (>!! ch [:TopicB 3])
      (Thread/sleep 10)
      (is (= (cell :state) ":TopicB3"))))

  (testing "create transform that takes the previous value, message topic and current value"
    (let [cell  (make-cell)
          ch    (make-transform nil cell nil (fn [p t v] (str p t v)))]
      (>!! ch [:TopicA 1])
      (Thread/sleep 10)
      (is (= (cell :state) ":TopicA1"))
      (>!! ch [:TopicB 3])
      (Thread/sleep 10)
      (is (= (cell :state) ":TopicA1:TopicB3"))))

  (testing "create transform that has value and static dependencies"
    (let [cell  (make-cell)
          s1    (make-cell)
          s2    (make-cell)
          ch    (make-transform [s1 s2] cell :valdeps (fn [v a b] (str v a b)))]
      (>!! (s1 :ch) "A")
      (>!! (s2 :ch) "B")
      ; Give some time for the cells to take their values
      (Thread/sleep 10) 
      (>!! ch "X")
      (Thread/sleep 10)
      (is (= (cell :state) "XAB"))
      (>!! (s2 :ch) "C")
      (Thread/sleep 10)
      (is (= (cell :state) "XAB"))
      (>!! ch "Y")
      (Thread/sleep 10)
      (is (= (cell :state) "YAC"))))

  (testing "create transform that has only static dependencies"
    (let [cell  (make-cell)
          s1    (make-cell)
          s2    (make-cell)
          ch    (make-transform [s1 s2] cell :depends (fn [a b] (str a b)))]
      (>!! (s1 :ch) "A")
      (>!! (s2 :ch) "B")
      ; Give some time for the cells to take their values
      (Thread/sleep 10) 
      (>!! ch "X")
      (Thread/sleep 10)
      (is (= (cell :state) "AB"))
      (>!! (s2 :ch) "C")
      (Thread/sleep 10)
      (is (= (cell :state) "AB"))
      (>!! ch "Y")
      (Thread/sleep 10)
      (is (= (cell :state) "AC"))))

  (testing "create transform that takes prev, topic, value and static dependencies"
    (let [cell  (make-cell)
          s1    (make-cell)
          s2    (make-cell)
          ch    (make-transform [s1 s2] cell nil
                                (fn [p t v a b] (str p t v a b)))]
      (>!! (s1 :ch) "A")
      (>!! (s2 :ch) "B")
      ; Give some time for the cells to take their values
      (Thread/sleep 10)
      (>!! ch "X")
      (Thread/sleep 10)
      (is (= (cell :state) ":TextXAB"))
      (>!! (s2 :ch) "C")
      (Thread/sleep 10)
      (is (= (cell :state) ":TextXAB"))
      (>!! ch "Y")
      (Thread/sleep 10)
      (is (= (cell :state) ":TextXAB:TextYAC"))))

  (testing "passing all the message variants to a transform"
    (let [cell  (make-cell)
          ch    (make-transform nil cell :msg (fn [t v] [t v]))]
      (>!! ch [:Topic])
      (Thread/sleep 10)
      (is (= (cell :state) [:Topic nil]))

      (>!! ch [:A :B])
      (Thread/sleep 10)
      (is (= (cell :state) [:A :B]))

      (>!! ch [:Topic :A :B])
      (Thread/sleep 10)
      (is (= (cell :state) [:Topic [:A :B]]))

      (>!! ch :Topic)
      (Thread/sleep 10)
      (is (= (cell :state) [:Topic nil])) 

      (>!! ch 123)
      (Thread/sleep 10)
      (is (= (cell :state) [:Number 123])) 

      (>!! ch "Hello")
      (Thread/sleep 10)
      (is (= (cell :state) [:Text "Hello"])))))


(deftest test-networks-of-transforms
  (testing "a simple chain of transforms"
    (let [cell1 (make-cell)
          cell2 (make-cell)
          ch1   (make-transform nil cell1 :value inc)
          ch2   (make-transform nil cell2 :value #(* 2 %))]
    ; Route cell1 to ch2
    (tap (cell1 :mult) ch2)
    ; Put some values on ch1
    (>!! ch1 2)
    (Thread/sleep 10)
    ; (* 2 (inc 2))
    (is (= (cell2 :state) 6))
    (>!! ch1 5)
    (Thread/sleep 10)
    ; (* 2 (inc 5))
    (is (= (cell2 :state) 12))
    (>!! ch1 8)
    (Thread/sleep 10)
    ; (* 2 (inc 8))
    (is (= (cell2 :state) 18))))
  
  (testing "two transforms feeding into one"
    (let [cell1 (make-cell)
          cell2 (make-cell)
          cell3 (make-cell)
          ch1   (make-transform nil cell1 :value inc)
          ch2   (make-transform nil cell2 :value dec)
          ch3   (make-transform nil cell3 :value #(* 2 %))]
    ; Route cell1 to ch3 and cell2 to ch3
    (tap (cell1 :mult) ch3)
    (tap (cell2 :mult) ch3)
    ; Put some values on ch1
    (>!! ch1 2)
    (Thread/sleep 10)
    ; (* 2 (inc 2))
    (is (= (cell3 :state) 6))
    (>!! ch2 5)
    (Thread/sleep 10)
    ; (* 2 (dec 5))
    (is (= (cell3 :state) 8))
    (>!! ch1 8)
    (Thread/sleep 10)
    ; (* 2 (inc 8))
    (is (= (cell3 :state) 18))))

  (testing "two transforms feeding into one, but also being depended on statically"
    (let [cell1 (make-cell)
          cell2 (make-cell)
          cell3 (make-cell)
          ch1   (make-transform nil cell1 :value inc)
          ch2   (make-transform nil cell2 :value dec)
          ch3   (make-transform [cell1 cell2] cell3 :depends (fnil + 0 0))]
    ; Route cell1 to ch3 and cell2 to ch3
    (tap (cell1 :mult) ch3)
    (tap (cell2 :mult) ch3)
    ; Put some values on ch1
    (>!! ch1 2)
    (Thread/sleep 10)
    (is (= (cell3 :state) 3))
    (>!! ch2 5)
    (Thread/sleep 10)
    (is (= (cell3 :state) 7))
    (>!! ch1 8)
    (Thread/sleep 10)
    (is (= (cell3 :state) 13))))

  (testing "init transforms to initialise cells"
    (let [cell1 (make-cell)
          cell2 (make-cell)
          cell3 (make-cell)
          ch1   (make-transform nil cell1 :value inc)
          ch2   (make-transform nil cell2 :value dec)
          ch3   (make-transform [cell1 cell2] cell3 :depends (fnil + 0 0))]
    ; Route cell1 to ch3 and cell2 to ch3
    (tap (cell1 :mult) ch3)
    (tap (cell2 :mult) ch3)
    ; Put some values on ch1
    (>!! ch1 2)
    (Thread/sleep 10)
    (is (= (cell3 :state) 3))
    (>!! ch2 5)
    (Thread/sleep 10)
    (is (= (cell3 :state) 7))
    (>!! ch1 8)
    (Thread/sleep 10)
    (is (= (cell3 :state) 13))))
  )
