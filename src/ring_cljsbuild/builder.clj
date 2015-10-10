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
(def ^:private mtimes-file-name ".last-mtimes")
(def ^:private default-main-js-name "main.js")

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
  ;; See https://github.com/clojure/clojurescript/wiki/Compiler-Options
  ;; for weird way they do this.
  (let [optlevel (:optimizations m)]
    (cond
      (= optlevel :none) (assoc m :source-map (boolean sm?))
      sm?                (assoc m :source-map (str (:output-to m) ".map"))
      :else              (dissoc m :source-map))))

(defmacro ^:private with-message-logging [java-logging? & body]
  `(if ~java-logging?
     (with-logs 'ring-cljsbuild (do ~@body))
     (do ~@body)))

(defn- gen-compile-fn [build-spec build-dir mtimes]
  (let [src-paths     (get-in build-spec [:cljsbuild :source-paths])
        mainjs        (get-in build-spec [:main-js-name] default-main-js-name)
        xover         (empty-dir build-dir)
        compiler-opts (-> default-compiler-opts
                          (merge (get-in build-spec [:cljsbuild :compiler]))
                          (assoc :output-to (str (jnio/npath build-dir mainjs))
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

(defn- gen-build-dir [build-spec]
  (doto (jnio/npath "."
                    "target"
                    "ring-cljsbuild"
                    (digest/md5 (pr-str build-spec)))
    (jnio/mkdirs)))

(defn- gen-mtimes [build-dir]
  (let [p (jnio/npath build-dir ".last-mtimes")]
    (atom (when (jnio/exists? p)
            (read-string (String. (jnio/read-bytes p)))))))

;; API —————————————————————————————————————————————————————————————————————————

(defn new-builder [build-spec]
  (when-not (:id build-spec)
    (throw (IllegalArgumentException. "build-spec must contain :id key")))
  (let [build-dir (gen-build-dir build-spec)
        mtimes    (gen-mtimes build-dir)]
    {:compile-fn
     (gen-compile-fn build-spec build-dir mtimes)
     :digest-fn
     (fn []
       (let [optlevel (get-in build-spec [:cljsbuild :compiler :optimizations])
             mainjs   (get-in build-spec [:main-js-name] default-main-js-name)]
         (if (= optlevel :none)
           (digest/md5 (apply str @mtimes))
           (jnio/cached-file-md5
            (jnio/npath build-dir mainjs)))))
     :file-bytes-fn
     (fn [relpath]
       (let [p (jnio/npath build-dir relpath)]
         (when (jnio/exists? p)
           (jnio/cached-file-bytes p))))}))

(defn compile! [{:keys [compile-fn]}]
  (locking global-compile-lock*
    (compile-fn)))

;; Digest of the WHOLE compile.  If any part of the compile changes, then
;; digest changes.
(defn get-build-digest [{:keys [compile-fn digest-fn]}]
  (locking global-compile-lock*
    (compile-fn)
    (digest-fn)))

(defn get-file-bytes [{:keys [compile-fn file-bytes-fn]} relpath]
  (locking global-compile-lock*
    (compile-fn)
    (file-bytes-fn relpath)))
