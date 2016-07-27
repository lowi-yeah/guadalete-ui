(ns guadalete-ui.events.mouse
  (:require
    [re-frame.core :refer [dispatch def-event def-event-fx def-fx]]
    [guadalete-ui.pd.mouse :as mouse]
    [guadalete-ui.util :refer [pretty kw*]]
    [guadalete-ui.console :as log]))

(def-event-fx
  :mouse/default-up
  (fn [{:keys [db]} [_ {:keys [type scene-id node-id position]}]]
    ; This is the standard behaviour upon mouse up.
    ; Canceles everything that might have been going on during move.
    ; Called by modes [:none :pd :move]
    (let [scene (get-in db [:scene scene-id])
          scene* (dissoc scene :pos-0 :pos-1 :flow/mouse :mode)
          db* (assoc-in db [:scene scene-id] scene*)]
      {:db       db*
       :dispatch [:node/reset-all scene-id]})
    ))

(def-event-fx
  :mouse/up
  (fn [{:keys [db]} [_ data]]
    (let
      [type (:type data)
       dispatch* (condp = type
                   :pd [:mouse/default-up data]
                   :node [:mouse/default-up data]
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
    (let [mode (get-in db [:scene (:scene-id data) :mode])
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

(def-event
  :mouse/enter
  (fn [db [_ data]]
    (if (= 0 (:buttons data))
      (mouse/up data db)
      db)))

(def-event
  :mouse/leave
  (fn [db [_ data]] db))

(def-event-fx
  :mouse/click
  (fn [world [_ data]]
    ;; do nothing for the moment
    world))

(def-event
  :mouse/double-click
  (fn [db [_ {:keys [room-id ilk scene-id id] :as data}]]
    (condp = (kw* ilk)
      :light (do
               (dispatch [:modal/open {:id :pd-light-node}])
               (assoc db :pd/modal-node-data {:room-id room-id :scene scene-id :node id}))
      :signal (do
                (dispatch [:modal/open {:id :pd-signal-node}])
                (assoc db :pd/modal-node-data {:room-id room-id :scene scene-id :node id}))
      :color (do
               (dispatch [:modal/open {:id :pd-color-node}])
               (assoc db :pd/modal-node-data {:room-id room-id :scene scene-id :node id}))
      db)))

