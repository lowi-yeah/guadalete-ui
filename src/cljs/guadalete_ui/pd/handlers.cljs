(ns guadalete-ui.pd.handlers
  (:require
    [re-frame.core :refer [dispatch def-event def-event-fx]]
    [thi.ng.geom.core :as g]
    [thi.ng.geom.core.vector :refer [vec2]]
    [guadalete-ui.pd.mouse :as mouse]
    [guadalete-ui.pd.nodes :as node]
    [guadalete-ui.console :as log]
    [guadalete-ui.util :refer [pretty kw* vec-map offset-position]]
    [guadalete-ui.views.modal :as modal]
    [guadalete-ui.pd.util :refer [modal-room modal-scene modal-node]]
    [guadalete-ui.pd.nodes :as node]
    [guadalete-ui.pd.flow :as flow]
    [guadalete-ui.pd.link :as link]
    [guadalete-ui.pd.color :refer [make-color]]
    [guadalete-ui.events.scene :as scene]
    [guadalete-ui.items :refer [reset-scene]]))


;//               _
;//   _ _  ___ __| |___
;//  | ' \/ _ \ _` / -_)
;//  |_||_\___\__,_\___|
;//
(defn- make-node [db {:keys [scene-id ilk position] :as data}]
  (let [scene (get-in db [:scene scene-id])
        nodes (:nodes scene)
        data* (assoc data :position (offset-position position scene))
        node (node/make ilk data* db)
        nodes* (assoc nodes (keyword (:id node)) node)
        scene* (assoc scene :nodes nodes*)
        db* (assoc-in db [:scene scene-id] scene*)]
    {:db    db*
     :sente (scene/sync-effect {:old scene :new scene*})}))

(defn- make-color-node
  "Color nodes need special handling, since their items (ie. the colors they represent)
  are being dynamically created."
  [db {:keys [scene-id ilk position] :as data}]
  (let [scene (get-in db [:scene scene-id])
        nodes (:nodes scene)
        color (make-color)
        data* (assoc data
                :position (offset-position position scene)
                :color color)
        node (node/make ilk data* db)
        nodes* (assoc nodes (keyword (:id node)) node)
        scene* (assoc scene :nodes nodes*)
        db* (-> db
                (assoc-in [:scene scene-id] scene*)
                (assoc-in [:color (:id color)] color))]
    {:db       db*
     :dispatch [:color/make color]
     :sente    (scene/sync-effect {:old scene :new scene*})}))

;; event handler for adding nodes to a pd-scene. Called when an item is dropped from the pallete
(def-event-fx
  :node/make
  (fn [{:keys [db]} [_ {:keys [ilk] :as data}]]
    (condp = ilk
      :color (make-color-node db data)
      (make-node db data))))


(def-event
  :node/mouse-down
  (fn [db [_ data]]
    (node/select data db)))

(def-event
  :node/mouse-move
  (fn [db [_ data]]
    (node/move data db)))

(def-event-fx
  :node/mouse-up
  (fn [{:keys [db]} [_ {:keys [scene-id]}]]
    (let [scene (get-in db [:scene scene-id])
          scene* (reset-scene scene)
          stashed-scene (get-in db [:tmp :scene])]
      {:db    (-> db
                  (assoc-in [:scene scene-id] scene*))
       :sente (scene/sync-effect {:old stashed-scene :new scene*})})))


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


