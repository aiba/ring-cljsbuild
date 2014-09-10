(ns ring-cljsbuild.core
  (:require [clojure.tools.logging :as log]
            [ring.util.response :as response]
            [cljsbuild.compiler :as compiler]
            [clojure.java.io :as io]))

;; TODO: only recompile when we need to?
;; TODO: better tempfile?

(defn compile! [opts tmpdir]
  (log/info "compiling...")
  (let [emptydir (.getCanonicalPath (doto (io/file tmpdir "empty")
                                      (.mkdir)))]
    (compiler/run-compiler (:source-paths opts)
                           emptydir ;; TODO: support crossover?
                           []       ;; TODO: support crossover?
                           (merge  {:libs []
                                    :externs []}
                                   (:compiler opts)
                                   {:output-to (str tmpdir "/main.js")
                                    :output-dir (str tmpdir "/out")})
                           nil                 ;; notify-commnad
                           (:incremental opts) ;; TODO: default to true?
                           (:assert opts)      ;; TODO: default to true?
                           nil                 ;; ignore mtimes for now
                           false               ;; don't run forever watching the build
                           )))

(defn respond-with-compiled-cljs [path opts tmpdir]
  (compile! opts tmpdir)
  (-> (slurp (io/file tmpdir path))
      (response/response)
      (response/content-type "application/javascript")))

(defn wrap-cljsbuild [handler path opts]
  (let [tmp-prefix "/tmp/ring-cljsbuild"
        tmpdir (str tmp-prefix "/" (System/currentTimeMillis))]
    (.mkdir (io/file tmp-prefix))
    (.mkdir (io/file tmpdir))
    (log/info "compiling cljs to: " tmpdir)
    (fn [req]
      (if (.startsWith (:uri req) path)
        (respond-with-compiled-cljs (.substring (:uri req) (.length path)) opts tmpdir)
        (handler req)))))
