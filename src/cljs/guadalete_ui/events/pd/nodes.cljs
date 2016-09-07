(ns guadalete-ui.events.pd.nodes
  (:require
    [re-frame.core :refer [dispatch def-event def-event-fx]]
    [thi.ng.geom.core :as g]
    [thi.ng.geom.core.vector :refer [vec2]]
    [guadalete-ui.pd.mouse :as mouse]
    [guadalete-ui.pd.nodes :as node]
    [guadalete-ui.console :as log]
    [guadalete-ui.util :refer [pretty validate! vec->map offset-position]]
    [guadalete-ui.views.modal :as modal]
    [guadalete-ui.pd.util :refer [modal-room modal-scene modal-node]]
    [guadalete-ui.pd.nodes :as node]

    [guadalete-ui.pd.link :as link]
    [guadalete-ui.pd.color :refer [make-color]]
    [guadalete-ui.events.scene :as scene]

    [guadalete-ui.pd.nodes.signal :as signal]

    [schema.core :as s]
    [guadalete-ui.schema :as gs]
    ))

;//
;//   _ __  ___ _  _ ______
;//  | '  \/ _ \ || (_-< -_)
;//  |_|_|_\___/\_,_/__\___|
;//
(def-event
  :node/mouse-down
  ;; upon mousedown on a node, mark it as selected by storing its
  ;; id in db/tmp/nodes
  (fn [db [_ {:keys [scene-id id position modifiers]}]]
    (let [selected-items (if (:shift modifiers) (get-in db [:tmp :selected]) #{})
          node (get-in db [:scene scene-id :nodes (keyword id)])
          node-reference {:scene-id scene-id
                          :id       id
                          :type     :node
                          :position (vec->map (:position node))}
          selected-items* (conj selected-items node-reference)
          scene (get-in db [:scene scene-id])]
      (-> db
          (assoc-in [:tmp :selected] selected-items*)
          (assoc-in [:tmp :mode] :move)
          (assoc-in [:tmp :pos] position)
          (assoc-in [:tmp :scene] scene)))))



(s/defn ^:always-validate move-node                         ;:- [s/Str gs/Node]
  "helper function for adjusting the position of a single node during 'move'"
  [db :- gs/DB
   mouse-position :- gs/Vec2
   scene-id :- s/Str
   {:keys [id position]} :- gs/MouseEventData
   ]
  (let [node (get-in db [:scene scene-id :nodes (keyword id)])
        δ (g/- (vec2 mouse-position) (vec2 (get-in db [:tmp :pos])))
        position* (g/+ (vec2 position) δ)
        node* (assoc node :position (vec->map position*))]
    [(keyword id) node*]))


(def-event
  :node/mouse-move
  (fn [db [_ {:keys [scene-id position]}]]
    (let [selected-ids (->> (get-in db [:tmp :selected])
                            (filter #(= :node (:type %))))
          moved-nodes (into {} (map #(move-node db position scene-id %) selected-ids))
          scene-nodes (get-in db [:scene scene-id :nodes])
          scene-nodes* (into scene-nodes moved-nodes)
          db* (assoc-in db [:scene scene-id :nodes] scene-nodes*)]
      db*)))


;//                   _
;//   __ _ _ ___ __ _| |_ ___
;//  / _| '_/ -_) _` |  _/ -_)
;//  \__|_| \___\__,_|\__\___|
;//
(defmulti create-node*
          (fn [_db {:keys [ilk]}] ilk))

(defmethod create-node* :signal
  [db data]
  (validate! gs/DB db)
  (signal/make-node db data))

(defmethod create-node* :mixer
  [db data]
  (log/debug "create-node :mixer")
  {})

(defmethod create-node* :color
  [db data]
  (log/debug "create-node :color")
  {})

(defmethod create-node* :light
  [db data]
  (log/debug "create-node :light")
  {})

;; event handler for adding nodes to a pd-scene. Called when an item is dropped from the pallete
(s/defn ^:always-validate make-node* :- gs/Effect
  [{:keys [db]} [_ {:keys [scene-id position] :as data}]]
  (let [scene (get-in db [:scene scene-id])
        node-position (offset-position position scene)
        data* (assoc data :position node-position)
        new-node (create-node* db data*)

        nodes* (assoc (:nodes scene) (keyword (:id new-node)) new-node)
        scene* (assoc scene :nodes nodes*)
        db* (assoc-in db [:scene scene-id] scene*)]
    {:db    db*
     :sente (scene/sync-effect {:old scene :new scene*})}))

(def-event-fx :node/make make-node* )


;//      _                          _          _
;//   __| |___ _ __ _ _ ___ __ __ _| |_ ___ __| |
;//  / _` / -_) '_ \ '_/ -_) _/ _` |  _/ -_) _` |
;//  \__,_\___| .__/_| \___\__\__,_|\__\___\__,_|
;//           |_|
;
;(defn- make-node [db {:keys [scene-id ilk position] :as data}]
;  (log/debug "make node" data)
;  (let [scene (get-in db [:scene scene-id])
;        nodes (:nodes scene)
;        data* (assoc data :position (offset-position position scene))
;        node (node/make ilk data* db)
;        nodes* (assoc nodes (kw* (:id node)) node)
;        scene* (assoc scene :nodes nodes*)
;        db* (assoc-in db [:scene scene-id] scene*)]
;
;    (log/debug "validate new node" node)
;    (s/validate gs/Node node)
;    (log/debug "valid?ddddd")
;
;    ;{:db    db*
;    ; :sente (scene/sync-effect {:old scene :new scene*})}
;    {:db db}
;    ))
;
;(defn- make-color-node
;  "Color nodes need special handling, since their items (ie. the colors they represent)
;  are being dynamically created."
;  [db {:keys [scene-id ilk position] :as data}]
;  (let [scene (get-in db [:scene scene-id])
;        nodes (:nodes scene)
;        color (make-color)
;        data* (assoc data
;                :position (offset-position position scene)
;                :color color)
;        node (node/make ilk data* db)
;        nodes* (assoc nodes (kw* (:id node)) node)
;        scene* (assoc scene :nodes nodes*)
;        db* (-> db
;                (assoc-in [:scene scene-id] scene*)
;                (assoc-in [:color (:id color)] color))]
;    {:db       db*
;     :dispatch [:color/make color]
;     :sente    (scene/sync-effect {:old scene :new scene*})}))
;
;(defn- make-mixer-node
;  "Mixer nodes need special handling, since their items (ie. the mixing-function they represent)
;  are being dynamically created."
;  [db {:keys [scene-id ilk position] :as data}]
;
;  (log/debug "make-mixer-node")
;
;  (let [scene (get-in db [:scene scene-id])
;        nodes (:nodes scene)
;        mix {:id     (str (random-uuid))
;             :mix-fn :add}
;        data* (assoc data
;                :position (offset-position position scene)
;                :item mix)
;        node (node/make ilk data* db)
;        nodes* (assoc nodes (kw* (:id node)) node)
;        scene* (assoc scene :nodes nodes*)
;        db* (-> db
;                (assoc-in [:scene scene-id] scene*)
;                (assoc-in [:mixers (:id mix)] mix))]
;    {:db       db*
;     :dispatch [:mixer/make mix]
;     :sente    (scene/sync-effect {:old scene :new scene*})}))
;
