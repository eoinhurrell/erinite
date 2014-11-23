(defproject erinite/server "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/core.async "0.1.338.0-5c5012-alpha"]
                 [com.stuartsierra/component "0.2.1"]
                 ;; Utility
                 [com.taoensso/timbre "3.2.0"]  ; Logging
                 [clj-time "0.6.0"]             ; Time
                 ;; Configuration
                 [environ "0.5.0"]              ; Environment vars
                 [conf-er "1.0.1"]              ; Config files
                 ;; Cryptographic
                 [crypto-password "0.1.3"]      ; Password hashing
                 [crypto-random "1.2.0"]        ; Random numbers
                 [crypto-equality "1.0.0"]
                 ;; Web
                 [http-kit "2.1.18"]            ; Web server
                 [compojure "1.1.8"]            ; Web routes
                 [ring "1.3.0"]
                 [com.taoensso/sente "1.2.0" :exclusions [com.taosensso/encore]]
                 ])
