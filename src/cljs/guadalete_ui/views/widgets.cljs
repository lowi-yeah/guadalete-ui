(ns guadalete-ui.views.widgets
  (:require
    [thi.ng.geom.core :as g]
    [thi.ng.math.core :as math :refer [PI HALF_PI TWO_PI]]
    [thi.ng.geom.core.vector :refer [vec2]]
    [thi.ng.geom.svg.core :as svg]
    [thi.ng.color.core :as col]
    [guadalete-ui.console :as log])
  (:require-macros [thi.ng.math.macros :as mm]))



(defn- map-x
  "maps the x values from [min-time now] to [0 w]"
  [t domain range]
  (math/map-interval t domain range))

(defn- map-y
  "maps the y value from [0 255] to [h 0]"
  [v h]
  (math/map-interval v (vec2 0 255) (vec2 h 0)))

(defn- signal-sparkline
  "renders a sparkline of the given signal"
  []
  (fn [signal]
    (let [w 240
          h 64
          id (str "signal-" (:id signal))
          values (or (:values signal) [[0 0] [1 0]])
          t-min (int (first (first values)))
          t-max (int (first (last values)))
          points (into [] (map (fn [[t v]] (vec2 (map-x (int t) (vec2 t-min t-max) (vec2 0 w)) (map-y v h))) values))
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

(defn sparky
  "renders a sparkline of the given signal"
  []
  (fn [signal]
    (let [w 92
          h 32
          id (str "signal-" (:id signal))
          values (or (:values signal) [[0 0] [1 0]])
          t-min (int (first (first values)))
          t-max (int (first (last values)))
          points (into [] (map (fn [[t v]] (vec2 (map-x (int t) (vec2 t-min t-max) (vec2 0 w)) (map-y v h))) values))
          ]
      ^{:key id}
      [svg/group
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