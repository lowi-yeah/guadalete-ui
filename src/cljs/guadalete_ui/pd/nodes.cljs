(ns guadalete-ui.pd.nodes
  (:require-macros
    [reagent.ratom :refer [reaction]])
  (:require
    [clojure.string]
    [reagent.core :as r]
    [re-frame.core :refer [dispatch subscribe]]
    [thi.ng.geom.core :as g]
    [thi.ng.geom.svg.core :as svg]
    [thi.ng.geom.core.vector :refer [vec2]]
    [thi.ng.color.core :as color]
    [guadalete-ui.console :as log]
    [guadalete-ui.util :refer [pretty vec-map]]
    [guadalete-ui.pd.util :refer [target-id target-type]]
    [guadalete-ui.pd.color :refer [render-color]]
    [guadalete-ui.pd.links :refer [link-state]]))

;(events/listen (r/dom-node this) "dblclick"
;               #(double-click % room-id (:id scene)))

;(defn- double-click [ev room-id layout-id]
;       (.preventDefault ev)
;       (log/debug "DOUBLECLICK!")
;       (let [id (target-id (.-target ev))]
;            (dispatch [:pd/double-click-node [room-id layout-id id]])))

(defn- default-node
       []
       (fn [n]
           (let [position (:position n)]
                [svg/group
                 {:id        (:id n)
                  :class     (if (:selected n) "node selected" "node")
                  :transform (str "translate(" (:x position) " " (:y position) ")")
                  :data-type "default-node"}
                 [svg/rect (vec2) 96 32 {:rx 2 :class "bg"}]
                 [svg/text (vec2 8 21) (:name n) {}]])))

(defn- light-node
       []
       (fn [room-id scene layout node item]
           (let [id (:id node)
                 position (:position node)
                 type (keyword (:type node))]
                [(with-meta identity
                            {:component-did-mount
                             (fn [this]
                                 ; set the correct width to fit the text
                                 (let [text-selector (str "#" id " .node-text")
                                       rect-selector (str "#" id " rect")
                                       text-width (-> text-selector
                                                      (js/$)
                                                      (.css "width")
                                                      (clojure.string/replace #"px" "")
                                                      (int))]
                                      (-> rect-selector
                                          (js/$)
                                          (.attr "width" (+ 42 text-width)))))})

                 [svg/group
                  {:id        id
                   :class     (if (:selected node) "light node selected" "light node")
                   :transform (str "translate(" (:x position) " " (:y position) ")")
                   :data-type "node/light"}

                  [svg/rect (vec2 0 0) 32 32
                   {
                    ;:class (if item-id "bg" "bg  invalid")
                    :class "bg"
                    :rx    2}]

                  [svg/group
                   {:class "node-content"}
                   [:use.icon {:xlink-href "/images/bulb-on.svg#main"
                               :width      18
                               :height     18
                               :x          4
                               :y          7}]
                   [svg/text (vec2 32 21) (str (:name item))
                    {:class "node-text"}]]


                  (doall
                    (for [i (range (count (:inlets node)))]
                         (let [inlet (nth (:inlets node) i)
                               inlet-width 18
                               inlet-gap 4
                               inlet-offset (vec2 (* i (+ inlet-width inlet-gap)) 0)
                               inlet-position (g/+ inlet-offset (vec2 8 -8))]
                              ^{:key (str "in-" (:name inlet))}
                              [svg/rect inlet-position 0 0
                               {:class     "inlet"
                                :data-type (str "inlet/" (:name inlet))}])))

                  [svg/rect (vec2 0 0) 32 32
                   {:rx    2
                    :class "click-target"}]
                  ]])))

(defn- color-node
       []
       (fn [room-id scene layout node item]
           (let [node-size 36
                 outlet-size (vec2 18 8)
                 outlet-position (vec2 9 34)
                 id (:id node)
                 position (:position node)

                 ; hackedy hack:
                 ; to make the rendering a bit nicer, take the brightness value and set it as the alpha channel
                 ; that way not a black splot gets rendered when the birightness is low, but rather a transparent one
                 hacked-color (render-color item)

                 ]
                [svg/group
                 {:id        id
                  :class     (if (:selected node) "light node selected" "light node")
                  :transform (str "translate(" (:x position) " " (:y position) ")")
                  :data-type "node/color"}

                 [svg/rect (vec2 0 0) node-size node-size
                  {:class "bg"
                   :rx    2}]

                 [svg/circle [(/ node-size 2) (/ node-size 2)] 12
                  {:class "color"
                   :fill  @(color/as-css hacked-color)}]

                 [svg/rect outlet-position (:x outlet-size) (:y outlet-size)
                  {:class      "outlet"
                   :data-type  "outlet/color"
                   :data-state (link-state node)}]

                 [svg/rect (vec2 0 0) node-size node-size
                  {:rx    2
                   :class "click-target"}]
                 ])))

(defn node
      [room-id scene layout n item]
      (condp = (keyword (:type n))
             :light [light-node room-id scene layout n item]
             :color [color-node room-id scene layout n item]
             ;:output [output-node n]
             [default-node n]))

(defn nodes
      "Renders all nodes into the editor"
      []
      (fn [room-id scene]
          (let [layout (:layout scene)]
               [svg/group
                {:id "nodes"}
                (doall (for [n (:nodes layout)]
                            (let [type (keyword (:type n))
                                  item-rctn (subscribe [:pd/node-item {:type type :id (:item-id n)}])]
                                 ^{:key (str "n-" (:id n))}
                                 [node room-id scene layout n @item-rctn])))])))


;//              _
;//   _ __  __ _| |_____
;//  | '  \/ _` | / / -_)
;//  |_|_|_\__,_|_\_\___|
;//
(defn- position [pos layout]
       (let [pos* (vec2 pos)
             translation (vec2 (:translation layout))
             result (g/- pos* translation)]
            (vec-map result)))

(defmulti make-node
          (fn [type pos layout] type))

(defmethod make-node :light
           [type pos layout]
           {:id       (str (random-uuid))
            :type     "light"
            :position (position pos layout)
            :inlets   [{:unit "color"
                        :name "color"}]})

(defmethod make-node :color
           [type pos layout]
           {:id       (str (random-uuid))
            :type     "color"
            :position (position pos layout)
            :item-id  "rgb 0.8 0.9 0.9"
            :outlets  [{:unit "color"
                        :name "color"}]
            })

(defmethod make-node :sgnl
           [_type pos layout]
           {:id       (str (random-uuid))
            :type     "signal"
            :position (position pos layout)
            :outlets  [{:unit "01"}]})
