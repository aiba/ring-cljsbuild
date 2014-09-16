(ns ring-cljsbuild.core
  (:require [clojure.tools.logging :as log]
            [ring.util.response :as response]
            [cljsbuild.compiler :as compiler]
            [clojure.java.io :as io]))

;; TODO: better tempfile?
;; TODO: "/main.cljs" should be arg, not hard coded
;; TODO: use with-out-str wrapping actual compile call to get it to log to tools.logging
;;       rather than stdout?

(def compile-lock* (Object.))

(defn compile! [opts tmpdir mtimes]
  (let [emptydir (.getCanonicalPath (doto (io/file tmpdir "empty")
                                      (.mkdir)))]
    ;; The swap also prevents compiler from running on simultaneous requests.
    (swap! mtimes
           (fn [last-mtimes]
             (compiler/run-compiler (:source-paths opts)
                                    emptydir ;; TODO: support crossover?
                                    []       ;; TODO: support crossover?
                                    (merge  {:libs []
                                             :externs []}
                                            (:compiler opts)
                                            {:output-to (str tmpdir "/main.js")
                                             :output-dir (str tmpdir "/out")})
                                    nil ;; notify-commnad
                                    (:incremental opts)
                                    (:assert opts)
                                    last-mtimes
                                    false ;; don't run forever watching the build
                                    )))))

(defn respond-with-compiled-cljs [path opts tmpdir mtimes]
  (locking compile-lock*
    (compile! opts tmpdir mtimes)
    (-> (slurp (io/file tmpdir path))
        (response/response)
        (response/content-type "application/javascript"))))

(defn wrap-cljsbuild [handler path opts]
  (let [tmp-prefix "/tmp/ring-cljsbuild"
        tmpdir (str tmp-prefix "/" (System/currentTimeMillis))
        mtimes (atom nil)]
    (.mkdir (io/file tmp-prefix))
    (.mkdir (io/file tmpdir))
    (log/info "compiling cljs to: " tmpdir)
    (fn [req]
      (if (.startsWith (:uri req) path)
        (respond-with-compiled-cljs (.substring (:uri req) (.length path)) opts tmpdir mtimes)
        (handler req)))))

