(ns shi.api.glance
  (:require [org.httpkit.client :as http])
  (:require [shi.environment.config :as cfg])
  (:require [shi.common.common :refer [sanitize-url not-nil?]])
  (:require [cheshire.core])
  (:require [clojure.tools.logging :as log])
  (:require [clojure.pprint :as pp]))


(defn get-versions [{:keys [url]}]
  "gets the glance api versions"
  (let [options {:headers {"Content-Type" "application/json", "Accept" "application/json"}}]
    @(http/get url options)))


(defn image-create [])


(defn image-list [])