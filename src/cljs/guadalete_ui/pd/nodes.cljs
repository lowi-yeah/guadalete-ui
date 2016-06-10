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

;(defn- light-node
;       []
;       (fn [room-id scene-id node item]
;           (let [id (:id node)
;                 position (:position node)
;                 type (keyword (:type node))]
;                [(with-meta identity
;                            {:component-did-mount
;                             (fn [this]
;                                 ; set the correct width to fit the text
;                                 (let [text-selector (str "#" id " .node-text")
;                                       rect-selector (str "#" id " rect")
;                                       text-width (-> text-selector
;                                                      (js/$)
;                                                      (.css "width")
;                                                      (clojure.string/replace #"px" "")
;                                                      (int))]
;                                      (-> rect-selector
;                                          (js/$)
;                                          (.attr "width" (+ 42 text-width)))))})
;
;                 [svg/group
;                  {:id             id
;                   :class          (if (:selected node) "light node selected" "light node")
;                   :transform      (str "translate(" (:x position) " " (:y position) ")")
;                   :data-type      "node"
;                   :data-node-type "light"}
;
;                  [svg/rect (vec2 0 0) 32 32
;                   {
;                    ;:class (if item-id "bg" "bg  invalid")
;                    :class "bg"
;                    :rx    2}]
;
;                  [svg/group
;                   {:class "node-content"}
;                   [:use.icon {:xlink-href "/images/bulb-on.svg#main"
;                               :width      18
;                               :height     18
;                               :x          4
;                               :y          7}]
;                   [svg/text (vec2 32 21) (str (:name item))
;                    {:class "node-text"}]]
;
;                  [links scene-id node]
;
;                  [svg/rect (vec2 0 0) 32 32
;                   {:rx    2
;                    :class "click-target"}]
;                  ]])
;
;           ))

(defn- light-node
       []
       (fn [room-id scene-id node item]
           (let [id (:id node)
                 position (:position node)
                 type (keyword (:type node))]
                [svg/group
                 {:id        id
                  :class     (if (:selected node) "light node selected" "light node")
                  :transform (str "translate(" (:x position) " " (:y position) ")")
                  :data-type "node"
                  :data-ilk  (:ilk node)
                  :data-scene-id scene-id
                  }

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

                 [links scene-id node]

                 [svg/rect (vec2 0 0) 32 32
                  {:rx    2
                   :class "click-target"}]
                 ])
           ))

(defn- color-node
       []
       (fn [room-id scene-id node item]
           (let [node-size 36
                 outlet (first (:outlets node))
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
                  :data-type "node"
                  :data-scene-id scene-id
                  :data-ilk  (:ilk node)}

                 [svg/rect (vec2 0 0) node-size node-size
                  {:class "bg"
                   :rx    2}]

                 [svg/circle [(/ node-size 2) (/ node-size 2)] 12
                  {:class "color"
                   :fill  @(color/as-css hacked-color)}]

                 [links scene-id node]

                 [svg/rect (vec2 0 0) node-size node-size
                  {:rx    2
                   :class "click-target"}]
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

(defn- reset
       "Resets a node (selection, links, tmp-positions…)"
       [[id node]]
       (let [links* (link/reset-all (:links node))
             node* (-> node
                       (dissoc :pos-0)
                       (assoc :selected false :links links*))]
            [id node*]))

(defn- reset-all*
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

;//
;//   _ __  _____ _____
;//  | '  \/ _ \ V / -_)
;//  |_|_|_\___/\_/\___|
;//
(defn begin-move [{:keys [scene-id id position] :as data} db]
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
