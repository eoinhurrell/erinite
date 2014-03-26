;;; This namespace is used for development and testing purpose only.
(ns ring.server
  (:require [cemerick.austin.repls :refer (browser-connected-repl-js)]
            [net.cgrand.enlive-html :as enlive]
            [compojure.route :refer  (resources)]
            [compojure.core :refer (GET defroutes)]
            [ring.adapter.jetty :as jetty]
            [clojure.java.io :as io]))

(enlive/defsnippet page
  "public/index.html"
  [:#erinite-content]
  [])

(enlive/deftemplate dev-page
  (io/resource "dev/dev-index.html")
  []
  [:body] (enlive/append
            (enlive/html [:script (browser-connected-repl-js)]))
  [:#erinite-content] (enlive/content (page)))

(defroutes site
  (resources "/" {:root "dev"})
  (GET "/*" req (dev-page)))

(defn run
  "Run the ring server. It defines the server symbol with defonce."
  []
  (defonce server
    (jetty/run-jetty #'site {:port 3000 :join? false}))
  server)
