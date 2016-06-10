(ns guadalete-ui.pd.handlers
  (:require
    [re-frame.core :refer [dispatch register-handler]]
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
(register-handler
  :mouse/move
  (fn [db [_ data]]
      (mouse/move data db)))

(register-handler
  :mouse/down
  (fn [db [_ data]]
      (mouse/down data db)))

(register-handler
  :mouse/up
  (fn [db [_ data]]
      (mouse/up data db)))

(register-handler
  :mouse/enter
  (fn [db [_ data]]
      (if (= 0 (:buttons data))
        (mouse/up data db)
        db)))

(register-handler
  :mouse/leave
  (fn [db [_ data]] db))

(register-handler
  :mouse/click
  (fn [db [_ data]]
      (mouse/up data db)))

(register-handler
  :mouse/double-click
  (fn [db [_ {:keys [ilk room-id scene-id node-id] :as data}]]
      (log/debug ":mouse/double-click" (pretty data))
      (condp = (kw* ilk)
             :light (do
                      (dispatch [:pd/modal-open :pd-light-node])
                      (assoc db :pd/modal-node-data {:room room-id :scene scene-id :node node-id}))
             :color (do
                      (dispatch [:pd/modal-open :pd-color-node])
                      (assoc db :pd/modal-node-data {:room room-id :scene scene-id :node node-id}))
             db)))

(register-handler
  :mouse/default-up
  (fn [db [_ data]]
      (mouse/default-up data db)))


;//           _
;//   _ __ __| |
;//  | '_ \ _` |
;//  | .__\__,_|
;//  |_|
(register-handler
  :pd/mouse-down
  (fn [db [_ {:keys [scene-id node-id position]}]]
      (let [scene (get-in db [:scene scene-id])  
            scene* (assoc scene 
                          :mode :pan 
                          :pos-0 (vec-map position)  
                          :pos-1 (vec-map (:translation scene)))]  
           (assoc-in db [:scene scene-id] scene*))))

(register-handler
  :pd/mouse-move
  (fn [db [_ {:keys [scene-id position]}]]
      (let [scene (get-in db [:scene scene-id])
            δ (g/- (vec2 position) (vec2 (:pos-0 scene)))
            translation* (g/+ (vec2 (:pos-1 scene)) δ)
            scene* (assoc scene :translation (vec-map translation*))]
           (assoc-in db [:scene scene-id] scene*))))

;//               _
;//   _ _  ___ __| |___
;//  | ' \/ _ \ _` / -_)
;//  |_||_\___\__,_\___|
;//
(register-handler
  :node/make
  (fn [db [_ {:keys [room-id scene-id ilk position] :as data}]]
      (let [
            scene (get-in db [:scene scene-id])
            nodes (:nodes scene)
            node (make-node ilk (offset-position position scene))
            nodes* (assoc nodes (keyword (:id node)) node)
            scene* (assoc scene :nodes nodes*)]
           (dispatch [:scene/update scene*])
           ;(assoc-in db [:scene scene-id] scene*)
           db)))

(register-handler
  :node/reset-all
  (fn [db [_ scene-id]]
      (node/reset-all scene-id db)))

(register-handler
  :node/mouse-down
  (fn [db [_ data]]
      (node/begin-move data db)))

(register-handler
  :node/mouse-move
  (fn [db [_ data]]
      (node/move data db)))


;//    __ _
;//   / _| |_____ __ __
;//  |  _| / _ \ V  V /
;//  |_| |_\___/\_/\_/
;//
(register-handler
  :flow/mouse-down
  (fn [db [_ data]]
      (flow/begin data db)))

(register-handler
  :flow/mouse-move
  (fn [db [_ data]]
      (flow/move data db)))

(register-handler
  :flow/mouse-up
  (fn [db [_ data]]
      (log/debug "f00")
      (flow/end data db)))

(register-handler
  :flow/check-connection
  (fn [db [_ data]]
      (flow/check-connection data db)))

(register-handler
  :flow/reset-target
  (fn [db [_ data]]
      (flow/reset-target data db)))


;//                _      _
;//   _ __  ___ __| |__ _| |
;//  | '  \/ _ \ _` / _` | |
;//  |_|_|_\___\__,_\__,_|_|
;//
(register-handler
  :pd/modal-open
  (fn [db [_ modal-id]]
      (modal/open modal-id)
      db))

(register-handler
  :pd/modal-approve
  (fn [db _]
      (dissoc db :pd/modal-node-data)))

(register-handler
  :pd/modal-deny
  (fn [db _]
      (dissoc db :pd/modal-node-data)))

(register-handler
  :modal/register-node
  (fn [db [_ {:keys [item-id]}]]
      (if (= item-id "nil")
        db
        (let [room (modal-room db)
              scene (modal-scene db)
              nodes (get scene :nodes)
              node (modal-node db)
              ;ilk (keyword (:ilk node))
              type (keyword (:type node))
              item (get-in db [type item-id])
              scene-items (get scene type)
              node* (assoc node :item-id item-id)           ; set the item id in the pd node
              nodes* (assoc nodes (:id node*) node*)
              scene* (assoc scene :nodes nodes*)            ; update the scene
              db* (assoc-in db [:scene (:id scene)] scene*)]
             (dispatch [:scene/update scene*])
             db*))))
