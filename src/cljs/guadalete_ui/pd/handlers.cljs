(ns guadalete-ui.pd.handlers
  (:require
    [re-frame.core :refer [dispatch def-event def-event-fx]]
    [thi.ng.geom.core :as g]
    [thi.ng.geom.core.vector :refer [vec2]]
    [guadalete-ui.pd.mouse :as mouse]
    [guadalete-ui.pd.nodes :refer [make-node]]

    [guadalete-ui.console :as log]
    [guadalete-ui.util :refer [pretty kw* vec-map offset-position]]
    [guadalete-ui.views.modal :as modal]
    [guadalete-ui.pd.util :refer [modal-room modal-scene modal-node]]
    [guadalete-ui.pd.nodes :as node]
    [guadalete-ui.pd.flow :as flow]
    [guadalete-ui.pd.link :as link]))


;//
;//   _ __  ___ _  _ ______
;//  | '  \/ _ \ || (_-< -_)
;//  |_|_|_\___/\_,_/__\___|
;//
(def-event
  :mouse/move
  (fn [db [_ data]]
    (mouse/move data db)))

(def-event
  :mouse/down
  (fn [db [_ data]]
    (mouse/down data db)))

(def-event
  :mouse/up
  (fn [db [_ data]]
    (mouse/up data db)))

(def-event
  :mouse/enter
  (fn [db [_ data]]
    (if (= 0 (:buttons data))
      (mouse/up data db)
      db)))

(def-event
  :mouse/leave
  (fn [db [_ data]] db))

(def-event
  :mouse/click
  (fn [db [_ data]]
    (mouse/up data db)))

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

(def-event
  :mouse/default-up
  (fn [db [_ data]]
    (mouse/default-up data db)))


;//           _
;//   _ __ __| |
;//  | '_ \ _` |
;//  | .__\__,_|
;//  |_|
(def-event
  :pd/mouse-down
  (fn [db [_ {:keys [scene-id node-id position]}]]
    (let [scene (get-in db [:scene scene-id])  
          scene* (assoc scene 
                   :mode :pan 
                   :pos-0 (vec-map position)  
                   :pos-1 (vec-map (:translation scene)))]  
                                                           (assoc-in db [:scene scene-id] scene*))))

(def-event
  :pd/mouse-move
  (fn [db [_ {:keys [scene-id position]}]]
    (let [scene (get-in db [:scene scene-id])
          δ (g/- (vec2 position) (vec2 (:pos-0 scene)))
          translation* (g/+ (vec2 (:pos-1 scene)) δ)
          scene* (assoc scene :translation (vec-map translation*))]
      (assoc-in db [:scene scene-id] scene*))))

(def-event
  :pd/mouse-up
  (fn [db [_ {:keys [scene-id position]}]]
    (let [scene (get-in db [:scene scene-id])
          scene* (dissoc scene :pos-0 :pos-1 :flow/mouse :mode)]
      (dispatch [:scene/update scene*])
      (dispatch [:node/reset-all scene-id])
      db)
    db))

;//               _
;//   _ _  ___ __| |___
;//  | ' \/ _ \ _` / -_)
;//  |_||_\___\__,_\___|
;//

;; event handler for adding nodes to a pd-scene. Called when an item is dropped from the pallete
(def-event-fx
  :node/make
  (fn [{:keys [db]} [_ {:keys [scene-id ilk position] :as data}]]
    (let [scene (get-in db [:scene scene-id])
          nodes (:nodes scene)
          data* (assoc data :position (offset-position position scene))
          node (make-node ilk data* db)
          nodes* (assoc nodes (keyword (:id node)) node)
          scene* (assoc scene :nodes nodes*)
          db* (assoc-in db [:scene scene-id] scene*)]
      {:db       db*
       :dispatch [:scene/update scene* scene]})))

(def-event
  :node/reset-all
  (fn [db [_ scene-id]]
    (node/reset-all scene-id db)))

(def-event
  :node/mouse-down
  (fn [db [_ data]]
    (node/select data db)))

(def-event
  :node/mouse-move
  (fn [db [_ data]]
    (node/move data db)))


;//    __ _
;//   / _| |_____ __ __
;//  |  _| / _ \ V  V /
;//  |_| |_\___/\_/\_/
;//
(def-event
  :flow/mouse-down
  (fn [db [_ data]]
    (flow/begin data db)))

(def-event
  :flow/mouse-move
  (fn [db [_ data]]
    (flow/move data db)))

(def-event
  :flow/mouse-up
  (fn [db [_ data]]
    (flow/end data db)))

(def-event
  :flow/check-connection
  (fn [db [_ data]]
    (flow/check-connection data db)))

(def-event
  :flow/reset-target
  (fn [db [_ data]]
    (flow/reset-target data db)))


