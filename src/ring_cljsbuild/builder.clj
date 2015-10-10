(ns ring-cljsbuild.builder
  (:require [clojure.tools.logging :as log]
            [clj-stacktrace.repl :refer [pst+]]
            [cljsbuild.compiler :as compiler]
            [ring-cljsbuild.jnio :as jnio]
            [ring-cljsbuild.utils :refer [logtime with-logs]]
            [digest :as digest]))

;; NOTE: parallel cljsbuild compiliation disabled because new cljs compiler
;; doesn't appear to be threadsafe.  Revist later.
(defonce ^:private global-compile-lock* (Object.))

(def main-file-name "main.js")
(def mtimes-file-name ".last-mtimes")

;; See lein-cljsbuild/plugin/src/leiningen/cljsbuild/config.clj
(def ^:private default-compiler-opts
  {:optimizations :whitespace
   :libs          []
   :externs       []
   :warnings      true
   :pretty-print  false})

(def ^:private empty-dir
  (memoize (fn [build-dir]
             (-> (doto (jnio/npath build-dir "empty")
                   (jnio/mkdirs))
                 (str)))))

(defn- update-source-map [m sm?]
  (if sm?
    (assoc m :source-map (str (:output-to m) ".map"))
    (dissoc m :source-map)))

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
        (jnio/write-str! (jnio/npath build-dir mtimes-file-name)
                         (pr-str @mtimes))))
    (catch Exception e
      (pst+ e))))

(defmacro with-message-logging [java-logging? & body]
  `(if ~java-logging?
     (with-logs 'ring-cljsbuild (do ~@body))
     (do ~@body)))

(defn gen-compile-fn [build-spec build-dir mtimes]
  (let [src-paths     (get-in build-spec [:cljsbuild :source-paths])
        xover         (empty-dir build-dir)
        compiler-opts (-> default-compiler-opts
                          (merge (get-in build-spec [:cljsbuild :compiler]))
                          (assoc :output-to (str (jnio/npath build-dir main-file-name))
                                 :output-dir (str (jnio/npath build-dir "out")))
                          (update-source-map (:source-map build-spec)))
        incremental?  (get-in build-spec [:cljsbuild :incremental])
        assert?       (get-in build-spec [:cljsbuild :assert])]
    (fn []
      (with-message-logging (:java-logging build-spec)
        (try
          (let [mtimes' (compiler/run-compiler src-paths
                                               [] ;; checkout paths?
                                               xover ;; crossover path
                                               [] ;; crossover-macro-paths
                                               compiler-opts
                                               nil ;; notify-command
                                               incremental?
                                               assert?
                                               @mtimes
                                               false ;; dont run forever watching
                                               )]
            (when (not= @mtimes mtimes')
              (reset! mtimes mtimes')
              (jnio/write-str! (jnio/npath build-dir mtimes-file-name)
                               (pr-str @mtimes))))
          (catch Throwable t
            (pst+ t)))))))

(defn gen-build-dir [build-spec]
  (doto (jnio/npath "."
                    "target"
                    "ring-cljsbuild"
                    (digest/md5 (pr-str build-spec)))
    (jnio/mkdirs)))

(defn gen-mtimes [build-dir]
  (let [p (jnio/npath build-dir ".last-mtimes")]
    (atom (when (jnio/exists? p)
            (read-string (String. (jnio/read-bytes p)))))))

;; API —————————————————————————————————————————————————————————————————————————

(defn new-builder [build-spec]
  (when-not (:id build-spec)
    (throw (IllegalArgumentException. "build-spec must contain :id key")))
  (let [build-dir (gen-build-dir build-spec)
        mtimes    (gen-mtimes build-dir)
        compile!  (gen-compile-fn build-spec build-dir mtimes)]
    {:compile-fn compile!
     :js-fn      (fn []
                   (jnio/read-bytes (jnio/npath build-dir
                                                main-file-name)))
     :srcmap-fn  (fn []
                   (when (:source-map build-spec)
                     (jnio/read-bytes (jnio/npath build-dir
                                                  (str main-file-name
                                                       ".map")))))}))

(defn compile! [{:keys [compile-fn]}]
  (locking global-compile-lock*
    (compile-fn)))

(defn get-js [{:keys [compile-fn js-fn]}]
  (locking global-compile-lock*
    (compile-fn)
    (js-fn)))

(defn get-srcmap [{:keys [srcmap-fn]}]
  (locking global-compile-lock*
    (srcmap-fn)))

;; Testing —————————————————————————————————————————————————————————————————————

(comment

  )

;; TODO:
;; - keep bytes in ram
;; - js-hash

