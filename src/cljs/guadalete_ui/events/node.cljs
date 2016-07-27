(ns guadalete-ui.events.node

  (:require
    [re-frame.core :refer [dispatch def-event def-event-fx def-fx]]
    [guadalete-ui.pd.mouse :as mouse]
    [guadalete-ui.util :refer [pretty kw*]]
    [guadalete-ui.console :as log]
    [guadalete-ui.pd.nodes :as node]))


(def-event
  :node/reset-all
  (fn [db [_ scene-id]]
    (log/debug ":node/reset-all" scene-id)
    ;(node/reset-all scene-id db)
    db
    ))