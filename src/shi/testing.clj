(require '[shi.api.keystone :as ks])
(require '[shi.environment.config :as cfg])
(require '[clojure.pprint :as cpp])
(require '[clojure.reflect :as refl])
(require '[cheshire.core :as ches])

(def cred3 (ks/make-creds-v3 :userid (cfg/config :user-name)
                             :auth-url (cfg/config :auth-url)
                             :authmethod "password"
                             :secret (cfg/config :user-pass)))
(def resp (ks/authorize cred3))

(let [r (ks/parse-resp resp)
      catalog (ks/get-catalog r)
      token (ks/get-token r)]
  (println "Response: " r)
  (println "Catalog: " catalog)
  (println "Token: " token))


