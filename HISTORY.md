
# 1.0.0 (2015-08-26)

* newest lein-cljsbuild dependency
* :java-logging true will cause compiler output to go through clojure.tools.logging
* serves correct text/javascript mime type
* convenient way of specifying source maps

# 1.0.0-alpha1 (2015-07-07)

* watch filesystem for changes when :auto true
* log compiler errors
* preserve cljsbuild color output

# 0.3.0 (2015-01-02)

* Store temp build files in target/ring-cljsbuild instead of /tmp/ring-cljsbuild.
* Better documentation.

# 0.2.1 (2015-01-01)

* Example server uses :cache-analysis true for big speedup.
* Updated README with examples.

# 0.2.0 (2014-12-31)

* Updated cljsbuild dep to 1.0.4.
* Test server now shows example of optimized & dev builds coexisting.

# 0.1.1-SNAPSHOT (2014-09-18)

* Fixed a bunch of bugs, feels stable now.

# 0.1.0-SNAPSHOT (2014-09-10)

* Initial proof of concept.
