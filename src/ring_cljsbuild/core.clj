(ns ring-cljsbuild.core
  (:require [clojure.tools.logging :as log]
            [clojure.string :as string]
            [ring.util.response :as response]
            [ring-cljsbuild.builder :as builder]))

(defn wrap-cljsbuild [handler path-prefix build-spec]
  (let [path-prefix (if (.endsWith path-prefix "/")
                      path-prefix
                      (str path-prefix "/"))
        build-spec (update-in build-spec [:cljsbuild :compiler]
                              (fn [m]
                                (if (and (= (:optimizations m) :none)
                                         (nil? (:asset-path m)))
                                  (assoc m :asset-path (str path-prefix "out/"))
                                  m)))
        builder    (builder/new-builder build-spec)]
    (fn [req]
      (if-not (.startsWith (:uri req) path-prefix)
        (handler req)
        (let [relpath (as-> (:uri req) $
                        (.substring $ (.length path-prefix)))
              data    (builder/get-file-bytes builder relpath)]
          (if-not data
            (handler req)
            (-> (java.io.ByteArrayInputStream. data)
                (response/response)
                (response/content-type "text/javascript"))))))))
