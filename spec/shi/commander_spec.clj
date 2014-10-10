(ns shi.test_commander
  (:require [clojure.java.io :as io])
  (:require [speclj.core :refer :all])
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

(describe "shi.commander tests"
  (before-all
    (spit "dummy.py" dummy))

  (after-all
    (io/delete-file "dummy.py"))

  (it "Runs a python dummy test that returns 0 on success"
    (should=  0
      (let [[c pb] (testme)
             proc (.start pb)
             thr (sc/start-reader sc/reader-loop-unbuffered proc)]
        (println "Started the thread reader")
        (Thread/sleep 11000)
        (println "done")
        (.exitValue proc)))))

(run-specs)
