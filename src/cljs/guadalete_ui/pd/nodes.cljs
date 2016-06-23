(ns guadalete-ui.pd.nodes
  (:require-macros
    [reagent.ratom :refer [reaction]])
  (:require
    [clojure.string :as string]
    [reagent.core :as r]
    [re-frame.core :refer [dispatch subscribe]]
    [thi.ng.geom.core :as g]
    [thi.ng.geom.svg.core :as svg]
    [thi.ng.geom.core.vector :refer [vec2]]
    [thi.ng.color.core :as color]
    [guadalete-ui.console :as log]
    [guadalete-ui.util :refer [pretty kw* vec-map]]
    [guadalete-ui.pd.color :refer [render-color]]
    [guadalete-ui.pd.link :as link :refer [links]]
    ))

;(events/listen (r/dom-node this) "dblclick"
;               #(double-click % room-id (:id scene)))

;(defn- double-click [ev room-id layout-id]
;       (.preventDefault ev)
;       (log/debug "DOUBLECLICK!")
;       (let [id (target-id (.-target ev))]
;            (dispatch [:pd/double-click-node [room-id layout-id id]])))

;//      _
;//   __| |_ _ __ ___ __ __
;//  / _` | '_/ _` \ V  V /
;//  \__,_|_| \__,_|\_/\_/
;//

(def node-size 36)
(def node-width 92)
(def node-height 28)

(defn- default-node
       []
       (fn [n]
           (let [position (:position n)]
                [svg/group
                 {:id        (:id n)
                  :class     (if (:selected n) "node selected" "node")
                  :transform (str "translate(" (:x position) " " (:y position) ")")
                  :data-type "node"
                  :data-ilk  "none"
                  }
                 [svg/rect (vec2) 96 32 {:rx 2 :class "bg"}]
                 [svg/text (vec2 8 21) (:name n) {}]])))

(defn- node-name []
       (fn [name]
           (let [words (string/split name #" ")
                 ;char-counts (map #(count %) words)
                 ]
                [svg/group
                 {:transform (str "translate(28 12)")}
                 [:text
                  {:x           0
                   :y           0
                   :class       "node-text"
                   :text-anchor "left"}
                  (doall
                    (for [index (range (count words))
                          :let [word (nth words index)
                                offset (str (* index 1.2) "em")]]
                         ^{:key (str (random-uuid))}
                         [:tspan {:x "0" :dy offset} word]
                         ))]])))

(defn- light-node
       []
       (fn [room-id scene-id node item]
           (let [id (:id node)
                 position (:position node)
                 type (keyword (:type node))
                 radius 18
                 ]
                [svg/group
                 {:id            id
                  :class         (if (:selected node) "light node selected" "light node")
                  :transform     (str "translate(" (:x position) " " (:y position) ")")
                  :data-type     "node"
                  :data-ilk      (:ilk node)
                  :data-scene-id scene-id}

                 [svg/rect (vec2) node-width node-height
                  {
                   ;:class (if item-id "bg" "bg  invalid")
                   :rx    2
                   :class "bg"}]

                 [svg/group
                  {:class "node-content"}
                  [:use.icon {:xlink-href "/images/bulb-on.svg#main"
                              :width      radius
                              :height     radius
                              :x          4
                              :y          4}]
                  [node-name (:name item)]]

                 [links scene-id node]

                 ;[svg/rect (vec2 0 0) 32 32
                 ; {:rx    2
                 ;  :class "click-target"}]
                 ])
           ))

(defn- color-node
       []
       (fn [room-id scene-id node item]
           (let [outlet (first (:outlets node))
                 outlet-size (vec2 18 8)
                 id (:id node)
                 position (:position node)

                 ; hackedy hack:
                 ; to make the rendering a bit nicer, take the brightness value and set it as the alpha channel
                 ; that way not a black splot gets rendered when the birightness is low, but rather a transparent one
                 hacked-color (render-color item)

                 ]
                [svg/group
                 {:id            id
                  :class         (if (:selected node) "light node selected" "light node")
                  :transform     (str "translate(" (:x position) " " (:y position) ")")
                  :data-type     "node"
                  :data-scene-id scene-id
                  :data-ilk      (:ilk node)}

                 [svg/rect (vec2 0 0) node-width node-height
                  {:class "bg"
                   :rx    2}]

                 [svg/rect (vec2 (- node-width node-height) 0) node-height node-height
                  {:fill @(color/as-css hacked-color)
                   :rx   2}]

                 [svg/text (vec2 4 (/ node-height 2)) (str (:type item))
                  {:class       "node-text"
                   :text-anchor "left"}]

                 [links scene-id node]

                 ;[svg/rect (vec2 0 0) node-width node-height
                 ; {:rx    2
                 ;  :class "click-target"}]
                 ])))

(defn node
      [room-id scene-id n item]
      (condp = (kw* (:ilk n))
             :light [light-node room-id scene-id n item]
             :color [color-node room-id scene-id n item]
             ;:output [output-node n]
             [default-node n]))

(defn nodes
      "Renders all nodes into the editor"
      []
      (fn [room-id scene]
          [svg/group
           {:id "nodes"}
           (doall (for [n (vals (:nodes scene))]
                       (let [ilk (keyword (:ilk n))
                             item-rctn (subscribe [:pd/node-item {:ilk ilk :id (:item-id n)}])]
                            ^{:key (str "n-" (:id n))}
                            [node room-id (:id scene) n @item-rctn])))]))


;//              _
;//   _ __  __ _| |_____
;//  | '  \/ _` | / / -_)
;//  |_|_|_\__,_|_\_\___|
;//
(defmulti make-node
          (fn [ilk pos] ilk))

(defmethod make-node :light
           [_ pos]
           (let [node-id (str (random-uuid))
                 link-id (str (random-uuid))]
                {:id       node-id
                 :ilk      "light"
                 :position (vec-map pos)
                 :links    {(keyword link-id)
                            {:id        link-id
                             :ilk       "color"
                             :state     "normal"
                             :direction "in"}}}))

(defmethod make-node :color
           [_ pos]
           (log/debug "make color node")
           (let [node-id (str (random-uuid))
                 link-id (str (random-uuid))]
                {:id       node-id
                 :ilk      "color"
                 :position (vec-map pos)
                 :item-id  "rgb 0.8 0.9 0.9"
                 :links    {(keyword link-id)
                            {:id        link-id
                             :ilk       "color"
                             :state     "normal"
                             :direction "out"}}}))

(defmethod make-node :signal
           [_ pos]
           (let [node-id (str (random-uuid))
                 link-id (str (random-uuid))]
                {:id       node-id
                 :ilk      "signal"
                 :position (vec-map pos)
                 :links    {(keyword link-id)
                            {:id        link-id
                             :ilk       "signal"
                             :state     "normal"
                             :direction "out"}}}))

;//   _        _
;//  | |_  ___| |_ __ ___ _ _ ___
;//  | ' \/ -_) | '_ \ -_) '_(_-<
;//  |_||_\___|_| .__\___|_| /__/
;//             |_|

(defn reset
       "Resets a node (selection, links, tmp-positions…)"
       [[id node]]
       (let [links* (link/reset-all (:links node))
             node* (-> node
                       (dissoc :pos-0)
                       (assoc :selected false :links links*))]
            [id node*]))

(defn reset-all*
       "internal helper for resets-all"
       [nodes]
       (into {} (map reset) nodes))

(defn reset-all
      "Resets all nodes"
      [scene-id db]
      (let [nodes (get-in db [:scene scene-id :nodes])
            nodes* (reset-all* nodes)]
           (assoc-in db [:scene scene-id :nodes] nodes*)))

(defn- selected-node
       "Returns the first node ith a selected attribute."
       [nodes]
       (let [[_ node] (first (filter (fn [[k v]] (:selected v)) nodes))]
            node))

;(defn- get-node [scene-id room-id node-id db])


;//          _        _
;//   ______| |___ __| |_
;//  (_-< -_) / -_) _|  _|
;//  /__\___|_\___\__|\__|
;//

;//
;//   _ __  _____ _____
;//  | '  \/ _ \ V / -_)
;//  |_|_|_\___/\_/\___|
;//
(defn select [{:keys [scene-id id position] :as data} db]
      (let [id-key (keyword id)
            scene (get-in db [:scene scene-id])
            nodes (get-in db [:scene scene-id :nodes])
            node (get nodes id-key)
            node* (assoc node
                         :selected true
                         :pos-0 (vec-map (:position node)))
            nodes* (-> nodes
                       (reset-all*)
                       (assoc id-key node*))
            scene* (assoc scene
                          :mode :move
                          :pos-0 (vec-map position)
                          :nodes nodes*)]
           (assoc-in db [:scene scene-id] scene*)))

(defn move [{:keys [scene-id id position] :as data} db]
      (let [id-key (keyword id)
            scene (get-in db [:scene scene-id])
            nodes (get-in db [:scene scene-id :nodes])
            node (selected-node nodes)
            δ (g/- (vec2 position) (vec2 (:pos-0 scene)))
            node-position* (g/+ (vec2 (:pos-0 node)) δ)
            node* (assoc node :position (vec-map node-position*))
            scene* (assoc-in scene [:nodes (keyword (:id node))] node*)]
           (assoc-in db [:scene scene-id] scene*)))

