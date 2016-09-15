(ns guadalete-ui.events.room
  (:require
    [re-frame.core :refer [dispatch def-event def-event-fx def-fx]]
    [differ.core :as differ]
    [guadalete-ui.util :refer [pretty]]
    [guadalete-ui.console :as log]))

(defn- patch [db old new]
  (let [id (:id new)
        patch (differ/diff old new)
        empty-patch? (and (empty? (first patch)) (empty? (second patch)))]
    (if empty-patch?
      {:db db}
      {:db    db
       :sente {:topic      :room/update
               :data       [id patch :patch]
               :on-success [:success-room-update]
               :on-failure [:failure-room-update]}})))

(defn- reset [db room]
  (let [sente-effect {:topic      :room/update
                      :data       room
                      :on-success [:success-room-update]
                      :on-failure [:failure-room-update]}]
    {:db    db
     :sente sente-effect}))


(def-event-fx
  :room/update
  (fn [{:keys [db]} [_ {:keys [old new]}]]
    (log/debug ":room/update")
    (log/debug "\t old " old)
    (log/debug "\t new" new)
    (if old
      (patch db old new)
      (reset db new))))

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


(def-event-fx
  :room/prepare-trash
  (fn [{:keys [db]} [_ room-id]]
    (let [data {:item-id    room-id
                :ilk        :room
                :modal-type :room/trash}]
      {:db    (assoc db :modal data)
       :modal [:show {
                      :onApprove #(dispatch [:room/do-trash room-id])
                      :closable  false}]})))


(def-event-fx
  :room/do-trash
  (fn [{:keys [db]} [_ room-id]]


    ;; room
    ;;    |--> scenes
    ;;    |--> lights
    ;;    |--> sensors


    (log/debug ":room/do-trash" room-id)
    (let [
          room (get-in db [:room room-id])
          lights (map #(get-in db [:light %]) (:light room))
          scenes (map #(get-in db [:scene %]) (:scene room))

          ;light (get-in db [:light light-id])
          ;lights* (dissoc (:light db) light-id)
          ;
          ;room-lights (:light room)
          ;room-lights* (->> room-lights
          ;                  (remove #(= light-id %))
          ;                  (into []))
          ;room* (assoc room :light room-lights*)
          ;db* (-> db
          ;        (assoc :light lights*)
          ;        (assoc-in [:room (:id room)] room*))
          ]
      (log/debug "room" room)
      (log/debug "lights" lights)
      {:db db
       ;:sente (trash-light-effect light-id)
       }
      )))
