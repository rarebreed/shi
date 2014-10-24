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

(defn get-from-body
  "Retrieve an element from a nested data structure

  Params
    body: The :body portion of the map which is the response from an http request
    args: a sequence of args which is used to drill down into the nested map

  returns-> The sub-map obtained from get-in"
  [body & args]
  (get-in body args))


(defn get-body
  "Take the :body from our response and convert to a map"
  [resp]
  (cheshire.core/parse-string (:body resp)))


(defn parse-resp
  "Returns a map where :body is a map instead of a string"
  [resp]
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


(defmulti set-catalog
          "Gets the service catalog from within the body"
          :version )


(defmethod set-catalog  "v2.0" [resp catalog]
  (assoc-in resp [:body "access" "serviceCatalog"] catalog))


(defmethod set-catalog  "v3" [resp catalog]
  (assoc-in resp [:body "token" "catalog"] catalog))


(defn get-service
  "Gets the service endpoints maps from the catalog from a sequence of service names

   Args:
     -names: sequence of service names (eg ['nova', 'glance'])
     -catalog: the map returned from get-catalog
   Returns a lazy sequence of a map of the service"
  [names catalog]
  {:post (not-empty %)}
  (for [name (keys catalog) :when (in? names name)] (catalog name)))


(defn- get-endpoints [svc]
  (svc "endpoints"))


(defn- convert-endpoints
  "The way OpenStack keystone lays out the service endpoints is not user friendly. This function converts
  the endpoints from a list to a map, where the keys are 'public', 'internal' and 'admin'

  For example, to obtain the admin url you now only need to do this:

  (get-in svc ['endpoints' 'admin' 'url])"
  [svc]
  (let [convert (fn [m ept]
                  (if (nil? m)
                    (let [m {(ept "interface") ept}]
                      m)
                    (assoc m (ept "interface") ept)))
        epts (get-endpoints svc)]
    (assoc svc "endpoints" (reduce convert {} epts))))


(defn- convert-all [auth]
  (let [catalog (get-catalog auth)
        converted (vec (map convert-endpoints catalog))
        trans (fn [m svc]
                (let [name (svc "name")
                      kname (keyword name)]
                  (assoc m kname (dissoc svc name))))
        final (reduce trans {} converted)]
    (set-catalog auth final)))


(defn get-url [svc etype]
  (get-in svc ["endpoints" etype "url"]))


(defn- get-service-info [auth name]
  (let [catalog (get-catalog auth)
        svc (first (get-service [name] catalog))]
    (if (nil? svc)
      (throw (Exception. "Could not find service"))
      (let [url (get-url svc "public")
            token (get-token auth)]
        {:url url :token token}))))


(defmulti get-rest-basics
  "Kind of a hack, but since keystone-v3 isn't in the catalog of the response body, this is one way
  to separate the logic of how to get the needed rest information.  This is simply a helper function
  thaat gives us the url and token to make a REST call"
  (fn [auth name]
     (= name :keystone-v3)))


(defmethod get-rest-basics false [auth ^String svc-name]
  (get-service-info auth svc-name))


(defmethod get-rest-basics true [auth svc-name]
  (let [keystone (get-service-info auth :keystone)
        token (:token keystone)
        v2url (:url keystone)
        url (clojure.string/replace v2url #"v2.0" "v3")]
    (log/info "In get-rest-basics")
    {:url url :token token}))


(defn make-rest
  "Basic wrapper around the required elements to make an http Request

  Keywords:
    method: a keyword of the function type (eg :get :post etc)
    auth: the response object returned from (authorize)
    url-end: a String which will be concatened to the url endpoint
    svc-name: the name of the server (eg 'nova' or 'glance')
    body: (optional) a map containing the body of the request
    query-params: (optional) a map of keyword|value pairs that the rest call may need

  returns-> a request map that can be passed to (http/request)"
  [& {:keys [auth svc-name url-end method body query-params]}]
  (let [{:keys [url token]} (get-rest-basics auth svc-name)
        req {:url (sanitize-url url url-end)
             :headers {"Content-Type" "application/json",
                       "Accept" "application/json"
                       "X-Auth-Token" token}
             :method method}
        req-q (if query-params
                (assoc req :query-params (first (query-params)))
                req)
        req-f (if body
                (assoc req-q :body body)
                req-q)]
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
      (-> @(http/post url req) (assoc :version "v2.0") (parse-resp) (convert-all))))

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
      (->  @(http/post url req) (assoc :version "v3") (parse-resp ) (convert-all))))

  (service-catalog [this resp] (get-catalog resp)))


(defn make-creds-v3
  "Returns a CredentialsV3 defrecord object which can be used for authentication to keystone V3

  KeyArgs:
    - username: string of the user's name
    - userid: string for the user's id
    - authmethod: one of 'password' or 'token'
    - domain: a map which can contain the keys :id or :name (corresponding to the domain id or name)
    - auth-url: a string representing the authenticaltion url (eg http://192.168.122.244:5000/v3)
  "
  [& {:keys [username userid authmethod secret domain auth-url]
      :as opts
      :or {authmethod "password"}}]
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


;============================================================================
; service catalog
;============================================================================

(defn list-services
  "It appears that keystone v3 doesn't show up in the service catalog returned
  from authorization, so we use this function to get a list of services.  Since
  it was returned in the authorization response, we cant use that returned map
  to get our URL, so we have to explicitly know it for now"
  [auth svc & {:keys [query-params]}]
  (let [req (make-rest :auth auth :svc-name svc :url-end "services" :method :get
                       :query-params query-params)]
    (sr/send-request req)))


(defn get-service-url
  "return a sequence of vectors [servicename url]

  Args:
    - services: the map returned in the :body from list-services

  returns-> lazy sequence of service-name,service-url vectors"
  [services]
  (for [svc (services "services")]
    (let [name (svc "name")
          url (get-in svc ["links" "self"])]
       [name url])))


(defn endpoints-list
  "REST call to retrieve service endpoints"
  [auth svc & {:keys [query-params]}]
  (let [req (make-rest :auth auth :svc-name svc :url-end "endpoints" :method :get
                       :query-params query-params)]
    (sr/send-request req)))


(defn show-service
  "REST call to retrieve detailed information for all the services

  (Note: this doesn't seem to provide any more info than what service-list does)"
  [auth services]
  (for [[name url] (get-service-url services)]
    (let [token (get-token auth)
          req {:url url
               :headers {"Content-Type" "application/json",
                         "Accept" "application/json"
                         "X-Auth-Token" token}
               :method :get}]
      {:name name :result (:body (sr/send-request req))})))


;============================================================================
; Domain functions
;============================================================================

(defn domain-list [auth svc & {:keys [query-params]}]
  (let [req (make-rest :auth auth :svc-name svc :url-end "domains" :method :get
                       :query-params query-params)]
    (sr/send-request req)))


;============================================================================
; projects related functions
;============================================================================

(defn projects-list [auth svc & {:keys [query-params]}]
  (let [req (make-rest :auth auth :svc-name svc :url-end "projects" :method :get
                       :query-params query-params)]
    (sr/send-request req)))

(defrecord ProjectV3 [^String description
                      ^String domain_id
                      ^String enabled
                      ^String name])
(defn make-project
  "Creates a project

  KeyArgs:
    - description: a description of the project
    - domain_id: The name of domain 'Default' by default
    - enabled: whether the project is enabled or not (true by default)
    - name: The name of the project
  "
  [& {:keys [description domain_id enabled name]
      :or {description "" domain_id "Default" enabled true}}]
  {:pre [(not-nil? name)]}
  (ProjectV3. description domain_id enabled name))


(defn project-create [auth svc project]
  (let [body (cheshire.core/generate-string {:project project})
        req (make-rest :auth auth :svc-name svc :url-end "projects" :method :post
                       :body body)]
      (sr/send-request req)))


; hierarchy is a pseudo grammar rule for the JSON structure
(def hierarchy  {:auth
                  {:user [:domain :name :password :id]}
                  {:identity [:methods :password :token]}
                  {:methods ["password" "token"]}
                  {:password [:user]}
                  {:domain [:id :name]}
                  {:scope [:project :domain]}
                  {:project [:id]}})


;=============================================================================
; User related functions
;=============================================================================

(defn user-list
  "Isses REST call to retrieve users"
  [auth svc & {:keys [query-params]}]
  (let [req (make-rest :auth auth :svc-name svc :url-end "users" :method :get
                       :query-params query-params)]
    (sr/send-request req)))


(defn find-user-by-name
  "Takes the list of users (as returned from users-list) and returns matches by name
  Args:
    - user-list: the :body returned from users-list"
  [user-list])


(defrecord UserV3 [^String default_project_id
                   ^String description
                   ^String domain_id
                   ^String email
                   ^String enabled
                   ^String name
                   ^String password])
(defn make-user-v3
  "Creates a UserV3 type which can be passed to user-create.  It must contain at a minimum
  the default_project_id and name"
  [& {:keys [default_project_id name description domain_id enabled password email]
      :or {description "" domain_id "Default" enabled true password "" email ""}}]
  {:pre [(not-nil? default_project_id) (not-nil? name)]}
  (->UserV3 default_project_id description domain_id email enabled name password))


(defn user-create
  "Creates a user

  Args:
    - auth: The authentication response map
    - svc: one of :keystone or :keystone-v3
    - user: a UserV3 object"
  [auth svc ^shi.api.keystone.UserV3 user]
  (let [body (cheshire.core/generate-string {:user user})
        req (make-rest :auth auth :svc-name svc :url-end "users" :method :post
                       :body body)]
    (sr/send-request req)))
