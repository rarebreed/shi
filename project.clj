(defproject shi "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/tools.logging "0.3.0"]
                 [http-kit "2.1.18"]
                 [cheshire "5.3.1"]
                 [clj-ssh "0.5.11"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [org.clojars.hozumi/clj-commons-exec "1.1.0"]]
  :aot :all
  :main shi.testing)
