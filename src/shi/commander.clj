;; Author: Sean Toner
;; This is a clojure library that will help execute subprocesses, and optionally capture their output and error


(ns shi.commander
  (:require [clojure.reflect :as r])
  (:require [clojure.string])
  (:require [clojure.java.io :refer [reader]])
  (:import [java.io InputStreamReader BufferedReader File]))


(defrecord Command [cmd env wdir cmbstderr? saveout? saveerr? inh outh errh])


(defn make-command
  "Helper function to make a Command record
   TODO:  Turn this into a multimethod?  that will handle putting in the proper type"
  ([cmd]
    ;; Make sure command is a sequence
   (if (not (isa?  (type cmd) java.util.List))
     (throw (IllegalArgumentException. "cmd must be a sequence"))
     (make-command cmd nil nil true false false nil nil nil)))
  ([cmd environ work-dir combine-stderr saveout saveerr input-hdl output-hdl error-hdl]
   (Command. cmd environ work-dir combine-stderr saveout saveerr input-hdl output-hdl error-hdl)))


(defn set-environ! [pb cmd]
  "Add any required environment variables
   pb is a ProcessBuilder
   cmd is the Command type"
  (when-let [new-entries (:env cmd)]
    (let [env (.environment pb)]
      (for [[k v] new-entries]
        (.put env k v)))))


(defn set-dir! [pb cmd]
  (when-let [wd (:wdir cmd)]
    (let [d (File. wd)]
      (.directory pb d))))


(defn make-builder [cmd]
  "Creates and sets the environment and working directory for a ProcessBuilder
   
   cmd(Collection<String>): a list of strings indicating the command and any args"
  (let [pb (ProcessBuilder. (:cmd cmd))]
    (-> pb (set-environ! cmd) (set-dir! cmd))
  pb))


(defn reader-out [proc rdrfn]
  "Starts to read the output stream of a java.lang.Process
   proc(Process): A Process object (from make-builder)
   rdrfn: a function that takes a Process object"
  (do
    (println "In reader-out ")
    (rdrfn proc)))


(defn reader-ready [proc]
  (with-open [rdr (-> (.getInputStream proc) (InputStreamReader.))]
    (loop [rdy (.ready rdr)]
      (if (true? rdy)
        (print (char (.read rdr)))
          ;(-> (.read rdr) (char) (print))
        (do
          (println "Not ready")
          (Thread/sleep 1000)
          (recur (.ready rdr)))))))


(defn reader-loop-unbuffered [proc]
  (with-open [rdr (-> (.getInputStream proc) (InputStreamReader.))]
    (loop [ch (.read rdr)]  ;; May block here or the other .read
      (if (= ch -1)
        (println "End of stream")
        (do
          (.print System/out (char ch))
          ;(print ch)
          (recur (.read rdr)))))))

(defn reader-loop-buffered [proc]
  (with-open [rdr (-> (.getInputStream proc) (InputStreamReader.) (BufferedReader.))]
    (loop [line (.readLine rdr)]  ;; May block here or the other .read
      (if (nil? line)
        (println "End of stream")
        (do
          (println line)
          (recur (.readLine rdr)))))))


(defn reader-out-unbuff [proc]
  (reader-loop-unbuffered proc))


(defn reader-out-buff [proc]
  (reader-loop-buffered proc))


(defn start-reader [proc]
  ;; Simple wrapper that launches a thread to get the output of a java.lang.Process
  (.start (Thread. (partial reader-out proc reader-ready))))


(defn testme []
  ;; simple test
  (let [testcmd (make-command ["python" "-u" "dummy.py"])
        testpb (make-builder testcmd)]
    [testcmd testpb]))


(defn -main []
  ;; Test
  (let [[c pb] (testme)
        proc (.start pb)
        thr (start-reader proc)]
    (println "Started the thread reader")
    (Thread/sleep 21000)
    (println "done")))
