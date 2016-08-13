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

    [guadalete-ui.pd.link :as link]
    [guadalete-ui.pd.color :refer [make-color]]
    [guadalete-ui.events.scene :as scene]))

;//               _
;//   _ _  ___ __| |___
;//  | ' \/ _ \ _` / -_)
;//  |_||_\___\__,_\___|
;//
(defn- make-node [db {:keys [scene-id ilk position] :as data}]
  (log/debug "make node" data)
  (let [scene (get-in db [:scene scene-id])
        nodes (:nodes scene)
        data* (assoc data :position (offset-position position scene))
        node (node/make ilk data* db)
        nodes* (assoc nodes (kw* (:id node)) node)
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
        nodes* (assoc nodes (kw* (:id node)) node)
        scene* (assoc scene :nodes nodes*)
        db* (-> db
                (assoc-in [:scene scene-id] scene*)
                (assoc-in [:color (:id color)] color)
                )]
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
