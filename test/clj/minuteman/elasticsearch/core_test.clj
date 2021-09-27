(ns minuteman.elasticsearch.core-test
  (:require
   [clojure.test :refer [deftest testing is]]
   [minuteman.elasticsearch.client :refer [get-indices]]
   [minuteman.elasticsearch.core :as nut]))

(def instances
  [{:id 1 :name "One"} {:id 2 :name "Two"}])

(def indices
  [{:id 1 :name "One" :watching true :es-instance-id 1}
   {:id 2 :name "Two" :watching true :es-instance-id 2}
   {:id 3 :name "Three" :watching false :es-instance-id 1}])

(def indices-raw
  [{:name "Raw-1"} {:name "Raw-2"}])

(def states
  [{:name "Four" :es-instance-id 1 :docs-count 40}
   {:name "One" :es-instance-id 1 :docs-count 10}])

(def states-map
  {[1 "Four"] (first states)
   [1 "One"] (second states)})

(deftest elasticsearch-core-test
  (testing "can detect new indices"
    (let [f #'nut/detect-new-indices
          result (f states-map indices)]
      (is (seq result))
      (is (= 1 (count result)))
      (is (= "Four" (-> result first :name)))
      (is (= 1 (-> result first :es-instance-id)))))

  (testing "can detect deleted indices"
    (let [f #'nut/mark-n-tag-index-states
          result (f states-map indices)
          deleted (filter #(= "DELETED" (:health %)) (vals result))]
      (is (seq deleted))
      (is (= 2 (count deleted)))
      (is (= [2 3] (map :es-index-id deleted))))))
