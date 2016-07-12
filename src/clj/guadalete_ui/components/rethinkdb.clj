(ns guadalete-ui.components.rethinkdb
    (:require
      [com.stuartsierra.component :as component]
      [taoensso.timbre :as log]
      [rethinkdb.query :as r]
      [rethinkdb.core :refer [close]]
      [clojure.core.async :refer (go >!)]
      ))

;(defn publish
;      [pubsub id message]
;      (let [pubsub-channel (:input-channel pubsub)
;            data {:topik :db/events :id id :message message}]
;           (go (>! pubsub-channel data))))

(defrecord Rethink [host port auth-key db]
           component/Lifecycle
           (start [component]
                  (let [conn (r/connect :host host :port port :auth-key auth-key :db db)]
                       (log/info (str "Connecting to rethinkDB: " host ":" port " " db))
                       ;(go (Thread/sleep 400) ((:publish pubsub) :db/events db :connected))
                       (assoc component
                              :conn conn)))

           (stop [component]
                 (close (:conn component))
                 ;(if host
                 ;  ((:publish pubsub) :db/events db :disconected)
                 ;  ((:publish pubsub) :db/events "rethink_dev" :disconected))
                 (dissoc component :conn)))

(defn new-rethink-db
      [config]
      (map->Rethink config))
