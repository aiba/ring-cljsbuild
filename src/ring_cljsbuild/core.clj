(ns ring-cljsbuild.core
  (:require [clojure.tools.logging :as log]
            [cljsbuild.compiler :as compiler]))

(defn respond-with-compiled-cljs [path opts]
  ;; TODO: only recompile when we need to?
  (log/info "running compiler...")
  ;;(compiler/run-compiler )
  )

(defn wrap-cljsbuild [handler path opts]
  (fn [req]
    (if (.startsWith (:uri req) path)
      (respond-with-compiled-cljs (.substring (:uri req) (.length path)) opts)
      (handler req))))
