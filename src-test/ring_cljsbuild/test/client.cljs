(ns ring-cljsbuild.test.client)

(defn main []
  (aset (js/document.getElementById "main") "innerHTML" "Hello clojurescript!"))

(main)

;; foo
