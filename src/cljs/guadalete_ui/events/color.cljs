(ns guadalete-ui.events.color
  (:require
    [re-frame.core :refer [dispatch def-event def-event-fx def-fx]]
    [guadalete-ui.util :refer [pretty]]
    [guadalete-ui.console :as log]))

;// re-frame_  __        _
;//   ___ / _|/ _|___ __| |_ ___
;//  / -_)  _|  _/ -_) _|  _(_-<
;//  \___|_| |_| \___\__|\__/__/
;//

(defn- new-color-effect
  "Creates a sente-effect for syncing the new light with the backend"
  [color]
  {:topic      :color/make
   :data       color
   :on-success [:success-color-make]
   :on-failure [:failure-color-make]})


;// re-frame          _     _                 _ _
;//   _____ _____ _ _| |_  | |_  __ _ _ _  __| | |___ _ _ ___
;//  / -_) V / -_) ' \  _| | ' \/ _` | ' \/ _` | / -_) '_(_-<
;//  \___|\_/\___|_||_\__| |_||_\__,_|_||_\__,_|_\___|_| /__/
;//

;; MAKE
;; ********************************
(def-event-fx
  ;; create a new light (a map) and sync it via sente
  :color/make
  (fn [{:keys [db]} [_ color]]
    (let [sente (new-color-effect color)]
      (log/debug "creating new color" sente)
      {:db    db
       :sente sente})))

(def-event-fx
  ;; Handler called after the light has been updated sucessfully
  :success-color-make
  (fn [{:keys [db]} [_ _response]]
    {:db db}))

(def-event-fx
  :failure-color-make
  (fn [{:keys [db]} [_ response]]
    (let []
      (log/warn "update make color failed" response)
      {:db db})))