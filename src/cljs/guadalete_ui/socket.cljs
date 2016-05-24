(ns guadalete-ui.socket
  (:require [re-frame.core :refer [dispatch]]
            [cljs.core.async :as async :refer [<! >! put! chan]]
            [taoensso.sente :as sente]
            [system.components.sente :refer [new-channel-socket-client]]
            [com.stuartsierra.component :as component]
            [cljs.core.match :refer-macros [match]]
            [ajax.core :refer [GET POST]]
            [cljs-utils.core :as utils :refer [by-id]]

            [guadalete-ui.console :as log]

            )
  (:require-macros [cljs.core.async.macros :refer (go go-loop)]))

(defonce sente-client (component/start (new-channel-socket-client)))
(def chsk (:chsk sente-client))
(def chsk-send! (:chsk-send! sente-client))
(def chsk-state (:chsk-state sente-client))

(defn event-handler [event]
      (log/debug "Event: %s" (pr-str event))
      (match [event]
             [[:chsk/state state]] (log/debug "state change: %s" (pr-str state))
             [[:chsk/handshake _]] (log/debug "Sente handshake")
             ;[[:chsk/recv [:demo/flash payload]]] (log/debug "Flash:" payload)
             ;[[:chsk/recv payload]] (log/debug "Push event from server: %s" (pr-str payload))
             :else (log/debug "Unmatched event: %s" event)))

(defn event-loop
      "Handle inbound events."
      []
      (log/debug "Handle inbound events.")
      (go-loop []
               (let [{:as ev-msg :keys [event]} (<! (:ch-chsk sente-client))]
                    (event-handler event)
                    (recur))))

;
;;; SENTE SOCKET
;
;;; It will survive to Figwheel reload.
;(defonce sente-socket
;         (sente/make-channel-socket! "/chsk"
;                                     {:type        :auto
;                                      :chsk-url-fn #(str "ws://localhost:3001" %) ;; Use the server url
;                                      }))
;
;(let [{:keys [chsk ch-recv send-fn state]}
;      sente-socket]
;     (def chsk chsk)
;     (def ch-chsk ch-recv)                                  ; ChannelSocket's receive channel
;     (def chsk-send! send-fn)                               ; ChannelSocket's send API fn
;     (def chsk-state state)                                 ; Watchable, read-only atom
;     )
;
;(defmulti handle-event
;          "Handle events based on the event Id."
;          (fn [[ev-id ev-arg]] ev-id))
;
;;; Print answer
;(defmethod handle-event :test/reply
;           [[_ msg]]
;           (dispatch [:test/reply msg]))
;
;;; Ignoring unknown events.
;(defmethod handle-event :default
;           [event]
;           (println "UNKNOW EVENT" event))
;
(defn test-session
      "Ping the server."
      []
      (chsk-send! [:session/status]))
;
;(defn event-loop
;      "Handle inbound events."
;      []
;      (go (loop [[op arg] (:event (<! ch-chsk))]
;                (println "-" op)
;                (case op
;                      :chsk/recv (handle-event arg)
;                      (test-session))
;                (recur (:event (<! ch-chsk))))))
