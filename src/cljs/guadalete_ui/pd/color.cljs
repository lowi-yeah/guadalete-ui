(ns guadalete-ui.pd.color
  (:require-macros
    [cljs.core.async.macros :refer [go]])
  (:require
    [clojure.string :as str]
    [cljs.reader :refer [read-string]]
    [cljs.core.async :as async :refer [<! >! put! chan]]
    [thi.ng.geom.core :as g]
    [thi.ng.math.core :as math :refer [PI HALF_PI TWO_PI]]
    [thi.ng.geom.core.vector :refer [vec2]]
    [thi.ng.geom.svg.core :as svg]
    [thi.ng.color.core :as col]
    [reagent.core :as reagent]
    [re-frame.core :refer [dispatch]]
    [guadalete-ui.util :refer [pretty map-value]]
    [guadalete-ui.console :as log]))




(defn change-brightness [v color channel]
      (go (>! channel (assoc color :v (/ v 255.0)))))

(defn change-temperature [s color channel]
      (go (>! channel (assoc color :s (/ s 255.0)))))


(defn- brightness-slider []
       (fn [color channel]
           [:div.color.brightness
            [(with-meta identity
                        {:component-did-mount
                         (fn [this]
                             (-> this
                                 (reagent/dom-node)
                                 (js/$)
                                 (.range
                                   (js-obj "onChange" #(change-brightness % color channel)
                                           "min" 0
                                           "max" 255
                                           "start" (* 255 (:v color))))))})
             [:div#white-range.ui.range]]]))

(defn- temperature-slider []
       (fn [color channel]
           [:div.color.temperature
            [(with-meta identity
                        {:component-did-mount
                         (fn [this]
                             (-> this
                                 (reagent/dom-node)
                                 (js/$)
                                 (.range
                                   (js-obj "onChange" #(change-temperature % color channel)
                                           "min" 0
                                           "max" 255
                                           "start" (* 255 (:s color))))))})
             [:div#white-range.ui.range]]]))


;//          _                  _            _
;//   __ ___| |___ _ _  __ __ __ |_  ___ ___| |
;//  / _/ _ \ / _ \ '_| \ V  V / ' \/ -_) -_) |
;//  \__\___/_\___/_|    \_/\_/|_||_\___\___|_|
;//
(defn- size []
       (let [width (.-innerWidth js/window)
             size* (- (* 0.704 width) 48 8)]
            (vec2 size* size*)))



(def padding (vec2 4 4))
(def radius (/ (:x (size)) 2))

(defn- frame-size []
       (-> (size) (g/+ (g/* padding 2))))

(defn- centre [] (g/div (frame-size) 2))


; it should be sufficient to use local state (atoms) here instead of re-frame dispatches
(def color-wheel-state (reagent/atom nil))


(defn- vec-sqrt-sum [vector]
       (.sqrt js/Math (+ (:x vector) (:y vector))))

(defn- get-hue-saturation
       "Helper function to convert a point within the color circle to hue/saturaion values"
       [position]
       (let [radius* (-> (g/* position position) (vec-sqrt-sum))
             angle* (.atan2 js/Math (:y position) (:x position))
             saturation (map-value radius* (vec2 0 radius) (vec2 0 1))
             hue (map-value angle* (vec2 (* -1 PI) PI) (vec2 0 1))]
            ;(log/debug "get-hue-saturation | position:" (str position) "-> radius*" radius* "angle*" angle* "-> hue:" hue " saturation:" saturation  )
            {:h hue :s saturation}))

(defn color-wheel-coordinates
      "Helper function to convert hue/saturation values to a point within the color circle."
      [color]
      (let [radius* (map-value (:s color) (vec2 0 1) (vec2 0 radius))
            angle* (map-value (:h color) (vec2 0 1) (vec2 (* -1 PI) PI))
            x (* (.cos js/Math angle*) radius*)
            y (* (.sin js/Math angle*) radius*)]
           ;(log/debug "color-wheel-coordinates | hue:" (:h color) " saturation:" (:s color) "-> radius*" radius* "angle*" angle* "->" (str [x y]))
           (vec2 x y)))


(defn- get-horizontal-points
       "Helper function to get the endpoints of the horizontal crosshair line"
       [coordinates]
       (let [x (->>
                 (.pow js/Math (:y coordinates) 2)
                 (- (* radius radius))
                 (.sqrt js/Math))
             start (vec2 x (:y coordinates))
             end (vec2 (* -1 x) (:y coordinates))
             start* (g/+ start (centre))
             end* (g/+ end (centre))]
            {:start start* :end end*}))

(defn- get-vertical-points
       "Helper function to get the endpoints of the vertical crosshair line"
       [coordinates]
       (let [y (->>
                 (.pow js/Math (:x coordinates) 2)
                 (- (* radius radius))
                 (.sqrt js/Math))
             start (vec2 (:x coordinates) y)
             end (vec2 (:x coordinates) (* -1 y))
             start* (g/+ start (centre))
             end* (g/+ end (centre))]
            {:start start* :end end*}))

(defn- calculate-crosshair [color]
       (let [coordinates (color-wheel-coordinates color)]
            {:h (get-horizontal-points coordinates)
             :v (get-vertical-points coordinates)}))


; mouse
; ********************************
(defn- mouse-down [ev]
       (.preventDefault ev)
       (let [ev* (.-nativeEvent ev)
             position (vec2 (.-offsetX ev*) (.-offsetY ev*))]
            (reset! color-wheel-state position)))

(defn- mouse-move [ev color channel]
       (.preventDefault ev)
       (when @color-wheel-state
             (let [ev* (.-nativeEvent ev)
                   position (vec2 (.-offsetX ev*) (.-offsetY ev*))
                   hue-saturation (get-hue-saturation (g/- position (centre)))
                   color* (merge color hue-saturation)]
                  (go (>! channel color*)))))

(defn- mouse-up [ev color channel]
       (mouse-move ev color channel)
       (reset! color-wheel-state nil))

(defn- mouse-enter [ev]
       (let [ev* (.-nativeEvent ev)]
            (if (= 0 (.-buttons ev*)) (reset! color-wheel-state nil))))

(defn- mouse-leave [ev]
       (log/debug "mouse-leave" ev))


; component
; ********************************
(defn- color-picker []
       (fn [color channel]
           (let [crosshair-vectors (calculate-crosshair color)]
                ^{:key "cp-svg"}
                [svg/svg
                 {:id     "color-picker"
                  :width  (:x (frame-size))
                  :height (:y (frame-size))}

                 ^{:key "cp-bg"}
                 [svg/group
                  {:id "cp-bg"}

                  ^{:key "cp-defs"}
                  [svg/defs

                   ^{:key "cp-pattern"}
                   [:pattern
                    {:id           "cp-pattern"
                     :patternUnits "userSpaceOnUse"
                     :width        (:x (frame-size))
                     :height       (:y (frame-size))
                     }
                    ^{:key "cp-pattern-img"}
                    [:image {:x         (:x padding)
                             :y         (:y padding)
                             :width     (:x (size))
                             :height    (:y (size))
                             :xlinkHref "/images/color-wheel.png"
                             }]]

                   ^{:key "cp-clip-path"}
                   [:clipPath#clippy
                    [svg/circle (centre) radius]
                    ]]

                  ^{:key "cp-circle"}
                  [svg/circle (centre) radius
                   {:id             "color-wheel"
                    :fill           "url(#cp-pattern)"
                    :on-mouse-down  #(mouse-down %)
                    :on-mouse-move  #(mouse-move % color channel)
                    :on-mouse-up    #(mouse-up % color channel)
                    :on-mouse-enter #(mouse-enter %)
                    ;:on-mouse-leave #(mouse-leave %)
                    }]

                  ^{:key "v-line"}
                  [svg/line (get-in crosshair-vectors [:v :start]) (get-in crosshair-vectors [:v :end])
                   {:class "crosshair h"}]

                  ^{:key "h-line"}
                  [svg/line (get-in crosshair-vectors [:h :start]) (get-in crosshair-vectors [:h :end])
                   {:class "crosshair v"}]]])))

(defn- white-widget []
       (fn [color channel]
           [:div#w.widget
            [brightness-slider color channel]]))

(defn- two-tone-widget []
       (fn [color channel]
           [:div#ww.widget
            [temperature-slider color channel]
            [brightness-slider color channel]]))

(defn- rgb-widget []
       (fn [color channel]
           [:div#www.widget
            [brightness-slider color channel]
            [color-picker color channel]]))

(defn- blank-widget []
       (fn [color])
       [:pre.code "blank-widget"])

(defn make-id [color type]
      (let [color-array @color
            h (first color-array)
            s (second color-array)
            v (nth color-array 2)]
           (str type " " h " " s " " v)))

(defn from-id [color-id]
      (let [[type h s v] (str/split color-id #" ")
            color (col/hsva (read-string h) (read-string s) (read-string v))]
           {:type type :color color}))

(defn color-widget
      "Renders a color widget for the given color-item.
      A color-tiem consits of a color type ['w' 'ww' 'rgb']
      and the color in hsv.
      The channel is used to write changes back to whomever created the widget
      (pressumably the color-modal)"
      []
      (fn [color-item-rctn channel]
          (condp = (:type @color-item-rctn)
                 "w" [white-widget (:color @color-item-rctn) channel]
                 "ww" [two-tone-widget (:color @color-item-rctn) channel]
                 "rgb" [rgb-widget (:color @color-item-rctn) channel]
                 [blank-widget])))

(defn render-color
      ; little hack used by color nodes for prettier rendering
      [{:keys [type color] :as color-item}]
      (let [color* (-> color
                       (assoc :a (:v color))
                       (assoc :v 1))
            color* (if (= type "w") (assoc color* :s 0) color*)
            color* (if (= type "ww") (assoc color* :h 0.125) color*)]
           color*))