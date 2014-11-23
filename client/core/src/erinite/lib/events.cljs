(ns erinite.lib.events
  "Event bus library"
  (:refer-clojure :exclude [send])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]) 
  (:require [cljs.core.async :as async]))

(defonce events-ch (async/chan))
(defonce events-pub (async/pub events-ch first))

(defn intercept!
  "Pass every event sent with send! to `func` before delivering it to listeners.
   This is meant for debugging and testing and not for production use."
  ;; TODO: Should it be allowed for production use at all?
  ;; TODO: Can it be implemented in dev builds only so the overhead of mult/tap
  ;; TODO: is only present in debug builds when intercept! is available?
  [func]
  (assert false "Not yet implemented"))

(defn listen! 
  "Listen for events with `topic`,  calling `callback` with their value.
   Alternatively, can take a map {topic callback} to define multiple listeners."
  ([topic-map]
   (doseq [[topic callback] topic-map]
     (listen! topic callback)))
  ([topic callback! & [kill-mult]]
    (let [channel (async/chan (async/sliding-buffer 10))]
      ;; Subscribe to the correct topic
      (async/sub events-pub topic channel) 
      ;; Receive events and call the callback
      (go-loop []
        (when-let [[topic values] (async/<! channel)]
          (apply callback! values)
          (recur))
        (async/unsub events-pub topic channel))
      ;; Return the listener channel, so it can be closed to kill the listener
      channel)))

(defn send!
  "Send an event with `topic` and `values`"
  [topic & values]
  (async/put! events-ch [topic values])
  ; If topic is namespaced, also send un-namespaced event
  (when (namespace topic) 
    (async/put! events-ch [(keyword (name topic)) values])))

(defn send
  "Returns a function which, when called, sends events"
  ([] send!)
  ([topic] (partial send! topic))
  ([topic & values] (apply partial send! topic values)))


; (comment
;   ;; Since send! is expected to be called frequently and the `when` is the
;   ;; expected common case, converting `topic` to a string and back to a keyword 
;   ;; _may_ be a performance bottleneck. If profiling reveals this to be the
;   ;; case, then the below macro can be used instead to perform this check and
;   ;; conversion at compile-time in the expected common case that `topic` is a
;   ;; keyword literal. If `topic` is a dynamic value, then the code falls back to
;   ;; a runtime check. `raw-send!` is used as an intermediary in order to scope
;   ;; `event-ch` to the correct namespace. If the additional function call adds
;   ;; too much overhead, look into eihter marking it as inline or manually
;   ;; inlining the code in the macro and fully namespace qualifying `event-ch`
;   ;; instead: (async/put! ~'erinite.lib.events/event-ch message)
;   (defn raw-send!
;     [message]
;     (async/put! event-ch message))

;   (defmacro send!
;     "Send an event with `topic` and `values`"
;     [topic & values]
;     (if (keyword? topic)
;       (if (namespace topic)
;         `(let [values# ~values]
;            (raw-send! [~topic values#]) 
;            (raw-send! [~(keyword (name topic)) values#]))
;         `(send-! [~topic ~values]))
;       `(let [topic# topic]
;          (raw-send! [topic# ~values])
;          (when (namespace topic#)
;            (raw-send! [(keyword (name topic#)) ~values])))))
;   
;   ;; The following version is a highly optimised version specialised for zero,
;   ;; one or multiple arguments. The zero and one -argument versions are designed
;   ;; to overcome one of the disadvantages of using a macro: that it cannot be
;   ;; used as a higher-order function. Calling `send!` with zero arguments will
;   ;; return the standard unoptimised runtime-checked funciton as an anonmyous
;   ;; function. Calling `send!` with one argument returns an anonymous function
;   ;; specialised for sending messages with the given topic - if the topic is a
;   ;; keyword literal, the returned function will be optimised, otherwise a
;   ;; runtime check occurs. Finally, calling `send!` with multiple arguments will
;   ;; not return a function at all and instead will result in an optimised call
;   ;; (if topic is a keyword literal) or a runtime checked call (if topic is not
;   ;; a keyword literal).
;   ;; NOTE: One shortcoming of this macro is that all messages must have values,
;   ;; even if just nil - ie (send! :foo) will not send a message of type :foo
;   ;; without any values, it will create a function (fn [& vals] ...) that when
;   ;; called will send messages of type :foo and given vals. To send a message
;   ;; :foo without a body, one must call (send! foo nil) instead
;   ;;
;   ;; Exmaple: (doall (map (send! :foo) [:A :B :C]))
;   ;; will send messages [:foo [:A]] [:foo [:B]] and [:foo [:C]]
;   ;;
;   (defmacro send!
;     "(send!) will return a function (fn [topic & vals] ...)
;      (send! topic) will return a function (fn [& vals] ...)
;      (send! topic & vals) will execute immediately
;      
;      (send! topic vals) = ((send! topic) vals) = ((send!) topic vals)
;      
;      `topic` is a keyword message type of the message to send
;      `vals` is one or more values that make up the message body"
;     ([]
;      `(fn [topic# & values#]
;         (async/put! ~'erinite.lib.events/event-ch
;                     [topic# values#])
;         (when (namespace topic) 
;           (async/put! ~'erinite.lib.events/event-ch
;                       [(keyword (name topic#)) values#]))))
;     ([topic]
;       (if (keyword? topic)
;         (if (namespace topic)
;           `(fn [values#]
;              (async/put! ~'erinite.lib.events/event-ch
;                          [~topic values#]) 
;              (async/put! ~'erinite.lib.events/event-ch
;                          [~(keyword (name topic)) values#]))
;           `(fn [values#]
;              (async/put! ~'erinite.lib.events/event-ch
;                          [~topic values#])))
;         `(let [topic# topic]
;            (fn [values#]
;              (async/put! ~'erinite.lib.events/event-ch
;                          [topic# values#])
;              (when (namespace topic#)
;                (async/put! ~'erinite.lib.events/event-ch
;                            [(keyword (name topic#)) values#]))))))
;     ([topic & values]
;       (if (keyword? topic)
;         (if (namespace topic)
;           `(let [values# ~values]
;              (async/put! ~'erinite.lib.events/event-ch
;                          [~topic values#]) 
;              (async/put! ~'erinite.lib.events/event-ch
;                          [~(keyword (name topic)) values#]))
;           `(async/put! ~'erinite.lib.events/event-ch
;                        [~topic ~values]))
;         `(let [topic# topic]
;            (async/put! ~'erinite.lib.events/event-ch
;                        [topic# ~values])
;            (when (namespace topic#)
;              (async/put! ~'erinite.lib.events/event-ch
;                          [(keyword (name topic#)) ~values])))))))

