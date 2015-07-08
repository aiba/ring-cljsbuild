(ns ring-cljsbuild.filewatcher
  (:require [clojure.tools.logging :as log]
            [clojure.java.io :as io])
  (:import org.apache.commons.lang3.SystemUtils))

(defn osx? [] SystemUtils/IS_OS_MAC_OSX)

(defn- sym->eventkind [sym]
  (if (osx?)
    (eval `(. com.barbarysoftware.watchservice.StandardWatchEventKind ~sym))
    (eval `(. java.nio.file.StandardWatchEventKinds ~sym))))

;; map of java class to friendlier keyword.
(def ^:private kindkeys*
  (into {}
        (for [[sym k] [['ENTRY_CREATE :created]
                       ['ENTRY_MODIFY :modified]
                       ['ENTRY_DELETE :deleted]
                       ['OVERFLOW :overflowed]]]
          [(sym->eventkind sym) k])))

(defn- new-watch-service [^String path]
  (log/info "watching directory:" path)
  (if (osx?)
    (let [ws (com.barbarysoftware.watchservice.WatchService/newWatchService)
          wf (com.barbarysoftware.watchservice.WatchableFile. (io/file path))]
      (.register wf ws (into-array (keys kindkeys*)))
      ws)
    (let [ws (. (java.nio.file.FileSystems/getDefault) (newWatchService))
          wp (java.nio.file.Paths/get path (into-array String []))]
      (.register wp ws (into-array (keys kindkeys*)))
      ws)))

(defn- parse-context [we] ;; WatchEvent
  (let [ctx (.context we)]
    (cond
      (instance? java.io.File ctx) ctx
      (instance? java.nio.file.Path ctx) (.toFile ctx)
      :else (do (log/warn "unknown event context:" ctx) ctx))))

;; Converts a watch key to a list of [kind path]
(defn- parse-events [wk]
  (let [events (.pollEvents wk)]
    (mapv #(do [(kindkeys* (.kind %)) (parse-context %)])
          events)))

(defn- process-watchdir [{:keys [ws dir onevent] :as state}]
  (log/info "process-watchdir, waiting for event.")
  (let [wk (.take ws)] ; blocking
    (try
      (log/info "onevent:" wk)
      (onevent (parse-events wk))
      (catch Exception e
        (log/error e "process-watchdir-error"))
      (finally
        (.reset wk)
        (send-off *agent* process-watchdir))) ; loop
    state))

(defn watch! [^String path callback]
  (let [ws (new-watch-service path)
        a (agent {:ws ws :path path :onevent callback}
                 :error-handler #(log/error % "watchdir-agent-error")
                 :error-mode :continue)]
    (send-off a process-watchdir)
    nil))

;; Clojure interface ———————————————————————————————————————————————————————————

(def watches* (atom {}))

(defn clear-all! []
  nil)

(defn watch-dirs! [paths callback]
  nil)


(comment

  (watch! "/tmp/somedir" (fn [events] (println "events:" (pr-str events))))

  )
