(ns shi.test_commander
  (:require [speclj.core :refer :all])
  (:require [shi.api.keystone :as ks])
  (:require [shi.environment.config :as cfg]))

; The Credentials object used for Keystone v2 testing
(def cred (Credentials. (cfg/config :user-name)
                        (cfg/config :user-pass)
                        "demo"
                        (cfg/config :auth-url)))
