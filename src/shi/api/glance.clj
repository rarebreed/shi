(ns shi.api.glance
  (:require [org.httpkit.client :as http])
  (:require [shi.environment.config :as cfg])
  (:require [shi.common.common :refer [sanitize-url not-nil?]])
  (:require [shi.api.keystone :as ks])
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


(defn image-list [auth-resp]
  (let [{:keys [url token]} (ks/get-rest-basics auth-resp "glance")
        req {:url (sanitize-url url "/v2/images")
             :headers {"Content-Type" "application/json",
                       "Accept" "application/json"
                       "X-Auth-Token" token}
             :method :get}
        resp @(http/request req)
        body (cheshire.core/parse-string (resp :body))
        images (body "images")]
    images))


(defn get-image-id [name images]
  "Returns a lazy sequence of image uuids from a given image name

  Args:
    name: The name to look for
    images: a sequence of image objects (as returned from image-list)
  "
  (filter #(= name (% "name")) images))