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
              (into-array ps))))

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
  (write-str! (npath "/tmp/" "d" "foo") "hello again.")
  (exists? (npath "/tmp" "d" "foo"))
  (mkdirs (npath "/tmp" "d" "foo" "bar"))
  (str (npath "/tmp" "."))
  (let [p (npath "/tmp/" "data.clj")]
    (write-str! p "{:a 1}")
    (read-string (String. (slurp (input-stream p)))))
  )
