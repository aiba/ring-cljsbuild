(ns basic.server
  (:require [hiccup.page :refer [html5 include-js]]
            [hiccup.element :refer [javascript-tag]]
            [ring.util.response :as response]
            (ring.middleware reload stacktrace)
            [ring.adapter.jetty :refer [run-jetty]]
            [ring-cljsbuild.core :refer [wrap-cljsbuild]]))

(defn html-response [s]
  (-> s
      (response/response)
      (response/content-type "text/html")
      (response/charset "utf-8")))

(defn app [req]
  (html-response
   (html5
    [:body
     [:div#main "Loading..."]
     (include-js "/cljsbuild/out/goog/base.js")
     (include-js "/cljsbuild/main.js")
     (javascript-tag "goog.require('basic.client');")
     (javascript-tag "basic.client.main();")])))

(def handler
  (-> #'app
      (wrap-cljsbuild "/cljsbuild/" {:source-paths ["src-cljs"]
                                     :incremental true
                                     :compiler {:optimizations :none
                                                :cache-analysis true}})
      (ring.middleware.reload/wrap-reload {:dirs "src-clj"})
      (ring.middleware.stacktrace/wrap-stacktrace)))

(defn -main [p]
  (let [port (if (number? p) p (Integer/parseInt p))]
    (run-jetty #'handler {:port port :join? false})))
