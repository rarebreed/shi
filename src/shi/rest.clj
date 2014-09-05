;; Contains rest client helper base functions for Openstack
;; This namespace will contain functionality that can be used generally
;; for any REST request.  It can therefore be used as a low-level library
;; to make REST requests.
;;
;; Inside the shi.api parent namespace are the higher level wrappers around
;; the various services.  For example shi.api.keystone contains calls
;; suited to make requests on the keystone service and shi.api.nova headers
;; calls for nova compute related services

(ns shi.rest
  (:require [shi.api.keystone :as sak])
  (:require [org.httpkit.client :as http])
  (:require [cheshire.core])
  (:require [clojure.tools.logging :as log])
  (:require [clojure.pprint :as pp]))


(defrecord Request [method  ; :get, :post, etc
                    headers ; A map containing optional headers :Content-Type :Accept :User-Agent
                    url     ; the http url
                    body    ; The body to be included
                    ])


(defn make-request [method headers url body token]
  "Basic wrapper around the required elements to make an http Request

  Params:
    method: a keyword of the function type (eg :get :post etc)
    headers: a map which contains keyword-value of the matching header
    url: a String which is the url that the request will be sent to
    body: a String representing the body of the http request
    token: the authentication token for keystone

  returns-> a Request object"
  (let [b (if (= (type body) java.lang.String)
            body
            (cheshire.core/generate-string body))
        h (assoc headers :X-Auth-Token token)]
    (Request. method headers url body)))


(defn -main [& args]
  ; Tests
  (let [resp (sak/authenticate sak/cred)
        token (sak/get-token resp)
        catalog (sak/get-catalog resp)]
    (log/info "The response is:" resp "\n")
    (log/info "The token is: " token "\n")
    (log/info "The service catalog is" (pp/write catalog :stream nil))))
