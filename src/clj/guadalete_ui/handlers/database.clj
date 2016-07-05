(ns guadalete-ui.handlers.database
    (:require
      [rethinkdb.query :as r]
      [differ.core :as differ]
      [taoensso.timbre :as log]
      [guadalete-ui.helpers.util :refer [mappify]]
      ))

;//                         _
;//   __ _ ___ _ _  ___ _ _(_)__
;//  / _` / -_) ' \/ -_) '_| / _|
;//  \__, \___|_||_\___|_| |_\__|
;//  |___/
(defn- get-all [connection type]
       (-> (r/table type)
           (r/run connection)))

(defn- get-one [connection type id]
       (-> (r/table type)
           (r/get id)
           (r/run connection)))

(defn- table-map [connection table map-key]
       "Generic convenience function for extracting all enties of a given table into a map"
       (mappify map-key (get-all connection table)))

(defn- create-item [connection type item]
       (log/debug "create item" item)
       (-> (r/table type)
           (r/insert item)
           (r/run connection)))

(defn- update-item
       "Generic update function called by update-room, update-light & update-sensor"
       ([connection id diff type] (update-item id diff type :replace))
       ([connection id diff type flag]
         (let [item (get-one connection type id)
               patch (condp = flag
                            :patch (differ/patch item diff)
                            :replace diff
                            (str "unexpected flag, \"" (str flag) \"))]

              (log/debug "update item" patch)
              (-> (r/table type)
                  (r/get id)
                  (r/update patch)
                  (r/run connection)))))

;//                            _ _      _   _
;//   _ _ ___ ___ _ __  ___   | (_)__ _| |_| |_ ___    _____ ___ _ _  ___ ___
;//  | '_/ _ \ _ \ '  \(_-<_  | | / _` | ' \  _(_-<_  (_-< _/ -_) ' \/ -_)_-<_ _ _
;//  |_| \___\___/_|_|_/__( ) |_|_\__, |_||_\__/__( ) /__\__\___|_||_\___/__(_)_)_)
;//                       |/      |___/           |/

; Room
; ****************
(defn get-rooms [connection]
      (get-all connection :room))

(defn update-room [connection id diff flag]
      (update-item connection id diff :room flag))


; Light
; ****************
(defn get-lights [connection]
      (get-all connection :light))

(defn create-light [connection light]
      (log/debug "creaating light" light)
      (create-item connection :light light))

(defn update-light [connection id diff flag]
      (update-item connection id diff :light flag))




; Scene
; ****************
(defn- mode-keyword [scene]
       (let [mode (:mode scene)
             mode* (keyword mode)]
            (assoc scene :mode mode*)))

(defn get-scenes [connection]
      (let [scenes (get-all connection :scene)
            scenes* (map mode-keyword scenes)]
           (into [] scenes*)))

(defn update-scene [connection id diff flag]
      (update-item connection id diff :scene flag))

; Color
; ****************
(defn get-colors [connection]
      (get-all connection :color))

(defn create-color [connection color]
      (log/debug "creaating color" color)
      (create-item connection :color color))

;//
;//   _  _ ______ _ _ ___
;//  | || (_-< -_) '_(_-<
;//   \_,_/__\___|_| /__/
;//
(defn all-users-as-map
      "get all users from the database. Instead of returning just an array, a map in which the usernames are the keys is returned"
      [connection]
      (table-map connection "user" :username))

;//   _   _                 _        _          _        _
;//  | |_| |_  ___  __ __ __ |_  ___| |___   ___ |_  ___| |__ __ _ _ _  __ _
;//  |  _| ' \/ -_) \ V  V / ' \/ _ \ / -_) (_-< ' \/ -_) '_ \ _` | ' \/ _` |
;//   \__|_||_\___|  \_/\_/|_||_\___/_\___| /__/_||_\___|_.__\__,_|_||_\__, |
;//                                                                    |___/
(defn everything
      "get the complete db"
      [connection]
      (let [rooms (get-rooms connection)
            lights (get-lights connection)
            scenes (get-scenes connection)
            colors (get-colors connection)
            ]
           {:room   rooms
            :light  lights
            :scene  scenes
            :color colors}))