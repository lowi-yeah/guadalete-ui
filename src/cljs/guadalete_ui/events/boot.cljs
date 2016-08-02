(ns guadalete-ui.events.boot
  (:require
    [re-frame.core :refer [dispatch def-event def-event-fx def-fx]]
    [thi.ng.geom.core.vector :refer [vec2]]
    [guadalete-ui.util :refer [pretty kw* mappify]]
    [guadalete-ui.console :as log]))


;//   _            _
;//  | |__ ___ ___| |_   ______ __ _ _  _ ___ _ _  __ ___
;//  | '_ \ _ \ _ \  _| (_-< -_) _` | || / -_) ' \/ _/ -_)
;//  |_.__\___\___/\__| /__\___\__, |\_,_\___|_||_\__\___|
;//                               |_|

;//              _
;//   _ __  __ _(_)_ _
;//  | '  \/ _` | | ' \
;//  |_|_|_\__,_|_|_||_|
;//
(def-event-fx
  :boot
  (fn [_ _]
    {:db       {:ws/connected? false
                :loading?      false
                :user/role     :none
                :view          {:panel      :blank
                                :section    :dash
                                :segment    :scene
                                :dimensions {:root   (vec2)
                                             :view   (vec2)
                                             :header (vec2)}}
                :tmp           {}                           ; for storing temporary items
                :name          "guadalete-ui"
                :message       ""}
     :dispatch [:do-sync-role]}))

;//                             _
;//   ____  _ _ _  __   _ _ ___| |___
;//  (_-< || | ' \/ _| | '_/ _ \ / -_)
;//  /__/\_, |_||_\__| |_| \___/_\___|
;//      |__/
(def-event-fx
  :do-sync-role
  (fn [world _]
    (let [sente-effect {:topic      :sync/role
                        :timeout    8000                    ;; optional see API docs
                        :on-success [:success-sync-role]
                        :on-failure [:failure-sync-role]
                        }]
      {:db    (:db world)
       :sente sente-effect})))

(def-event-fx
  :success-sync-role
  (fn [{:keys [db]} [_ {:keys [role]}]]
    (let [role* (keyword role)
          next-dispatch (if (= role* (or :admin :user))
                          [:do-sync-state]
                          [:set-panel :anonymous])]
      {:db       (assoc db :user/role role* :ws/connected? true)
       :dispatch next-dispatch})))

(def-event-fx
  :error-sync-role
  (fn [world error]
    (log/error ":error-sync-role" (str error))
    ;; todo: implement me!
    world))

;//   _             _              _           _
;//  | |___ __ _ __| |  __ ___ _ _| |_ ___ _ _| |_
;//  | / _ \ _` / _` | / _/ _ \ ' \  _/ -_) ' \  _|
;//  |_\___\__,_\__,_| \__\___/_||_\__\___|_||_\__|
;//
(def-event-fx
  :do-sync-state
  (fn [{:keys [db]} [_ role]]
    (let [sente-effect {:topic      :sync/state
                        :data       {:role role}
                        :timeout    8000                    ;; optional see API docs
                        :on-success [:success-sync-state]
                        :on-failure [:failure-sync-state]}]
      {:db    db
       :sente sente-effect})))

(def-event-fx
  :success-sync-state
  (fn [{:keys [db]} [_ state]]
    (let [role (:user/role db)
          rooms-map (mappify :id (:room state))
          lights-map (mappify :id (:light state))
          scenes-map (mappify :id (:scene state))
          colors-map (mappify :id (:color state))
          signals-map (mappify :id (:signal state))
          config (:config state)
          db* (assoc db
                :loading? false
                :room rooms-map
                :light lights-map
                :signal signals-map
                :color colors-map
                :scene scenes-map
                :config config)]
      {:db       db*
       :dispatch [:set-panel role]})))

(def-event
  :chsk/connect
  (fn [db _]
    ;; redirect back to / after login.
    (.assign js/location "/")
    db))