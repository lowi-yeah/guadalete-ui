(ns guadalete-ui.events.mixer
  (:require
    [re-frame.core :refer [dispatch def-event def-event-fx def-fx]]
    [guadalete-ui.util :refer [pretty kw*]]
    [guadalete-ui.console :as log]
    [differ.core :as differ]
    [guadalete-ui.console :as log])
  )

(defn- new-mixer-effect
  "Creates a sente-effect for syncing the new mixer item with the backend"
  [mixer]
  {:topic      :mixer/make
   :data       mixer
   :on-success [:success-mixer-make]
   :on-failure [:failure-mixer-make]})


;// re-frame          _     _                 _ _
;//   _____ _____ _ _| |_  | |_  __ _ _ _  __| | |___ _ _ ___
;//  / -_) V / -_) ' \  _| | ' \/ _` | ' \/ _` | / -_) '_(_-<
;//  \___|\_/\___|_||_\__| |_||_\__,_|_||_\__,_|_\___|_| /__/
;//
;; MAKE
;; ********************************
(def-event-fx
  ;; create a new mixer and sync it via sente
  :mixer/make
  (fn [{:keys [db]} [_ mixer]]
    (let [sente (new-mixer-effect mixer)]
      (log/debug ":mixer/make" mixer sente)
      {:db    db
       :sente sente})))

(def-event-fx
  ;; Handler called after the light has been updated sucessfully
  :success-mixer-make
  (fn [{:keys [db]} [_ _response]]
    {:db db}))

(def-event-fx
  :failure-mixer-make
  (fn [{:keys [db]} [_ response]]
    (let []
      (log/warn "make mixer failed" response)
      {:db db})))