(ns guadalete-ui.events.scene
  (:require
    [re-frame.core :refer [dispatch def-event def-event-fx def-fx]]
    [differ.core :as differ]
    [guadalete-ui.util :refer [pretty]]
    [guadalete-ui.console :as log]))

(defn sync-effect [{:keys [old new scene]}]
  (let [id (if scene (:id scene) (:id new))
        flag (if scene :replace :patch)
        diff (if (= :replace flag)
               scene
               (differ/diff old new))
        data {:id   id
              :flag flag
              :diff diff}]
    (log/debug "sync scene" (pretty diff))
    {:topic      :scene/update
     :data       data
     :on-success [:success-scene-update]
     :on-failure [:failure-scene-update]}))


(def-event
  :success-scene-update
  (fn [world response]
    (log/debug ":success-scene-update" response)
    world))

(def-event
  :failure-scene-update
  (fn [world response]
    (log/error "Scene update failed:" response)
    world))
