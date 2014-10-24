(ns shi.test_commander
  (:require [clojure.java.io :as io])
  (:use clojure.test)
  (:require [shi.commander :as sc]))

(def dummy
"import time,sys

counter = 10

while counter > 0:
    print(counter)
    counter -= 1
    time.sleep(1)

sys.exit(0)
")

(defn testme []
  ;; simple helper that will create the Command record and ProcessBuilder object
  (let [testcmd (sc/make-command ["python" "-u" "dummy.py"])
        testpb (sc/make-builder testcmd)]
    [testcmd testpb]))

(defn dummy-fixture [f]
  (spit "dummy.py" dummy)
  f
  (io/delete-file "dummy.py"))

(use-fixtures :each dummy-fixture)

(deftest shi-commander-tests
  (testing "Runs a python dummy test that returns 0 on success"
    (is  (=  0
      (let [[c pb] (testme)
             proc (.start pb)
             thr (sc/start-reader sc/reader-loop-unbuffered proc)]
        (println "Started the thread reader")
        (Thread/sleep 11000)
        (println "done")
        (.exitValue proc))))))

