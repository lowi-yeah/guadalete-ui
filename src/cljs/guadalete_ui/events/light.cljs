(ns guadalete-ui.events.light
  (:require
    [re-frame.core :refer [dispatch def-event def-event-fx def-fx]]
    [guadalete-ui.util :refer [pretty validate!]]
    [differ.core :as differ]
    [guadalete-ui.console :as log]
    [guadalete-ui.dmx :as dmx]
    [guadalete-ui.util.dickens :as dickens]
    [schema.core :as s]
    [guadalete-ui.schema :as gs]))

;//   _        _
;//  | |_  ___| |_ __ ___ _ _ ___
;//  | ' \/ -_) | '_ \ -_) '_(_-<
;//  |_||_\___|_| .__\___|_| /__/
;//             |_|
(defn- remove-channels
  "Internal helper to remove channel-assignments during :light/update-new"
  [light new-count]
  (into []
        (map
          (fn [index] (get-in light [:channels index]))
          (range new-count))))

(defn- add-channels
  "Internal helper to remove channel-assignments during :light/update-new"
  [light additional assignable-channes]
  (let [num-channels (int (:num-channels light))
        channels (:channels light)
        range (range num-channels (+ num-channels additional))
        channels* (into channels (map (fn [i] [(nth assignable-channes (- i 1))]) range))]
    (into [] channels*)))

(defn- update-channels
  "Internal function for correctly setting the number of channels and their values."
  [update original assignable-channes]
  (let [num-update-channels (int (:num-channels update))
        num-original-channels (int (:num-channels original))]
    (cond
      (< num-update-channels num-original-channels) (remove-channels update num-update-channels)
      (> num-update-channels num-original-channels) (add-channels original (- num-update-channels num-original-channels) assignable-channes)
      :default (:channels update))))

(defn- make-light
  "Internal helper to generate a new basic light with a random name"
  [room-id channel]
  {:id           (str (random-uuid))
   :name         (dickens/generate-name)
   :room-id      room-id
   :num-channels 1
   :transport    :dmx
   ; obacht:
   ; nested arrays, since each color can be assigned multiple channels
   :channels     [[channel]]
   :color        {:brightness 0
                  :saturation 0
                  :hue        0}})


;// re-frame_  __        _
;//   ___ / _|/ _|___ __| |_ ___
;//  / -_)  _|  _/ -_) _|  _(_-<
;//  \___|_| |_| \___\__|\__/__/
;//
(s/defn ^:always-validate new-light-effect
  "Creates a sente-effect for syncing the new light with the backend"
  [light :- gs/Light]
  {:topic      :light/make
   :data       light
   :on-success [:success-light-make]
   :on-failure [:failure-light-make]})

(defn- update-light-effect
  "Creates a sente-effect for syncing an existing light with the backend"
  [id diff flag]
  {:topic      :light/update
   :data       [id diff flag]
   :on-success [:success-light-update]
   :on-failure [:failure-light-update]})

(defn- trash-light-effect
  "Creates a sente-effect for removing a light fromthe backend"
  [id]
  {:topic      :light/trash
   :data       id
   :on-success [:success-light-trash]
   :on-failure [:failure-light-trash]})


;// re-frame          _     _                 _ _
;//   _____ _____ _ _| |_  | |_  __ _ _ _  __| | |___ _ _ ___
;//  / -_) V / -_) ' \  _| | ' \/ _` | ' \/ _` | / -_) '_(_-<
;//  \___|\_/\___|_||_\__| |_||_\__,_|_||_\__,_|_\___|_| /__/
;//

;; MAKE
;; ********************************

(s/defn ^:always-validate make-light* :- gs/Effect
  [db :- gs/DB
   room-id :- s/Str]
  (let [channels (sort (into [] (dmx/assignable db)))
        new-light (make-light room-id (first channels))]
    {:db    db
     :sente (new-light-effect new-light)}))

(def-event-fx
  ;; create a new light (a map) and sync it via sente
  :light/make
  (fn [{:keys [db]} [_ room-id]]
    (make-light* db room-id)))

(def-event-fx
  ;; when the light has been created successfully,
  ;; dispatch two things:
  ;;  1. update the root to which the light belongs
  ;;  2. show the edit-modal for the new light
  :success-light-make
  (fn [{:keys [db]} [_ response]]
    (let [light (:ok response)
          error-msg (:error response)]
      (if light
        (let [room-id (:room-id light)
              room (get-in db [:room room-id])
              room-lights* (conj (:light room) (:id light))
              room* (assoc room :light room-lights*)
              modal-data {:item-id    (:id light)
                          :ilk        :light
                          :modal-type :light}
              db* (-> db
                      (assoc-in [:room room-id] room*)
                      (assoc-in [:light (:id light)] light)
                      (assoc :modal modal-data))]

          {:db       db*
           :dispatch [:room/update {:new room* :old room}]
           :modal    [:show {}]})
        (do
          (log/error "error during light creation:" error-msg)
          {:db db})))))

;; EDIT
;; ********************************
(def-event-fx
  :light/edit
  (fn [{:keys [db]} [_ light-id]]
    (let [data {:item-id    light-id
                :ilk        :light
                :modal-type :light}]
      {:db    (assoc db :modal data)
       :modal [:show {}]})))


;(def-event-fx
;  :mouse/double-click
;  (fn [{:keys [db]} [_ {:keys [room-id scene-id id ilk]}]]
;    (let [item-id (get-in db [:scene scene-id :nodes (keyword id) :item-id])
;          data {:room-id    room-id
;                :scene-id   scene-id
;                :node-id    id
;                :item-id    item-id
;                :ilk        ilk
;                :modal-type (keyword (str "pd/" (name ilk)))}]
;      {:db    (assoc db :modal data)
;       :modal :show})))

(s/defn ^:always-validate update-dmx :- gs/Light
  [db :- gs/DB
   original :- gs/Light
   update :- gs/Light]
  (if (= :dmx (:transport original))
    (let [assignables (sort (into [] (dmx/assignable db)))
          channels (update-channels update original assignables)]
      (assoc update :channels channels))
    ;; if no DMX light, do nothin'
    update))

(s/defn ^:always-validate update-mqtt :- gs/Light
  [db :- gs/DB
   original :- gs/Light
   update :- gs/Light]
  (log/debug "update-mqtt")
  update)

(s/defn ^:always-validate update-transport :- gs/Light
  [db :- gs/DB
   original :- gs/Light
   update :- gs/Light]
  (log/debug "update-dmx")
  (if (= :dmx (:transport original))
    (update-dmx db original update)
    (update-mqtt db original update)))

;; UPDATE
;; ********************************
(def default-color
  {:color {:brightness   0
           :hue          0
           :saturatation 0}})

(def-event-fx
  :light/update
  (fn [{:keys [db]} [_ update]]
    (log/debug ":light/update")
    (log/debug "\t light" update)
    (let [original-light (get-in db [:light (:id update)])
          light-update (update-transport db original-light update)

          light-update (merge default-color light-update)

          light-patch (differ/diff original-light light-update)

          ;; in case of mqtt lights the root with which they are associated might change
          room-changed? (not= (:room-id original-light) (:room-id light-update))
          original-room (get-in db [:room (:room-id original-light)])
          update-room (get-in db [:room (:room-id light-update)])

          original-room* (when (and room-changed? (not (nil? original-room)))
                           (assoc original-room :light
                                                (->> (:light original-room)
                                                     (remove #(= % (:id update)))
                                                     (into []))))

          update-room* (when (and room-changed? (not (nil? update-room)))
                         (assoc update-room :light (conj (:light update-room) (:id update))))

          original-room-dispatch (if (not (nil? original-room*))
                                   [:room/update {:new original-room* :old original-room}])
          update-room-dispatch (if (not (nil? update-room*))
                                 [:room/update {:new update-room* :old update-room}])

          room-dispatches (->> [original-room-dispatch update-room-dispatch]
                               (remove #(nil? %))
                               (into ()))]

      {:db       (assoc-in db [:light (:id update)] light-update)
       :sente    (update-light-effect (:id update) light-patch :patch)
       :dispatch room-dispatches})))

(def-event-fx
  ;; Handler called after the light has been updated sucessfully
  :success-light-update
  (fn [{:keys [db]} [_ response]]
    (let [light (:ok response)
          error-msg (:error response)]
      {:db db})))

(def-event-fx
  ;; Handler called after the light has been updated sucessfully
  :failure-light-update
  (fn [{:keys [db]} [_ response]]
    (let []
      (log/warn "update light failed" response)
      {:db db})))

;; TRASH
;; ********************************
(def-event-fx
  :light/trash
  (fn [{:keys [db]} [_ light-id]]
    (let [light (get-in db [:light light-id])
          lights* (dissoc (:light db) light-id)
          room (get-in db [:room (:room-id light)])
          room-lights (:light room)
          room-lights* (->> room-lights
                            (remove #(= light-id %))
                            (into []))
          room* (assoc room :light room-lights*)
          db* (-> db
                  (assoc :light lights*)
                  (assoc-in [:room (:id room)] room*))]
      {:db    db*
       :sente (trash-light-effect light-id)
       :modal [:close {}]}
      )))

(def-event-fx
  :success-light-trash
  (fn [{:keys [db]} [_ response]]
    (let [light (:ok response)
          error-msg (:error response)]
      (log/info ":success-light-trash" response)
      {:db db})))

(def-event-fx
  :failure-light-trash
  (fn [{:keys [db]} [_ response]]
    (let []
      (log/warn "trash light failed" response)
      {:db db})))