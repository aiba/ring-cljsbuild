(ns ring-cljsbuild.test.server
  (:require [hiccup.core :as hiccup]
            [hiccup.page :refer [doctype include-js]]
            [hiccup.element :refer [javascript-tag]]
            [ring.util.response :as response]
            (ring.middleware stacktrace params keyword-params reload)
            [org.httpkit.server :as httpserver]
            [ring-cljsbuild.core :refer [wrap-cljsbuild]]
            [clojure.java.io :as io]
            [clj-logging-config.log4j :refer [set-loggers!]]
            [clojure.tools.logging :as log]))

(defn render-html5 [htmlv]
  (-> (hiccup/html (doctype :html5) htmlv)
      (response/response)
      (response/content-type "text/html")
      (response/charset "utf-8")))

(defn app [req]
  (render-html5
   [:html
    [:head]
    [:body
     [:div#main "loading..."]
     (include-js "/cljsbuild/main.js")]]))

(defn make-handler []
  (-> #'app
      (wrap-cljsbuild "/cljsbuild/"
                      {:id           :ws
                       :auto         true
                       :java-logging false
                       :main-js-name "main.js"
                       :source-map   false
                       :cljsbuild    {:source-paths ["src-test"]
                                      :incremental true
                                      :assert true
                                      :compiler {:optimizations :none
                                                 :pretty-print false
                                                 :cache-analysis true
                                                 :warnings true
                                                 :preamble []
                                                 :externs []
                                                 :main "ring-cljsbuild.test.client"}}})
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

(defn config-logging! []
  (let [log-file "ring-cljsbuild-test.log"]
    (set-loggers! :root {:level :info
                         :out (org.apache.log4j.FileAppender.
                               (org.apache.log4j.PatternLayout.
                                "%d{ISO8601} %-5p %c | %m%n") log-file true)})
    (println "logging to" log-file)))

(defn -main []
  (config-logging!)
  (restart!))

(comment
  (log/info "hello")
  (-main)
  (restart!)
  )
