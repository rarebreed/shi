(ns shi.api.keystone
  (:require [org.httpkit.client :as http])
  (:require [cheshire.core])
  (:require [clojure.tools.logging :as log])
  (:require [clojure.pprint :as pp]))


; The Credentials defrecord type is the preferred way to authenticate within
; as it holds all the necessary information
(defrecord Credentials [user password tenant token])


; An example Credentials object
(def cred (Credentials. "admin" "redhat" "demo" "http://192.168.122.141:5000/v2.0/tokens"))


(defn create-authcreds [username pwd tenant]
  "Generage the JSON string which will be embedded in the http request body

  Params
    username: String representing the user to get credentials format
    pwd: String representing the password for the user
    tenant: the tenant name which the user belongs to

  returns-> a JSON formatted string"
  (cheshire.core/generate-string
   {:auth {:tenantName tenant
           :passwordCredentials {:username username
                                 :password pwd}}}))

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


(defn get-from-body [body & args]
  "Retrieve an element from a nested data structure

  Params
    body: The :body portion of the map which is the response from an http request
    args: a sequence of args which is used to drill down into the nested map

  returns-> The sub-map obtained from get-in"
  (get-in body args))


(defn get-body [resp]
  "Take the :body from our response and convert to a map"
  (cheshire.core/parse-string (:body resp)))


(defn parse-resp [resp]
  "Returns a map where :body is a map instead of a string"
  (assoc resp :body (get-body resp)))


(defn get-token [resp]
  "Get the token id from the body of the response"
  (get-from-body (:body resp) "access" "token" "id"))


(defn get-catalog [resp]
  "Gets the service catalog from within the body"
  (-> (:body resp) (get-from-body "access" "serviceCatalog")))


(defn authenticate
  ;; uses a Credential type object
  ([{:keys [user password tenant token]}]
     (log/info user password tenant token)
     (authenticate user password tenant token))
  ([user pwd tenant auth-url]
   (let [url (sanitize-url auth-url "tokens")
         auth (create-authcreds user pwd tenant)
         req {:headers {"Content-Type" "application/json", "Accept" "application/json"}
              :body auth}]
     (log/debugf "url:%s\nauth:%s\nreq:%s" url auth (str req))
     (parse-resp @(http/post url req))))
  ;; metadata
  {:doc "Function to retrieve token from keystone"})
