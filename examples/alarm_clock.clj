(ns alarm-clock
  (:require [erinite.mapping :as erin]
            [clojure.core.match :refer [match]]))

;;
;; Alarm Clock sample application
;;
;; The alarm clock has a list of alarms.
;; Alarms have a name and a tick number when they go off.
;; Alarms are listed in order of creation and can be selected by scrolling
;; through them (previous and next). The currently selected alarm can be
;; toggled to enable or disable it. New alarms can be created and these are
;; appended to the end of the list. New alarms start as disabled.
;; The ticks increment periodically. If ticks is between an alarms ticks and
;; (+ ticks 25) that alarm is going off.
;;
;; Currently only the logic is implemented. Later, a UI would be added so that:
;;   [:prev] is sent when the user presses the up key
;;   [:next] is sent when the user presses the down key
;;   [:toggle] is sent when the user presses a toggle button
;;   [:create name ticks] is sent when the user presses a create button.
;;        name and ticks are taken from input boxes
;;   [:tick] is sent every second
;; 
;; The UI would also display the current list of alarms (name and enabled)
;; and highlight the selected alarm. The current ticks would be displayed and
;; when alarms are going off, a message containing the name of each alarm that
;; is going off would be displayed.
;;

(defn handle-selection
  "User is using prev/next to select an alarm from the list"
  [state topic value alarms]
  (let [num-alarms  (dec (count alarms))
        ; Set the current selection based on which user action was sent
        value       (match topic
                      :prev (dec state)
                      :next (inc state))]
    ; Make sure that the selection is within the correct range
    (or (when (< value 0) num-alarms)
        (when (> value num-alarms) 0)
        value)))

(defn handle-toggle
  "User is toggling the current selection to enable or disable the alarm"
  [state topic value selection]
  (map
    (fn [idx [name active ticks]]
      [name
       ; If this alarm is the selected alarm, then toggle it, otherwise leave it
       (if (= idx selection) (not active) active)
       ticks])
    (range) ; Get the index so it can be checked against selection
    state))

(defn handle-create
  "User is creating a new alarm"
  [state topic [name ticks]]
  ; Simply append the new alarm to the list of existing alarms
  (conj state [name false ticks]))

(defn handle-notices
  "Ticks have been updated, system now checks if any alarms must go off"
  [state topic ticks alarms]
  (->>
    alarms
    ; Keep only alarms that are going off
    (filter (fn [[_ enabled start-time]]
              ; To be going off, an alarm must be enabled and ticks be between
              ; its start time and the end time (+ start-time  duration)
              (and enabled (>= (+ start-time 25) ticks start-time))))
    ; Now change each alarm into the expected format
    (map (fn [[name _ _] name]))))


(def app (map-transforms
            ; INPUT TOPICS   DEPENDS       OUTPUT      OPTIONS FN
            [[#{:init}                    :alarms     :const  (fn [] [])]
             [#{:init}                    :selection  :const  (fn [] 0)]
             [#{:prev :next} [:alarms]    :selection          handle-selection]
             [#{:toggle}     [:selection] :alarms             handle-toggle]
             [#{:create}                  :alarms             handle-create]
             [#{:tick}                    :ticks      :prev   inc]
             [#{:ticks}      [:alarms]    :notices            handle-notices]]))

; Now, hook up events.
; First initialise the app:
;   (send! app :init)
; 
; Now hook up input events:
; previous button -> :prev
; next button -> :next
; toggle button -> :toggle
; create button -> [:create name start-time]
;   where name and start-time would be taken from input boxes
;
; Every second -> :tick
;
; Now hook up output events:
; [:alarms [name enabled ticks]] -> list of alarms and if they're enabled
; [:selection index] -> which item in the list to highlight
; [:ticks current-tick] -> current time
; [:notices alarms] -> list of alarm names of the alarms going off


