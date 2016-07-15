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
(def h 28)

(defn- map-x
       "maps the x values from [min-time now] to [0 w]"
       [x]
       (math/map-interval x (vec2 0 1) (vec2 h 0)))

(defn- map-y
       "maps the y values from [0 1] to [h 0]"
       [x]
       (math/map-interval x (vec2 0 1) (vec2 h 0)))

(defn- signal-sparkline
       "renders a sparkline of the given signal"
       []
       (fn [signal]
           (let [id (str "signal-" (:id signal))
                 values (or (:values signal) [0])
                 ]
                ^{:key id}
                [svg/svg
                 {:id     id
                  :class  "sparkline"
                  :width  w
                  :height h}
                 [svg/line (vec2 0 (map-y 0)) (vec2 w (map-y 0.9)) {}]
                 ])))