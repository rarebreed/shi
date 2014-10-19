(ns shi.api.keystone
  (:require [org.httpkit.client :as http])
  (:require [shi.environment.config :as cfg])
  (:require [shi.common.common :refer [sanitize-url not-nil? in?]])
  (:require [shi.rest :as sr])
  (:require [cheshire.core])
  (:require [clojure.tools.logging :as log])
  (:require [clojure.pprint :as pp]))

;================================================================================
; Response body functions
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


(defmulti get-token
  "Gets the authorization token from the REST response"
  :version)

(defmethod get-token "v2.0" [resp]
  (get-from-body (:body resp) "access" "token" "id"))


(defmethod get-token "v3" [resp]
  (get-from-body resp :headers :x-subject-token))


(defmulti get-catalog
  "Gets the service catalog from within the body"
  :version )


(defmethod get-catalog  "v2.0" [resp]
  (-> (:body resp) (get-from-body "access" "serviceCatalog")))


(defmethod get-catalog  "v3" [resp]
  (-> (:body resp) (get-from-body "token" "catalog")))


(defn get-service [names catalog]
  "Gets the service endpoints maps from the catalog from a sequence of service names

   Args:
     -names: sequence of service names (eg ['nova', 'glance'])
     -catalog: the map returned from get-catalog
   Returns a lazy sequence of a map of the service"
  (for [svc catalog :when (in? names (svc "name"))] svc))


(defn get-endpoints [svc]
  (svc "endpoints"))


(defn convert-endpoints [svc]
  "The way OpenStack keystone lays out the service endpoints is not user friendly. This function converts
  the endpoints from a list to a map, where the keys are 'public', 'internal' and 'admin'

  For example, to obtain the admin url you now only need to do this:

  (get-in svc ['endpoints' 'admin' 'url])"
  (let [convert (fn [m ept]
                  (if (nil? m)
                    (let [m {(ept "interface") ept}]
                      m)
                    (assoc m (ept "interface") ept)))
        epts (get-endpoints svc)]
    (assoc svc "endpoints" (reduce convert {} epts))))


(defn get-url [svc etype]
  (get-in svc ["endpoints" etype "url"]))


(defn get-rest-basics [resp svc-name]
  (let [catalog (get-catalog resp)
        orig-svc (first (get-service [svc-name] catalog))
        svc (convert-endpoints orig-svc)
        url (get-url svc "public")
        token (get-token resp)]
    {:svc svc :url url :token token}))


(defn make-rest [& {:keys [resp svc-name url-end method body query-params]}]
  "Basic wrapper around the required elements to make an http Request

  Keywords:
    method: a keyword of the function type (eg :get :post etc)
    resp: the response object returned from (authorize)
    url-end: a String which will be concatened to the url endpoint
    svc-name: the name of the server (eg 'nova' or 'glance')
    body: (optional) a map containing the body of the request
    query-params: (optional) a map of keyword|value pairs that the rest call may need

  returns-> a request map that can be passed to (http/request)"
  (let [{:keys [url token]} (get-rest-basics resp svc-name)
        req {:url (sanitize-url url url-end)
             :headers {"Content-Type" "application/json",
                       "Accept" "application/json"
                       "X-Auth-Token" token}
             :method method}
        req-f (if query-params
                (assoc req :query-params (first (query-params)))
                req)]
    req-f))


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
; Credentials methods that implement Identity protocol
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
      (assoc @(http/post url req) :version "v2.0")))
  (service-catalog [this resp] (get-catalog resp)))


; =======================================================================
; CredentialsV3
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
      (assoc @(http/post url req) :version "v3")))
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