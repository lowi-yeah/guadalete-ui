(ns guadalete-ui.handlers
  ;(:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :refer [dispatch register-handler path trim-v after]]
            [taoensso.sente :as sente]
            [taoensso.encore :refer [ajax-lite]]
            [guadalete-ui.console :as log]
            [guadalete-ui.socket :refer [chsk-send! chsk-state chsk-reconnect!]]
            [guadalete-ui.util :refer [pretty mappify]]

    ;[secretary.core :as secretary]
    ;        [redonaira.schema :as schema]
    ;[schema.core :as s]
    ;[differ.core :as differ]
    ;[taoensso.encore :refer [ajax-lite]]
    ;[clojure.string :as string]
    ;[thi.ng.geom.core.vector :as v]
    ;[guadalete-ui.ws :as ws]
    ;[guadalete-ui.helpers :as helpers]
    ;[guadalete-ui.toaster :refer [toast]]
            ))

; ____ ___ ____ ____ ___ _  _ ___
; [__   |  |__| |__/  |  |  | |__]
; ___]  |  |  | |  \  |  |__| |

(register-handler
  :initialize-db
  ;check-schema-mw
  (fn [_db _]
      (dispatch [:sync/role])
      ;return default db
      {:ws/connected?   false
       :loading?        false
       :user/role       :none
       :main-panel      :blank-panel
       :name            "guadalete-ui"
       :message         ""
       :current/view    :blank
       :current/segment :scene
       }))

;//          _
;//   ______| |_ _  _ _ __
;//  (_-< -_)  _| || | '_ \
;//  /__\___|\__|\_,_| .__/
;//                  |_|
(register-handler
  :ws/handshake
  ;check-schema-mw
  (fn [db [_ role]]
      (dispatch [:set-root-panel role])
      (assoc db :ws/connected? true :user/role role)))

; more of a convenience handler for development
; when auto-reload updates the page, no new handshake is being exchanged,
; and thus the userr-role is not set. Here this is done explicitly
; irrelevant for normal operation, as not auto-reload happens there
(register-handler
  :sync/role
  (fn [db _]
      (chsk-send! [:sync/role]
                  8000
                  (fn [reply]
                      (dispatch [:ws/handshake (keyword (:role reply))])))
      db))


(register-handler
  :set-root-panel
  (fn [db _]
      (condp = (:user/role db)
             :none (assoc db :main-panel :blank-panel)
             :anonymous (assoc db :main-panel :login-panel)
             :admin (do
                      (dispatch [:sync/state])
                      (assoc db :main-panel :root-panel)))))

;//   _          _
;//  | |___ __ _(_)_ _
;//  | / _ \ _` | | ' \
;//  |_\___\__, |_|_||_|
;//        |___/

(defn- ajax-login
       "POST the login information via ajax"
       [name pwd]
       (ajax-lite "/login"
                  {:method            :post
                   :params            {:username   (str name)
                                       :password   (str pwd)
                                       :csrf-token (:csrf-token @chsk-state)}
                   :resp-type         :text
                   :timeout-ms        8000
                   :with-credentials? false                 ; Enable if using CORS (requires xhr v2+)
                   }
                  (fn async-callback [resp-map]
                      (dispatch [:chsk/reconnect]))))

(register-handler
  :login
  (fn [db [_ name pwd]]
      (ajax-login name pwd)
      db))

(register-handler
  :chsk/reconnect
  (fn [db]
      (chsk-reconnect!)
      (assoc db :ws/connected? false)))


;//       _
;//  __ ___)_____ __ _____
;//  \ V / / -_) V  V (_-<
;//   \_/|_\___|\_/\_//__/
;//
(register-handler
  :view/room
  (fn [db [_ [room-id segment]]]
      (log/debug ":view/room" room-id (str segment))

      (if (= segment :current)
        (-> db
            (assoc :current/view :room)
            (assoc :current/room-id room-id))
        (-> db
            (assoc :current/view :room)
            (assoc :current/segment segment)
            (assoc :current/room-id room-id)))))


;//                _      _
;//   _ __  ___ __| |__ _| |___
;//  | '  \/ _ \ _` / _` | (_-<
;//  |_|_|_\___\__,_\__,_|_/__/
;//

(register-handler
  :modal/new-room
  (fn [db _]
      (log/debug ":modal/new-room")
      db))

;//      _      _        _
;//   __| |__ _| |_ __ _| |__ __ _ ______
;//  / _` / _` |  _/ _` | '_ \ _` (_-< -_)
;//  \__,_\__,_|\__\__,_|_.__\__,_/__\___|
;//
(register-handler
  :sync/state
  (fn [db _]
      (chsk-send! [:sync/state]
                  8000                                      ; Timeout
                  (fn [reply]                               ; Reply is arbitrary Clojure data
                      (when (sente/cb-success? reply)       ; Checks for :chsk/closed, :chsk/timeout, :chsk/error
                            (dispatch [:state reply]))))
      db))

(register-handler
  :state
  (fn [db [_ state]]
      (let [rooms-map (mappify :id (:room state))
            lights-map (mappify :id (:light state))
            scenes-map (mappify :id (:scene state))]
           (assoc db
                  :room rooms-map
                  :light lights-map
                  :scene scenes-map))))


;//           _
;//   _ __ __| |
;//  | '_ \ _` |
;//  | .__\__,_|
;//  |_|

;// => see pd.handlers
