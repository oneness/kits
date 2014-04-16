(ns kits.test.job-test
  (:require [clojure.test :refer :all]
            [kits.job :refer :all]
            [kits.logging.log-async :as log]))

(defn- successful-inc [val job]
  [(inc val) job])

(defn- failing-inc [val job]
  [val (abort-job! job "Cannot succeed!")])

(deftest job-test
  (log/reset-q! 100)

  (testing "when run successfully"
    (let [[result job] (run-with-short-circuiting (create-job) 0 (repeat 5 successful-inc))]
      (is (= 5 result))
      (is (not (aborting? job)))))

  (testing "it short circuits correctly"
    (let [[result job] (run-with-short-circuiting (create-job) 0 [successful-inc failing-inc successful-inc])]
      (is (= 1 result))
      (is (aborting? job))
      (is (= ["Cannot succeed!"] (error-messages job))))))
