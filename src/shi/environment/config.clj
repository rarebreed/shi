(ns shi.environment.config
  (:require [shi.common.common :refer [sanitize-url]]))


(defn conf [& {:keys [auth-url user userpass domain-id domain-name version]
               :or {auth-url (System/getenv "OS_AUTH_URL")
                    user (or (System/getenv "OS_USERNAME") (System/getenv "OS_USER_ID"))
                    userpass (System/getenv "OS_PASSWORD")
                    domain-id (System/getenv "OS_DOMAIN_ID")
                    domain-name (System/getenv "OS_DOMAIN_NAME")
                    version (System/getenv "OS_AUTH_VERSION")}}]
  "Retrieves information that will be needed for authentication

  By default, it will get information from the system environment. The keys have these meanings:

  auth-url: The keystone service endpoint (eg http://10.8.30.99:5000/v3)
  user: a string representing one of either the user id, or user name. If user name is used, the domain
        must also be specified
  userpass: Either the password for the user or v2/tokens
  domain-id: Domain id user belongs (optional if user id type is used)
  domain-name: The name of the domain user belongs to (optional if user id is used, also only one of
               domain-id or domain-name is required)
  version: the keystone version to use (ie 2 or 3)"
  (let [domain-type (cond
                      domain-id {:id domain-id}
                      domain-name {:name domain-name}
                      :else nil)
        end (cond
              (= version "3") "auth/tokens"
              :else "tokens")
        auth {:auth-url (sanitize-url auth-url end)
              :version version
              :user-name user
              :user-pass userpass}
        final (if domain-type
                (assoc auth :domain domain-type)
                auth)]
        final))

