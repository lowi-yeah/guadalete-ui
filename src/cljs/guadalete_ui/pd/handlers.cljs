(ns guadalete-ui.pd.handlers
  (:require
    [re-frame.core :refer [dispatch register-handler]]
    [thi.ng.geom.core :as g]
    [thi.ng.geom.core.vector :refer [vec2]]
    [guadalete-ui.pd.mouse :as mouse]
    [guadalete-ui.pd.nodes :refer [make-node]]

    [guadalete-ui.console :as log]
    [guadalete-ui.util :refer [pretty offset-position]]
    [guadalete-ui.views.modal :as modal]
    [guadalete-ui.pd.util :refer [modal-room modal-scene modal-node]]))

(register-handler
  :pd/mouse-move
  (fn [db [_ data]]
      (mouse/move data db)))

(register-handler
  :pd/mouse-down
  (fn [db [_ data]]
      (mouse/down data db)))

(register-handler
  :pd/mouse-up
  (fn [db [_ data]]
      (mouse/up data db)))

(register-handler
  :pd/mouse-enter
  (fn [db [_ data]]
      (if (= 0 (:buttons data))
        (mouse/up data db)
        db)))

(register-handler
  :pd/mouse-leave
  (fn [db [_ data]] db))

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
            nodes (:nodes scene)
            node (make-node type (offset-position pos scene))
            nodes* (assoc nodes (keyword (:id node)) node)
            scene* (assoc scene :nodes nodes*)]
           (dispatch [:scene/update scene*])
           ;(assoc-in db [:scene scene-id] scene*)
           db)))

(register-handler
  :pd/link-modal-node
  (fn [db [_ {:keys [item-id]}]]
      (if (= item-id "nil")
        db
        (let [room (modal-room db)
              scene (modal-scene db)
              nodes (get scene :nodes)
              node (modal-node db)
              node-type (keyword (:type node))
              item (get-in db [node-type item-id])
              scene-items (get scene node-type)
              node* (assoc node :item-id item-id)           ; set the item id in the pd node
              nodes* (assoc nodes (:id node*) node*)
              scene* (assoc scene :nodes nodes*)            ; update the scene
              db* (assoc-in db [:scene (:id scene)] scene*)]
             (dispatch [:scene/update scene*])
             db*))))

(register-handler
  :pd-link/enter
  (fn [db [_ data]]
      (log/debug ":pd-link/enter" (pretty data))
      db))

(register-handler
  :pd-link/exit
  (fn [db [_ data]]
      (log/debug ":pd-link/exit" (pretty data))
      db))