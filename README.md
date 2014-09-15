# ring-cljsbuild

Ring middleware that compiles ClojureScript and serves the JS.

This is an alternative to [lein-cljsbuild](https://github.com/emezeske/lein-cljsbuild)
when your clojurescript is being served from a
[ring-clojure](https://github.com/ring-clojure/ring) server.

This is an experiment. I'm still not sure if it's a good idea, but here are some
potential benefits:

## Benefits

* No extra JVM process. Since you already have a JVM process running for the ring
  server, this will use the same one to compile the CLJS.
* Consequently, you can change the cljsbuild configuration without restarting any JVM
  processes.
* Instead of an extra terminal to monitor for compiler errors, ring-cljsbuild will use
  clojure.tools.logging to log compiler errors to the same place your server-side errors
  go. Thus, you only need to monitor one log file.
* This library compiles and serves the clojurescript/javascript when the browsers
  requests it, so after reloading the browser, there's no wondering, "did it pick up the
  latest version of my clojurescript code?" The answer is always yes.
* The cljs compiler options can be in the same .clj file as the code that includes the
  compiled files, so changing the prefix or the name of the main class can all happen in
  the same file.  See e.g. [server.clj](src-test/ring_cljsbuild/test/server.clj).

