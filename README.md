# ring-cljsbuild

Ring middleware interface to cljsbuild. It compiles ClojureScript and serves the JS
directly from your ring-clojure webserver, instead of requiring a separate lein command
and separate JVM as [lein-cljsbuild](https://github.com/emezeske/lein-cljsbuild) does.

Under the hood, it uses the same cljsbuild library that lein-cljsbuild uses, so the
format for specifying compiler options is the same.

## Benefits (vs. lein-cljsbuild)

* No extra JVM process / terminal window.
* cljsbuild configuration changes do not require a JVM restart.
* cljs compiler errors logged in same place as server-side errors, so there are fewer
  terminal windows to monitor.
* HTTP requests are blocked until cljs compiler is done, so you can be certain you have
  latest js.
* cljs compiler options can live in same file that serves and calls the resultant js.
  See e.g. [server.clj](example-projects/basic/src-clj/basic/server.clj).

## Drawbacks

* It slightly bloats the server because it adds all the ClojureScript compiler code as a
  dependency.
* It doesn't recompile clojurescript until your browser reloads the page, so this could
  delay feedback about a compiler error. (The flip side of this is that it can reduce
  unecessary compiles).

## Usage

Add ring-cljsbuild as a dependency to `project.clj`. As with lein-cljsbuild, you will
also want to add a specific clojurescript version dependency.

```clj
(defproject my-project "0.0.1"
  :dependencies [[org.clojure/clojurescript "0.0-XXXX"]]
                 [ring-cljsbuild "0.2.0"]])
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
      (wrap-cljsbuild "/cljsbuild/" {:source-paths ["src-cljs"]
                                     :incremental true
                                     :compiler {:optimizations :none
                                                :cache-analysis true}})))
```

The first arg to `wrap-cljsbuild` is the URL prefix from which the compiled javascript
will be served. Here the webserver will respond to "GET /cljsbuild/main.js" with the
output of the clojurescript compiler.

The second arg is the same build spec that lein-cljsbuild uses.

Finally, render a page that references the javascript.

```clj
(defn render-index [req]
  (html5
    [:body
     (include-js "/cljsbuild/out/goog/base.js")
     (include-js "/cljsbuild/main.js")]))
```

## Example

The [basic example server](example-projects/basic/src-clj/basic/server.clj) shows
having a ring webserver and clojurescript compiler in 1 file.

You can try it out:

```
$ git clone https://github.com/aiba/ring-cljsbuild.git
$ cd ring-cljsbuild/example-projects/basic
$ lein run -m basic.server 8888
```

Now visit http://localhost:8888/. As you edit either `server.clj` or
`client.cljs` and reload the page, your changes will be automatically recompiled.

## Advanced compilation

You can dynamically serve optimized compiles and unomptimized compiles from the same
webserver, via multiple calls to wrap-cljsbuild with different URL prefixes. Then you
can decide in the request handler whether to serve optimized or unoptimzed, perhaphs by
looking at a URL parameter. (TODO: provide example of this).

## Implementation Issues

* Crossovers are not supported (but these seem to be a deprecated concept anyway). The
  use of `emptydir` as a crossovers arg to `run-compiler` is particularly sketchy.
* The compiler prints to stdout, but arguably this is an issue with the cljsbuild
  library. (Library functions probably shouldn't print to stdout). We could try wrapping
  this in `with-out-str`.
* The `:output-to` filename is hard-coded to `"main.js"`. Perhaps this should be
  configurable.

## License

Source Copyright © Aaron Iba, 2014-2015.
Released under the MIT license, see [LICENSE](/LICENSE).

