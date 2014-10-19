(ns shi.api.nova
  (:require [org.httpkit.client :as http])
  (:require [shi.environment.config :as cfg])
  (:require [shi.common.common :refer [sanitize-url not-nil? in?]])
  (:require [shi.api.keystone :as ks])
  (:require [shi.rest :as sr])
  (:require [cheshire.core])
  (:require [clojure.tools.logging :as log])
  (:require [clojure.pprint :as pp]))


(defn server-list [auth & {:keys [query-params]}]
  "Returns a list of server instances"
  (let [req (ks/make-rest :resp auth :svc-name "nova" :url-end "servers" :method :get
                          :query-params query-params)]
    (sr/send-request req)))


(defn server-details [auth & {:keys [query-params]}]
  "Returns detailed info of server instances"
  (let [req (ks/make-rest :resp auth :svc-name "nova" :url-end "servers/detail" :method :get
                          :query-params query-params  )]
    (sr/send-request req)))


(defn server-create-body [& {:keys [name image-ref flavor-ref]
                        :as opts}]
  "Creates the request body to create a server

  Keywords:
    name: The name of the server instance
    image-ref:
  "
  (let [extras #{:adminPass :security_groups :user_data :availability_zone :networks :fixedip :metadata
                 :personality :config_drive :min_count :max_count}
        convert (fn [m pair]
                  (assoc m (first pair) (second pair)))
        pairs (for [x opts :when (contains? extras (first x))] x)
        required {:name name :imageRef image-ref :flavorRef flavor-ref}]
     {:server (reduce convert required pairs)}))


(defn server-create [auth & {:keys [body query-params]}]
  (let [req (ks/make-rest :resp auth :svc-name "nova" :url-end "servers" :method :post :body body
                          :query-params query-params)]
    (sr/send-request req)))