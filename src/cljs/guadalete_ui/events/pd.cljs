(ns guadalete-ui.events.pd
  (:require
    [re-frame.core :refer [dispatch def-event def-event-fx def-fx]]
    [thi.ng.geom.core :as g]
    [thi.ng.geom.core.vector :refer [vec2]]
    [guadalete-ui.util :refer [pretty kw* vec->map offset-position in?]]
    [guadalete-ui.console :as log]
    [guadalete-ui.events.scene :as scene]))

(def-event
  :pd/mouse-down
  (fn [db [_ {:keys [scene-id position modifiers]}]]
    (let [scene (get-in db [:scene scene-id])
          stash-scene (assoc scene  :position position)
          selected-items (if (:shift modifiers) (get-in db [:tmp :selected]) #{})]
      (-> db
          (assoc-in [:tmp :mode] :pan )
          (assoc-in [:tmp :selected] selected-items)
          (assoc-in [:tmp :scene] stash-scene)))))

(def-event
  :pd/mouse-move
  (fn [db [_ {:keys [scene-id position]}]]
    (let [scene (get-in db [:scene scene-id])
          stashed-scene (get-in db [:tmp :scene])
          ;; might be nil, as somtimes mouse-events arrive in incorrect order (:up before the last :move)
          δ (if (nil? (:position stashed-scene))
              (vec2 position)
              (g/- (vec2 position) (vec2 (:position stashed-scene))))
          translation* (if (nil? (:position stashed-scene))
                         δ
                         (g/+ (vec2 (:translation stashed-scene)) δ))
          scene* (assoc scene :translation (vec->map translation*))]
      (assoc-in db [:scene scene-id] scene*))))


(def-event-fx
  :pd/mouse-up
  (fn [{:keys [db]} [_ {:keys [scene-id]}]]
    (let [scene (get-in db [:scene scene-id])
          stashed-scene (get-in db [:tmp :scene])]
      {:db    (-> db
                  (assoc-in [:tmp :mode] :none)
                  (assoc-in [:tmp :scene] nil))
       :sente (scene/sync-effect {:old stashed-scene :new scene})})))

(def-event-fx
  :pd/register-node
  (fn [{:keys [db]} [_ {:keys [scene-id node-id new-id]}]]
    (let [scene (get-in db [:scene scene-id])
          scene* (assoc-in scene [:nodes (kw* node-id) :item-id] new-id)]
      {:db    (assoc-in db [:scene scene-id] scene*)
       :sente (scene/sync-effect {:old scene :new scene*})})))


(defn- get-flow-ids
  "Internal helper for looking up all flows (ie. their ids),
  which begin or end in the given node"
  [db {:keys [scene-id id]}]
  (let [all-flows (-> db
                      (get-in [:scene scene-id :flows])
                      (vals))
        node-flow-ids (->> all-flows
                           (filter
                             (fn [{:keys [from to]}]
                               (or
                                 (= (:node-id from) id)
                                 (= (:node-id to) id))))
                           (map #(get % :id)))]
    node-flow-ids))

(def-event-fx
  :pd/trash-selected
  (fn [{:keys [db]} [_ scene-id]]
    (let [selected-items (get-in db [:tmp :selected])
          selected-nodes (->> selected-items
                              (filter #(= (:type %) :node)))
          selected-node-ids (->> selected-nodes
                                 (map #(:id %))
                                 (set))

          _ (log/debug "selected-nodes" selected-nodes)
          _ (log/debug "selected-node-ids" selected-node-ids)

          ;; get the ids of all flows where one of the selected nodes
          ;; is either the :from or :to tip respectively.
          node-flow-ids (->> selected-nodes
                             (map #(get-flow-ids db %))
                             (flatten)
                             (set))
          ;; get the idfs of all selected flows
          selected-flow-ids (->> selected-items
                                 (filter #(= (:type %) :flow))
                                 (map #(get % :id)))
          ;; the flows that need to be removed is the set of both lists of ids:
          flow-ids (into node-flow-ids selected-flow-ids)

          ;; update and reassemble
          scene (get-in db [:scene scene-id])
          scene-flows (:flows scene)
          scene-flows* (->> scene-flows
                            (remove (fn [[id _]] (in? flow-ids (name id))))
                            (into {}))

          scene-nodes (:nodes scene)
          scene-nodes* (->> scene-nodes
                            (remove (fn [[id _]] (in? selected-node-ids (name id))))
                            (into {}))

          _ (log/debug "scene-flows" (into {} (map #(identity %) scene-flows)))
          _ (log/debug "scene-flows*" (into {} (map #(identity %) scene-flows*)))

          ;_ (log/debug "scene-flows" (into [] (map #(first %) scene-flows)))
          ;_ (log/debug "scene-flows*" (into [] (map #(first %) scene-flows*)))

          _ (log/debug "scene-nodes" (into [] (map #(first %) scene-nodes)))
          _ (log/debug "scene-nodes*" (into [] (map #(first %) scene-nodes*)))

          scene* (-> scene
                     (assoc :flows scene-flows*)
                     (assoc :nodes scene-nodes*))]


      {:db    (assoc-in db [:scene scene-id] scene*)
       :sente (scene/sync-effect {:old scene :new scene*})})))