(ns guadalete-ui.subscriptions
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :as re-frame :refer [def-sub subscribe]]
            [clojure.set :refer [difference]]
            [clojure.string :as string]
            [guadalete-ui.items :refer [assemble-item]]
            [guadalete-ui.util :refer [pretty in? kw*]]
            [guadalete-ui.dmx :as dmx]
            [guadalete-ui.console :as log]
            ))


;//           _              _      _   _
;//   ____  _| |__ _____ _ _(_)_ __| |_(_)___ _ _  ___
;//  (_-< || | '_ (_-< _| '_| | '_ \  _| / _ \ ' \(_-<
;//  /__/\_,_|_.__/__\__|_| |_| .__/\__|_\___/_||_/__/
;//                           |_|
(def-sub
  :main-panel
  (fn [db _]
    (:main-panel db)))

(def-sub
  :user/role
  (fn [db _]
    (:user/role db)))

;//
;//   _ _ ___ ___ _ __  ___
;//  | '_/ _ \ _ \ '  \(_-<
;//  |_| \___\___/_|_|_/__/
;//
(def-sub
  :rooms
  (fn [db _]
    (vals (:room db))))

;//      _                _
;//   ____)__ _ _ _  __ _| |
;//  (_-< / _` | ' \/ _` | |
;//  /__/_\__, |_||_\__,_|_|
;//       |___/
(def-sub
  :signal/all
  (fn [db _]
    (->> (:signal db)
         (remove (fn [s] (nil? s))))))

;//      _
;//   __| |_ __ __ __
;//  / _` | '  \\ \ /
;//  \__,_|_|_|_/_\_\
;//
(def-sub
  :dmx/available
  (fn [db _]
    (dmx/assignable db)))

(def-sub
  :dmx/all
  (fn [db _]
    (dmx/all db)))

;//                           _
;//   __ _  _ _ _ _ _ ___ _ _| |_
;//  / _| || | '_| '_/ -_) ' \  _|
;//  \__|\_,_|_| |_| \___|_||_\__|
;//
(def-sub
  :current/view
  (fn [db _]
    (:current/view db)))

(def-sub
  :current/segment
  (fn [db _]
    (:current/segment db)))


(def-sub
  :current/room
  (fn [db [_ {:keys [assemble]}]]
    (let [room-id (reaction (:current/room-id db))
          room (get-in db [:room @room-id])]
      (if assemble
        (assemble-item :room db room)
        room
        ))))

(def-sub
  :current/light
  (fn [db _]
    (let [light-id (:current/light-id db)
          light (get-in db [:light light-id])]
      light)))

(def-sub
  :current/scene
  (fn [db _]
    (let [scene-id (:current/scene-id db)
          _ (log/debug "current scene" scene-id)
          scene (get-in db [:scene scene-id])]
      scene)))

(def-sub
  :selected
  (fn [db _]
    (let [nodes (get-in db [:scene "scene2" :nodes])
          selected-nodes (filter (fn [[k v]] (:selected v)) nodes)]
      selected-nodes)))


;//   _ _      _   _
;//  | (_)__ _| |_| |_
;//  | | / _` | ' \  _|
;//  |_|_\__, |_||_\__|
;//      |___/
(def-sub
  :light/unused-by-scene
  (fn [db [_ room-id scene-id]]
    (let [all-light-ids (into #{} (get-in db [:room room-id :light]))
          used-light-ids (->>
                           (get-in db [:scene scene-id :nodes])
                           (filter (fn [[id l]] (= :light (kw* (:ilk l)))))
                           (map (fn [[id l]] (:item-id l)))
                           (filter (fn [id] id))
                           (into #{}))
          unused (difference all-light-ids used-light-ids)]
      (into [] unused))))


;//           _
;//   _ __ __| |
;//  | '_ \ _` |
;//  | .__\__,_|
;//  |_|
;// => see pd.subscriptions

;//   _   _                 _        _          _      _        _
;//  | |_| |_  ___  __ __ __ |_  ___| |___   __| |__ _| |_ __ _| |__ __ _ ______
;//  |  _| ' \/ -_) \ V  V / ' \/ _ \ / -_) / _` / _` |  _/ _` | '_ \ _` (_-< -_)
;//   \__|_||_\___|  \_/\_/|_||_\___/_\___| \__,_\__,_|\__\__,_|_.__\__,_/__\___|
;//
(def-sub :db (fn [db _] db))
