(ns guadalete-ui.handlers.socket
  (:require
    [clojure.core.match :refer [match]]
    [clojure.core.async :as async :refer [chan >! >!! <! go go-loop thread close! alts!]]
    [taoensso.timbre :as log]
    [cheshire.core :refer [generate-string]]
    [guadalete-ui.helpers.session :refer [session-role session-uid]]
    [guadalete-ui.helpers.config :refer [load-config]]
    [guadalete-ui.handlers.database :as db]
    [guadalete-ui.handlers.redis :as redis]))

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
          state* (assoc state :config (frontend-config))
          values-channel (signal-values redis-conn (db/signal-ids db-conn))]

      (go
        (let [signal-values (<! values-channel)]
          (send-fn uid [:signals/values (generate-string signal-values)])
          ;(send-fn uid [:signals/values (generate-string signal-values)])
          ;(send-fn uid [:signal/values signal-values])

          ;(doseq [i (range 100)]
          ;  (send-fn uid [:fast-push/is-fast (str "hello " i "!!")]))
          ))

      (?reply-fn state*))))

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
  [{:keys [?data db-conn]}]
  (let [[id diff flag] ?data
        flag (or flag :patch)]
    (db/update-room db-conn id diff flag)))

(defmethod event-handler :room/trash
  [{:keys [?data]}]
  (let [room ?data]
    ;(db/trash-scene scene-id)
    ))

; SCENE
; ****************
(defmethod event-handler :scene/create
  [{:keys [?data db-conn]}]
  (let [[room-id scene] ?data]
    ;(db/make-scene room-id scene)
    ))

(defmethod event-handler :scene/update
  [{:keys [?data db-conn]}]
  (let [[id diff flag] ?data
        flag (or flag :patch)]
    (db/update-scene db-conn id diff flag)))

(defmethod event-handler :scene/trash
  [{:keys [?data]}]
  (let [scene-id ?data]
    ;(db/trash-scene scene-id)
    ))

; LIGHT
; ****************
(defmethod event-handler :light/create
  [{:keys [?data db-conn]}]
  (db/create-light db-conn ?data))

(defmethod event-handler :light/update
  [{:keys [?data db-conn]}]
  (let [[id diff flag] ?data
        flag (or flag :patch)]
    (db/update-light db-conn id diff flag)))

(defmethod event-handler :light/trash
  [{:keys [?data]}]
  (let [light-id ?data]
    (log/debug ":light/trash" light-id)
    ;(db/trash-light (:conn db) light-id)
    ))

(defmethod event-handler :color/make
  [{:keys [?data db-conn]}]
  (let [color ?data]
    (db/create-color db-conn color)))

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