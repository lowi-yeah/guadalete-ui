(ns guadalete-ui.pd.core
  (:import [goog.dom query])
  (:require-macros
    [thi.ng.math.macros :as mm]
    [reagent.ratom :refer [reaction]])
  (:require
    [clojure.walk :as walk]
    [re-frame.core :refer [dispatch]]
    [clojure.string :as string]
    [reagent.core :as reagent]

    [goog.dom]
    [goog.dom.dataset]

    [thi.ng.geom.core :as g]
    [thi.ng.geom.core.vector :refer [vec2]]
    [thi.ng.geom.core.matrix :as mx]
    [thi.ng.geom.types]
    [thi.ng.geom.circle :as c]
    [thi.ng.geom.rect :as rect]
    [thi.ng.math.core :as math :refer [PI HALF_PI TWO_PI]]
    [thi.ng.geom.svg.core :as svg]

    [guadalete-ui.pd.palette :refer [palette]]

    [guadalete-ui.console :as log]
    [guadalete-ui.util :refer [pretty target-id target-type css-matrix-string]]))


;//
;//   _ __  ___ _  _ ______
;//  | '  \/ _ \ || (_-< -_)
;//  |_|_|_\___/\_,_/__\___|
;//
;(defn- ->offset [ev]
;       (vec2 (.-offsetX ev) (.-offsetY ev)))
;(defn- ->page [ev]
;       (vec2 (.-pageX ev) (.-pageY ev)))
(defn- ->screen [ev]
       (vec2 (.-screenX ev) (.-screenY ev)))

(defn- dispatch-mouse
       [msg ev scene-id layout]
       (.preventDefault ev)
       (let [id (target-id (.-target ev))
             type (keyword (target-type (.-target ev)))
             data {:scene-id scene-id
                   :node-id  id
                   :type     type
                   :position (->screen ev)
                   :layout   layout}]
            (dispatch [msg data])))


(defn- pd-dimensions []
       (let [jq-svg (goog.dom/$ "pd-svg")]
            (if jq-svg
              (let [bounding-box (.getBoundingClientRect jq-svg)

                    width (.-width bounding-box )
                    height (.-height bounding-box )
                    ; subtract the bottom padding
                    ; (workaround for strange layout/css behaviour)
                    height* (- height 48)]
                   (reaction (vec2 width height*)))
              (reaction (vec2)))))

(def default-layout
  {:translation (vec2)
   :nodes       {}
   :mode        :none})

(defn- is-line?
       "checks whether a form represents a grid line.
       used in grid @see below."
       [x]
       (if (vector? x) (= :line (first x)) false))

(defn grid
      "the background grid"
      []
      (fn []
          (let [dim-rctn (pd-dimensions)
                start-x (* -1 (:x @dim-rctn))
                stop-x (* 2 (:x @dim-rctn))
                start-y (* -1 (:y @dim-rctn))
                stop-y (* 2 (:y @dim-rctn))
                step 24
                x-range (range start-x stop-x step)
                y-range (range start-y stop-y step)]
               ;as react needs unique keys for each element in a collection,
               ;postwalk is used to attach a :meta key to each svg/line (ie. to all forms in doall)
               (svg/group {:id "grid"}
                          (svg/group {:id "x-grid"}
                                     (walk/postwalk (fn [x]
                                                        (if (is-line? x)
                                                          (with-meta x {:key (str (random-uuid))})
                                                          x))
                                                    (doall (for [x x-range]
                                                                (svg/line [x start-y] [x stop-y])))))
                          (svg/group {:id "y-grid"}
                                     (walk/postwalk (fn [y]
                                                        (if (is-line? y)
                                                          (with-meta y {:key (str (random-uuid))})
                                                          y))
                                                    (doall (for [y y-range]
                                                                (svg/line [start-x y] [stop-x y])))))))))


(defn artboard []
      (fn []
          (let [dim-rctn (pd-dimensions)
                offset (vec2 8 8)
                size (g/- (vec2 (:x @dim-rctn) (:y @dim-rctn)) (g/* offset 2))
                size* (vec2 (max 0 (:x size)) (max 0 (:y size)))
                ]
               (svg/rect offset (:x size*) (:y size*) {:id "artboard"}))))

(defn pd []
      "A PDish editor for wiring up scenes"
      (fn [scene]
          (let [layout (or (:layout scene) default-layout)
                css-matrix (css-matrix-string layout)]
               [:div#pd
                ;[:button#reset.btn-floating
                ; {:on-click #(dispatch [:pd/reset-view])}
                ; [:i.mdi-device-gps-fixed]]
                ;[(editor-wrapper layout-id)

                ^{:key "svg"}
                [svg/svg
                 {
                  :id            "pd-svg"
                  :data-type     "pd"
                  ;:on-drop        #(dr0p %)
                  ;:on-drag-over   #(allow-drop %)
                  :on-mouse-down #(dispatch-mouse :pd/mouse-down % (:id scene) layout)
                  :on-mouse-move #(dispatch-mouse :pd/mouse-move % (:id scene) layout)
                  :on-mouse-up   #(dispatch-mouse :pd/mouse-up % (:id scene) layout)

                  }

                 ^{:key "pan-group"}
                 [svg/group {:id    "pan-group"
                             :style {:transform css-matrix}
                             ;:class (if (= :none (:mode @editor-rctn)) "transition")
                             }

                  ^{:key "grid"} [grid]
                  ^{:key "artboard"} [artboard]

                  ; ^{:key "nodes"}
                  ; [nodes (:id @room-rctn) scene-layout-rctn]
                  ]]
                [palette]

                ;[:pre.code
                ; (pretty layout)]
                ])))