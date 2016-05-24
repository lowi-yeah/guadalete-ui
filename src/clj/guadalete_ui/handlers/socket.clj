(ns guadalete-ui.handlers.socket
    (:require
      [clojure.core.match :refer [match]]
      [taoensso.timbre :as log]
      [guadalete-ui.helpers.session :refer [session-uid]]))

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
(defmethod event-handler :state/sync
           [ws-req]
           (when-let [uid (session-uid (:ring-req ws-req))]
                     ;((:chsk-send! (:sente system)) uid [:state/db (db/everything)])
                     ))
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
(defn sente-handler [{db :db}] event-handler)
