(ns ring-cljsbuild.core
  (:require [clojure.string :as string]
            [clojure.pprint :refer [pprint]]
            [clojure.tools.logging :as log]
            [ring.util.response :as response]
            [cljsbuild.compiler :as compiler]
            [digest :as digest]
            [clj-stacktrace.repl :refer [pst+]]
            [ring-cljsbuild.jnio :as jnio]
            [ring-cljsbuild.filewatcher :as filewatcher]
            [ring-cljsbuild.utils :refer [logtime with-logs debounce]]))

;; NOTE: parallel cljsbuild compiliation disabled because new cljs compiler
;; doesn't appear to be threadsafe.  Revist later.
(defonce ^:private global-compile-lock* (Object.))
(defn ^:private compile-lock [_] global-compile-lock*)

;; See lein-cljsbuild/plugin/src/leiningen/cljsbuild/config.clj
(def ^:private default-compiler-opts
  {:optimizations :whitespace
   :libs          []
   :externs       []
   :warnings      true
   :pretty-print  false})

(defn- update-source-map [m sm?]
  (if sm?
    (assoc m :source-map (str (:output-to m) ".map"))
    (dissoc m :source-map)))

(def ^:private empty-dir
  (memoize (fn [build-dir]
             (-> (doto (jnio/npath build-dir "empty")
                   (jnio/mkdirs))
                 (str)))))

(defn- run-compiler! [opts build-dir mtimes main-js source-map?]
  (try
    (let [full-opts (-> default-compiler-opts
                        (merge (:compiler opts))
                        (merge {:output-to (str (jnio/npath build-dir main-js))
                                :output-dir (str (jnio/npath build-dir "out"))})
                        (update-source-map source-map?))
          new-mtimes (compiler/run-compiler
                      (:source-paths opts)
                      []    ;; checkout paths?
                      (empty-dir build-dir) ;; crossover path
                      []       ;; crossover-macro-paths
                      full-opts
                      nil ;; notify-commnad
                      (:incremental opts)
                      (:assert opts)
                      @mtimes
                      false ;; don't run forever watching the build
                      )]
      (when (not= @mtimes new-mtimes)
        (reset! mtimes new-mtimes)
        (jnio/write-str! (jnio/npath build-dir ".last-mtimes") (pr-str @mtimes))))
    (catch Exception e
      (pst+ e))))

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

(defn with-message-logging [java-logging? f]
  (if java-logging?
    (with-logs 'ring-cljsbuild (f))
    (f)))

(defn wrap-cljsbuild [handler pathspec opts]
  (let [id pathspec ;; unique identifier of this build
        target-dir (doto (jnio/npath "." "target")
                     (jnio/mkdirs))
        base-dir (doto (jnio/npath target-dir "ring-cljsbuild")
                   (jnio/mkdirs))
        [path-prefix main-js] (parse-path-spec pathspec)
        build-dir (doto (jnio/npath base-dir (digest/md5 (pr-str [pathspec opts])))
                    (jnio/mkdirs))
        lock (compile-lock (str (jnio/npath build-dir ".")))
        mtimes (let [p (jnio/npath build-dir ".last-mtimes")]
                 (atom (when (jnio/exists? p)
                         (read-string (String. (jnio/read-bytes p))))))
        compile! (fn []
                   (with-message-logging (:java-logging opts)
                     (fn [] (run-compiler! (:cljsbuild opts)
                                          build-dir
                                          mtimes
                                          main-js
                                          (:public-source-map opts)))))]
    (clear-watchers! id)
    (when (:auto opts)
      (future (locking lock (compile!)))
      (watch-source-dirs! id
                          (get-in opts [:cljsbuild :source-paths])
                          (-> (fn []
                                (locking lock (compile!)))
                              (debounce 5))))
    (fn [req]
      (if-not (.startsWith (:uri req) path-prefix)
        (handler req)
        (let [relpath (.substring (:uri req) (.length path-prefix))
              p       (jnio/npath build-dir relpath)]
          (locking lock
            (compile!)
            (if (and (jnio/exists? p)
                     (or (= (:uri req) pathspec)
                         (:public-source-map opts)))
              (-> (jnio/input-stream p)
                  (response/response)
                  (response/content-type "text/javascript"))
              (-> (response/response "404 not found")
                  (response/content-type "text/plain")
                  (response/status 404)))))))))

;; Testing —————————————————————————————————————————————————————————————————————
(comment
  ((:dev @filewatchers*) 0)
  (log/info "hello")
  (empty-dir "/tmp/empty")
  )
