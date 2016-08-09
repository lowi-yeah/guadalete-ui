(ns guadalete-ui.events.flow
  (:require
    [re-frame.core :refer [dispatch def-event def-event-fx def-fx]]
    [thi.ng.geom.core :as g]
    [thi.ng.geom.core.vector :refer [vec2]]
    [guadalete-ui.pd.flow :as flow]
    [guadalete-ui.util :refer [pretty kw* vec-map]]
    [guadalete-ui.pd.link :as link]
    [guadalete-ui.console :as log]))

(defn- decorate-link
  "Decorates a given link with data required for properly renderingit .
  Called during 'begin'"
  [db scene-id node-id link-id]
  (let [link (link/->get db scene-id node-id link-id)
        link* (assoc link :state :active)]
    (link/->update db scene-id node-id link-id link*)))


;//    __ _
;//   / _| |_____ __ __
;//  |  _| / _ \ V  V /
;//  |_| |_\___/\_/\_/
;//
(def-event
  :flow/mouse-down
  (fn [db [_ {:keys [scene-id node-id id position]}]]
    (log/debug ":flow/mouse-down")
    (let [scene (get-in db [:scene scene-id])
          node-link (link/->get db scene-id node-id id)
          flow (condp = (kw* (:direction node-link))
                 :in {:from :mouse :to {:node-id node-id :id id}}
                 :out {:from {:node-id node-id :id id} :to :mouse}
                 nil)]
      (-> db
          (assoc-in [:tmp :flow] flow)
          (assoc-in [:tmp :start-pos] (vec-map position))
          (assoc-in [:tmp :mouse-pos] (vec-map position))
          (assoc-in [:tmp :mode] :link)
          (assoc-in [:tmp :scene] scene)))))


;; Called when moving the mouse during link creation.
;; Updates the mouse mosition (for rendering the temporary flow.
;; Also Checks the current target and sets appropriae values in the db.
(def-event-fx
  :flow/mouse-move
  (fn [{:keys [db]} [_ {:keys [scene-id position type] :as data}]]
    (log/debug ":flow/mouse-move")
    (let [dispatch* (if (= :link (kw* type))
                      [:flow/check-connection data]
                      [:flow/reset-target data])]
      {:db       (assoc-in db [:tmp :mouse-pos] (vec-map position))
       :dispatch dispatch*})))

(def-event-fx
  :flow/mouse-up
  (fn [{:keys [db]} [_ {:keys [scene-id position type] :as data}]]
    (let []
      (log/debug ":flow/mouse-up")
      {:db (-> db
               (assoc-in [:tmp :mode] :none)
               (assoc-in [:tmp :scene] nil)
               (assoc-in [:tmp :start-pos] nil)
               (assoc-in [:tmp :mouse-pos] nil)
               (assoc-in [:tmp :flow] nil))})))

(def-event
  :flow/check-connection
  (fn [db [_ data]]
    ;(flow/check-connection data db)
    ;(log/debug "checking flow connection")
    db))

(def-event
  :flow/reset-target
  (fn [db [_ data]]
    ;(flow/reset-target data db)
    ;(log/debug "resetting flow target")
    db))

