(ns ring-cljsbuild.core
  (:require [clojure.tools.logging :as log]
            [clojure.pprint :refer [pprint]]
            [ring.util.response :as response]
            [cljsbuild.compiler :as compiler]
            [clojure.java.io :as io]
            [digest :as digest]))

;; TODO: better tempfile, dont assume unix filesystem
;; TODO: "/main.js" should be arg, not hard coded?
;; TODO: use with-out-str wrapping actual compile call to get it to log to tools.logging
;;       rather than stdout?

(def compile-lock* (Object.))

;; See lein-cljsbuild/plugin/src/leiningen/cljsbuild/config.clj
(def default-compiler-opts
  {:optimizations :whitespace
   :libs []
   :externs []
   :warnings true
   :pretty-print true})

;; (require 'cljsbuild.compiler)
;; (alter-var-root #'cljsbuild.compiler/run-compiler
;;                 (fn [f]
;;                   (fn [& args]
;;                     (println "compiling with options:\n")
;;                     (println (with-out-str (clojure.pprint/pprint args)))
;;                     (println "\n")
;;                     (apply f args))))

(defn compile! [opts tmpdir mtimes]
  (let [emptydir (.getCanonicalPath (doto (io/file tmpdir "empty")
                                      (.mkdir)))
        new-mtimes (compiler/run-compiler (:source-paths opts)
                                          emptydir ;; TODO: support crossover?
                                          []       ;; TODO: support crossover?
                                          (merge  default-compiler-opts
                                                  (:compiler opts)
                                                  {:output-to (str tmpdir "/main.js")
                                                   :output-dir (str tmpdir "/out")})
                                          nil ;; notify-commnad
                                          (:incremental opts)
                                          (:assert opts)
                                          @mtimes
                                          false ;; don't run forever watching the build
                                          )]
    (reset! mtimes new-mtimes)
    (spit (str tmpdir ".last-mtimes") (pr-str @mtimes))))

(defn respond-with-compiled-cljs [path opts tmpdir mtimes]
  (locking compile-lock*
    (compile! opts tmpdir mtimes)
    (-> (slurp (io/file tmpdir path))
        (response/response)
        (response/content-type "application/javascript"))))

(defn wrap-cljsbuild [handler path opts]
  (let [tmp-prefix "/tmp/ring-cljsbuild"
        tmpdir (str tmp-prefix "/" (digest/md5 (pr-str [path opts])))
        mtimes-file (io/file (str tmpdir ".last-mtimes"))
        mtimes (atom (when(.exists mtimes-file)
                       (read-string (slurp mtimes-file))))]
    (.mkdir (io/file tmp-prefix))
    (.mkdir (io/file tmpdir))
    (log/info "compiling cljs to: " tmpdir)
    (fn [req]
      (if (.startsWith (:uri req) path)
        (respond-with-compiled-cljs (.substring (:uri req) (.length path)) opts tmpdir mtimes)
        (handler req)))))

