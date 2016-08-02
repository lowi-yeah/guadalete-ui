(ns guadalete-ui.events.pd
  (:require
    [re-frame.core :refer [dispatch def-event def-event-fx def-fx]]
    [thi.ng.geom.core :as g]
    [thi.ng.geom.core.vector :refer [vec2]]
    [guadalete-ui.util :refer [pretty kw* vec-map offset-position]]
    [guadalete-ui.console :as log]
    [guadalete-ui.items :refer [reset-scene]]
    [guadalete-ui.events.scene :as scene]))

(def-event
  :pd/mouse-down
  (fn [db [_ {:keys [scene-id position]}]]
    (let [scene (get-in db [:scene scene-id])  
          scene* (assoc scene  :mode :pan )
          stash-scene (assoc scene  :position position)]
      (-> db
          (assoc-in [:scene scene-id] scene*)
          (assoc-in [:tmp :scene] stash-scene)))))

(def-event
  :pd/mouse-move
  (fn [db [_ {:keys [scene-id position]}]]
    (let [scene (get-in db [:scene scene-id])
          stashed-scene (get-in db [:tmp :scene])
          δ (g/- (vec2 position) (vec2 (:position stashed-scene)))
          translation* (g/+ (vec2 (:translation stashed-scene)) δ)
          scene* (assoc scene :translation (vec-map translation*))]
      (assoc-in db [:scene scene-id] scene*))))


(def-event-fx
  :pd/mouse-up
  (fn [{:keys [db]} [_ {:keys [scene-id]}]]
    (let [scene (get-in db [:scene scene-id])
          scene* (reset-scene scene)
          stashed-scene (get-in db [:tmp :scene])]
      {:db    (assoc-in db [:scene scene-id] scene*)
       :sente (scene/sync-effect {:old stashed-scene :new scene*})})))

(def-event-fx
  :pd/register-node
  (fn [{:keys [db]} [_ {:keys [scene-id node-id new-id]}]]
    (let [scene (get-in db [:scene scene-id])
          scene* (assoc-in scene [:nodes (kw* node-id) :item-id] new-id)]
      {:db    (assoc-in db [:scene scene-id] scene*)
       :sente (scene/sync-effect {:old scene :new scene*})})))

