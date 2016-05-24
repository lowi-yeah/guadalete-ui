;//              _
;//   ______ _ _| |_ ___
;//  (_-< -_) ' \  _/ -_)
;//  /__\___|_||_\__\___|
;//

(ns guadalete-ui.socket
  (:require [re-frame.core :refer [dispatch]]
            [cljs.core.async :as async :refer [<! >! put! chan]]
            [taoensso.sente :as sente]
            [system.components.sente :refer [new-channel-socket-client]]
            [com.stuartsierra.component :as component]
            [cljs.core.match :refer-macros [match]]
            [ajax.core :refer [GET POST]]
            [guadalete-ui.console :as log])
  (:require-macros [cljs.core.async.macros :refer (go go-loop)]))


;// setup sente (adapted from https://github.com/danielsz/system-websockets)
(defonce sente-client (component/start (new-channel-socket-client)))
(def chsk (:chsk sente-client))
(def chsk-send! (:chsk-send! sente-client))
(def chsk-state (:chsk-state sente-client))


(defn- ->chsk-state [state]
       (if (= state {:first-open? true})
         (log/info "Socket connection opened")))

(defn- ->chsk-handshake [event]
       (log/debug "chsk-handshake" (pr-str event))
       ;(let [[_ _ ?user-role] ?data]
       ;     (re-frame/dispatch [:ws/handshake (keyword ?user-role)]))
       )


(defn- event-handler [event]
       (match [event]
              [[:chsk/state state]] (->chsk-state state)
              [[:chsk/handshake _]] (->chsk-handshake event)
              :else (log/debug "Unmatched event: %s" event)))

;; Wrap for logging, catching, etc.:
(defn- event-handler* [{:as ev-msg :keys [id ?data event]}]
       (log/debug "ev-msg: " (pr-str event))
       (log/debug "id: " (pr-str id))
       (log/debug "?data: " (pr-str ?data))
       (log/debug "event: " (pr-str event))
       (event-handler event))

(defn event-loop
      "Handle inbound events."
      []
      (go-loop []
               (let [ev-msg (<! (:ch-chsk sente-client))]
                    (event-handler* ev-msg)
                    (recur))))
