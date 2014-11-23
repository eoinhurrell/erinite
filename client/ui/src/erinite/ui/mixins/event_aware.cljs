(ns erinite.mixins.event-aware
  "Om-tools mixin to let components be aware of and work with events"
  (:require [om.core :as om :include-macros true]
            [om-tools.mixin :refer-macros [defmixin]]
            [cljs.core.async :as async]
            #_[erinite.lib.events :as ev]))

;; TODO: Move this to support
#_(defn get-or-create-state!
  "Get a value from component local state, or, if it doesn't exist (is nil),
   create it by calling the constructor function, store the new value in
   component local state and return it."
  [owner kw ctor]
  (if-let [value (om/get-state owner kw)]
    value
    (let [new-value (ctor)]
      (om/set-state! owner kw new-value)
      new-value)))

#_(defmixin event-aware
  "Mixin to allow component to take part in the event system.
   
   (.listen! owner topic dest)
   (.listen! owner topic dest func)
      Listen for events with `topic`, storing their value in local state with
      key `dest`.
      `func` can optionally be a function (fn [old-state value] ...) where
      `old-state` is the previous value at `dest` and `value` is the value of
      the event with `topic`. The return value of this function is then stored
      in local state at key `dest`."

  (will-unmount [owner]
    ;; Kill all listeners by closing the channel
    (when-let [ch (om/get-state owner ::event-ch)]
      (async/close! ch)))

  (listen! [owner topic destination & [func]]
    ;; Listen for topic. Callback writes value to local state
    (let [cb  (if func
                (fn [_ & v] (om/update-state! owner destination #(func % v)))
                (fn [_ v] (om/set-state! owner destination v)))]
      (om/set-state owner ::event-ch (ev/listen! topic listen-func)))))

