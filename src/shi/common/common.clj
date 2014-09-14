(ns shi.common.common)

(defn sanitize-url [auth-url end]
  "Makes sure that the auth-url ends in end arg

  Params
    auth-url: String representing the endpoint url
    end: A String which we will append to if necessary

  returns-> the properly formatted URL"
  (let [ending (format "/%s" end)]
    (cond
      (.endsWith auth-url ending) auth-url
      (.endsWith auth-url "/") (str auth-url end)
      :else (str auth-url ending))))
