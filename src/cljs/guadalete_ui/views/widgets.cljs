(ns guadalete-ui.views.widgets
  (:require
    [thi.ng.geom.core :as g]
    [thi.ng.math.core :as math :refer [PI HALF_PI TWO_PI]]
    [thi.ng.geom.core.vector :refer [vec2]]
    [thi.ng.geom.svg.core :as svg]
    [thi.ng.color.core :as col]
    [guadalete-ui.console :as log])
  (:require-macros [thi.ng.math.macros :as mm])
  )

(def w 240)
(def h 64)

(defn- map-x
  "maps the x values from [min-time now] to [0 w]"
  [t min max]
  (math/map-interval t (vec2 min max) (vec2 0 w)))

(defn- map-y
  "maps the y value from [0 255] to [h 0]"
  [v]
  (math/map-interval v (vec2 0 255) (vec2 h 0)))

(defn- signal-sparkline
  "renders a sparkline of the given signal"
  []
  (fn [signal]
    (let [id (str "signal-" (:id signal))
          values (or (:values signal) [[0 0] [1 0]])
          t-min (int (first (first values)))
          t-max (int (first (last values)))
          points (into [] (map (fn [[t v]] (vec2 (map-x (int t) t-min t-max) (map-y v))) values))
          ]
      ^{:key id}
      [svg/svg
       {:id     id
        :class  "sparkline"
        :width  w
        :height h}
       ^{:key (str "s-" id)}
       [svg/line-strip
        points
        {}
        ]
       ])))