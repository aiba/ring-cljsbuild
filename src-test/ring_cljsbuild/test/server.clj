(ns ring-cljsbuild.test.server
  (:require [clojure.tools.logging :as log]
            [hiccup.core :as hiccup]
            [hiccup.page :as hpage :refer [include-js]]
            [hiccup.element :refer [javascript-tag]]
            [org.httpkit.server :as httpserver]
            [ring.util.response :as response]
            (ring.middleware stacktrace params keyword-params)
            [ring-cljsbuild.core :refer [wrap-cljsbuild]]))

(defn render-html5 [& elts]
  (-> (hiccup/html (hpage/doctype :html5) (list* elts))
      (response/response)
      (response/content-type "text/html")
      (response/charset "utf-8")))

(defn app [req]
  (let [dev? (nil? (-> req :params :opt))
        ijs (fn [p] (include-js (format "/cljsbuild/%s/%s" (if dev? "dev" "opt") p)))]
    (render-html5
     [:html
      [:head]
      [:body
       [:div#main "loading..."]
       (when dev? (ijs "out/goog/base.js"))
       (ijs "main.js")
       (when dev? (javascript-tag "goog.require('ring_cljsbuild.test.client');"))
       (javascript-tag "ring_cljsbuild.test.client.main();")]])))

(defn handler []
  (-> #'app
      (wrap-cljsbuild "/cljsbuild/dev/" {:source-paths ["src-test"]
                                         :incremental true
                                         :assert true
                                         :compiler {:optimizations :none
                                                    :cache-analysis true}})
      (wrap-cljsbuild "/cljsbuild/opt/" {:source-paths ["src-test"]
                                         :incremental true
                                         :assert false
                                         :compiler {:optimizations :advanced
                                                    :pretty-print false}})
      (ring.middleware.keyword-params/wrap-keyword-params)
      (ring.middleware.params/wrap-params)
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
