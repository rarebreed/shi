;; This is a configuration file used to get at a minimum the keystone
;; identity, authentication and service endpoints.

(ns shi.environment.config
  (:require [shi.common.common :refer [sanitize-url]]))

(def config
  (let [auth-url (System/getenv "OS_AUTH_URL")
        admin-name (System/getenv "OS_ADMIN_NAME")
        admin-pass (System/getenv "OS_PASSWORD")
        version (System/getenv "OS_AUTH_VERSION")]
      {:auth-url (sanitize-url auth-url version)
       :version version
       :admin-name admin-name
       :admin-pass admin-pass}))


