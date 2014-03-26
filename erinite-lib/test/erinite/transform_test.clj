(ns erinite.transform-test
  (:require [clojure.test :refer :all]
            [erinite.transform :refer :all]))

(deftest test-make-cell
  (testing "create a cell"
    (let [cell  (make-cell)]
      ; ineck that the cell state is empty
      (is (= (cell :state) nil))))

  (testing "sending data to to a cell that has no transforms is a no-op"
    (let [cell  (make-cell)]
      (cell :in "Test")
      (is (= (cell :state) nil)))))


(deftest test-make-transform
  (testing "create a transform that outputs a constant value"
    (let [cell  (make-cell)
          _     (make-transform #{:Ignored} nil cell :const (fn [] 5))]
      (cell :in [:Ignored nil])
      (is (= (cell :state) 5))
      (cell :in [:Ignored 10])
      (is (= (cell :state) 5))))

  (testing "create a transform that takes in only the previous value"
    (let [cell  (make-cell)
          _     (make-transform #{:Ignored} nil cell :prev (fn [p] (if p (inc p) 0)))]
      (cell :in [:Ignored nil])
      (is (= (cell :state) 0))
      (cell :in [:Ignored nil])
      (is (= (cell :state) 1))))
  
  (testing "create transform that takes only the new value"
    (let [cell  (make-cell)
          _     (make-transform #{:Ignored} nil cell :value inc)]
      (cell :in [:Ignored 1])
      (is (= (cell :state) 2))
      (cell :in [:Ignored 3])
      (is (= (cell :state) 4))))

  (testing "create transform that takes only the topic"
    (let [cell  (make-cell)
          _     (make-transform #{:TopicA :TopicB} nil cell :topic identity)]
      (cell :in [:TopicA 1])
      (is (= (cell :state) :TopicA))
      (cell :in [:TopicB 3])
      (is (= (cell :state) :TopicB))))

  (testing "create transform that takes the message topic and value"
    (let [cell  (make-cell)
          _     (make-transform #{:TopicA :TopicB} nil cell :msg (fn [t v] (str t v)))]
      (cell :in [:TopicA 1])
      (is (= (cell :state) ":TopicA1"))
      (cell :in [:TopicB 3])
      (is (= (cell :state) ":TopicB3"))))

  (testing "create transform that takes the previous value, message topic and current value"
    (let [cell  (make-cell)
          _     (make-transform #{:TopicA :TopicB} nil cell nil (fn [p t v] (str p t v)))]
      (cell :in [:TopicA 1])
      (is (= (cell :state) ":TopicA1"))
      (cell :in [:TopicB 3])
      (is (= (cell :state) ":TopicA1:TopicB3"))))

  (testing "create transform that has value and static dependencies"
    (let [cell  (make-cell)
          s1    (make-cell)
          s2    (make-cell)
          _     (make-transform #{:Init} nil s1 :value identity) 
          _     (make-transform #{:Init} nil s2 :value identity) 
          _     (make-transform #{:Text} [s1 s2] cell :valdeps (fn [v a b] (str v a b)))]
      (s1 :in [:Init "A"])
      (s2 :in [:Init "B"])
      ; Give some time for the cells to take their values
      (cell :in [:Text "X"])
      (is (= (cell :state) "XAB"))
      (s2 :in [:Init "C"])
      (is (= (cell :state) "XAB"))
      (cell :in [:Text "Y"])
      (is (= (cell :state) "YAC"))))

  (testing "create transform that has only static dependencies"
    (let [cell  (make-cell)
          s1    (make-cell)
          s2    (make-cell)
          _     (make-transform #{:Init} nil s1 :value identity) 
          _     (make-transform #{:Init} nil s2 :value identity) 
          _     (make-transform #{:Text} [s1 s2] cell :depends (fn [a b] (str a b)))]
      (s1 :in [:Init "A"])
      (s2 :in [:Init "B"])
      ; Give some time for the cells to take their values
      (cell :in [:Text "X"])
      (is (= (cell :state) "AB"))
      (s2 :in [:Init "C"])
      (is (= (cell :state) "AB"))
      (cell :in [:Text "Y"])
      (is (= (cell :state) "AC"))))

  (testing "create transform that takes prev, topic, value and static dependencies"
    (let [cell  (make-cell)
          s1    (make-cell)
          s2    (make-cell)
          _     (make-transform #{:Init} nil s1 :value identity) 
          _     (make-transform #{:Init} nil s2 :value identity) 
          _     (make-transform #{:Text} [s1 s2] cell nil 
                                (fn [p t v a b]
                                  (str p t v a b)))]
      (s1 :in [:Init "A"])
      (s2 :in [:Init "B"])
      ; Give some time for the cells to take their values
      (cell :in [:Text "X"])
      (is (= (cell :state) ":TextXAB"))
      (s2 :in [:Init "C"])
      (is (= (cell :state) ":TextXAB"))
      (cell :in [:Text "Y"])
      (is (= (cell :state) ":TextXAB:TextYAC")))))


(deftest test-networks-of-transforms
  (testing "a simple inain of transforms"
    (let [cell1 (make-cell :Number)
          cell2 (make-cell)
          _     (make-transform #{:Number} nil cell1 :value inc)
          _     (make-transform #{:Number} nil cell2 :value #(* 2 %))]
    ; Connect cell1 -> cell2
    (cell1 :out cell2)
    ; Put some values in cell1
    (cell1 :in [:Number 2])
    ; (* 2 (inc 2))
    (is (= (cell2 :state) 6))
    (cell1 :in [:Number 5])
    ; (* 2 (inc 5))
    (is (= (cell2 :state) 12))
    (cell1 :in [:Number 8])
    ; (* 2 (inc 8))
    (is (= (cell2 :state) 18))))
  
  (testing "two transforms feeding into one"
    (let [cell1 (make-cell :Number)
          cell2 (make-cell :Number)
          cell3 (make-cell)
          _     (make-transform #{:Number} nil cell1 :value inc)
          _     (make-transform #{:Number} nil cell2 :value dec)
          _     (make-transform #{:Number} nil cell3 :value #(* 2 %))]
    ; Connect cell1 -> cell3 and cell2 -> cell3
    (cell1 :out cell3)
    (cell2 :out cell3)
    ; Put some values on cell1
    (cell1 :in [:Number 2])
    ; (* 2 (inc 2))
    (is (= (cell3 :state) 6))
    (cell2 :in [:Number 5])
    ; (* 2 (dec 5))
    (is (= (cell3 :state) 8))
    (cell1 :in [:Number 8])
    ; (* 2 (inc 8))
    (is (= (cell3 :state) 18))))

  (testing "two transforms feeding into one, but also being depended on statically"
    (let [cell1 (make-cell :Number)
          cell2 (make-cell :Number)
          cell3 (make-cell)
          _     (make-transform #{:Number} nil cell1 :value inc)
          _     (make-transform #{:Number} nil cell2 :value dec)
          _     (make-transform #{:Number} [cell1 cell2] cell3 :depends (fnil + 0 0))]
    ; Connect cell1 -> cell3 and cell2 -> cell3
    (cell1 :out cell3)
    (cell2 :out cell3)
    ; Put some values on cell1
    (cell1 :in [:Number 2])
    (is (= (cell3 :state) 3))
    (cell2 :in [:Number 5])
    (is (= (cell3 :state) 7))
    (cell1 :in [:Number 8])
    (is (= (cell3 :state) 13))))

  (testing "init transforms to initialise cells"
    (let [cell1 (make-cell :Number)
          cell2 (make-cell :Number)
          cell3 (make-cell :Number)
          _     (make-transform #{:Number} nil cell1 :value inc)
          _     (make-transform #{:Number} nil cell2 :value dec)
          _     (make-transform #{:Number} [cell1 cell2] cell3 :depends (fnil + 0 0))]
    ; Connect cell1 -> cell3 and cell2 -> cell3
    (cell1 :out cell3)
    (cell2 :out cell3)
    ; Put some values on cell1
    (cell1 :in [:Number 2])
    (is (= (cell3 :state) 3))
    (cell2 :in [:Number 5])
    (is (= (cell3 :state) 7))
    (cell1 :in [:Number 8])
    (is (= (cell3 :state) 13)))))
