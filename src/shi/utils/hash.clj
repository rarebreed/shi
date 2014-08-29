;; This library will contain methods to help retrieve various checksum from objects such as md5, sha1
;; etc.

(ns shi.utils.hash
  (:require clojure.string)
  (:import [java.security MessageDigest]))


(defn get-checksum
  [algo fname]
  (let [cs (MessageDigest/getInstance algo)]))

