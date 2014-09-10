(ns ring-cljsbuild.test.server
  (:require [clojure.tools.logging :as log]
            [hiccup.core :as hiccup]
            [hiccup.page :as hpage :refer [include-js]]
            [hiccup.element :refer [javascript-tag]]
            [org.httpkit.server :as httpserver]
            [ring.util.response :as response]
            (ring.middleware stacktrace)
            [ring-cljsbuild.core :refer [wrap-cljsbuild]]))

(defn render-html5 [& elts]
  (-> (hiccup/html (hpage/doctype :html5) (list* elts))
      (response/response)
      (response/content-type "text/html")
      (response/charset "utf-8")))

(defn app [req]
  (render-html5
   [:html
    [:head]
    [:body
     [:div#main "loading..."]
     (include-js "/cljsbuild/out/goog/base.js")
     (include-js "/cljsbuild/main.js")
     (javascript-tag "goog.require('ring_cljsbuild.test.client');")]]))

(defn handler []
  (-> #'app
      (wrap-cljsbuild "/cljsbuild/" {:source-paths ["src-test"]
                                     :incremental true
                                     :assert true
                                     :compiler {:optimizations :none}})
      (ring.middleware.stacktrace/wrap-stacktrace)))

(defonce stopper* (atom nil))

(defn restart! []
  (swap! stopper*
         (fn [s]
           (when s (s))
           (httpserver/run-server (handler) {:port 7000}))))

(comment
  (restart!)
  )
