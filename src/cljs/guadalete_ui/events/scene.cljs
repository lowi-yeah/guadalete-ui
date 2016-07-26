(ns guadalete-ui.events.scene
  (:require
    [re-frame.core :refer [dispatch def-event def-event-fx def-fx]]
    [differ.core :as differ]
    [guadalete-ui.util :refer [pretty]]
    [guadalete-ui.console :as log]))


(defn- patch [db update original]
  (let [patch (differ/diff original update)
        empty-patch? (and (empty? (first patch)) (empty? (second patch)))]
    (if empty-patch?
      {:db db}
      {:db    db
       :sente {:topic      :scene/update
               :data       patch
               :on-success [:success-scene-update]
               :on-failure [:failure-scene-update]}})))

(defn- reset [db update]
  (let [sente-effect {:topic      :scene/update
                      :data       update
                      :timeout    8000
                      :on-success [:success-scene-update]
                      :on-failure [:failure-scene-update]}]
    {:db    db
     :sente sente-effect}))


(def-event-fx
  :scene/update
  (fn [{:keys [db]} [_ update original]]
    (if original
      (patch db update original)
      (reset db update))))

(def-event
  :success-scene-update
  (fn [world response]
    (log/error "Scene update success!:" response)
    world))

(def-event
  :failure-scene-update
  (fn [world response]
    (log/error "Scene update failed:" response)
    world))
