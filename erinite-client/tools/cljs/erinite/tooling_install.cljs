(ns erinite.tooling-install
  (:require [erinite.tooling :as tools] 
            [ankha.core :as ankha]))


(tools/uninstall-all)
(tools/install "Inspector" ankha/inspector)
;(tools/install "Messages" ankha/inspector)



