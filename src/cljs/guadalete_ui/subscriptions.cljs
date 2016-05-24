(ns guadalete-ui.subscriptions
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :as re-frame]
            [clojure.set :refer [difference]]
            [clojure.string :as string]
            [guadalete-ui.console :as log]))


(re-frame/register-sub
  :active-panel
  (fn [db _]
      (reaction (:active-panel @db))))

(re-frame/register-sub
  :user/role
  (fn [db _]
      (reaction (:user/role @db))))

(re-frame/register-sub                                      ;; we can check if there is data
  :initialised?                                             ;; usage (subscribe [:initialised?])
  (fn [db]
      (reaction (contains? @db :room))))




(re-frame/register-sub
  :name
  (fn [db]
      (reaction (:name @db))))

(re-frame/register-sub
  :message
  (fn [db]
      (reaction (:message @db))))




(defn- items
       "Generic function for retrieving items form the db.
       An optional room-id may be given, which filters the items belonging to that room only."
       [db type current?]
       (cond
         (not current?) (vals (get @db type))
         :else (let [room-id (:current/room-id @db)
                     room-item-ids (set (get-in @db [:room room-id type])) ; convert to set, otherwise the contains? filter won't work
                     filter-fn #(contains? room-item-ids (:id %))
                     items (vals (get @db type))]
                    (filter filter-fn items))))

;//                           _
;//   ___  ___  __ _ _ __ ___| |__
;//  / __|/ _ \/ _` | '__/ __| '_ \
;//  \__ \  __/ (_| | | | (__| | | |
;//  |___/\___|\__,_|_|  \___|_| |_|
;//
(re-frame/register-sub
  :search/term
  (fn [db _]
      (reaction (:search/term @db))))

;//
;//   _ __ ___   ___  _ __ ___  ___
;//  | '__/ _ \ / _ \| '_ ` _ \/ __|
;//  | | | (_) | (_) | | | | | \__ \
;//  |_|  \___/ \___/|_| |_| |_|___/
;//
(re-frame/register-sub
  :rooms
  (fn [db _]
      (reaction (vals (:room @db)))))

(re-frame/register-sub
  :room/scenes
  (fn [db [_ room-id]]
      (let [room (get-in @db [:room room-id])
            scenes (into [] (map #(get-in @db [:scene %]) (:scene room)))]
           (reaction scenes))))

(re-frame/register-sub
  :room/lights
  (fn [db [_ room-id]]
      (let [room (get-in @db [:room room-id])
            lights (into [] (map #(get-in @db [:light %]) (:light room)))]
           (reaction lights))))

(re-frame/register-sub
  :current/room-id
  (fn [db _]
      (reaction (:current/room-id @db))))

(re-frame/register-sub
  :current/room
  (fn [db _]
      (let [room-id (reaction (:current/room-id @db))]
           (reaction (get-in @db [:room @room-id])))))


;//
;//   ___  ___ _ __  ___  ___  _ __ ___
;//  / __|/ _ \ '_ \/ __|/ _ \| '__/ __|
;//  \__ \  __/ | | \__ \ (_) | |  \__ \
;//  |___/\___|_| |_|___/\___/|_|  |___/
;//
; retrieves the sensor list
; there are the following options:
;   - current?: if truthy, returns only the sensors attached to the current room
;   - filter?: if truthy, filters the sensor list according to ':search/term'
(re-frame/register-sub
  :sensors
  (fn [db [_ {:keys [current? filter?]}]]
      (reaction
        (let [sensors (items db :sensor current?)
              filter-term-rctn (reaction (:search/term @db))
              filter-regex (re-pattern (str "(?i)^.*" @filter-term-rctn ".*$"))
              filter-fn #(or
                          (re-matches filter-regex (or (get-in % [:room :name]) "")) ; might be nil, so we need an 'or'
                          (re-matches filter-regex (:name %))
                          (re-matches filter-regex (:hostname %)))
              filtered-sensors (if filter?
                                 (filter filter-fn sensors)
                                 sensors)
              ]
             filtered-sensors
             ))))

(re-frame/register-sub
  :sensor
  (fn [db [_ sensor-id]]
      (reaction
        (let [rooms-rctn (reaction (vals (:room @db)))
              filter-fn #(some (partial = sensor-id) (:sensor %))
              room (first (filter filter-fn @rooms-rctn))
              sensor (get-in @db [:sensor sensor-id])
              ]
             (assoc sensor :room {:name (:name room) :id (:id room)})))))

(re-frame/register-sub
  :sort/sensor
  (fn [db _]
      (reaction (:sort/sensor @db))))

(re-frame/register-sub
  :sort-order/sensor
  (fn [db _]
      (reaction (:sort-order/sensor @db))))

(re-frame/register-sub
  :sensor/edit?
  (fn [db [_ sensor-id]]
      (reaction
        (let [edits (:edit @db)]
             (some #(= sensor-id %) edits)))))

(re-frame/register-sub
  :current/sensor
  (fn [db _]
      (let [sensor-id (reaction (:current/sensor-id @db))]
           (reaction (get-in @db [:sensor @sensor-id])))))

;//
;//   ___  ___ ___ _ __   ___ ___
;//  / __|/ __/ _ \ '_ \ / _ \ __|
;//  \__ \ (__  __/ | | |  __\__ \
;//  |___/\___\___|_| |_|\___|___/
;//

(re-frame/register-sub
  :scenes
  (fn [db [_ {:keys [current?]}]]
      (reaction (into [] (items db :scene current?)))))

(re-frame/register-sub
  :current/scene-id
  (fn [db _]
      (reaction (:current/scene-id @db))))

(re-frame/register-sub
  :current/scene
  (fn [db _]
      (let [scene-id (reaction (:current/scene-id @db))]
           (reaction (get-in @db [:scene @scene-id])))))

;//   _ _       _     _
;//  | (_) __ _| |__ | |_ ___
;//  | | |/ _` | '_ \| __/ __|
;//  | | | (_| | | | | |_\__ \
;//  |_|_|\__, |_| |_|\__|___/
;//       |___/
(re-frame/register-sub
  :lights
  (fn [db [_ {:keys [current? filter?]}]]
      (reaction
        (let [lights (items db :light current?)
              filter-term-rctn (reaction (:search/term @db))
              filter-regex (re-pattern (str "(?i)^.*" @filter-term-rctn ".*$"))
              filter-fn #(or
                          (re-matches filter-regex (:name %))
                          (re-matches filter-regex (str (:dmx %))))]
             (if filter?
               (filter filter-fn lights)
               lights)))))

(re-frame/register-sub
  :light
  (fn [db [_ light-id]]
      (reaction (get-in @db [:light light-id]))))

(re-frame/register-sub
  :light/edit?
  (fn [db [_ light-id]]
      (reaction
        (let [edits (:edit @db)
              edit? (some #(= light-id %) edits)]
             edit?))))

(re-frame/register-sub
  :sort/light
  (fn [db _]
      (reaction (:sort/light @db))))

(re-frame/register-sub
  :sort-order/light
  (fn [db _]
      (reaction (:sort-order/light @db))))


;//       _
;//    __| |_ __ ___ __  __
;//   / _` | '_ ` _ \\ \/ /
;//  | (_| | | | | | |>  <
;//   \__,_|_| |_| |_/_/\_\
;//
(re-frame/register-sub
  :dmx/available
  (fn [db _]
      (reaction
        (let [lights-rctn (reaction (vals (:light @db)))
              assigned-dmx (set (flatten (into [] (map #(:dmx %)) @lights-rctn)))
              all-dmx (set (range 1 513))]
             (sort (difference all-dmx assigned-dmx))))))

(re-frame/register-sub
  :dmx/assigned
  (fn [db _]
      (reaction
        (let [lights (vals (:light @db))]
             (sort (set (flatten (into [] (map #(:dmx %)) lights))))))))

;//       _                   _
;//   ___(_) __ _ _ __   __ _| |___
;//  / __| |/ _` | '_ \ / _` | / __|
;//  \__ \ | (_| | | | | (_| | \__ \
;//  |___/_|\__, |_| |_|\__,_|_|___/
;//         |___/

(re-frame/register-sub
  :signals
  (fn [db _]
      (reaction (vals (:signal @db)))))

(re-frame/register-sub
  :signal
  (fn [db [_ signal-id]]
      (reaction (get-in @db [:signal signal-id]))))


;//   _   _                 _        _          _      _        _
;//  | |_| |_  ___  __ __ __ |_  ___| |___   __| |__ _| |_ __ _| |__ __ _ ______
;//  |  _| ' \/ -_) \ V  V / ' \/ _ \ / -_) / _` / _` |  _/ _` | '_ \ _` (_-< -_)
;//   \__|_||_\___|  \_/\_/|_||_\___/_\___| \__,_\__,_|\__\__,_|_.__\__,_/__\___|
;//
(re-frame/register-sub
  :db
  (fn [db _]
      (reaction @db)))
