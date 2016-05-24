(ns guadalete-ui.handlers
  ;(:require-macros [reagent.ratom :refer [reaction]])
  (:require [ajax.core :refer [GET]]
            [re-frame.core :refer [dispatch register-handler path trim-v after]]

    ;[secretary.core :as secretary]
    ;        [redonaira.schema :as schema]
    ;[schema.core :as s]
    ;[differ.core :as differ]

    ;[taoensso.encore :refer [ajax-lite]]
    ;[redonaira.views.util :refer [pretty]]
    ;[taoensso.sente :as sente]
    ;[clojure.string :as string]
    ;[thi.ng.geom.core.vector :as v]

            [guadalete-ui.console :as log]
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
      (log/debug "handlers/init!")
      {:ws/connected? false
       :loading?      false
       :user/role     :none
       :active-panel  :root-panel

       :name "re-frame"
       :message ""
       }))

(register-handler
  :ws/handshake
  ;check-schema-mw
  (fn [db [_ role]]
      (dispatch [:state/sync])
      (assoc db :ws/connected? true :user/role role)))


(register-handler
  :set-root-panel
  (fn [db _]
      (-> db
          (assoc :active-panel :root)
          ;(dissoc :current/room-id)
          ;(dissoc :current/sensor-id)
          )))



(register-handler
  :test/reply
  (fn [db msg]
      (assoc db :message msg)))

(register-handler
  :test/send
  (fn [db [_ msg]]
      ;(chsk-send! [:test/send msg])
      db))
