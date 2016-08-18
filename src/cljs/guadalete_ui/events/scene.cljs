(ns guadalete-ui.events.scene
  (:require
    [re-frame.core :refer [dispatch def-event def-event-fx def-fx]]
    [differ.core :as differ]
    [guadalete-ui.util :refer [pretty]]
    [guadalete-ui.console :as log]
    [guadalete-ui.util.dickens :as dickens]))

;// re-frame_  __        _
;//   ___ / _|/ _|___ __| |_ ___
;//  / -_)  _|  _/ -_) _|  _(_-<
;//  \___|_| |_| \___\__|\__/__/
;//
(defn- new-scene-effect
  "Creates a sente-effect for syncing the new light with the backend"
  [scene]
  {:topic      :scene/make
   :data       scene
   :on-success [:success-scene-make]
   :on-failure [:failure-scene-make]})

(defn sync-effect [{:keys [old new scene]}]
  (let [id (if scene (:id scene) (:id new))
        flag (if scene :replace :patch)
        diff (if (= :replace flag)
               scene
               (differ/diff old new))
        data {:id   id
              :flag flag
              :diff diff}]
    {:topic      :scene/update
     :data       data
     :on-success [:success-scene-update]
     :on-failure [:failure-scene-update]}))

(defn trash-effect [scene-id]
  {:topic      :scene/trash
   :data       scene-id
   :on-success [:success-scene-trash]
   :on-failure [:failure-scene-trash]})

;// re-frame          _     _                 _ _
;//   _____ _____ _ _| |_  | |_  __ _ _ _  __| | |___ _ _ ___
;//  / -_) V / -_) ' \  _| | ' \/ _` | ' \/ _` | / -_) '_(_-<
;//  \___|\_/\___|_||_\__| |_||_\__,_|_||_\__,_|_\___|_| /__/
;//

(def-event-fx
  :scene/make
  (fn [{:keys [db]} [_ room-id]]
    (let [scene {:flows       {}
                 :id          (str (random-uuid))
                 :room-id     room-id
                 :mode        "none"
                 :name        (dickens/generate-name)
                 :nodes       {}
                 :on?         false
                 :translation {:x 0 :y 0}}]
      {:db    db
       :sente (new-scene-effect scene)}
      )))


(def-event-fx
  ;; when the light has been created successfully,
  ;; dispatch two things:
  ;;  1. update the root to which the light belongs
  ;;  2. show the edit-modal for the new light
  :success-scene-make
  (fn [{:keys [db]} [_ response]]
    (let [scene (:ok response)
          error-msg (:error response)]
      (if scene
        (let [room-id (:room-id scene)
              room (get-in db [:room room-id])
              scenes* (conj (:scene room) (:id scene))
              room* (assoc room :scene scenes*)
              modal-data {:item-id    (:id scene)
                          :ilk        :scene
                          :modal-type :scene}
              db* (-> db
                      (assoc-in [:room room-id] room*)
                      (assoc-in [:scene (:id scene)] scene)
                      (assoc :modal modal-data))]
          {:db       db*
           :dispatch [:room/update {:new room* :old room}]
           :modal    [:show {}]})
        (do
          (log/error "error during scene creation:" error-msg)
          {:db db})))))

;; EDIT
;; ********************************
(def-event-fx
  :scene/edit
  (fn [{:keys [db]} [_ scene-id]]
    (let [data {:item-id    scene-id
                :ilk        :scene
                :modal-type :scene}]

      {:db    (assoc db :modal data)
       :modal [:show {}]})))

;; UPDATE
;; ********************************
(def-event-fx
  :scene/update
  (fn [{:keys [db]} [_ scene]]
    (let [old (get-in db [:scene (:id scene)])
          new scene]
      {:db    (assoc-in db [:scene (:id scene)] new)
       :sente (sync-effect {:old old :new new})})))

(def-event
  :success-scene-update
  (fn [world response]
    (log/debug ":success-scene-update" response)
    world))

(def-event
  :failure-scene-update
  (fn [world response]
    (log/error "Scene update failed:" response)
    world))


;; TRASH
;; ********************************
(def-event-fx
  :scene/trash
  (fn [{:keys [db]} [_ scene-id]]

    (let [scenes (:scene db)
          scene (get scenes scene-id)
          scenes* (dissoc scenes scene-id)
          room-id (:room-id scene)
          room (get-in db [:room room-id])
          scene-ids* (into [] (remove #(= scene-id %) (:scene room)))
          room* (assoc room :scene scene-ids*)

          db* (-> db
                  (assoc-in [:room room-id] room*)
                  (assoc :scene scenes*)
                  (dissoc :modal))]
      {:db       db*
       :dispatch [:room/update {:old room :new room*}]
       :sente    (trash-effect scene-id)
       :modal    [:hide {}]
       })))


(def-event-fx
  :success-scene-trash
  (fn [{:keys [db]} [_ response]]
    (let [light (:ok response)
          error-msg (:error response)]
      {:db db})))

(def-event-fx
  :failure-scene-trash
  (fn [{:keys [db]} [_ response]]
    (log/warn "trash scene failed" response)
    {:db db}))
