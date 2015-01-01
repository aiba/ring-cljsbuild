(ns basic.client)

(defn ^:export main []
  (doto (js/document.getElementById "main")
    (aset "innerHTML" "Hello from ClojureScript!")))

