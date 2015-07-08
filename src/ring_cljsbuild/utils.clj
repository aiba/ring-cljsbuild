(ns ring-cljsbuild.utils
  (:require [clojure.tools.logging :as log]))

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

(comment
  ;; testing
  (binding [*out* (logging-writer "TEST")]
    (println "hello"))
  (log/with-logs 'ring-cljsbuild.utils
    (log/info "again2"))

  )
