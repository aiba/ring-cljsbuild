# ring-cljsbuild

Ring middleware that compiles ClojureScript and serves the JS directly from your
ring-clojure webserver.

This is an alternative to runing a separate command and separate JVM with
[lein-cljsbuild](https://github.com/emezeske/lein-cljsbuild).

## Benefits (vs. lein-cljsbuild)

* No extra JVM process / terminal window
* Make cljsbuild configuration changes without any JVM restarts.
* cljs compiler errors logged in same place as server-side errors, so there's only 1 log
  file to monitor.  (We use tools.logging).
* Blocks HTTP requests until cljs compiler is done, so you can be certain you have
  latest js.
* cljs compiler options can live in same file that serves and calls the resultant js.
  See e.g. [server.clj](src-test/ring_cljsbuild/test/server.clj).

## Example

First add ring-cljsbuild as a dependency to `project.clj`.

```clj
(defproject ring-cljsbuild-example "0.0.1"
  :dependencies [[ring-cljsbuild "0.2.0"]])
```

As with lein-cljsbuild, you should add an explicit ClojureScript dependency as well:

```clj
:dependencies [[org.clojure/clojurescript "0.0-XXXX"]]
```

The ClojureScript compiler is configured and served using
[ring middleware](https://github.com/ring-clojure/ring/wiki/Concepts#middleware), all in
the same file as your main ring webserver. The following example is a complete webserver
that serves compiled clojurescript:

__TODO: make an entire self-contained example that can be super easily git cloned and
    started up.___

```clj
(ns ring-cljsbuild.test.server
  (:require
```

## Drawbacks of this approach

* It slightly bloats the server because it adds all the ClojureScript compiler code as a
  dependency.
* It doesn't recompile clojurescript until your browser reloads the page, so this could
  delay feedback about a compiler error. (The flip side of this is that it can reduce
  unecessary compiles).

## Implementation Issues

* The use of files in "/tmp/" is sketchy and it assumes a unix filesystem. This should
  be cleaned up.
* Crossovers are not supported (but these seem to be a deprecated concept anyway). The
  use of `emptydir` as a crossovers arg to `run-compiler` is particularly sketchy.
* The compiler prints to stdout, but arguably this is an issue with the cljsbuild
  library. (Library functions probably shouldn't print to stdout). We could try wrapping
  this in `with-out-str`.
* The `:output-to` filename is hard-coded to `"main.js"`. Perhaps this should be
  configurable.

