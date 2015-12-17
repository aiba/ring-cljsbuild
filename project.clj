(defproject ring-cljsbuild "2.1.0"
  :description "ClojureScript compiler as ring middleware"
  :url "https://github.com/aiba/ring-cljsbuild"
  :license {:name "Eclipse Public License - v 1.0"
            :url "http://www.eclipse.org/legal/epl-v10.html"
            :distribution :repo}
  :pedantic? :abort
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/tools.logging "0.3.1"]
                 [digest "1.4.4"]
                 [cljsbuild "1.1.1"]
                 [org.apache.commons/commons-lang3 "3.4"]
                 [clj-stacktrace "0.2.8"]
                 [net.incongru.watchservice/barbary-watchservice "1.0"]]
  :source-paths ["src"]
  :profiles {:dev {:source-paths ["src" "src-test"]
                   :dependencies [[org.clojure/clojurescript "1.7.170"]
                                  [org.clojure/tools.logging "0.3.1"]
                                  [clj-logging-config/clj-logging-config "1.9.12"]
                                  [ring "1.4.0"]
                                  [hiccup "1.0.5"]
                                  [http-kit "2.1.19"]]}}
  :target-path "target/%s/"
  :jvm-opts ["-server"])
