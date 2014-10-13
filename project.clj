(defproject shi "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/tools.logging "0.3.0"]
                 [aleph "0.3.3"]
                 [http-kit "2.1.18"]
                 [cheshire "5.3.1"]]
  :dev-dependencies [[speclj "2.9.0"]]
  :profiles {:dev {:dependencies [[speclj "2.9.0"]]}}
  :plugins [[speclj "2.9.0"]]
  :test-paths ["spec"]
  ;:main ^:skip-aot shi.commander
  )
