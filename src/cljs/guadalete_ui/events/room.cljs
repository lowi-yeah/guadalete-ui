(ns guadalete-ui.events.room
  (:require
    [re-frame.core :refer [dispatch def-event def-event-fx def-fx]]
    [differ.core :as differ]
    [guadalete-ui.util :refer [pretty]]
    [guadalete-ui.console :as log]))

(defn- patch [db update original]
  (let [id (:id update)
        patch (differ/diff original update)
        empty-patch? (and (empty? (first patch)) (empty? (second patch)))]
    (log/debug "patching room" patch)

    (if empty-patch?
      {:db db}
      {:db    db
       :sente {:topic      :room/update
               :data       [id patch :patch]
               :on-success [:success-room-update]
               :on-failure [:failure-room-update]}})))

(defn- reset [db update]
  (let [sente-effect {:topic      :room/update
                      :data       update
                      :on-success [:success-room-update]
                      :on-failure [:failure-room-update]}]
    {:db    db
     :sente sente-effect}))


(def-event-fx
  :room/update
  (fn [{:keys [db]} [_ update original]]
    (if original
      (patch db update original)
      (reset db update))))

(def-event-fx
  :success-room-update
  (fn [{:keys [db]} [_ response]]
    (log/debug ":success-room-update" response)
    (let [room (:ok response)
          error-msg (:error response)]
      (if room
        {:db (assoc-in db [:room (:id room)] room)}
        (do
          (log/error "error during light creation:" error-msg)
          {:db db})))))

