(ns erinite.mixins.event-aware
  (:require [cljs.core.async :as async]
            [om-tools.mixin :refer-macros [defmixin]]
            [om.core :as om]
            [erinite.lib.events :as events]))

(defmixin event-aware
  "Mixin to allow component to take part in the event system.
   
   (.listen! owner topic-map)"

  (will-unmount [owner]
    ;; Kill all listeners by closing the channel
    (when-let [ch (om/get-state owner ::event-ch)]
      (async/close! ch)))

  (listen! [owner topic-map]
    (om/set-state! owner ::event-ch (events/listen! topic-map))))
