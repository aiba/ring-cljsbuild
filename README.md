# ring-cljsbuild

Ring middleware that compiles ClojureScript and serves the JS directly from your
ring-clojure webserver.

This is an alternative to runing a separate command and separate JVM with
[lein-cljsbuild](https://github.com/emezeske/lein-cljsbuild).

## Benefits of this Approach (vs. lein-cljsbuild)

* No extra JVM process. Since you already have a JVM process running for the ring
  server, this will use the same one to compile the CLJS.
* Consequently, you can change the cljsbuild configuration without restarting any JVM
  processes. Just update the build options the way you would update any other part of
  your web app, and reload the page.
* Instead of an extra terminal to monitor for compiler errors, ring-cljsbuild will use
  clojure.tools.logging to log compiler errors to the same place your server-side errors
  go. Thus, you only need to monitor one log file.
* This library compiles and serves the clojurescript/javascript when the browsers
  requests it, so after reloading the browser, there's no wondering, "did it pick up the
  latest version of my clojurescript code?" The answer is always yes.
* The cljs compiler options can be in the same .clj file as the code that includes the
  compiled files, so changing the prefix or the name of the main class can all happen in
  the same file.  See e.g. [server.clj](src-test/ring_cljsbuild/test/server.clj).

## Drawbacks:

* This is only useful if you're using clojurescript in conjunction with a ring-clojure
  webserver.
* Since the cljs is recompiled when your browser request the javascript
