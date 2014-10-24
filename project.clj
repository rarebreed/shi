(defproject shi "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/tools.logging "0.3.0"]
                 [http-kit "2.1.18"]
                 [cheshire "5.3.1"]]
  ;:profiles {:dev {:dependencies [[midje "1.6.3"]]}}
  ;:plugins [[speclj "3.1.0"]]
  ;:test-paths ["spec"]
  :aot :all
  :main shi.testing)
