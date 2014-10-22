(ns shi.api.glance
  (:require [org.httpkit.client :as http])
  (:require [shi.environment.config :as cfg])
  (:require [shi.common.common :refer [sanitize-url not-nil?]])
  (:require [shi.api.keystone :as ks])
  (:require [shi.rest :as sr])
  (:require [cheshire.core])
  (:require [clojure.tools.logging :as log])
  (:require [clojure.pprint :as pp]))


(defrecord Image [owner container-format disk-format tags scheme id protected visibility name
                  updated-at status min_ram self created-at file size checksum min-disk])


(defmacro make-image [img]
  "Given a map representing an image (as are contained in the returned sequence from image-list)
   create an Image instance"
  `(let [fields# ["owner" "container_format" "disk_format" "tags" "schema" "id" "protected" "visibility" "name"
                  "updated_at" "status" "min_ram" "self" "created_at" "file" "size" "checksum" "min-disk"]
         values# (for [x# fields#] (~img x#))]
     `(Image. ~@(vec values#))))


(defn get-versions [{:keys [url]}]
  "gets the glance api versions"
  (let [options {:headers {"Content-Type" "application/json", "Accept" "application/json"}}]
    @(http/get url options)))


(defn image-create [])


(defn image-list [auth & {:keys [query-params]}]
  (let [req (ks/make-rest :auth auth :svc-name :glance :url-end "v2/images" :method :get
                          :query-params query-params)]
    (sr/send-request req)))


(defn get-image-id [name images]
  "Returns a lazy sequence of image uuids from a given image name

  Args:
    name: The name to look for
    images: a sequence of image objects (as returned from image-list)
  "
  (filter #(= name (% "name")) images))