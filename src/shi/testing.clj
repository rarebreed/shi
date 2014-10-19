(require '[shi.api.keystone :as ks])
(require '[shi.api.glance :as gl])
(require '[shi.api.nova :as nova])
(require '[shi.environment.config :as cfg])
(require '[clojure.pprint :as cpp])
(require '[clojure.reflect :as refl])
(require '[cheshire.core :as ches])
(require '[org.httpkit.client :as http])

(def cred3 (ks/make-creds-v3 :userid (cfg/config :user-name)
                             :auth-url (cfg/config :auth-url)
                             :authmethod "password"
                             :secret (cfg/config :user-pass)))
(def resp (ks/authorize cred3))
(def auth (ks/parse-resp resp))

(let [catalog (ks/get-catalog auth)
      token (ks/get-token auth)
      images (gl/image-list auth)]
  (println "Response: " auth)
  (println "Catalog: " catalog)
  (println "Token: " token)
  (print "Images: ")
  (cpp/pprint images))