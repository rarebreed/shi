;; Contains rest client helper base functions for Openstack

(ns shi.rest
  (:require [org.httpkit.client :as http])
  (:require [cheshire.core])
  (:require [clojure.tools.logging :as log]))

(defrecord Credentials [user password tenant token])

(def cred (Credentials. "admin" "redhat" "demo" "http://192.168.122.141:5000/v2.0/tokens"))

(def options {:timeout 200             ; ms
              :basic-auth ["admin" "pass"]
              :query-params {:param "value" :param2 ["value1" "value2"]}
              :user-agent "User-Agent-string"
              :headers {"X-Header" "Value"}})

(defrecord request [url ; the http url
                    method ; :get, :post project.clj
                    user-agent  ;
                    ])


(defn create-authcreds [username pwd tenant]
  (cheshire.core/generate-string
   {:auth {:tenantName tenant
           :passwordCredentials {:username username
                                 :password pwd}}}))

(defn sanitize-url [auth-url end]
  "Makes sure that the auth-url ends in end arg"
  (let [ending (format "/%s" end)]
    (cond
      (.endsWith auth-url ending) auth-url
      (.endsWith auth-url "/") (str auth-url end)
      :else (str auth-url ending))))


(defn get-token
  ;; uses a Credential type object
  ([{:keys [user password tenant token]}]
   (do
     (log/info user password tenant token)
     (get-token user password tenant token)))
  ([user pwd tenant auth-url]
   (let [url (sanitize-url auth-url "tokens")
         auth (create-authcreds user pwd tenant)
         req {:headers {"Content-Type" "application/json", "Accept" "application/json"}
              :body auth}]
     (log/debugf "url:%s\nauth:%s\nreq:%s" url auth (str req))
     @(http/post url req)))
  ;; metadata
  {:doc "Function to retrieve token from keystone"})


(quote "TODO: the serviceCatalog isn't structured conveniently, so convert it to a
       map keyed by the service name, rather than as an array of services"
(defn endpoint-convert [ctlg]
  "Return a transformed map"
  ; FIXME: I think we can make this a macro
  (loop [[ep & r] ctlg]
    (if ep
      ))
)


(defn get-from-body [body & args]
  "retrieve an element from a nested data structure
  usage:
  (def m {:order {:id 1001 :date 12012014}})
  (get-from-body m :order :id)"
  (get-in body args))


(defn get-body [resp]
  "Take the :body from our response and convert to a map"
  (cheshire.core/parse-string (:body resp)))


(defn parse-resp [resp]
  "Returns a map where :body is a map instead of a string"
  (assoc resp :body (get-body resp)))


(defn get-catalog [resp]
  "Gets the service catalog from within the body"
  (let [r (parse-resp resp)]
    (-> (:body r) (get-from-body "access" "serviceCatalog"))))


