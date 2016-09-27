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

(s/defn ^:always-validate make-channels :- [gs/ColorChannel]
        "Internal helper create the light channels during light creation/update"
        [light assignable-channels]
        (let [num-channels (:num-channels light)
              channels (:channels light)

              current-channels (->> (:channels light)
                                    (map (fn [channel]
                                             (:dmx channel)))
                                    (apply conj)
                                    (into []))

              assignable-channels* (->> current-channels
                                        (into assignable-channels)
                                        (flatten)
                                        (sort))


              new-channels (condp = (:num-channels light)
                                  1 [{:name  :white
                                      :dmx   [(nth assignable-channels* 0)]
                                      :index 0}]
                                  2 [{:name  :cool-white
                                      :dmx   [(nth assignable-channels* 0)]
                                      :index 0}
                                     {:name  :warm-white
                                      :dmx   [(nth assignable-channels* 1)]
                                      :index 1}]
                                  3 [{:name  :red
                                      :dmx   [(nth assignable-channels* 0)]
                                      :index 0}
                                     {:name  :green
                                      :dmx   [(nth assignable-channels* 1)]
                                      :index 1}
                                     {:name  :blue
                                      :dmx   [(nth assignable-channels* 2)]
                                      :index 2}]
                                  4 [{:name  :white
                                      :dmx   [(nth assignable-channels* 0)]
                                      :index 0}
                                     {:name  :red
                                      :dmx   [(nth assignable-channels* 1)]
                                      :index 1}
                                     {:name  :green
                                      :dmx   [(nth assignable-channels* 2)]
                                      :index 2}
                                     {:name  :blue
                                      :dmx   [(nth assignable-channels* 3)]
                                      :index 3}]
                                  (log/error "A DMX light must have between one and four (rgbw) color channels."))]
             new-channels))

(defn- update-channels
       "Internal function for correctly setting the number of channels and their values."
       [update original assignable-channels]
       (if (not= (:num-channels update) (:num-channels original))
         (make-channels update assignable-channels)
         (:channels update)))

(s/defn ^:always-validate make-light
        "Internal helper to generate a new basic light with a random name."
        [room-id assignable-channels]
        (let [light {:id           (str (random-uuid))
                     :name         (dickens/generate-name)
                     :room-id      room-id
                     :color-type   :v
                     :num-channels 1
                     :transport    :dmx
                     :channels     []
                     :color        {:brightness 0}}
              channels (make-channels light assignable-channels)
              light* (assoc light :channels channels)]
             (log/debug "channels" channels)
             (log/debug "new light" light*)
             light*))

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
       (log/debug "making update-light-effect")
       (log/debug "[id diff flag]" id diff flag)
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
        (let [assignable-channels (dmx/assignable db)
              new-light (make-light room-id assignable-channels)]
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


;; UPDATE
;; ********************************
(s/defn ^:always-validate update-dmx :- gs/Light
        [db :- gs/DB
         original :- gs/Light
         update :- gs/Light]
        (if (= :dmx (:transport original))
          (let [assignable-channels (dmx/assignable db)
                channels (update-channels update original assignable-channels)]
               (assoc update :channels channels))
          ;; if no DMX light, do nothin'
          update))

(s/defn ^:always-validate update-mqtt :- gs/Light
        [db :- gs/DB
         original :- gs/Light
         update :- gs/Light]
        update)

(s/defn ^:always-validate update-transport :- gs/Light
        ;(s/defn update-transport :- gs/Light
        [db :- gs/DB
         original :- gs/Light
         update :- gs/Light]
        (if (= :dmx (:transport original))
          (update-dmx db original update)
          (update-mqtt db original update)))

(defn- default-color [type]
       (condp = type
              :v {:brightness 0}
              :sv {:brightness 0 :saturation 0}
              :hsv {:brightness 0 :hue 0 :saturation 0}))

(s/defn ^:always-validate prune-color :- gs/SimpleColor
        [color type]
        (condp = type
               :v (dissoc color :hue :saturation)
               :sv (dissoc color :hue)
               :hsv color))

(s/defn ^:always-validate update-color :- gs/Light
        [light :- gs/Light]
        (let [default* (default-color (:color-type light))
              existing* (or (:color light) {})
              merged* (merge default* existing*)
              pruned* (prune-color merged* (:color-type light))]
             (assoc light :color pruned*)))

(def-event-fx
  :light/update
  (fn [{:keys [db]} [_ update]]
      (let [original-light (get-in db [:light (:id update)])
            update* (update-transport db original-light update)
            update* (update-color update*)
            light-patch (differ/diff original-light update*)

            ;; in case of mqtt lights the root with which they are associated might change
            room-changed? (not= (:room-id original-light) (:room-id update*))
            original-room (get-in db [:room (:room-id original-light)])
            update-room (get-in db [:room (:room-id update*)])

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
           {:db       (assoc-in db [:light (:id update*)] update*)
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