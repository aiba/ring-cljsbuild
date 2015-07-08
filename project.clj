(defproject ring-cljsbuild "1.0.0-alpha1"
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/tools.logging "0.3.1"]
                 [digest "1.4.4"]
                 [cljsbuild "1.0.6"]
                 [org.apache.commons/commons-lang3 "3.4"]
                 [clj-stacktrace "0.2.8"]
                 [net.incongru.watchservice/barbary-watchservice "1.0"]]
  :source-paths ["src"]
  :profiles {:dev {:source-paths ["src" "src-test"]
                   :dependencies [[org.clojure/clojurescript "0.0-3308"]
                                  [ring "1.3.2"]
                                  [hiccup "1.0.5"]
                                  [http-kit "2.1.19"]]}}
  :target-path "target/%s/"
  :jvm-opts ["-server"])
