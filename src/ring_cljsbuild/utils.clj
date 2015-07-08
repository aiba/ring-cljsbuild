(ns ring-cljsbuild.utils
  (:require [clojure.tools.logging :as log]
            [clojure.tools.logging.impl :as logimpl]))

(defmacro logtime [label & body]
  `(let [start# (System/currentTimeMillis)
         r# (do ~@body)
         elapsed# (- (System/currentTimeMillis) start#)]
     (log/info (format "%s: %d ms" ~label elapsed#))
     r#))

;; debounce from https://gist.github.com/loganlinn/4719107

(defn- debounce-future
  "Returns future that invokes f once wait-until derefs to a timestamp in the past."
  [f wait wait-until]
  (future
    (loop [wait wait]
      (Thread/sleep wait)
      (let [new-wait (- @wait-until (System/currentTimeMillis))]
        (if (pos? new-wait)
          (recur new-wait)
          (f))))))

(defn debounce
  "Takes a function with no args and returns a debounced version.
  f does not get invoked until debounced version hasn't been called for `wait` ms.
  The debounced function returns a future that completes when f is invoked."
  [f wait]
  (let [waiting-future (atom nil)
        wait-until (atom 0)]
    (fn []
      (reset! wait-until (+ (System/currentTimeMillis) wait))
      (locking waiting-future
        (let [fut @waiting-future]
          (if (or (not (future? fut)) (future-done? fut))
            (reset! waiting-future (debounce-future f wait wait-until))
            fut))))))


;; log-stream from clojure.tools.logging, but replaces control characters.
(defn log-stream [level logger-ns]
  (let [logger (logimpl/get-logger log/*logger-factory* logger-ns)]
    (java.io.PrintStream.
     (proxy [java.io.ByteArrayOutputStream] []
       (flush []
         ;; deal with reflection in proxy-super
         (let [^java.io.ByteArrayOutputStream this this]
           (proxy-super flush)
           (let [message (-> (.toString this)
                             (.replaceAll "\u001B\\[[;\\d]*m" "")
                             (.trim))]
             (proxy-super reset)
             (if (pos? (.length message))
               (log/log* logger level nil message))))))
     true)))

(defmacro with-logs [lns & body]
  `(binding [*out* (java.io.OutputStreamWriter. (log-stream :info ~lns))
             *err* (java.io.OutputStreamWriter. (log-stream :error ~lns))]
     (do ~@body)))


(comment
  ;; testing
  (binding [*out* (logging-writer "TEST")]
    (println "hello"))

  (do
    (def reset-color "\u001b[0m")
    (def foreground-red "\u001b[31m")
    (def foreground-green "\u001b[32m")

    (defn- colorizer [c]
      (fn [& args]
        (str c (apply str args) reset-color)))

    (def red (colorizer foreground-red))
    (def green (colorizer foreground-green))

    (with-logs 'ring-cljs
      (println "-----------")
      (println "normal")
      (println (red "ascii stuff"))
      (println (green "ascii stuff+++"))))
  )
