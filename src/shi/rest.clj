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
  (:require [org.httpkit.client :as http])
  (:require [cheshire.core])
  (:require [clojure.tools.logging :as log])
  (:require [clojure.pprint :as pp]))

(defrecord Request [method  ; :get, :post, etc
                    headers ; A map containing optional headers :Content-Type :Accept :User-Agent
                    url     ; the http url
                    body    ; Optional, The body to be included
                    ])

(defn send-request [req]
  (let [resp @(http/request req)
        body (cheshire.core/parse-string (resp :body))
        result (assoc resp :body body)]
    {:resp result :body body :status (resp :status)}))

