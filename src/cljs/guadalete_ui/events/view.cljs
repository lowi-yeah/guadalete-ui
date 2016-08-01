(ns guadalete-ui.events.view
  (:require
    [re-frame.core :refer [dispatch def-event def-event-fx def-fx]]
    [guadalete-ui.console :as log]))


(def-event
  :view/dimensions
  (fn [db [_ data]]
    (let [dimensions (into {} (map (fn [[key vec]] [key [(:x vec) (:y vec)]]) data))
          db* (assoc-in db [:view :dimensions] dimensions)]
      db*)))