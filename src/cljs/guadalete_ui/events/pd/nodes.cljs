(ns guadalete-ui.events.pd.nodes
  (:require
    [re-frame.core :refer [dispatch def-event def-event-fx]]
    [thi.ng.geom.core :as g]
    [thi.ng.geom.core.vector :refer [vec2]]
    [guadalete-ui.console :as log]
    [guadalete-ui.util :refer [pretty validate! vec->map offset-position]]

    [guadalete-ui.events.scene :as scene]

    [guadalete-ui.pd.nodes.signal :as signal]
    [guadalete-ui.pd.nodes.color :as color]
    [guadalete-ui.pd.nodes.mixer :as mixer]
    [guadalete-ui.pd.nodes.light :as light]

    [schema.core :as s]
    [guadalete-ui.schema :as gs]
    [differ.core :as differ]))

;// re-frame_  __        _
;//   ___ / _|/ _|___ __| |_ ___
;//  / -_)  _|  _/ -_) _|  _(_-<
;//  \___|_| |_| \___\__|\__/__/
;//
(defn- empty-diff? [diff]
  (= diff [{} {}]))

(s/defn sync-items-effect
  "Creates a sente effect fro syncing all items of a given ilk with the backend."
  [{:keys [ilk old new]}]
  (let [diff (differ/diff old new)]
    (if (not (empty-diff? diff))
      {:topic      :items/update
       :data       {:ilk ilk :diff diff}
       :on-success [:success-items-update]
       :on-failure [:failure-items-update]}
      ;; else
      {})))

(def-event
  :success-items-update
  (fn [world response]
    ;(log/info ":success-items-update" response)
    world))

(def-event
  :failure-items-update
  (fn [world response]
    (log/error "Scene update failed:" response)
    world))


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

(s/defn ^:always-validate move-node
  "helper function for adjusting the position of a single node during 'move'"
  [db :- gs/DB
   mouse-position :- gs/Vec2
   scene-id :- s/Str
   {:keys [id position]} :- gs/NodeReference]

  ;; sometimes it happens that the last :mouse-move event arrives AFTER :mouse-up
  ;; (dunno why this is, but what shall one do)
  ;; as the :mouse-up un-registers the :tmp data, and (db [:tmp :pos]) returns nil
  ;; To work around that problem, query [:tmp :pos] exists, and if it doesn't, just retrun the unchanged node
  (if (nil? (get-in db [:tmp :pos]))
    (let [node (get-in db [:scene scene-id :nodes (keyword id)])]
      [(keyword id) node])
    ;; [:tmp :pos] ain't nil
    (let [node (get-in db [:scene scene-id :nodes (keyword id)])
          δ (g/- (vec2 mouse-position) (vec2 (get-in db [:tmp :pos])))
          position* (g/+ (vec2 position) δ)
          node* (assoc node :position (vec->map position*))]
      [(keyword id) node*])
    )
  )

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
(defmulti make-node*
          (fn [_item {:keys [ilk]}] ilk))

(defmethod make-node* :signal
  [item data]
  (signal/make-node item data))

(defmethod make-node* :mixer
  [item data]
  (mixer/make-node item data))

(defmethod make-node* :color
  [item data]
  (color/make-node item data))

(defmethod make-node* :light
  [item data]
  (light/make-node item data))

(defmulti get-node-item*
          (fn [_db {:keys [ilk]}] ilk))

(defmethod get-node-item* :signal
  [db data]
  (signal/get-avilable db data))

(defmethod get-node-item* :mixer
  [_ _]
  (mixer/make))

(defmethod get-node-item* :color
  [_ _]
  (color/make))

(defmethod get-node-item* :light
  [db data]
  (light/get-avilable db data))

;; event handler for adding nodes to a pd-scene. Called when an item is dropped from the pallete
(s/defn ^:always-validate make-node :- gs/Effect
  "Event handler called when a new node shall be created after an item from the pallete has been dropped."
  [{:keys [db]} [_ {:keys [scene-id position ilk] :as data}]]
  (let [
        scene (get-in db [:scene scene-id])
        node-position (offset-position position scene)
        data (assoc data :position node-position)

        ;; first of all, get the appropriate item (or create one if necessary)
        item (get-node-item* db data)
        ;; then construct the node
        node (make-node* item data)
        ;; reassemble items map
        items (get db ilk)
        items* (assoc items (:id item) item)
        ;; reassemble nodes map
        nodes (get-in db [:scene scene-id :nodes])
        nodes* (assoc nodes (keyword (:id node)) node)
        scene* (assoc scene :nodes nodes*)

        ;; reassemble the database
        db* (-> db
                (assoc ilk items*)
                (assoc-in [:scene scene-id] scene*))

        ;; might be nil
        items-effect (sync-items-effect {:ilk ilk :old items :new items*})
        scene-effect (scene/sync-effect {:old scene :new scene*})
        effects (->>
                  (list items-effect scene-effect)
                  (filter not-empty)
                  (into []))]
    {:db    db*
     :sente effects}))

(def-event-fx :node/make make-node)
