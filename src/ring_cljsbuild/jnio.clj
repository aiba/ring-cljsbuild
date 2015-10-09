(ns ring-cljsbuild.jnio
  (:import (java.nio.file Files
                          Paths
                          Path
                          OpenOption
                          LinkOption)
           (java.nio.file.attribute FileAttribute)))

(defn npath [dir ^String p]
  (.normalize
   (Paths/get (str dir)
              (into-array [p]))))

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

;; Testing —————————————————————————————————————————————————————————————————————
(comment
  (write-str! (npath "/tmp/" "x") "hello again.")
  (exists? (npath "/tmp/x" "x"))
  (str (npath "/tmp" "."))
  (let [p (npath "/tmp/" "data.clj")]
    (write-str! p "{:a 1}")
    (read-string (String. (slurp (input-stream p)))))
  )
