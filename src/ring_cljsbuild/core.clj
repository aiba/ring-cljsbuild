(ns ring-cljsbuild.core
  (:require [clojure.tools.logging :as log]
            [ring.util.response :as response]
            [ring-cljsbuild.builder :as builder]))

;; TODO: only works for whitespac, simple, advanced... "full" builds.
(defn wrap-cljsbuild [handler urlpath build-spec]
  (let [builder (builder/new-builder build-spec)]
    (fn [req]
      (if (not= (:uri req) urlpath)
        (handler req)
        (let [data (builder/get-file-bytes builder (:main-js-name build-spec))]
          (-> (java.io.ByteArrayInputStream. data)
              (response/response)
              (response/content-type "text/javascript")))))))
