(ns ring-cljsbuild.core
  (:require [clojure.tools.logging :as log]
            [clojure.string :as string]
            [ring.util.response :as response]
            [ring-cljsbuild.builder :as builder]))

;; TODO: automatically set asset-path
;; TODO: call handler when relpath not found, rather than 500 error.

(defn wrap-cljsbuild [handler urlpath build-spec]
  (let [builder     (builder/new-builder build-spec)
        path-prefix (as-> urlpath $
                      (string/split $ #"/")
                      (butlast $)
                      (string/join "/" $)
                      (str $ "/"))]
    (fn [req]
      (if-not (.startsWith (:uri req) path-prefix)
        (handler req)
        (let [relpath (as-> (:uri req) $
                        (.substring $ (.length path-prefix)))
              data    (builder/get-file-bytes builder relpath)]
          (-> (java.io.ByteArrayInputStream. data)
              (response/response)
              (response/content-type "text/javascript")))))))
