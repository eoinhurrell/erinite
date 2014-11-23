(defproject erinite.client/core "0.1.0-alpha"
  :description "Erinite web framework, client, core library"
  :url "http://github.com/ActionableInsight/erinite"
  :license {:name "Eclipse Public License v1.0 "
            :url "https://eclipse.org/legal/epl-v10.htm"}

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [com.taoensso/sente "1.2.0" :exclusions [com.taosensso/encore]]
                 [org.clojure/clojurescript "0.0-2268"]
                 [quile/component-cljs "0.2.2"]
                 [om "0.8.0-alpha2"]
                 [kioo "0.4.0" :exclusions [reagent]]
                 [bootstrap-cljs "0.0.3" :exclusions [org.clojure/clojure]]
                 [shodan "0.3.0"]
                 [fence "0.1.0"]
                 [prismatic/plumbing "0.3.3"]
                 [im.chit/purnam "0.4.3"]
                 [alandipert/storage-atom "1.2.3"]]

  :plugins [[lein-cljsbuild "1.0.3"] ]
  :source-paths ["src"]  

  :cljsbuild {
    :builds [
      {:id "erinite"
       :source-paths ["src"]
       :compiler {
          :output-to "resources/public/app/zardoz.js"
          :optimizations :advanced }}]}

  :source-paths ["src"]
  :hooks [leiningen.cljsbuild])
