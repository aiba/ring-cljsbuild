(ns ring-cljsbuild.test.server
  (:require [hiccup.core :as hiccup]
            [hiccup.page :refer [doctype include-js]]
            [hiccup.element :refer [javascript-tag]]
            [ring.util.response :as response]
            (ring.middleware stacktrace params keyword-params reload)
            [org.httpkit.server :as httpserver]
            [ring-cljsbuild.core :refer [wrap-cljsbuild]]))

(defn render-html5 [htmlv]
  (-> (hiccup/html (doctype :html5) htmlv)
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

(defn make-handler []
  (-> #'app
      (wrap-cljsbuild "/cljsbuild/dev/main.js"
                      {:id :dev
                       :log-messages false
                       :source-paths ["src-test"]
                       :incremental true
                       :assert true
                       :compiler {:optimizations :none
                                  :cache-analysis true
                                  :pretty-print true}})
      ;; TODO: add back opt build.
      #_(rcb/wrap-cljsbuild "/cljsbuild/opt/main.js"
                            {:source-paths ["src-test"]
                             :incremental true
                             :assert false
                             :compiler {:optimizations :advanced
                                        :pretty-print false}})
      (ring.middleware.keyword-params/wrap-keyword-params)
      (ring.middleware.params/wrap-params)
      (ring.middleware.reload/wrap-reload)
      (ring.middleware.stacktrace/wrap-stacktrace)))

(defonce http-stopper* (atom nil))
(def http-port* 7000)

(defn restart! []
  (swap! http-stopper*
         (fn [s]
           (when s (s :timeout 100))
           (httpserver/run-server (make-handler) {:port http-port*}))))

(defn -main []
  (restart!))

(comment
  http-stopper*
  (restart!)
  )
