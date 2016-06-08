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

    [guadalete-ui.console :as log]
    [guadalete-ui.util :refer [pretty]]
    [guadalete-ui.pd.mouse :as mouse]
    [guadalete-ui.pd.flow :refer [flows]]
    [guadalete-ui.pd.util
     :refer
     [pd-dimensions
      is-line?
      css-matrix-string]]))


;//
;//   _ __  ___ _  _ ______
;//  | '  \/ _ \ || (_-< -_)
;//  |_|_|_\___/\_,_/__\___|
;//
(defn- dispatch-mouse
       [msg ev mouse-event-data]
       (let [target (mouse/event-target ev)
             buttons (mouse/event-buttons ev)
             position (mouse/event-position ev)
             data (merge mouse-event-data target position buttons)]
            ;(log/debug "dispatch-mouse id" (:id data))
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
          (let [css-matrix (css-matrix-string (:translation scene))
                mouse-event-data {:room-id  (:id @room-rctn)
                                  :scene-id (:id scene)}]
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
                  :on-double-click #(dispatch-mouse :pd/double-click % mouse-event-data)
                  :on-click        #(dispatch-mouse :pd/click % mouse-event-data)
                  :on-mouse-down   #(dispatch-mouse :pd/mouse-down % mouse-event-data)
                  :on-mouse-move   #(dispatch-mouse :pd/mouse-move % mouse-event-data)
                  :on-mouse-up     #(dispatch-mouse :pd/mouse-up % mouse-event-data)
                  ;:on-mouse-enter  #(dispatch-mouse :pd/mouse-enter % mouse-event-data)
                  }

                 ^{:key "pan-group"}
                 [svg/group {:id    "pan-group"
                             :style {:transform css-matrix}}
                  ^{:key "grid"} [grid]
                  ^{:key "flows"} [flows (:id @room-rctn) scene]
                  ^{:key "nodes"} [nodes (:id @room-rctn) scene]
                  ]]
                [palette (:id @room-rctn) (:id scene)]
                ])))