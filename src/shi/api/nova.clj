(ns shi.api.nova
  (:require [org.httpkit.client :as http])
  (:require [shi.common.common :refer [sanitize-url not-nil? in?]])
  (:require [shi.api.keystone :as ks])
  (:require [shi.rest :as sr])
  (:require [cheshire.core])
  (:require [clojure.tools.logging :as log]))


(defn server-list [auth & {:keys [query-params]}]
  "Returns a list of server instances"
  (let [req (ks/make-rest :auth auth :svc-name :nova :url-end "servers" :method :get
                          :query-params query-params)]
    (sr/send-request req)))


(defn server-details [auth & {:keys [query-params]}]
  "Returns detailed info of server instances"
  (let [req (ks/make-rest :auth auth :svc-name :nova :url-end "servers/detail" :method :get
                          :query-params query-params  )]
    (sr/send-request req)))


(defn server-create-body [& {:keys [name image-ref flavor-ref]
                             :as opts}]
  "Creates the request body to create a server

  Keywords:
    name: The name of the server instance
    image-ref:
  "
  {:pre [(every? not-nil? [name image-ref flavor-ref])]}
  (let [extras #{:adminPass :security_groups :user_data :availability_zone :networks :fixedip :metadata
                 :personality :config_drive :min_count :max_count}
        convert (fn [m pair]
                  (assoc m (first pair) (second pair)))
        pairs (for [x opts :when (contains? extras (first x))] x)
        required {:name name :imageRef image-ref :flavorRef flavor-ref}]
     {:server (reduce convert required pairs)}))


(defn server-create [auth & {:keys [body query-params]}]
  "Creates a new server instance"
  (let [bodyj (cheshire.core/generate-string body)
        req (ks/make-rest :auth auth, :svc-name :nova, :url-end "servers", :method :post, :body bodyj
                          :query-params query-params)]
    (sr/send-request req)))


(defn flavors-list [auth & {:keys [query-params]}]
  "Gets all the flavors"
  (let [req (ks/make-rest :auth auth :svc-name :nova :url-end "flavors" :method :get
                          :query-params query-params  )]
    (sr/send-request req)))


(defn find-flavor [flavors & {:keys [id name]}]
  "Returns a lazy sequence of matching flavors based on id or name"
  (let [flavs (flavors "flavors")
        fltr  (if id
                #(= id (% "id"))
                #(= name (% "name")))]
    (for [f flavs :when (fltr f)]
      f)))


(defn flavors-details [auth & {:keys [query-params]}]
  "Gets all the flavors"
  (let [req (ks/make-rest :auth auth :svc-name :nova :url-end "flavors/detail" :method :get
                          :query-params query-params  )]
    (sr/send-request req)))


(defrecord Flavor [^String name ^Integer ram ^Integer vcpus ^Integer disk ^String id
                   ^Boolean os-flavor-access:is_public])
(defn make-flavor [& {:keys [name
                             ram
                             vcpus
                             disk
                             id
                             swap
                             ephemeral
                             os-flavor-access:is_public
                             rxtx_factor]
                      :or {ram 1024 vcpus 1 disk 10 swap "" ephemeral 0 rxtx_factor 1.0
                           os-flavor-access:is_public true}}]
  {:pre [(not-nil? name) (not-nil? id)]}
  (Flavor. name ram vcpus disk id os-flavor-access:is_public))


(defn flavor-create-private [auth & {:keys [body]}]
  (let [b {"flavor" body}
        req (ks/make-rest :auth auth :svc-name :nova :url-end "flavors" :method :post
                          :body (cheshire.core/generate-string b))]
    (sr/send-request req)))


(defn valid-extra-specs []
  #{"vcpus" "mem" "hw:numa_nodes" "hw:numa_cpus." "hw:numa_mem" "hw:numa_mempolicy"})


(defn flavor-extra-specs-list [auth flavor-id]
  "Lists any extra os-specs for a specified flavor

  Args:
    - auth: authentication map"
  (let [route (format "flavors/%d/os-extra_specs" flavor-id)
        req (ks/make-rest :auth auth :svc-name :nova :url-end route
                          :method :get)]
    (sr/send-request req)))


(defn flavor-extra-specs-create [auth flavor-id & {:keys [body]}]
  "Creates extra specs for a flavor

  Args:
    - auth: authentication map (from ks/authorize)
    - flavor-id: The flavor to add the extra specs to
    - body: a map of extra key:value pairs"
  (let [specs {"extra_specs" body}
        route (format "flavors/%d/os-extra_specs" flavor-id)
        req (ks/make-rest :auth auth :svc-name :nova :url-end route
                          :method :post :body (cheshire.core/generate-string specs))]
    (sr/send-request req)))