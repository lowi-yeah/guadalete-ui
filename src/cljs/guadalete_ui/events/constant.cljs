(ns guadalete-ui.events.constant
  (:require
    [re-frame.core :refer [dispatch def-event def-event-fx def-fx]]
    [guadalete-ui.util :refer [pretty kw*]]
    [guadalete-ui.console :as log]
    [differ.core :as differ]
    [guadalete-ui.console :as log]))

(defn- update-constant-effect
       "Creates a sente-effect for syncing an existing light with the backend"
       [id update]
       {:topic      :constant/update
        :data       [id update]
        :on-success [:success-constant-update]
        :on-failure [:failure-constant-update]})

(def-event-fx
  :success-constant-update
  (fn [{:keys [db]} [_ response]]
      (let [light (:ok response)
            error-msg (:error response)]
           {:db db})))

(def-event-fx
  :failure-constant-update
  (fn [{:keys [db]} [_ response]]
      (let []
           (log/warn "update constant failed" response)
           {:db db})))

(def-event-fx
  :constant/update
  (fn [{:keys [db]} [_ update]]
      {:db    (assoc-in db [:constant (:id update)] update)
       :sente (update-constant-effect (:id update) update)}))


;; TRASH
;; ********************************
(defn- trash-constant-effect
       "Creates a sente-effect for removing a constant from the backend"
       [id]
       {:topic      :constant/trash
        :data       id
        :on-success [:success-constant-trash]
        :on-failure [:failure-constant-trash]})

(def-event-fx
  :constant/trash
  (fn [{:keys [db]} [_ constant-id]]
      (let [light (get-in db [:constant constant-id])
            constants* (dissoc (:constant db) constant-id)
            db* (-> db (assoc :constant constants*))]
           {:db    db*
            :sente (trash-constant-effect constant-id)
            :modal [:close {}]})))

(def-event-fx
  :success-constant-trash
  (fn [{:keys [db]} [_ response]]
      (let [light (:ok response)
            error-msg (:error response)]
           (log/debug ":success-constant-trash" response)
           {:db db})))

(def-event-fx
  :failure-constant-trash
  (fn [{:keys [db]} [_ response]]
      (let []
           (log/warn "trash constant failed" response)
           {:db db})))