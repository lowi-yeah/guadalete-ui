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
    [thi.ng.geom.core.vector :as v :refer [vec2 vec3]]
    [thi.ng.geom.core.matrix :as mx]
    [thi.ng.geom.types]
    [thi.ng.geom.circle :as c]
    [thi.ng.geom.rect :as rect]
    [thi.ng.math.core :as math :refer [PI HALF_PI TWO_PI]]
    [thi.ng.geom.svg.core :as svg]

    [guadalete-ui.pd.palette :refer [palette]]

    [guadalete-ui.console :as log]
    [guadalete-ui.util :refer [pretty target-id target-type]]))


;//
;//   _ __  ___ _  _ ______
;//  | '  \/ _ \ || (_-< -_)
;//  |_|_|_\___/\_,_/__\___|
;//
;(defn- ->offset [ev]
;       (v/vec2 (.-offsetX ev) (.-offsetY ev)))
;(defn- ->page [ev]
;       (v/vec2 (.-pageX ev) (.-pageY ev)))
(defn- ->screen [ev]
       (v/vec2 (.-screenX ev) (.-screenY ev)))

(defn- dispatch-mouse
       [msg ev]
       (.preventDefault ev)
       (let [editor-dom (goog.dom/$ "pd-svg")
             layout-id (goog.dom.dataset/get editor-dom "layout-id")
             id (target-id (.-target ev))
             type (keyword (target-type (.-target ev)))
             data {:layout-id layout-id
                   :node-id   id
                   :type      type
                   :position  (->screen ev)}]
            (dispatch [msg data])))


(defn- pd-dimensions []
       (let [jq-svg (goog.dom/$ "pd-svg")]
            (if jq-svg
              (let [width (.-offsetWidth jq-svg)
                    height (.-offsetHeight jq-svg)
                    ; subtract the bottom padding
                    ; (workaround for strange layout/css behaviour)
                    height* (- height 48)]
                   (reaction (v/vec2 width height*))
                   )
              (reaction (v/vec2))
              )))

(defn- is-line?
       "checks whether a form represents a grid line.
       used in grid @see below."
       [x]
       (if (vector? x) (= :line (first x)) false)
       )

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
                                                                (svg/line [start-x y] [stop-x y])))))))

          ;(let [dim-rctn (subscribe [:pd/editor-dimensions])
          ;      start-x (* -1 (:w @dim-rctn))
          ;      stop-x (* 2 (:w @dim-rctn))
          ;      start-y (* -1 (:h @dim-rctn))
          ;      stop-y (* 2 (:h @dim-rctn))
          ;      step 24
          ;      x-range (range start-x stop-x step)
          ;      y-range (range start-y stop-y step)]
          ;     ;as react needs unique keys for each element in a collection,
          ;     ;postwalk is used to attach a :meta key to each svg/line (ie. to all forms in doall)
          ;     (svg/group {:class "grid"}
          ;                (svg/group {:class "x-grid"}
          ;                           (walk/postwalk (fn [x]
          ;                                              (if (is-line? x)
          ;                                                (with-meta x {:key (str (random-uuid))})
          ;                                                x))
          ;                                          (doall (for [x x-range]
          ;                                                      (svg/line [x start-y] [x stop-y])))))
          ;                (svg/group {:class "y-grid"}
          ;                           (walk/postwalk (fn [y]
          ;                                              (if (is-line? y)
          ;                                                (with-meta y {:key (str (random-uuid))})
          ;                                                y))
          ;                                          (doall (for [y y-range]
          ;                                                      (svg/line [start-x y] [stop-x y])))))))
          ))


(defn artboard []
      (fn []
          ;(let [dim-rctn (subscribe [:pd/editor-dimensions])
          ;      offset (v/vec2 8 8)
          ;      size (g/- (vec2 (:w @dim-rctn) (:h @dim-rctn)) (g/* offset 2))]
          ;     (svg/rect offset (:x size) (:y size) {:id "artboard"})
          ;     )
          ))




(defn pd []
      "A PDish editor for wiring up scenes"
      (fn [scene]
          (let [layout (or (:layout scene) {})]
               [:div#pd
                ;[:button#reset.btn-floating
                ; {:on-click #(dispatch [:pd/reset-view])}
                ; [:i.mdi-device-gps-fixed]]
                ;[(editor-wrapper layout-id)

                [svg/svg
                 {
                  :id        "pd-svg"
                  :data-type "pd"
                  ;:width          (:w @dim-rctn)
                  ;:height         (:h @dim-rctn)
                  ;:data-type      "pd"
                  ;:data-layout-id layout-id
                  ;:on-drop        #(dr0p %)
                  ;:on-drag-over   #(allow-drop %)
                  ;:on-mouse-down  #(mouse-dispatch :pd/mouse-down %)
                  ;:on-mouse-move  #(mouse-dispatch :pd/mouse-move %)
                  ;:on-mouse-up    #(mouse-dispatch :pd/mouse-up %)

                  }

                 ^{:key "grid"}
                 [grid]

                 ^{:key "artboard"}
                 [artboard]

                 ;^{:key "bg"}
                 ;[svg/rect (vec2) 0 0 {:id "bg"}]

                 ;^{:key "zoom-group"}
                 ;[svg/group {:id    "zoom-group"
                 ;            :style {:transform css-matrix}
                 ;            :class (if (= :none (:mode @editor-rctn)) "transition")
                 ;            }
                 ;
                 ; ^{:key "grid"}
                 ; [grid]
                 ;
                 ; ^{:key "artboard"}
                 ; [artboard]
                 ;
                 ; ^{:key "nodes"}
                 ; [nodes (:id @room-rctn) scene-layout-rctn]
                 ; ]
                 ]
                ;[palette]

                ;[:pre.code
                ; (pretty layout)]
                ])))