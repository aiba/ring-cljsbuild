(ns ring-cljsbuild.test.client)

(set! (.-innerHTML (.getElementById js/document "main"))
      "hello from clojurescript")

