(ns guadalete-ui.pd.handlers
  (:require
    [re-frame.core :refer [dispatch register-handler]]
    [thi.ng.geom.core :as g]
    [thi.ng.geom.core.vector :refer [vec2]]
    [guadalete-ui.pd.mouse :as mouse]
    [guadalete-ui.console :as log]
    [guadalete-ui.util :refer [pretty]]
    ))


(defn- pan-canvas [db scene-id layout position]
       (let [scene (get-in db [:scene scene-id])
             δ (g/- (vec2 position) (vec2 (:pos-0 layout)))
             translation-0 (g/+ (vec2 (:pos-1 layout)) δ)
             layout-0 (assoc layout :translation translation-0)
             scene-0 (assoc scene :layout layout-0)]
            (assoc-in db [:scene scene-id] scene-0)))


(register-handler
  :pd/mouse-down
  (fn [db [_ {:keys [type scene-id node-id position layout] :as data}]]
      (mouse/down data db)))

(register-handler
  :pd/mouse-move
  (fn [db [_ {:keys [scene-id layout position]}]]
      ;(log/debug ":pd/mouse-move" (str position))
      (condp = (:mode layout)
             ;:move (move-node db position)
             :move db
             :pan (pan-canvas db scene-id layout position)
             ; if :none, do nothing
             :none db)
      ))

(register-handler
  :pd/mouse-up
  (fn [db [_ {:keys [scene-id layout]}]]

      (log/debug ":pd/mouse-up")
      ;(let [pd-0 (assoc (:pd db) :mode :none)
      ;      mode (get-in db [:pd :mode])
      ;      layout-id (get-in db [:pd :node :layout-id])]
      ;     ; in case a node is being dragged arounbd, push its position to the backend
      ;     (if (= :move mode) (dispatch [:scenelayout/push layout-id]))
      ;     (assoc db :pd pd-0)
      ;     ;db
      ;     )
      (let [scene (get-in db [:scene scene-id])
            layout-0 (assoc layout :mode :none)
            scene-0 (assoc scene :layout layout-0)]
           (assoc-in db [:scene scene-id] scene-0))

      ))