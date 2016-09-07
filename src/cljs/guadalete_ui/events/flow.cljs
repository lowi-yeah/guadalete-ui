(ns guadalete-ui.events.flow

  (:require
    [re-frame.core :refer [dispatch def-event def-event-fx def-fx]]
    [thi.ng.geom.core :as g]
    [thi.ng.geom.core.vector :refer [vec2]]
    [guadalete-ui.pd.flow :as flow]
    [guadalete-ui.util :refer [pretty kw* vec->map]]
    [guadalete-ui.pd.link :as link]
    [guadalete-ui.console :as log]
    ; schema
    [schema.core :as s]
    [guadalete-ui.schema.pd :refer [Flow]]
    [guadalete-ui.events.scene :as scene]))

;//    __ _
;//   / _| |_____ __ __
;//  |  _| / _ \ V  V /
;//  |_| |_\___/\_/\_/
;//
(def-event-fx
  :flow/mouse-down
  ;; called when the mouse is pressed above a flow,
  ;; marks it as selected and returns
  (fn [{:keys [db]} [_ {:keys [scene-id id modifiers]}]]
    (let [selected-items (if (:shift modifiers) (get-in db [:tmp :selected]) #{})
          flow-reference {:scene-id scene-id :id id :type :flow}
          selected-items* (conj selected-items flow-reference)]
      {:db (assoc-in db [:tmp :selected] selected-items*)})))


;; Called when moving the mouse during link creation.
;; Updates the mouse mosition (for rendering the temporary flow.
;; Also Checks the current target and sets appropriae values in the db.
(def-event-fx
  :flow/mouse-move
  (fn [{:keys [db]} [_ data]]
    (log/debug ":flow/mouse-move" data)
    {:db db}))

(def-event-fx
  :flow/mouse-up
  (fn [{:keys [db]} [_ data]]
    (log/debug ":flow/mouse-up" data)
    {:db db}))
