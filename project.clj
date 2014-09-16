(defproject ring-cljsbuild "0.1.1-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/tools.logging "0.3.0"]
                 [cljsbuild "1.0.3"]]
  :source-paths ["src"]
  :profiles {:dev {:plugins [[cider/cider-nrepl "0.7.0"]]
                   :source-paths ["src" "src-test"]
                   :dependencies [[org.clojure/clojurescript "0.0-2322"]
                                  [ring/ring-core "1.3.1"]
                                  [ring/ring-devel "1.3.1"]
                                  [hiccup "1.0.5"]
                                  [http-kit "2.1.19"]]}}
  :target-path "target/%s/")

