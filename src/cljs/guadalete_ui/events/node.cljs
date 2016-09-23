(ns guadalete-ui.events.node

  (:require
    [re-frame.core :refer [dispatch def-event def-event-fx def-fx]]
    [thi.ng.geom.core :as g]
    [thi.ng.geom.core.vector :refer [vec2]]
    [guadalete-ui.events.scene :as scene]
    [guadalete-ui.util :refer [pretty validate! vec->map]]
    [guadalete-ui.console :as log]
    [schema.core :as s]
    [guadalete-ui.schema :as gs]))

;//                                           _
;//   _ __  ___ _  _ ______   _____ _____ _ _| |_ ___
;//  | '  \/ _ \ || (_-< -_) / -_) V / -_) ' \  _(_-<
;//  |_|_|_\___/\_,_/__\___| \___|\_/\___|_||_\__/__/
;//

(def-event-fx
  :node/mouse-up
  (fn [{:keys [db]} [_ {:keys [scene-id]}]]
      (let [scene (get-in db [:scene scene-id])
            stashed-scene (get-in db [:tmp :scene])
            tmp* (-> (:tmp db)
                     (assoc :mode :none)
                     (dissoc :scene)
                     (dissoc :pos))]
           {:db    (assoc db :tmp tmp*)
            :sente (scene/sync-effect {:old stashed-scene :new scene})})))

(def-event
  :node/reset-all
  (fn [db [_ scene-id]]
      (log/debug ":node/reset-all" scene-id)
      ;(node/reset-all scene-id db)
      db))

;; UPDATE COLOR NODE
;; ********************************
(s/defn ^:always-validate make-in-link :- gs/InLink
        [channel :- s/Str
         index :- s/Num]
        {:id        channel
         :name      channel
         :accepts   :value
         :direction :in
         :index     index})

(defn- get-link-by-channel [links channel name index]
       (let [existing-link (->> links
                                (filter (fn [link] (= channel (keyword (:id link)))))
                                (first))]
            (or existing-link (make-in-link (clojure.core/name channel) index))))

(defmulti update-links (fn [color-type _ _] color-type))

(defmethod update-links :v
           [_ node-id in-links]
           [(get-link-by-channel in-links :brightness "brightness" 0)])

(defmethod update-links :sv
           [_ node-id in-links]
           [(get-link-by-channel in-links :brightness "brightness" 0)
            (get-link-by-channel in-links :saturation "saturation" 1)])

(defmethod update-links :hsv
           [_ node-id in-links]
           [(get-link-by-channel in-links :brightness "brightness" 0)
            (get-link-by-channel in-links :saturation "saturation" 1)
            (get-link-by-channel in-links :hue "hue" 2)])

(def-event-fx
  :node/update-color
  (fn [{:keys [db]} [_ {:keys [item-id scene-id node-id] :as data}]]
      (let [scene (get-in db [:scene scene-id])
            color (get-in db [:color item-id])
            node (get-in scene [:nodes (keyword node-id)])
            in-links (->> (:links node)
                          (filter #(= :in (:direction %)))
                          (into []))
            out-link (->> (:links node)
                          (filter #(= :out (:direction %)))
                          (first))

            ;; reassemble
            in-links* (update-links (-> color (get :type)) node-id in-links)
            out-link* (assoc out-link :index (count in-links*))
            links* (conj in-links* out-link*)
            node* (assoc node :links links*)
            scene* (assoc-in scene [:nodes (keyword node-id)] node*)
            db* (-> db (assoc-in [:scene scene-id] scene*))]
           {:db    db*
            :sente (scene/sync-effect {:old scene :new scene*})})))

