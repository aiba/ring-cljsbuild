(ns ring-cljsbuild.core
  (:require [clojure.tools.logging :as log]
            [clojure.string :as string]
            [clojure.pprint :refer [pprint]]
            [ring.util.response :as response]
            [cljsbuild.compiler :as compiler]
            [clojure.java.io :as io]
            [digest :as digest]
            [ring-cljsbuild.filewatcher :as filewatcher]
            [ring-cljsbuild.utils :refer [logtime with-logging-system-out]]))

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

(defn compile! [opts build-dir mtimes main-js]
  (with-logging-system-out "cljsbuild"
    (let [emptydir (.getCanonicalPath
                    (doto (io/file build-dir "empty") (.mkdir)))
          new-mtimes (compiler/run-compiler
                      (:source-paths opts)
                      emptydir ;; TODO: support crossover?
                      []       ;; TODO: support crossover?
                      (merge  default-compiler-opts
                              (:compiler opts)
                              {:output-to (.getCanonicalPath
                                           (io/file build-dir main-js))
                               :output-dir (.getCanonicalPath
                                            (io/file build-dir "out"))})
                      nil ;; notify-commnad
                      (:incremental opts)
                      (:assert opts)
                      @mtimes
                      false ;; don't run forever watching the build
                      )]
      (reset! mtimes new-mtimes)
      (spit (io/file build-dir ".last-mtimes") (pr-str @mtimes)))))

(defn respond-with-compiled-cljs [path opts build-dir mtimes main-js]
  (locking compile-lock*
    (compile! opts build-dir mtimes main-js)
    (-> (slurp (io/file build-dir path))
        (response/response)
        (response/content-type "application/javascript"))))

(defn parse-path-spec [p]
  (let [parts (.split p "\\/")]
    [(str (string/join "/" (butlast parts)) "/")
     (last parts)]))

(defn clear-builds! []
  #_(filewatcher/clear-all!))

(defn wrap-cljsbuild [handler pathspec opts]
  (let [target-dir (doto (io/file "./target") (.mkdir))
        base-dir (doto (io/file target-dir "ring-cljsbuild") (.mkdir))
        [path-prefix main-js] (parse-path-spec pathspec)
        build-dir (doto (io/file base-dir (digest/md5 (pr-str [pathspec opts])))
                    (.mkdir))
        mtimes-file (io/file build-dir ".last-mtimes")
        mtimes (atom (when (.exists mtimes-file)
                       (read-string (slurp mtimes-file))))]
    (log/info "ring-cljsbuild: cljs build dir: " (.getCanonicalPath build-dir))
    ;; set up watcher
    #_(filewatcher/watch-dirs! (:source-paths opts)
                               (fn []
                                 (log/info "detected source path file change.")
                                 (locking compile-lock*
                                   (compile! opts build-dir mtimes main-js))))
    (fn [req]
      (if (.startsWith (:uri req) path-prefix)
        (respond-with-compiled-cljs
         (.substring (:uri req) (.length path-prefix)) opts build-dir mtimes main-js)
        (handler req)))))
