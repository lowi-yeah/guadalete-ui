(ns guadalete-ui.pd.handlers
  (:require
    [re-frame.core :refer [dispatch register-handler]]
    [thi.ng.geom.core :as g]
    [thi.ng.geom.core.vector :refer [vec2]]
    [guadalete-ui.pd.mouse :as mouse]
    [guadalete-ui.pd.nodes :refer [make-node]]
    [guadalete-ui.console :as log]
    [guadalete-ui.util :refer [pretty]]
    [guadalete-ui.views.modal :as modal]
    [guadalete-ui.pd.util :refer [modal-room modal-scene modal-node]]))

(defn- move-node [db scene-id node-id layout position]
       (let [scene (get-in db [:scene scene-id])
             δ (g/- (vec2 position) (vec2 (:pos-0 layout)))
             nodes (:nodes layout)
             node (->> nodes
                       (filter #(:selected %))
                       (first))
             node-position (vec2 (:pos-0 node))
             node-position-0 (g/+ node-position δ)
             node-0 (assoc node :position {:x (:x node-position-0) :y (:y node-position-0)})
             nodes-0 (remove #(= (:id %) (:id node-0)) nodes)
             nodes-1 (conj nodes-0 node-0)
             layout-0 (assoc layout :nodes nodes-1)
             scene-0 (assoc scene :layout layout-0)]
            (assoc-in db [:scene scene-id] scene-0)))

(defn- pan-canvas [db scene-id layout position]
       (let [scene (get-in db [:scene scene-id])
             δ (g/- (vec2 position) (vec2 (:pos-0 layout)))
             translation-0 (g/+ (vec2 (:pos-1 layout)) δ)
             layout-0 (assoc layout :translation translation-0)
             scene-0 (assoc scene :layout layout-0)]
            (assoc-in db [:scene scene-id] scene-0)))


(register-handler
  :pd/mouse-down
  (fn [db [_ {:keys [type scene-id node-id position layout] :as data}]]
      (mouse/down data db)))

(register-handler
  :pd/mouse-move
  (fn [db [_ {:keys [scene-id node-id layout position]}]]
      (condp = (:mode layout)
             :move (move-node db scene-id node-id layout position)
             :pan (pan-canvas db scene-id layout position)
             ; if :none, do nothing
             :none db
             db)))

(register-handler
  :pd/mouse-up
  (fn [db [_ data]]
      (mouse/up data db)))

(register-handler
  :pd/click
  (fn [db [_ data]]
      (mouse/up data db)))

(register-handler
  :pd/double-click
  (fn [db [_ {:keys [type room-id scene-id node-id]}]]
      (condp = type
             :light (do
                      (dispatch [:pd/modal-open :pd-light-node])
                      (assoc db :pd/modal-node-data {:room room-id :scene scene-id :node node-id}))
             :color (do
                      (dispatch [:pd/modal-open :pd-color-node])
                      (assoc db :pd/modal-node-data {:room room-id :scene scene-id :node node-id}))
             db)))

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
  :pd/make-node
  (fn [db [_ [room-id scene-id type pos]]]
      (let [
            scene (get-in db [:scene scene-id])
            layout (:layout scene)
            nodes (:nodes layout)
            node (make-node type pos layout)
            nodes-0 (conj nodes node)
            layout-0 (assoc layout :nodes nodes-0)
            scene-0 (assoc scene :layout layout-0)]
           (dispatch [:scene/update scene-0])
           ;(assoc-in db [:scene scene-id] scene-0)
           db)))


(register-handler
  :pd/link-modal-node
  (fn [db [_ {:keys [item-id]}]]
      (if (= item-id "nil")
        db
        (let [room (modal-room db)
              scene (modal-scene db)
              layout (get scene :layout)
              nodes (get layout :nodes)
              node (modal-node db)
              node-type (keyword (:type node))
              item (get-in db [node-type item-id])
              scene-items (get scene node-type)
              node* (assoc node :item-id item-id)           ; set the item id in the pd node
              nodes* (remove #(= (:id node) (:id %)) nodes)    ; update the nodes
              nodes* (conj nodes* node*)
              layout* (assoc layout :nodes nodes*)          ; update the layout
              scene* (assoc scene :layout layout*)          ; update the scene
              db* (assoc-in db [:scene (:id scene)] scene*)]
             (dispatch [:scene/update scene*])
             db*))))