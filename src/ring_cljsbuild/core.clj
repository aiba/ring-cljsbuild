(ns ring-cljsbuild.core
  (:require [clojure.string :as string]
            [clojure.pprint :refer [pprint]]
            [ring.util.response :as response]
            [cljsbuild.compiler :as compiler]
            [clojure.java.io :as io]
            [digest :as digest]
            [clj-stacktrace.repl :refer [pst+]]
            [ring-cljsbuild.filewatcher :as filewatcher]
            [ring-cljsbuild.utils :refer [logtime with-logs debounce]]))

;; calls to compile hold a lock on the destination (output) directory path.
(defonce compile-locks* (atom {})) ;; map of String -> Object
(defn compile-lock [dst-path]
  (locking compile-locks*
    (when-not (@compile-locks* dst-path)
      (swap! compile-locks* assoc dst-path (Object.)))
    (@compile-locks* dst-path)))

;; See lein-cljsbuild/plugin/src/leiningen/cljsbuild/config.clj
(def default-compiler-opts
  {:optimizations :whitespace
   :libs []
   :externs []
   :warnings true
   :pretty-print true})

(defn run-compiler! [opts build-dir mtimes main-js]
  (try
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
      (spit (io/file build-dir ".last-mtimes") (pr-str @mtimes)))
    (catch Exception e
      (pst+ e))))

(defn respond-with-compiled-cljs [build-dir path]
  (-> (slurp (io/file build-dir path))
      (response/response)
      (response/content-type "application/javascript")))

(defn parse-path-spec [p]
  (let [parts (.split p "\\/")]
    [(str (string/join "/" (butlast parts)) "/")
     (last parts)]))

(defonce ^:private filewatchers* (atom {})) ;; map id -> vec of watchers.

(defn clear-watchers! [id]
  (swap! filewatchers*
         (fn [watchers]
           (doseq [w (watchers id)]
             (filewatcher/stop! w))
           (assoc watchers id []))))

(defn watch-source-dirs! [id dirs cb]
  (letfn [(file-event? [[_ f]]
            (and (.isFile f)
                 (not (.isHidden f))))]
    (doseq [d dirs]
      (let [w (filewatcher/watch! d
                                  (fn [events]
                                    (when (some file-event? events)
                                      (cb))))]
        (swap! filewatchers*
               (fn [watchers]
                 (update-in watchers [id]
                            (fn [v]
                              (vec (conj v w))))))))))

(defn with-message-logging [logs? f]
  (if logs?
    (with-logs 'ring-cljsbuild (f))
    (f)))

(defn wrap-cljsbuild [handler pathspec opts]
  (let [target-dir (doto (io/file "./target") (.mkdir))
        base-dir (doto (io/file target-dir "ring-cljsbuild") (.mkdir))
        [path-prefix main-js] (parse-path-spec pathspec)
        build-dir (doto (io/file base-dir (digest/md5 (pr-str [pathspec opts])))
                    (.mkdir))
        lock (compile-lock (.getCanonicalPath build-dir))
        mtimes-file (io/file build-dir ".last-mtimes")
        mtimes (atom (when (.exists mtimes-file)
                       (read-string (slurp mtimes-file))))
        compile! (fn []
                   (with-message-logging (:log-messages opts)
                     (fn [] (run-compiler! opts build-dir mtimes main-js))))]
    (clear-watchers! (:id opts))
    (watch-source-dirs! (:id opts)
                        (:source-paths opts)
                        (-> (fn []
                              (locking lock (compile!)))
                            (debounce 5)))
    (future (locking lock (compile!)))
    (fn [req]
      (if (.startsWith (:uri req) path-prefix)
        (locking lock
          (compile!)
          (respond-with-compiled-cljs build-dir
                                      (.substring (:uri req) (.length path-prefix))))
        (handler req)))))

(comment

  ((:dev @filewatchers*) 0)

  )
