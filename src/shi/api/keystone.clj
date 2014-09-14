(ns shi.api.keystone
  (:require [org.httpkit.client :as http])
  (:require [shi.environment.config :as cfg])
  (:require [shi.common.common :refer [sanitize-url]])
  (:require [cheshire.core])
  (:require [clojure.tools.logging :as log])
  (:require [clojure.pprint :as pp]))


; The Credentials defrecord type is the preferred way to authenticate within
; as it holds all the necessary information
(defrecord Credentials [user password tenant route])


; An example Credentials object
(def cred (Credentials. (cfg/config :admin-name)
                        (cfg/config :admin-pass)
                        "demo"
                        (cfg/config :auth-url)))

(defn create-authcreds [username pwd tenant]
  "Generate the JSON string which will be embedded in the http request body

  Params
    username: String representing the user to get credentials format
    pwd: String representing the password for the user
    tenant: the tenant name which the user belongs to

  returns-> a JSON formatted string"
  (cheshire.core/generate-string
   {:auth {:tenantName tenant
           :passwordCredentials {:username username
                                 :password pwd}}}))


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


(defn repr-project [name & {:keys [description enabled auth-url]
                            :or {description ""
                                 enabled true
                                 auth-url (cfg/config :auth-url)}}]
  {:auth (sanitize-url auth-url "/auth/tokens")
   :domain {:description description
            :enabled enabled
            :name name}})


(defn user
  {:auth {:identity
            {:methods ["password"],
             :password
               {:user {:id "0ca8f6",
                       :password "secrete"}}}}})

(defn resp-cb [{:keys [opts status body headers error] :as resp}]
  resp)


(defn authenticate-v2
  ;; uses a Credential type object
  ([{:keys [user password tenant route]}]
     (log/info user password tenant route)
     (authenticate-v2 user password tenant route))
  ([user pwd tenant auth-url]
   (let [url (sanitize-url auth-url "tokens")
         auth (create-authcreds user pwd tenant)
         req {:headers {"Content-Type" "application/json", "Accept" "application/json"}
              :body auth}]
     (log/debugf "url:%s\nauth:%s\nreq:%s" url auth (str req))
     (parse-resp @(http/post url req))))
  ;; metadata
  {:doc "Function to retrieve token from keystone"})

