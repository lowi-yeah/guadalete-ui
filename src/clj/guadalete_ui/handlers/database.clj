(ns guadalete-ui.handlers.database
    (:require
      [rethinkdb.query :as r]
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

(defn- table-map [connection table map-key]
       "Generic convenience function for extracting all enties of a given table into a map"
       (mappify map-key (get-all connection table)))

;//
;//   _  _ ______ _ _ ___
;//  | || (_-< -_) '_(_-<
;//   \_,_/__\___|_| /__/
;//
(defn all-users-as-map
      "get all users from the database. Instead of returning just an array, a map in which the usernames are the keys is returned"
      [connection]
      (log/debug "all-users-as-map")
      (table-map connection "user" :username))




(defn get-rooms [connection] (get-all connection :room))

;//   _   _                 _        _          _        _
;//  | |_| |_  ___  __ __ __ |_  ___| |___   ___ |_  ___| |__ __ _ _ _  __ _
;//  |  _| ' \/ -_) \ V  V / ' \/ _ \ / -_) (_-< ' \/ -_) '_ \ _` | ' \/ _` |
;//   \__|_||_\___|  \_/\_/|_||_\___/_\___| /__/_||_\___|_.__\__,_|_||_\__, |
;//                                                                    |___/
(defn everything
      "get the complete db"
      [connection]
      (let [rooms (get-rooms connection)
            ]
           {:room rooms}))