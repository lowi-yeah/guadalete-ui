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
                (re-frame/dispatch [:view/room [id :current]]))
                ;(re-frame/dispatch [:view/scene [id]]))

      (defroute "/room/:room-id/scene" [room-id]
                (re-frame/dispatch [:view/scene [room-id]]))

      (defroute "/room/:room-id/scene/:scene-id" [room-id scene-id]
                (re-frame/dispatch [:view/scene [room-id scene-id]]))

      (defroute "/room/:id/light" [id]
                (re-frame/dispatch [:view/room [id :light]]))
      (defroute "/room/:id/switch" [id]
                (re-frame/dispatch [:view/room [id :switch]]))
      (defroute "/room/:id/dmx" [id]
                (re-frame/dispatch [:view/room [id :dmx]]))



      ;(defroute "/room/:room-id/scene/:scene-id" {:as params}
      ;          (re-frame/dispatch [:set-room-scene-panel params]))
      ;
      ;(defroute "/sensor/:id" [id]
      ;          (re-frame/dispatch [:set-sensor-panel id]))
      ;
      ;(defroute "/signals/" []
      ;          (re-frame/dispatch [:set-signal-panel]))

      (defroute "/debug/" []
                (re-frame/dispatch [:view/debug]))

      ;//   _ _ _ _ _ _ _ _
      ;//  (_)_)_)_)_)_)_)_)
      (hook-browser-navigation!))
