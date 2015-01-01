(defproject ring-cljsbuild "0.2.0"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/tools.logging "0.3.1"]
                 [digest "1.4.4"]
                 [cljsbuild "1.0.4"]]
  :source-paths ["src"]
  :profiles {:dev {:source-paths ["src" "src-test"]
                   :dependencies [[org.clojure/clojurescript "0.0-2511"]
                                  [ring/ring-core "1.3.2"]
                                  [ring/ring-devel "1.3.2"]
                                  [hiccup "1.0.5"]
                                  [http-kit "2.1.19"]]}}
  :target-path "target/%s/")
