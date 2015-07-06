(ns ring-cljsbuild.utils
  (:require [clojure.tools.logging :as log]))

(defmacro logtime [label & body]
  `(let [start# (System/currentTimeMillis)
         r# (do ~@body)
         elapsed# (- (System/currentTimeMillis) start#)]
     (log/info (format "%s: %d ms" ~label elapsed#))
     r#))

(defn logging-print-stream [prefix]
  (java.io.PrintStream.
   (proxy [java.io.OutputStream] []
     (close [])
     (flush [])
     (write
       ([b]
        (log/info prefix
                  (if (sequential? b)
                    (String. b)
                    (Character/toString b))))
       ([b off len]
        (let [s (-> (java.util.Arrays/copyOfRange b off (+ off len))
                    (String.)
                    (.trim))]
          (when (pos? (.length s))
            (log/info (str prefix ":") s))))))))

(defn logging-writer [prefix]
  (proxy [java.io.Writer] []
    (close [])
    (flush [])
    (write
      ([b]
       (log/info (str prefix ":" (.trim (String. b)))))
      ([chrs off len]
       (log/info (str prefix ":" (.trim (String. chrs off len))))))))

(defmacro with-logging-system-out [prefix & body]
  `(let [outsave# System/out
         errsave# System/err
         lps# (logging-print-stream ~prefix)]
     (try
       (System/setOut lps#)
       (System/setErr lps#)
       (binding [*out* (logging-writer ~prefix)]
         (do ~@body))
       (finally
         (System/setOut outsave#)
         (System/setErr errsave#)))))
