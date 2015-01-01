(ns ring-cljsbuild.test.client)

(defn ^:export main []
  (aset (js/document.getElementById "main") "innerHTML" "Hello from clojurescript!"))

