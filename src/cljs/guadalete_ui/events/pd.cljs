(ns guadalete-ui.events.pd
  (:require
    [re-frame.core :refer [dispatch def-event def-event-fx def-fx]]
    [thi.ng.geom.core :as g]
    [thi.ng.geom.core.vector :refer [vec2]]
    [guadalete-ui.util :refer [pretty kw* vec-map offset-position]]
    [guadalete-ui.console :as log]))

(def-event
  :pd/mouse-down
  (fn [db [_ {:keys [scene-id node-id position]}]]
    (let [scene (get-in db [:scene scene-id])  
          scene* (assoc scene 
                   :mode :pan 
                   :pos-0 (vec-map position)  
                   :pos-1 (vec-map (:translation scene)))]
      (assoc-in db [:scene scene-id] scene*))))

(def-event
  :pd/mouse-move
  (fn [db [_ {:keys [scene-id position]}]]
    (let [scene (get-in db [:scene scene-id])
          δ (g/- (vec2 position) (vec2 (:pos-0 scene)))
          translation* (g/+ (vec2 (:pos-1 scene)) δ)
          scene* (assoc scene :translation (vec-map translation*))]
      (assoc-in db [:scene scene-id] scene*))))

(def-event
  :pd/mouse-up
  (fn [db [_ {:keys [scene-id position]}]]
    (let [scene (get-in db [:scene scene-id])
          scene* (dissoc scene :pos-0 :pos-1 :flow/mouse :mode)]
      (dispatch [:scene/update scene*])
      (dispatch [:node/reset-all scene-id])
      db)
    db))
