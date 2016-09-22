(ns guadalete-ui.handlers.socket
    (:require
      [clojure.core.match :refer [match]]
      [clojure.core.async :as async :refer [chan >! >!! <! go go-loop thread close! alts!]]
      [taoensso.timbre :as log]
      [cheshire.core :refer [generate-string]]
      [guadalete-ui.helpers.session :refer [session-role session-uid]]
      [guadalete-ui.helpers.config :refer [load-config]]
      [guadalete-ui.handlers.database :as db]
      [guadalete-ui.handlers.redis :as redis]
      [guadalete-ui.helpers.util :refer [pretty validate!]]
      [schema.core :as s]
      [guadalete-ui.schema :as gs]))

; custom sente functions
(def handshake-data-fn
  "Attach the user role to the handshake. can be one of [:admin :user :anonymous]"
  (fn [ring-req]
      (session-role ring-req)))

(def user-id-fn
  (fn [ring-req]
      (:session/key ring-req)))

(defn frontend-config []
      (:frontend (load-config)))


;//                   _     _                 _ _ _
;//   _____ _____ _ _| |_  | |_  __ _ _ _  __| | (_)_ _  __ _
;//  / -_) V / -_) ' \  _| | ' \/ _` | ' \/ _` | | | ' \/ _` |
;//  \___|\_/\___|_||_\__| |_||_\__,_|_||_\__,_|_|_|_||_\__, |
;//                                                     |___/
(defmulti event-handler :id)

; UIDPORT (ignored)
; ****************
(defmethod event-handler :chsk/uidport-open [_])
(defmethod event-handler :chsk/uidport-close [_])

;
;; PING
;; ****************
;(defmethod event-handler :chsk/ws-ping [ev-msg]
;           (log/debug ":chsk/ws-ping" ev-msg)
;           )

; STATE
; ****************

(defn- signal-values
       "Asynchronous helper for retrieveing the values for the given signal ids.
       Returns a channel onto which the timestamps & values will be written."
       [redis-conn signal-ids]
       (let [c (chan)]
            (redis/signal-values redis-conn signal-ids c)
            c))

;chsk-send! is a (fn [user-id event])


;; event handler for syncing the database state upon first contact from the browser.
;; Retrives all (relevant) data from the database and sends it in the reply function
;; Also reads transient values (e.g. sensor values) from redis and pushes them afterwards
(defmethod event-handler :sync/state
           [{:keys [uid ring-req ?reply-fn send-fn db-conn redis-conn redis-topics]}]
           (when (and (session-uid ring-req) ?reply-fn)
                 (let [state (db/everything db-conn)
                       state* (assoc state :config (frontend-config))]
                      (?reply-fn state*))))

; ROLE
; ****************
(defmethod event-handler :sync/role
           [{:keys [ring-req ?reply-fn]}]
           (log/debug ":sync/role")
           (when-let [role (session-role ring-req)]
                     (when ?reply-fn
                           (?reply-fn {:role role}))))

; ROOM
; ****************
;(defmethod event-handler :room/create
;           [{:keys [?data]}]
;           (let [room ?data]
;                (db/make-room room)))

(defmethod event-handler :room/update
           [{:keys [?data ?reply-fn db-conn]}]
           (let [[id diff flag] ?data
                 flag (or flag :patch)
                 response (db/update-room db-conn id diff flag)]
                (when ?reply-fn
                      (?reply-fn response))))

(defmethod event-handler :room/trash
           [{:keys [?data]}]
           (let [room ?data]
                ;(db/trash-scene scene-id)
                ))

; SCENE
; ****************
(defmethod event-handler :scene/make
           [{:keys [?data db-conn ?reply-fn]}]
           (log/debug ":scene/make" ?data)
           (let [response (db/create-scene db-conn ?data)]
                (log/debug "\tresponse" response)
                (when ?reply-fn
                      (?reply-fn response))))

(defmethod event-handler :scene/update
           [{:keys [?data db-conn ?reply-fn]}]
           (let [{:keys [id diff flag]} ?data
                 flag (or flag :patch)
                 response (db/update-scene db-conn id diff flag)]
                (when ?reply-fn
                      (?reply-fn response))))

(defmethod event-handler :scene/trash
           [{:keys [?data db-conn]}]
           (let [scene-id ?data]
                (log/debug "trash scene" scene-id)
                (db/trash-scene db-conn scene-id)))


; LIGHT
; ****************
(defmethod event-handler :light/make
           [{:keys [?data db-conn ?reply-fn]}]
           (let [response (db/create-light db-conn ?data)]
                (when ?reply-fn
                      (?reply-fn response))))

(defmethod event-handler :light/update
           [{:keys [?data ?reply-fn db-conn]}]
           (let [[id diff flag] ?data
                 flag (or flag :patch)
                 response (db/update-light db-conn id diff flag)]
                (when ?reply-fn
                      (?reply-fn response))))

(defmethod event-handler :light/trash
           [{:keys [?reply-fn ?data db-conn]}]
           (let [light-id ?data
                 response (db/trash-light db-conn light-id)]
                (when ?reply-fn
                      (log/debug "?reply-fn" ?reply-fn)
                      (?reply-fn response))))


;//                  _            _
;//   __ ___ _ _  ___ |_ __ _ _ _| |_
;//  / _/ _ \ ' \(_-<  _/ _` | ' \  _|
;//  \__\___/_||_/__/\__\__,_|_||_\__|
;//
(defmethod event-handler :constant/update
           [{:keys [?data ?reply-fn db-conn]}]
           (let [[id update] ?data
                 response (db/update-constant db-conn id update)]
                (when ?reply-fn
                      (?reply-fn response))))

(defmethod event-handler :constant/trash
           [{:keys [?reply-fn ?data db-conn]}]
           (let [constant-id ?data
                 response (db/trash-constant db-conn constant-id)]
                (when ?reply-fn
                      (log/debug "?reply-fn" ?reply-fn)
                      (?reply-fn response))))

;;//          _
;;//   __ ___| |___ _ _
;;//  / _/ _ \ / _ \ '_|
;;//  \__\___/_\___/_|
;;//
;(defmethod event-handler :color/make
;  [{:keys [?data db-conn ?reply-fn]}]
;  (let [color ?data
;        response (db/create-color db-conn color)]
;    (when ?reply-fn
;      (?reply-fn response))))
;
;(defmethod event-handler :color/update
;  [{:keys [?data db-conn ?reply-fn]}]
;  (let [[id diff flag] ?data
;        flag (or flag :patch)
;        response (db/update-color db-conn id diff flag)]
;    (log/debug ":color/update response:" response)
;    (when ?reply-fn
;      (?reply-fn response))))


;//         _
;//   _ __ (_)_ _____ _ _
;//  | '  \| \ \ / -_) '_|
;//  |_|_|_|_/_\_\___|_|
;//
(defmethod event-handler :mixer/make
           [{:keys [?data db-conn ?reply-fn]}]
           (let [response (db/create-mixer db-conn ?data)]
                (log/debug "creating color" ?data)
                (log/debug "response " response)
                (when ?reply-fn
                      (?reply-fn response))))




;//   _ _
;//  (_) |_ ___ _ __  ___
;//  | |  _/ -_) '  \(_-<
;//  |_|\__\___|_|_|_/__/
;//
(defmethod event-handler :items/update
           [{:keys [?data db-conn ?reply-fn]}]
           (let [response (db/update-items db-conn ?data)]
                (log/debug ":items/update" response)
                (when ?reply-fn
                      (?reply-fn response))))




; DEFAULT
; ****************
(defmethod event-handler :default
           [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
           (log/info "Unhandled sente event:" event ?data)
           (when ?reply-fn
                 (?reply-fn {:umatched-event-as-echoed-from-from-server event})))

;//              _
;//   _ __  __ _(_)_ _
;//  | '  \/ _` | | ' \
;//  |_|_|_\__,_|_|_||_|
;//
;redis :redis
(defn sente-handler [{db :db redis :redis}]
      (fn [ev-msg]
          ; put the database into the event map, so that those handlers that need it can use it
          ; (I'm looking at you :sync/state)
          ; also inject the redis connection object so we can get more transient data (eg signal values)
          (event-handler (assoc ev-msg
                                :db-conn (:conn db)
                                :redis-conn (:connection redis)
                                :redis-topics (:topics redis)))))