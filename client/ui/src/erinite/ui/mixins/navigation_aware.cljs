(ns erinite.ui.mixins.navigation-aware
  (:require [om-tools.mixin :refer-macros [defmixin]]
            #_[erinite.lib.navigation :as nav]
            #_[erinite.support.navigation :as support]
            #_[erinite.support.services :refer [get-service]]))

#_(defmixin navigation-aware
  "Mixin to allow component easy access to the navigation service.
   
   (.push-page! owner page)
     Navigate to a child page, returning its state.
   
   (.pop-page! owner)
     Navigate to parent page, returning its state.

   (.set-page! owner path))
     Navigate to another page, returning its state.

   (.pages owner)
   (.pages owner path)
     List child pages of current page.
     If `path` is specified, lists child pages of page with that path.

   (.page owner)
     Return the path to the current page.

   (.page-info owner)
     Get the state for the current page."

  (push-page!
    [owner page]
    (support/do-page-change! owner nav/push-page page))

  (pop-page!
    [owner]
    (support/do-page-change! owner nav/pop-page))

  (set-page!
    [owner path]
    (support/do-page-change! owner nav/set-page path))
  
  (pages
    [owner & [path]]
    (nav/pages @(get-service owner :navigation) path))

  (page
    [owner]
    (nav/page @(get-service owner :navigation)))
  
  (page-info
    [owner]
    (nav/page-info @(get-service owner :navigation))))

