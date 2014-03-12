(defproject erinite "0.1.0-SNAPSHOT"
  :description "Framework web applications"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :jar-exclusions [#"\.cljx|\.swp|\.swo|\.DS_Store"]
  :source-paths ["src/clj" "target/generated-src/clj" "target/generated-src/cljs"]
  :test-paths ["test" "target/generated-test"]
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/core.async "0.1.267.0-0d7780-alpha"]]

  :cljx {:builds [{:source-paths ["src/cljx"]
                   :output-path "target/generated-src/clj"
                   :rules :clj}

                  {:source-paths ["src/cljx"]
                   :output-path "target/generated-src/cljs"
                   :rules :cljs}

                  {:source-paths ["test/cljx"]
                   :output-path "target/generated-test"
                   :rules :clj}

                  {:source-paths ["test/cljx"]
                   :output-path "target/generated-test"
                   :rules :cljs}]}
  
  :hooks [cljx.hooks]

  :cljsbuild {:builds [{:source-paths ["target/generated-src/cljs" "target/generated-test"]
                        :compiler {:output-to "target/cljs/testable.js"
                                   :optimizations :whitespace
                                   :pretty-print true}}]
              :test-commands {"unit-tests" ["phantomjs" :runner "target/cljs/testable.js"]}}

  :profiles {:dev {:plugins [[org.clojure/clojurescript "0.0-2156"]
                             [com.keminglabs/cljx "0.3.2"]
                             [lein-cljsbuild "1.0.1"]]
                   :aliases {"cleantest" ["do" "clean," "cljx" "once," "test,"
                                          "cljsbuild" "test"]
                             "deploy" ["do" "clean," "cljx" "once," "deploy" "clojars"]}}})
