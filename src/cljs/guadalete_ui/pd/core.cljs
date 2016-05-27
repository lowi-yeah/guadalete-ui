(ns guadalete-ui.pd.core
  (:import [goog.dom query])
  (:require-macros
    [thi.ng.math.macros :as mm]
    [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :as re-frame]
            [clojure.string :as string]
            [reagent.core :as reagent]

            [thi.ng.geom.core :as g]
            [thi.ng.geom.core.vector :as v :refer [vec2 vec3]]
            [thi.ng.geom.core.matrix :as mx]
            [thi.ng.geom.types]
            [thi.ng.geom.circle :as c]
            [thi.ng.geom.rect :as rect]
            [thi.ng.math.core :as math :refer [PI HALF_PI TWO_PI]]
            [thi.ng.geom.svg.core :as svg]

            [guadalete-ui.console :as log]
            [guadalete-ui.util :refer [pretty]]))

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
                [:pre.code
                 (pretty layout)
                 ]])))