(ns guadalete-ui.events.node

  (:require
    [re-frame.core :refer [dispatch def-event def-event-fx def-fx]]
    [thi.ng.geom.core :as g]
    [thi.ng.geom.core.vector :refer [vec2]]
    [guadalete-ui.events.scene :as scene]
    [guadalete-ui.util :refer [pretty kw* vec-map]]
    [guadalete-ui.console :as log]))

;//                                           _
;//   _ __  ___ _  _ ______   _____ _____ _ _| |_ ___
;//  | '  \/ _ \ || (_-< -_) / -_) V / -_) ' \  _(_-<
;//  |_|_|_\___/\_,_/__\___| \___|\_/\___|_||_\__/__/
;//

(def-event
  :node/mouse-down
  ;; upon mousedown on a node, mark it as selected by storing its
  ;; id in db/tmp/nodes
  (fn [db [_ {:keys [scene-id id position modifiers]}]]
    (let [selected-items (if (:shift modifiers) (get-in db [:tmp :selected]) #{})
          node (get-in db [:scene scene-id :nodes (kw* id)])
          node-reference {:scene-id scene-id
                          :id       id
                          :type     :node
                          :position (vec-map (:position node))}
          selected-items* (conj selected-items node-reference)
          scene (get-in db [:scene scene-id])]
      (-> db
          (assoc-in [:tmp :selected] selected-items*)
          (assoc-in [:tmp :mode] :move)
          (assoc-in [:tmp :pos] position)
          (assoc-in [:tmp :scene] scene)))))


(defn- move-node
  "helper function for adjusting the position of a single node during 'move'"
  [db mouse-position scene-id {:keys [id position]}]
  (let [node (get-in db [:scene scene-id :nodes (kw* id)])
        δ (g/- (vec2 mouse-position) (vec2 (get-in db [:tmp :pos])))
        position* (g/+ (vec2 position) δ)
        node* (assoc node :position (vec-map position*))]
    [(kw* id) node*]))

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


(def-event-fx
  :node/mouse-up
  (fn [{:keys [db]} [_ {:keys [scene-id]}]]
    (let [scene (get-in db [:scene scene-id])
          stashed-scene (get-in db [:tmp :scene])]
      {:db    (-> db
                  (assoc-in [:tmp :mode] :none)
                  (assoc-in [:tmp :scene] nil)
                  (assoc-in [:tmp :pos] nil))
       :sente (scene/sync-effect {:old stashed-scene :new scene})})))

(def-event
  :node/reset-all
  (fn [db [_ scene-id]]
    (log/debug ":node/reset-all" scene-id)
    ;(node/reset-all scene-id db)
    db))

;; UPDATE COLOR NODE
;; ********************************
(defn- make-in-link [channel index]
  {:id        channel
   :name      (name channel)
   :ilk       "value"
   :direction "in"
   :index     index})

(defn- get-link-by-channel [links channel name index]
  (log/debug "get-link-by-channel " channel)
  (let [existing-link (->> links
                           (filter (fn [link] (= channel (keyword (:id link)))))
                           (first))
        link* (or existing-link (make-in-link channel index))]
    (log/debug "get link by channel" name)
    (log/debug "channel" channel)
    (log/debug "links" links)
    (log/debug "existing-link" existing-link)
    (log/debug "link*" link*)
    link*
    ))

(defmulti update-links (fn [color-type _ _] color-type))

(defmethod update-links :v
  [_ node-id in-links]
  [(get-link-by-channel in-links :brightness "brightness" 0)])

(defmethod update-links :sv
  [_ node-id in-links]
  (log/debug "update-links :sv")
  [(get-link-by-channel in-links :brightness "brightness" 0)
   (get-link-by-channel in-links :saturation "saturation" 1)])

(defmethod update-links :hsv
  [_ node-id in-links]
  (let [new-links (->> [:brightness :saturation :hue]
                       (map #(get-link-by-channel in-links % (name %) 0))
                       (into []))]
    (log/debug "update-links :hsv" new-links)
    new-links))

(def-event-fx
  :node/update-color
  (fn [{:keys [db]} [_ {:keys [item-id scene-id node-id] :as data}]]
    (let [scene (get-in db [:scene scene-id])
          color (get-in db [:color item-id])
          node (get-in scene [:nodes (kw* node-id)])

          in-links (->> (:links node)
                        (filter (fn [[_id link]] (= "in" (get link :direction))))
                        (map (fn [[_id link]] link))
                        (into []))

          out-link (->> (:links node)
                        (filter (fn [[_id link]] (= "out" (get link :direction))))
                        (map (fn [[_id link]] link))
                        (first))

          ;; reassemble
          in-links* (update-links (-> color (get :type)) node-id in-links)

          out-link* (assoc out-link :index (count in-links*))

          _ (log/debug "in-links*" (pretty in-links*))

          links* (->> (conj in-links* out-link*)
                      (map (fn [l] [(:id l) l]))
                      (into {}))
          node* (assoc node :links links*)
          scene* (assoc-in scene [:nodes (kw* node-id)] node*)
          db* (-> db (assoc-in [:scene scene-id] scene*))
          ]
      {:db    db*
       :sente (scene/sync-effect {:old scene :new scene*})})))

