(defproject erinite.client/ui "0.1.0-alpha"
  :description "Erinite web framework, client, ui library"
  :url "http://github.com/ActionableInsight/erinite"
  :license {:name "Proprietary"
            :url ""}

  :dependencies [[erinite.client/core "0.1.0-alpha"]
                 [om "0.8.0-alpha2"]
                 [prismatic/om-tools "0.3.6" :exclusions [org.clojure/clojure]]
                 [markdown-clj "0.9.47"]
                 [sablono "0.2.18"] ]

  :plugins [[lein-cljsbuild "1.0.3"]
            [lein-figwheel "0.1.3-SNAPSHOT"]] 

  :profiles {
    ;; Development profile
    :dev { 
      :dependencies [[ring "1.3.0"]
                     [com.cemerick/piggieback "0.1.3"]
                     [org.clojure/tools.nrepl "0.2.3"]
                     [compojure "1.1.8"]
                     [http-kit "2.1.18"]
                     [enlive "1.1.5"]
                     [weasel "0.3.0"]
                     [ankha "0.1.1"]
                     [figwheel "0.1.3-SNAPSHOT" :exclusions [compojure]]  
                     [devcards "0.1.1-SNAPSHOT" :exclusions [compojure]]]
      :resources-paths ["resources" "dev/resources"]
      :source-paths ["src" "dev/src" "dev/tools/http" "dev/tools/repl"]
      :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}
      :injections [(require 'ring.server 'weasel.repl.websocket)
                   (ring.server/repl)
                   (ring.server/run)]   
      :cljsbuild {
        :builds [
          ;; Default development build
          {:id "zardoz"
           :source-paths ["src" "dev/src" "dev/tools/repl"]
           :compiler {
              :output-to "resources/public/app/app.js"
              :output-dir "resources/public/app/"
              :optimizations :none
              :source-map true}}  
          ;; Devcard development build
          {:id "devcards"
           :source-paths ["dev/cards" "src"]
           :compiler {
              :output-to "dev/resources/public/devcards/devcards.js"
              :output-dir "dev/resources/public/devcards"
              :optimizations :none
              :source-map true}}]}
      :plugins [[lein-cljsbuild "1.0.3"]
                [lein-figwheel "0.1.3-SNAPSHOT"]]}
    ;; Production build
    :production {
      :source-paths ["src"]
      :cljsbuild {
        :builds [
          {:id "erinite"
           :source-paths ["src"]
           :compiler {
              :output-to "resources/public/app/zardoz.js"
              :optimizations :simple
              :pretty-print true
              ;:pseudo-names true
              :preamble ["/js/react-0.11.1.min.js"
                         "/js/react-bootstrap.min.js"
                         "/js/charts/d3.v3.min.js"
                         "/js/charts/dimple.v2.1.0.min.js"]
              :externs ["/js/charts/d3.v3.min.js"
                        "/js/charts/dimple.v2.1.0.min.js"]
              :closure-warnings {:externs-validation :off
                                 :non-standard-jsdoc :off}}}]}}}

  :aliases {"devcards" ["figwheel" "devcards"]
            "dev"      ["cljsbuild" "auto" "erinite"]}

  :source-paths ["src"]
  :hooks [leiningen.cljsbuild]
  :repl-options {:timeout 240000}


  ;; Figwheel configuration for devcards
  :figwheel {:css-dirs ["resources/public/css"]})
