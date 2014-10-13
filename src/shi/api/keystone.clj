(ns shi.api.keystone
  (:require [org.httpkit.client :as http])
  (:require [shi.environment.config :as cfg])
  (:require [shi.common.common :refer [sanitize-url not-nil?]])
  (:require [cheshire.core])
  (:require [clojure.tools.logging :as log])
  (:require [clojure.pprint :as pp]))

;================================================================================
; Response body functions
; FIXME: these will need to be multimethods or have a defprotocol because the
; response format changed from v2 to v3.
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

;==========================================================================
; Identity protocol
; Defines how to authorize based on keystone version
;==========================================================================

(defprotocol Identity
  (create-authcreds [this] "Creates the json request body for the token")
  (authorize [this] "Makes a REST call to get token and returns response")
  (service-catalog [this resp]))

;==========================================================================
; Credentials methods.
;==========================================================================

(defrecord Credentials [user password tenant route]
  Identity
  (create-authcreds [this]
    (cheshire.core/generate-string  {:auth {:tenantName (:tenant this)
                                     :passwordCredentials {:username (:user this)
                                     :password (:password this)}}}))
  (authorize [this]
    (let [url (sanitize-url route "tokens")
          auth (create-authcreds this)
          req {:headers {"Content-Type" "application/json", "Accept" "application/json"}
               :body auth}]
      (log/debugf "url:%s\nauth:%s\nreq:%s" url auth (str req))
      (parse-resp @(http/post url req))))
  (service-catalog [this resp] (get-catalog resp)))


; =======================================================================
; CredentialsV3 functions
; ========================================================================

(defrecord CredentialsV3 [user secret authmethod domain auth-url extra]
  Identity
  (create-authcreds [this]
    {:pre [(= (class this) CredentialsV3)]}
    (let [token (:token (:extra this))
          scope (:scope (:extra this))
          identity- {:identity
                     {:methods [(:authmethod this)]
                      :password (:user this)}}
          ident (if (not-nil? token)
                  (assoc identity- :token token)
                  identity-)
          id-scope (if (not-nil? scope)
                     (assoc ident :scope scope)
                     identity-)]
      (cheshire.core/generate-string {:auth id-scope})))
  (authorize [this]
    (log/info "Args passed in: " this)
    (let [url (sanitize-url (:auth-url this) "auth/tokens")
          auth (create-authcreds this)
          req {:headers {"Content-Type" "application/json", "Accept" "application/json"}
               :body auth}]
      (log/infof "url: %s\nauth: %s\nreq: %s" url auth req)
      ;(parse-resp @(http/post url req))))
      @(http/post url req)))
  (service-catalog [this resp] (get-catalog resp)))


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
                                                    (log/error "Must use domain object if not using userid")
                                                    nil)
                (not-nil? username) {:user {:domain domain
                                            :name username
                                            authtype secret}}
                (not-nil? userid) {:user {:id userid
                                          authtype secret}})
        extra (dissoc username userid authmethod secret domain auth-url)]
    (->CredentialsV3 user secret authmethod domain auth-url extra)))

; hierarchy is a pseudo grammar rule for the JSON structure
(def hierarchy  {:auth
                  {:user [:domain :name :password :id]}
                  {:identity [:methods :password :token]}
                  {:methods ["password" "token"]}
                  {:password [:user]}
                  {:domain [:id :name]}
                  {:scope [:project :domain]}
                  {:project [:id]}})
