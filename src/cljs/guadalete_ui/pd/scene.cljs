(ns guadalete-ui.pd.scene
  (:require
    [guadalete-ui.console :as log]
    [guadalete-ui.pd.nodes :as nodes]
    [guadalete-ui.util :refer [pretty]]))


(defn reset [[id scene]]
      (let [nodes* (nodes/reset-all* (:nodes scene))]
           [id (assoc scene :nodes nodes*)]))

(defn reset-all [scenes-map]
      (into {} (map reset scenes-map)))