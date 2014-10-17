(ns shi.common.common
  (:require clojure.string))

(defn sanitize-url [auth-url end]
  {:pre [(every?  #(= String (class %)) [auth-url end])]}
  "Makes sure that the auth-url ends in end arg and replaces any double //
   Params
     auth-url: String representing the endpoint url
     end: A String which we will append to if necessary

   returns-> the properly formatted URL"
  (let [ending (format "/%s" end)
        url (cond
              (.endsWith auth-url ending) auth-url
              (.endsWith auth-url "/") (str auth-url end)
              :else (str auth-url ending))]
    url))

(def not-nil? (complement nil?))


(defn in?
  "true if seq contains elm"
  [seq elm]
  (some #(= elm %) seq))
