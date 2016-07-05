(ns guadalete-ui.handlers.socket
    (:require
      [clojure.core.match :refer [match]]
      [taoensso.timbre :as log]
      [guadalete-ui.helpers.session :refer [session-role session-uid]]
      [guadalete-ui.handlers.database :as db]))

; custom sente functions
(def handshake-data-fn
  "Attach the user role to the handshake. can be one of [:admin :user :anonymous]"
  (fn [ring-req]
      (session-role ring-req)))

(def user-id-fn
  (fn [ring-req]
      (:session/key ring-req)))


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


; PING
; ****************
(defmethod event-handler :chsk/ws-ping [ev-msg]
           (log/debug ":chsk/ws-ping"))

; STATE
; ****************
(defmethod event-handler :sync/state
           [{:keys [ring-req ?reply-fn send-fn db]}]
           (when (and (session-uid ring-req) ?reply-fn)
                 (let [state (db/everything (:conn db))]
                      (log/debug "sync/state" state)
                      (?reply-fn state))))

; ROLE
; ****************
(defmethod event-handler :sync/role
           [{:keys [ring-req ?reply-fn]}]
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
           [{:keys [?data db]}]
           (let [[id diff flag] ?data
                 flag (or flag :patch)]
                (db/update-room (:conn db) id diff flag)))

(defmethod event-handler :room/trash
           [{:keys [?data]}]
           (let [room ?data]
                ;(db/trash-scene scene-id)
                ))

; SCENE
; ****************
(defmethod event-handler :scene/create
           [{:keys [?data db]}]
           (let [[room-id scene] ?data]
                ;(db/make-scene room-id scene)
                ))

(defmethod event-handler :scene/update
           [{:keys [?data db]}]
           (let [[id diff flag] ?data
                 flag (or flag :patch)]
                (db/update-scene (:conn db) id diff flag)))

(defmethod event-handler :scene/trash
           [{:keys [?data]}]
           (let [scene-id ?data]
                ;(db/trash-scene scene-id)
                ))

; LIGHT
; ****************
(defmethod event-handler :light/create
           [{:keys [?data db]}]
           (db/create-light (:conn db) ?data))

(defmethod event-handler :light/update
           [{:keys [?data db]}]
           (let [[id diff flag] ?data
                 flag (or flag :patch)]
                (db/update-light (:conn db) id diff flag)))

(defmethod event-handler :light/trash
           [{:keys [?data]}]
           (let [light-id ?data]
                (log/debug ":light/trash" light-id)
                ;(db/trash-light (:conn db) light-id)
                ))

(defmethod event-handler :color/make
           [{:keys [?data db]}]
           (let [color ?data]
                (db/create-color (:conn db) color)))

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
(defn sente-handler [{db :db}]
      (fn [ev-msg]
          ; put the database into the event map, so that those handlers that need it can use it
          ; (I'm looking at you :sync/state)
          (event-handler (assoc ev-msg :db db))))