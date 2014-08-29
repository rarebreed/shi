;; Contains rest client helper base functions for Openstack

(ns shi.rest
  (:require [org.httpkit.client :as http])
  (:require [cheshire.core]))

(def options {:timeout 200             ; ms
              :basic-auth ["admin" "pass"]
              :query-params {:param "value" :param2 ["value1" "value2"]}
              :user-agent "User-Agent-string"
              :headers {"X-Header" "Value"}})

(defrecord request [url ; the http url
                    method ; :get, :post project.clj
                    user-agent  ;
                    ])



;; Example of what org.httpkit.client/request contains
(let [request {:url "http://http-kit.org/"
               :method :get             ; :post :put :head or other
               :user-agent "User-Agent string"
               :oauth-token "your-token"
               :headers {"X-header" "value"
                         "X-Api-Version" "2"}
               :query-params {"q" "foo, bar"} ;"Nested" query parameters are also supported
               :form-params {"q" "foo, bar"} ; just like query-params, except sent in the body
               :basic-auth ["user" "pass"]
               :keepalive 3000          ; Keep the TCP connection for 3000ms
               :timeout 1000      ; connection timeout and reading timeout 1000ms
               :filter (http/max-body-filter (* 1024 100)) ; reject if body is more than 100k
               :insecure? true ; Need to contact a server with an untrusted SSL cert?

               ;; File upload. :content can be a java.io.File, java.io.InputStream, String
               ;; It read the whole content before send them to server:
               ;; should be used when the file is small, say, a few megabytes
               :multipart [{:name "comment" :content "httpkit's project.clj"}
                           {:name "file" :content (clojure.java.io/file "project.clj") :filename "project.clj"}]

               :max-redirects 10 ; Max redirects to follow
                ;; whether follow 301/302 redirects automatically, default to true
                ;; :trace-redirects will contain the chain of the redirections followed.
               :follow-redirects false
               } ])

(defn create-authcreds [username pwd tenant]
  (cheshire.core/generate-string {:auth {:tenantName tenant
                                         :passwordCredentials {:username username
                                                               :password pwd}}}))

(defn sanitize-url [auth-url]
  "Makes sure that the auth-url ends in /tokens"
  (cond
   (.endsWith auth-url "/tokens") auth-url
   (.endsWith auth-url "/") (str auth-url "tokens")
   :else (str auth-url "/tokens")))


(defn get-token
  "function to retrieve token from keystone"
  ([user pwd tenant auth-url]
    (let [url
          auth (create-authcreds user pwd tenant)
          req {:headers {"Content-Type" "application/json", "Accept" "application/json"}
               :body auth}]
      @(http/post auth-url req)))
  ([& args])


(defn endpoint-convert [m]
  "Return a transformed map"
  ; FIXME: I think we can make this a macro
  (let [newname (str (m "name") "-" (get-in catalog [0 "endpoints" 0 "id"]))]
    {newname (dissoc m "name")}))



(defn parse-token-resp [resp]
  "Take the :body from our response and break it up into the parts we need"
  (let [body (cheshire.core/parse-string (:body resp))]
    ; Get the endpoints
    ))






