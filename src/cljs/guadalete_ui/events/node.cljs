(ns guadalete-ui.events.node

  (:require
    [re-frame.core :refer [dispatch def-event def-event-fx def-fx]]
    [guadalete-ui.pd.mouse :as mouse]
    [guadalete-ui.util :refer [pretty kw*]]
    [guadalete-ui.console :as log]
    [guadalete-ui.pd.nodes :as node]
    [guadalete-ui.events.scene :as scene]))


(def-event
  :node/reset-all
  (fn [db [_ scene-id]]
    (log/debug ":node/reset-all" scene-id)
    ;(node/reset-all scene-id db)
    db))



;; UPDATE COLOR NODE
;; ********************************

(defn- color-in-link [type name node-id]
  {:id        (str type "-" node-id)
   :type      type
   :name      name
   :ilk       "signal"
   :state     "normal"
   :direction "in"})

(defn- get-link-by-type [links type node-id name]
  (let [existing-link (->> links
                           (filter #(= type (kw* (:type %))))
                           (first))]
    (or existing-link (color-in-link type name node-id))))

(defmulti update-links (fn [color-type _ _] color-type))

(defmethod update-links :v
  [_ node-id in-links]
  [(get-link-by-type in-links "v" node-id "brightness")])

(defmethod update-links :sv
  [_ node-id in-links]
  [(get-link-by-type in-links "v" node-id "brightness")
   (get-link-by-type in-links "s" node-id "saturation")])

(defmethod update-links :hsv
  [_ node-id in-links]
  [(get-link-by-type in-links "v" node-id "brightness")
   (get-link-by-type in-links "s" node-id "saturation")
   (get-link-by-type in-links "h" node-id "hue")])

(def-event-fx
  :node/update-color
  (fn [{:keys [db]} [_ {:keys [item-id scene-id node-id]}]]
    (let [scene (get-in db [:scene scene-id])
          color (get-in db [:color item-id])
          node (get-in scene [:nodes (kw* node-id)])
          out-link (first (vals (filter (fn [[_id link]] (= "out" (get link :direction))) (:links node))))
          in-links (vals (filter (fn [[_id link]] (= "in" (get link :direction))) (:links node)))

          ;; reassemble
          in-links* (update-links (-> color (get :type)) node-id in-links)
          links* (->> (conj in-links* out-link)
                      (map (fn [l] [(:id l) l]))
                      (into {}))
          node* (assoc node :links links*)
          scene* (assoc-in scene [:nodes (kw* node-id)] node*)
          db* (-> db (assoc-in [:scene scene-id] scene*))
          ]
      {:db    db*
       :sente (scene/sync-effect {:old scene :new scene*})})))
