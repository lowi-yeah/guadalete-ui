(ns guadalete-ui.handlers.database
    (:require
      [rethinkdb.query :as r]
      [differ.core :as differ]
      [taoensso.timbre :as log]
      [guadalete-ui.helpers.util :refer [mappify in? validate! pretty]]
      [guadalete-ui.handlers.defaults :as defaults]
      [schema.core :as s]
      [guadalete-ui.schema :as gs])
    (:import (clojure.lang ExceptionInfo)))

;//                         _
;//   __ _ ___ _ _  ___ _ _(_)__
;//  / _` / -_) ' \/ -_) '_| / _|
;//  \__, \___|_||_\___|_| |_\__|
;//  |___/
(defn- prune-all
       [coll]
       (map #(dissoc % :created :updated) coll))

(defn- prune
       [item]
       (dissoc item :created :updated))

(s/defn ^:always-validate get-all
        [connection
         type :- s/Keyword]
        (-> (r/table type)
            (r/run connection)
            (prune-all)
            (defaults/all type)
            (gs/coerce-all type)))

(s/defn ^:always-validate get-one
        [connection
         type :- s/Keyword
         id :- s/Str]
        (-> (r/table type)
            (r/get id)
            (r/run connection)
            (prune)
            (defaults/one type)
            (gs/coerce! type)))

(s/defn ^:always-validate table-map
        "Generic convenience function for extracting all enties of a given table into a map"
        [connection
         table-key :- s/Keyword
         map-key :- s/Keyword]
        (mappify map-key (get-all connection table-key)))

(defn create-item
      [connection type item]
      (try
        (do
          (-> (r/table type)
              (r/insert item)
              (r/run connection))
          ;return
          {:ok item})
        (catch ExceptionInfo ex
          (let [msg (.getMessage ex)]
               {:error msg}))))

(defn update-item
      "Generic update function called by update-room, update-light & update-sensor"
      ([_connection id diff type] (update-item id diff type :replace))
      ([connection id diff type flag]
        (let [item (get-one connection type id)
              patch (->
                      (condp = flag
                             :patch (differ/patch item diff)
                             :replace diff
                             (str "unexpected flag, \"" (str flag) \"))
                      (prune)
                      (gs/coerce! type))]
             (log/debug "update-item" type)
             (log/debug patch)
             (try
               (let [result (-> (r/table type)
                                (r/get id)
                                (r/replace patch)
                                (r/run connection))]
                    (log/debug "UPDATE result:" (pretty result))
                    {:ok patch})
               (catch ExceptionInfo ex
                 (let [msg (.getMessage ex)]
                      {:error msg}))))))

;(s/defn update-item :- gs/UpdateResponse
;  "Generic update function called by update-room, update-light & update-sensor"
;  ([_connection id diff type] (update-item id diff type :replace))
;  ([connection
;    id :- s/Str
;    diff :- gs/Diff
;    type :- s/Keyword
;    flag :- (s/enum :patch :replace)]
;    (let [item (get-one connection type id)
;          patch (condp = flag
;                  :patch (differ/patch item diff)
;                  :replace diff
;                  (str "unexpected flag, \"" (str flag) \"))]
;      (try
;        (do
;          (-> (r/table type)
;              (r/get id)
;              (r/replace patch)
;              (r/run connection))
;          {:ok patch})
;        (catch ExceptionInfo ex
;          (let [msg (.getMessage ex)]
;            {:error msg}))))))

(s/defn ^:always-validate trash-item :- gs/UpdateResponse
        "Generic update function called by update-room, update-light & update-sensor"
        ([connection type id]
          (try
            (do
              (-> (r/table type)
                  (r/get id)
                  (r/delete)
                  (r/run connection))
              {:ok id})
            (catch ExceptionInfo ex
              (let [msg (.getMessage ex)]
                   {:error msg})))))

(s/defn ^:always-validate update-items :- gs/UpdateResponse
        "Updates all items of a given ilk with the given patch"
        [db-conn
         {:keys [ilk diff]} :- gs/Patch]
        (let [items (get-all db-conn ilk)
              patch (->>
                      (differ/patch items diff)
                      (vals)
                      (into []))]
             (try
               (let [response (-> (r/table ilk)
                                  (r/insert patch)
                                  (r/run db-conn))]
                    {:ok response})

               (catch ExceptionInfo ex
                 (let [msg (.getMessage ex)]
                      {:error msg})))))


;//                            _ _      _   _
;//   _ _ ___ ___ _ __  ___   | (_)__ _| |_| |_ ___    _____ ___ _ _  ___ ___
;//  | '_/ _ \ _ \ '  \(_-<_  | | / _` | ' \  _(_-<_  (_-< _/ -_) ' \/ -_)_-<_ _ _
;//  |_| \___\___/_|_|_/__( ) |_|_\__, |_||_\__/__( ) /__\__\___|_||_\___/__(_)_)_)
;//                       |/      |___/           |/
; Room
; ****************
(s/defn ^:always-validate get-rooms :- [gs/Room]
        [connection]
        (->> :room
             (get-all connection)))

(defn update-room [connection id diff flag]
      (update-item connection id diff :room flag))

; Light
; ****************
(s/defn ^:always-validate get-lights :- [gs/Light]
        [connection]
        (->> (get-all connection :light)
             (map #(dissoc % :created :updated))))

(defn create-light [connection light]
      (create-item connection :light light))

(s/defn ^:always-validate update-light
        [connection
         id :- s/Str
         diff
         flag :- (s/enum :update :patch)]
        (update-item connection id diff :light flag))

(defn trash-light
      "Handler function for removing a light. Not only removes the light form the DB,
      but also removes the reference from the room it -the light- is assigned to."
      [connection id]
      (let [room (->> (get-rooms connection)
                      (filter #(in? (:light %) id))
                      (first))]
           (when (not (nil? room))
                 (let [room-lights (->> (:light room)
                                        (remove #(= id %))
                                        (into []))
                       room* (assoc room :light room-lights)]
                      (update-item (:id room*) room* :room :replace)))
           (trash-item connection :light id)))

; Scene
; ****************
(defn create-scene [connection scene]
      (create-item connection :scene scene))

(s/defn ^:always-validate get-scenes :- [gs/Scene]
        [connection]
        (->> :scene
             (get-all connection)))

(defn update-scene [connection id diff flag]
      (update-item connection id diff :scene flag))

(defn trash-scene [connection id]
      (trash-item connection :scene id))

; Color
; ****************
(defn get-colors [connection]
      (get-all connection :color))

(defn create-color [connection color]
      (create-item connection :color color))

(defn update-color [connection id diff flag]
      (update-item connection id diff :color flag))

; Signal
; ****************
(defn get-signals [connection]
      (->> :signal
           (get-all connection)
           (prune-all)))

(defn signal-ids [connection]
      (->> (get-all connection :signal)
           (map #(get % :id))))

; Mixer
; ****************
(defn get-mixers [connection]
      (get-all connection :mixer))

(defn create-mixer [connection mixer]
      (create-item connection :mixer mixer))

; Constants
; ****************
(defn get-constants [connection]
      (get-all connection :constant))


;//
;//   _  _ ______ _ _ ___
;//  | || (_-< -_) '_(_-<
;//   \_,_/__\___|_| /__/
;//
(defn all-users-as-map
      "get all users from the database. Instead of returning just an array, a map in which the usernames are the keys is returned"
      [connection]
      (table-map connection :user :username))

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
            constants (get-constants connection)
            signals (get-signals connection)
            mixers (get-mixers connection)]


           (log/debug "DB/all constants" (into [] constants))
           {:room     rooms
            :light    lights
            :scene    scenes
            :color    colors
            :signal   signals
            :mixer    mixers
            :constant constants}))