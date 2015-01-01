(ns basic.server
  (:require [hiccup.page :refer [html5 include-js]]
            [hiccup.element :refer [javascript-tag]]
            [ring.util.response :refer [response content-type charset]]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring.middleware.stacktrace :refer [wrap-stacktrace]]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring-cljsbuild.core :refer [wrap-cljsbuild]]))

(defn app [req]
  (-> (html5 [:body
              [:div#main "Loading..."]
              (include-js "/cljsbuild/out/goog/base.js")
              (include-js "/cljsbuild/main.js")
              (javascript-tag "goog.require('basic.client');")
              (javascript-tag "basic.client.main();")])
      (response)
      (content-type "text/html")
      (charset "utf-8")))

(def handler
  (-> #'app
      (wrap-cljsbuild "/cljsbuild/" {:source-paths ["src-cljs"]
                                     :incremental true
                                     :compiler {:optimizations :none
                                                :cache-analysis true}})
      (wrap-reload {:dirs "src-clj"})
      (wrap-stacktrace)))

(defn -main [p]
  (let [port (if (number? p) p (Integer/parseInt p))]
    (run-jetty #'handler {:port port :join? false})))
