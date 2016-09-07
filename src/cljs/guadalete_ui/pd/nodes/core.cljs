(ns guadalete-ui.pd.nodes.core
  (:require
    [clojure.set :refer [difference]]
    [re-frame.core :refer [dispatch def-event def-event-fx]]
    [thi.ng.geom.svg.core :as svg]
    [thi.ng.geom.core.vector :refer [vec2]]

    [guadalete-ui.util :refer [pretty vec->map]]
    [guadalete-ui.console :as log]
    [guadalete-ui.pd.layout :refer [node-width line-height node-height]]))

(defn node-title []
  (fn [title]
    [svg/group
     {:class "title"}
     [svg/rect (vec2 0 0) node-width line-height
      {:class "bg"
       :rx    1}]
     [svg/text
      (vec2 4 10)
      title
      {:class       "node-title"
       :text-anchor "left"}]]))

(defn click-target []
  (fn [height]
    [svg/rect (vec2 0 0) node-width height
     {:rx    1
      :class "click-target"}]))


