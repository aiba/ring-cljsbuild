(ns ring-cljsbuild.jnio
  (:import (java.nio.file Files
                          Paths
                          Path
                          OpenOption
                          LinkOption)
           (java.nio.file.attribute FileAttribute)))

(defn npath [dir & ps]
  (.normalize
   (Paths/get (str dir)
              (into-array String ps))))

(defn mkdirs [^Path p]
  (Files/createDirectories p (into-array FileAttribute [])))

(defn write-str! [^Path p ^String s]
  (Files/write p (.getBytes s) (into-array OpenOption [])))

(defn exists? [^Path p]
  (Files/exists p (into-array LinkOption [])))

(defn read-bytes [^Path p]
  (Files/readAllBytes p))

(defn input-stream [^Path p]
  (Files/newInputStream p (into-array OpenOption [])))

(defn last-modified [^Path p]
  (-> p
      (Files/getLastModifiedTime (into-array LinkOption []))
      (.toMillis)
      (/ 1000.0)
      (int)))

;; Cached reading ——————————————————————————————————————————————————————————————

(def ^:private cache* (atom {})) ;; maps path -> {:mtime, :md5, :bytes}

(defn update-cache! [^Path p]
  (let [{:keys [mtime]} (@cache* p)
        mtime'          (last-modified p)]
    (when (not= mtime mtime')
      (clojure.tools.logging/info "reading" (str p))
      (let [b (read-bytes p)]
        (swap! cache* assoc p {:mtime mtime'
                               :bytes b
                               :md5   (digest/md5 b)})))))

(defn cached-file-bytes [^Path p]
  (update-cache! p)
  (:bytes (@cache* p)))

(defn cached-file-md5 [^Path p]
  (update-cache! p)
  (:md5 (@cache* p)))

;; Testing —————————————————————————————————————————————————————————————————————
(comment
  (write-str! (npath "/tmp/" "d" "foo") "hello again.")
  (exists? (npath "/tmp" "d" "foo"))
  (mkdirs (npath "/tmp" "d" "foo" "bar"))
  (str (npath "/tmp" "."))
  (let [p (npath "/tmp/" "data.clj")]
    (write-str! p "{:a 1}")
    (read-string (String. (slurp (input-stream p)))))

  (last-modified (npath "/tmp/x"))

  (time (cached-file-bytes (npath "/tmp/main.js")))

  (cached-file-md5 (npath "/tmp/main.js"))

  )
