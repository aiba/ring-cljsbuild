# ring-cljsbuild

Ring middleware interface to cljsbuild. It compiles ClojureScript and serves the JS
directly from your ring-clojure webserver, instead of requiring a separate lein command
and separate JVM as [lein-cljsbuild](https://github.com/emezeske/lein-cljsbuild) does.

Under the hood, it uses the same cljsbuild library that lein-cljsbuild uses, so
compiler options are specified in the exact same way. In addition, there are
ring-cljsbuild specific options.

## Benefits (vs. lein-cljsbuild)

* No extra JVM process / terminal window.
* cljsbuild configuration changes do not require a JVM restart.
* HTTP requests are blocked until cljs compiler is done, so you can be certain
  the client received the latest compiled js.
* cljs compiler options can live in same file that serves and calls the resultant js.
  See e.g. [server.clj](src-test/ring_cljsbuild/test/server.clj).
* (optional) log4j logging so cljs compiler errors logged in same place as
  server-side errors, so there are fewer terminal windows to monitor.
* (on supported systems) uses modern inotify API so file changes are instantly
  detected and recompilation is kicked off, rather than polling every 100ms as
  lein-cljsbuild does.

## Drawbacks

* It slightly bloats the server because it adds all the ClojureScript compiler code as a
  dependency.

## Usage

Add ring-cljsbuild as a dependency to `project.clj`. As with lein-cljsbuild, you will
also want to add a specific clojurescript version dependency.

```clj
(defproject my-project "0.0.1"
  :dependencies [[org.clojure/clojurescript "0.0-XXXX"]]
                 [ring-cljsbuild "X.X.X"]])
```

Next require `wrap-cljsbuild` middleware.

```clj
(ns my-ring-server
  (:require [[ring-cljsbuild.core :refer [wrap-cljsbuild]]]))
```

Then add a call to `wrap-cljsbuild`.

```clj
(def app
  (-> #'handler
      (wrap-cljsbuild "/cljsbuild/"
                      {:id :myapp
                       :auto true ;; recompile when files change on disk
                       :java-logging false ;; log to stdout instead of log4j
                       :main-js-name "main.js"
                       :source-map true
                       :cljsbuild {:source-paths ["src-cljs"]
                                   :incremental true
                                   :compiler {:optimizations :none
                                              :cache-analysis true
                                              :pretty-print false
                                              :warnings true
                                              :main "ring-cljsbuild.test.client"}}))))
```

The first arg to `wrap-cljsbuild` is the URL prefix from which the compiled javascript
will be served. Here the webserver will respond to "GET /cljsbuild/main.js" with the
output of the clojurescript compiler.

The second arg is a map of options. Within this map, :cljsbuild takes arguments
the same way lein-cljsbuild does, and within that, :compiler takes a map of
options
[documented here](https://github.com/clojure/clojurescript/wiki/Compiler-Options).

Finally, render a page that references the javascript.

```clj
(defn render-index [req]
  (html5
    [:body
     (include-js "/cljsbuild/main.js")]))
```

<!-- ## Example -->

<!-- The [basic example server](example-projects/basic/src-clj/basic/server.clj) shows -->
<!-- having a ring webserver and clojurescript compiler in 1 file. -->

<!-- You can try it out: -->

<!-- ``` -->
<!-- $ git clone https://github.com/aiba/ring-cljsbuild.git -->
<!-- $ cd ring-cljsbuild/example-projects/basic -->
<!-- $ lein run -m basic.server 8888 -->
<!-- ``` -->

<!-- Now visit http://localhost:8888/. As you edit either `server.clj` or -->
<!-- `client.cljs` and reload the page, your changes will be automatically recompiled. -->

## Advanced compilation

You can dynamically serve optimized compiles and unomptimized compiles from the same
webserver, via multiple calls to wrap-cljsbuild with different URL prefixes. Then you
can decide in the request handler whether to serve optimized or unoptimized, perhaps by
looking at a URL parameter. (TODO: provide example of this).

## Implementation Issues

* Crossovers are not supported (but these seem to be a deprecated concept
  anyway).  cljc works fine.

## Related work and discussion

* https://github.com/brandonbloom/cljsd
* https://twitter.com/BrandonBloom/status/525053394059157504
* https://github.com/hiredman/nrepl-cljs-middleware

## License

Source Copyright © Aaron Iba, 2014-2015.
Distributed under the Eclipse Public License, the same as Clojure uses.
See [LICENSE](/LICENSE).
