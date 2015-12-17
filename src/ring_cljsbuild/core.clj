(ns ring-cljsbuild.core
  (:require [clojure.tools.logging :as log]
            [clojure.string :as string]
            [ring.util.response :as response]
            [ring-cljsbuild.builder :as builder]))

(defn wrap-cljsbuild [handler urlpath build-spec]
  (let [builder     (builder/new-builder build-spec)
        path-prefix (as-> urlpath $
                      (string/split $ #"/")
                      (butlast $)
                      (string/join "/" $)
                      (str $ "/"))]
    (log/info "path-prefix:" path-prefix)
    (fn [req]
      (if-not (.startsWith (:uri req) path-prefix)
        (handler req)
        (let [relpath (as-> (:uri req) $
                        (.substring $ (.length path-prefix)))
              _       (log/info "relpath:" relpath)
              data    (builder/get-file-bytes builder relpath)]
          (-> (java.io.ByteArrayInputStream. data)
              (response/response)
              (response/content-type "text/javascript")))))))
