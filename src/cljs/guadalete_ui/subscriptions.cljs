(ns guadalete-ui.subscriptions
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :as re-frame]
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
(re-frame/register-sub
  :main-panel
  (fn [db _]
      (reaction (:main-panel @db))))

(re-frame/register-sub
  :user/role
  (fn [db _]
      (reaction (:user/role @db))))

;//
;//   _ _ ___ ___ _ __  ___
;//  | '_/ _ \ _ \ '  \(_-<
;//  |_| \___\___/_|_|_/__/
;//
(re-frame/register-sub
  :rooms
  (fn [db _]
      (reaction (vals (:room @db)))))

;//      _                _
;//   ____)__ _ _ _  __ _| |
;//  (_-< / _` | ' \/ _` | |
;//  /__/_\__, |_||_\__,_|_|
;//       |___/
(re-frame/register-sub
  :signal/all
  (fn [db _]
      (->> (:signal @db)
           (remove (fn [s] (nil? s)))
           (reaction))))

;//      _
;//   __| |_ __ __ __
;//  / _` | '  \\ \ /
;//  \__,_|_|_|_/_\_\
;//
(re-frame/register-sub
  :dmx/available
  (fn [db _]
      (reaction (dmx/assignable @db))))

(re-frame/register-sub
  :dmx/all
  (fn [db _]
      (reaction (dmx/all @db))))

;//                           _
;//   __ _  _ _ _ _ _ ___ _ _| |_
;//  / _| || | '_| '_/ -_) ' \  _|
;//  \__|\_,_|_| |_| \___|_||_\__|
;//
(re-frame/register-sub
  :current/view
  (fn [db _]
      (reaction (:current/view @db))))

(re-frame/register-sub
  :current/segment
  (fn [db _]
      (reaction (:current/segment @db))))


(re-frame/register-sub
  :current/room
  (fn [db [_ {:keys [assemble]}]]
      (let [room-id (reaction (:current/room-id @db))
            room (get-in @db [:room @room-id])]
           (if assemble
             (reaction (assemble-item :room @db room))
             (reaction room)
             ))))

(re-frame/register-sub
  :current/light
  (fn [db _]
      (let [light-id (:current/light-id @db)
            light (get-in @db [:light light-id])]
           ;(reaction (:new/light @db))
           (reaction light))))

(re-frame/register-sub
  :current/scene
  (fn [db _]
      (let [scene-id (:current/scene-id @db)
            scene (get-in @db [:scene scene-id])]
           (reaction scene))))


(re-frame/register-sub
  :selected
  (fn [db _]
      (let [scene-id (:current/scene-id @db)
            nodes (get-in @db [:scene "scene2" :nodes])
            selected-nodes (filter (fn [[k v]] (:selected v)) nodes)]
           (reaction selected-nodes))))


;//   _ _      _   _
;//  | (_)__ _| |_| |_
;//  | | / _` | ' \  _|
;//  |_|_\__, |_||_\__|
;//      |___/
(re-frame/register-sub
  :light/unused-by-scene
  (fn [db [_ room-id scene-id]]
      (let [all-light-ids (into #{} (get-in @db [:room room-id :light]))
            used-light-ids (->>
                             (get-in @db [:scene scene-id :nodes])
                             (filter (fn [[id l]] (= :light (kw* (:ilk l)))))
                             (map (fn [[id l]] (:item-id l)))
                             (filter (fn [id] id))
                             (into #{}))
            unused (difference all-light-ids used-light-ids)]
           (reaction (into [] unused)))))


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
(re-frame/register-sub
  :db
  (fn [db _]
      (reaction @db)))
