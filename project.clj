(defproject ring-cljsbuild "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2322"]
                 [cljsbuild "1.0.3"]]
  :profiles {:dev {:plugins [[cider/cider-nrepl "0.7.0"]]}}
  :target-path "target/%s/")

