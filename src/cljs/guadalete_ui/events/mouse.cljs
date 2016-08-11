(ns guadalete-ui.events.mouse
  (:require
    [re-frame.core :refer [dispatch def-event def-event-fx def-fx]]
    [guadalete-ui.pd.mouse :as mouse]
    [guadalete-ui.util :refer [pretty kw*]]
    [guadalete-ui.console :as log]
    [guadalete-ui.events.scene :as scene]
    [differ.core :as differ]))

(def-event-fx
  :mouse/up
  (fn [{:keys [db]} [_ data]]
    (let [mode (get-in db [:tmp :mode])
          dispatch* (condp = mode
                      :pan [:pd/mouse-up data]
                      :move [:node/mouse-up data]
                      :link [:flow/mouse-up data]
                      nil)]
      (if dispatch*
        {:db       db
         :dispatch dispatch*}
        {:db db}))))

(def-event-fx
  :mouse/down
  (fn [{:keys [db]} [_ data]]
    (let
      [type (:type data)
       dispatch* (condp = type
                   :pd [:pd/mouse-down data]
                   :node [:node/mouse-down data]
                   :link [:flow/mouse-down data]
                   (log/error (str "mouse-down: I don't know the type: " type)))]
      {:db       db
       :dispatch dispatch*})))

(def-event-fx
  :mouse/move
  (fn [{:keys [db]} [_ data]]
    (let [mode (get-in db [:tmp :mode])
          dispatch* (condp = (kw* mode)
                      :none (comment "no nothing")
                      :pan [:pd/mouse-move data]
                      :move [:node/mouse-move data]
                      :link [:flow/mouse-move data]
                      nil (comment "no nothing")
                      (log/error (str "mouse move: I don't know the mode: " mode)))]
      (if dispatch*
        {:db       db
         :dispatch dispatch*}
        {:db db}))))


(def-event-fx
  :mouse/click
  (fn [world [_ data]]
    ;; do nothing for the moment
    world))

(def-event-fx
  :mouse/double-click
  (fn [{:keys [db]} [_ {:keys [room-id scene-id id ilk]}]]
    (let [item-id (get-in db [:scene scene-id :nodes (keyword id) :item-id])
          data {:room-id    room-id
                :scene-id   scene-id
                :node-id    id
                :item-id    item-id
                :ilk        ilk
                :modal-type (keyword (str "pd/" (name ilk)))}]
      {:db    (assoc db :modal data)
       :modal :show})))

