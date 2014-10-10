(ns shi.api.keystone
  (:require [org.httpkit.client :as http])
  (:require [shi.environment.config :as cfg])
  (:require [shi.common.common :refer [sanitize-url]])
  (:require [cheshire.core])
  (:require [clojure.tools.logging :as log])
  (:require [clojure.pprint :as pp]))

;================================================================================
; Response body functions
; FIXME: these might need to be multimethods and resp might need to become
; ResponseV2 and ResponseV3 defrecords
;================================================================================
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
                            :as opts
                            :or {description ""
                                 enabled true
                                 auth-url (cfg/config :auth-url)}}]
  {:auth (sanitize-url auth-url "/auth/tokens")
   :domain {:description description
            :enabled enabled
            :name name}})


(defprotocol Identity
  (authorize [this] "Generates token and returns response")
  (create-authcreds [this] "Creates the json request body for the token")
  (service-catalog [this resp]))


(defn create-authcreds-body-v2 [opts]
  "Generate the JSON string which will be embedded in the http request body

  Args-
    opts: Credentials object

  returns-> a JSON formatted string"
  (cheshire.core/generate-string
   {:auth {:tenantName (:tenant opts)
           :passwordCredentials {:username (:user opts)
                                 :password (:password opts)}}}))


(defn authenticate-v2 [{:keys [user password tenant route]
                        :as cred}]
  "REST call to /v2/tokens

  args:
    Either a Credentials object, or the keys listed above

  returns-> A map representing the JSON response
  "
  (let [url (sanitize-url route "tokens")
        auth (create-authcreds-body-v2 user password tenant)
        req {:headers {"Content-Type" "application/json", "Accept" "application/json"}
             :body auth}]
    (log/info user password tenant route)
    (log/debugf "url:%s\nauth:%s\nreq:%s" url auth (str req))
    (parse-resp @(http/post url req)))))


; The Credentials defrecord type is the preferred way to authenticate
; as it holds all the necessary information
(defrecord Credentials [user password tenant route]
  Identity
  (authorize [this] (authenticate-v2 this))
  (create-authcreds [this] ())
  (service-catalog [this resp] (get-catalog resp)))


; =======================================================================
; Keystone V3 functions
; ========================================================================

; hierarchy is a pseudo grammar rule for the JSON structure
(def hierarchy  {:auth
                  {:user [:domain :name :password :id]}
                  {:identity [:methods :password :token]}
                  {:methods ["password" "token"]}
                  {:password [:user]}
                  {:domain [:id :name]}
                  {:scope [:project :domain]}
                  {:project [:id]}})


(defn create-authcreds-body-v3 [opts]
  "Creates the map which will become the http request body for v3

  args:
    - opts: A CredentialsV3 object
  "
  {:pre [(= (class opts) CredentialsV3)]}
  (let [token (:token (:extra opts))
        scope (:scope (:extra opts))
        identity- {:identity
                   {:methods [authmethod]
                    :password user}}
        ident (if (not-nil? token)
                (assoc identity- :token token)
                identity-)
        id-scope (if (not-nil? scope)
                   (assoc ident :scope scope)
                   identity-)
        auth {:auth id-scope}]
    auth))


(defn authenticate-v3
  ([{:keys [user secret authmethod domain auth-url extra]
     :as opt}]
     (log/info "Args passed in: " user secret authmethod domain auth-url extra)
     (authenticate-v3 user secret authmethod domain auth-url extra))
  ([user sec method domain url ext]
    (let [url (sanitize-url auth-url "auth/tokens")
          auth (create-authcreds user pwd tenant)
          req {:headers {"Content-Type" "application/json", "Accept" "application/json"}
               :body auth}]
      (log/debugf "url:%s\nauth:%s\nreq:%s" url auth (str req))
      (parse-resp @(http/post url req)))))


(defn make-creds-v3 [& {:keys [username userid authmethod secret domain auth-url]
                        :as opts
                        :or {authmethod "password"}}]
  "Returns a CredentialsV3 defrecord object which can be used for authentication to keystone V3

  KeyArgs:
    - username: string of the user's name
    - userid: string for the user's id
    - authmethod: one of 'password' or 'token'
    - domain: a map which can contain the keys :id or :name (corresponding to the domain id or name)
    - auth-url: a string representing the authenticaltion url (eg http://192.168.122.244:5000/v3)
  "
  (let [authtype (keyword authmethod)
        user  (cond
                (and (nil? domain) (nil? userid)) (do
                                                    (log/warning "Must use domain object if not using userid")
                                                    nil)
                (not-nil? username) {:user {:domain domain
                                            :name username
                                            authtype secret}}
                (not-nil? userid) {:user {:id userid
                                          authtype secret}})
        extra (dissoc username userid authmethod secret domain auth-url)]
    (->CredentialsV3 user secret authmethod domain auth-url extra)))


(defrecord CredentialsV3 [user secret authmethod domain auth-url extra]
  Identity
  (authorize [this] (authenticate-v3 this))
  (create-authcreds [this] (create-authcreds-body-v3 this))
  (service-catalog [this resp] (get-catalog-v3 resp)))
