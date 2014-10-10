(ns shi.environment.config
  (:require [shi.common.common :refer [sanitize-url]]))

(defn conf [& {:keys [auth-url username userpass domain-id domain-name version]
               :or {auth-url (System/getenv "OS_AUTH_URL")
                    username (System/getenv "OS_USER_NAME")
                    userpass (System/getenv "OS_PASSWORD")
                    domain-id (System/getenv "OS_DOMAIN_ID")
                    domain-name (System/getenv "OS_DOMAIN_NAME")
                    version (System/getenv "OS_AUTH_VERSION")}}]
  (let [domain-type (if domain-id
                       {:id domain-id}
                       {:name domain-name})]
    {:auth-url (sanitize-url auth-url "tokens")
     :domain domain-type
     :version version
     :user-name username
     :user-pass userpass}))

(def config (conf))
