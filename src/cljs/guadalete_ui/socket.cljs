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
            [guadalete-ui.console :as log])
  (:require-macros [cljs.core.async.macros :refer (go go-loop)]))


;// setup sente (adapted from https://github.com/danielsz/system-websockets)
(defonce sente-client (component/start (new-channel-socket-client)))
(def chsk (:chsk sente-client))
(def chsk-send! (:chsk-send! sente-client))
(def chsk-state (:chsk-state sente-client))

(defn chsk-reconnect!
      "Delegate for reconneting sente. Called after login/logout for re-authentication"
      []
      (sente/chsk-reconnect! chsk))

(defn- ->chsk-state [state]
       (if (= state {:first-open? true})
         (log/info "Socket connection opened")))

(defn- ->chsk-handshake [[_ ?data]]
       (let [[_ _ ?user-role] ?data]
            (dispatch [:ws/handshake (keyword ?user-role)])))


(defn- event-handler [event]
       (match [event]
              [[:chsk/state state]] (->chsk-state state)
              [[:chsk/handshake _]] (->chsk-handshake event)
              :else (log/debug "Unmatched event: %s" event)))

;; Wrap for logging, catching, etc.:
(defn- event-handler* [{:as ev-msg :keys [id ?data event]}]
       (event-handler event))

(defn event-loop
      "Handle inbound events."
      []
      (go-loop []
               (let [ev-msg (<! (:ch-chsk sente-client))]
                    (event-handler* ev-msg)
                    (recur))))
