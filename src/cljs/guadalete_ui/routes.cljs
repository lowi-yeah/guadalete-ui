(ns guadalete-ui.routes
  (:require-macros [secretary.core :refer [defroute]])
  (:import goog.History)
  (:require [secretary.core :as secretary :refer-macros [defroute]]
            [goog.events :as events]
            [goog.history.EventType :as EventType]
            [re-frame.core :as re-frame]
            [guadalete-ui.console :as log]))

;//   _    _    _
;//  | |_ (_)___ |_ ___ _ _ _  _
;//  | ' \| (_-<  _/ _ \ '_| || |
;//  |_||_|_/__/\__\___/_|  \_, |
;//                         |__/
;; must be called after routes have been defined
(defn hook-browser-navigation! []
      (doto (History.)
            (events/listen
              EventType/NAVIGATE
              (fn [event]
                  (secretary/dispatch! (.-token event))))
            (.setEnabled true)))

;//                _
;//   _ _ ___ _  _| |_ ___ ___
;//  | '_/ _ \ || |  _/ -_)_-<
;//  |_| \___/\_,_|\__\___/__/
;//
(defn app-routes []
      (secretary/set-config! :prefix "#")

      ;; --------------------
      ;; define routes here
      (defroute "/" []
                (re-frame/dispatch [:set-root-panel]))

      (defroute "/room/:id" [id]
                (re-frame/dispatch [:set-room-panel id]))

      (defroute "/room/:id/switch" [id]
                (re-frame/dispatch [:set-room-switches-panel id]))

      (defroute "/room/:id/light" [id]
                (re-frame/dispatch [:set-room-lights-panel id]))

      (defroute "/room/:id/scene" [id]
                (re-frame/dispatch [:set-room-scene-panel {:room-id id}]))

      (defroute "/room/:room-id/scene/:scene-id" {:as params}
                (re-frame/dispatch [:set-room-scene-panel params]))

      (defroute "/sensor/:id" [id]
                (re-frame/dispatch [:set-sensor-panel id]))

      (defroute "/signals/" []
                (re-frame/dispatch [:set-signal-panel]))

      (defroute "/debug/" []
                (re-frame/dispatch [:set-debug-panel]))

      ;//   _ _ _ _ _ _ _ _
      ;//  (_)_)_)_)_)_)_)_)
      (hook-browser-navigation!))
