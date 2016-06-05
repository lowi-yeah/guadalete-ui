(ns guadalete-ui.pd.core
  (:import [goog.dom query])
  (:require-macros
    [thi.ng.math.macros :as mm]
    [reagent.ratom :refer [reaction]])
  (:require
    [clojure.walk :as walk]
    [re-frame.core :refer [dispatch subscribe]]
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

    [guadalete-ui.pd.palette :refer [palette drop* allow-drop]]
    [guadalete-ui.pd.nodes :refer [nodes]]
    [guadalete-ui.pd.links :refer [links]]

    [guadalete-ui.console :as log]
    [guadalete-ui.util :refer [pretty]]
    [guadalete-ui.pd.util
     :refer
     [pd-dimensions
      target-id
      target-type
      is-line?
      css-matrix-string
      pd-screen-offset]]))


;//
;//   _ __  ___ _  _ ______
;//  | '  \/ _ \ || (_-< -_)
;//  |_|_|_\___/\_,_/__\___|
;//
(defn- ->page [ev]
       (vec2 (.-pageX ev) (.-pageY ev)))

(defn- ->position [ev]
       (let [ev* (.-nativeEvent ev)
             pos (vec2 (.-x ev*) (.-y ev*))
             offset (pd-screen-offset)]
            (g/- pos offset)))

(defn- dispatch-mouse
       [msg ev room-id scene-id layout]
       (let [id (target-id (.-target ev))
             type (keyword (target-type (.-target ev)))
             buttons (.-buttons ev)
             data {:room-id  room-id
                   :scene-id scene-id
                   :node-id  id
                   :type     type
                   :position (->position ev)
                   :layout   layout
                   :buttons  buttons}]
            (dispatch [msg data])))

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

(defn pd []
      "A PDish editor for wiring up scenes"
      (fn [room-rctn scene]
          (let [layout (:layout scene)
                css-matrix (css-matrix-string layout)]
               [:div#pd
                ;[:button#reset.btn-floating
                ; {:on-click #(dispatch [:pd/reset-view])}
                ; [:i.mdi-device-gps-fixed]]
                ;[(editor-wrapper layout-id)

                ^{:key "svg"}
                [svg/svg
                 {
                  :id              "pd-svg"
                  :data-type       "pd"
                  :on-drop         #(drop* %)
                  :on-drag-over    #(allow-drop %)
                  :on-double-click #(dispatch-mouse :pd/double-click % (:id @room-rctn) (:id scene) layout)
                  :on-click        #(dispatch-mouse :pd/click % (:id @room-rctn) (:id scene) layout)
                  :on-mouse-down   #(dispatch-mouse :pd/mouse-down % (:id @room-rctn) (:id scene) layout)
                  :on-mouse-move   #(dispatch-mouse :pd/mouse-move % (:id @room-rctn) (:id scene) layout)
                  :on-mouse-up     #(dispatch-mouse :pd/mouse-up % (:id @room-rctn) (:id scene) layout)
                  :on-mouse-enter  #(dispatch-mouse :pd/mouse-enter % (:id @room-rctn) (:id scene) layout)
                  }

                 ^{:key "pan-group"}
                 [svg/group {:id    "pan-group"
                             :style {:transform css-matrix}
                             ;:class (if (= :none (:mode @editor-rctn)) "transition")
                             }
                  ^{:key "grid"} [grid]
                  ^{:key "links"} [links (:id @room-rctn) scene]
                  ^{:key "nodes"} [nodes (:id @room-rctn) scene]
                  ]]
                [palette (:id @room-rctn) (:id scene)]
                ])))