;; Anything you type in here will be executed
;; immediately with the results shown on the
;; right.
(use 'shi.core)
(require '[org.httpkit.client :as http])
(require '[clojure.reflect :as r])
(use '[clojure.pprint :only [print-table]])

(def result (http/get "http://www.python.org"))


(print-table (:members (r/reflect result)))

(doseq [x '(1 2 3) y [10 11 12]]
  (println (* x y)))

(def options {:timeout 200
              :basic-auth ["Pinky Brain" "redhat"]})

(def deployment "https://")
