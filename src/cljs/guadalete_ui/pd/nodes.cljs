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
    [guadalete-ui.pd.color :refer [make-color render-color]]
    [guadalete-ui.items :refer [find-unused-light find-unused-signal]]
    [guadalete-ui.pd.link :as link :refer [links]]
    [guadalete-ui.views.widgets :refer [sparky]]))

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
(def line-height 14)

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
                  {:rx    2
                   :class "bg"}]

                 [svg/group
                  {:class "node-content"}
                  [:use.icon {:xlink-href "/images/bulb-on.svg#main"
                              :width      radius
                              :height     radius
                              :x          4
                              :y          4}]
                  [node-name (:name item)]]

                 [links scene-id node]])))

(defn- color-node
       []
       (fn [room-id scene-id node item]
           (let [outlet (first (:outlets node))
                 outlet-size (vec2 18 8)
                 id (:id node)
                 position (:position node)
                 height (* line-height (+ 3 (count (name (:type item)))))
                 ; hackedy hack:
                 ; to make the rendering a bit nicer, take the brightness value and set it as the alpha channel
                 ; that way not a black splot gets rendered when the birightness is low, but rather a transparent one
                 hacked-color (render-color item)]

                [svg/group
                 {:id            id
                  :class         (if (:selected node) "color node selected" "color node")
                  :transform     (str "translate(" (:x position) " " (:y position) ")")
                  :data-type     "node"
                  :data-scene-id scene-id
                  :data-ilk      (:ilk node)}

                 [svg/rect (vec2 0 0) node-width height
                  {:class "bg"
                   :rx    1}]

                 [svg/rect (vec2 0 0) node-width line-height
                  {:fill hacked-color
                   :rx   1}]

                 [svg/text
                  (vec2 4 (+ line-height 8))
                  (str (:type item))
                  {:class       "node-text"
                   :text-anchor "left"}]

                 [svg/rect (vec2 0 0) node-width height
                  {:rx    1
                   :class "click-target"}]

                 [links scene-id node]])))

(defn- signal-node []
       (fn [room-id scene-id node item]
           (let [outlet (first (:outlets node))
                 outlet-size (vec2 18 8)
                 id (:id node)
                 position (:position node)
                 height (* line-height 3)
                 ]

                ;(log/debug "signel item" (str item))

                [svg/group
                 {:id            id
                  :class         (if (:selected node) "signal node selected" "signal node")
                  :transform     (str "translate(" (:x position) " " (:y position) ")")
                  :data-type     "node"
                  :data-scene-id scene-id
                  :data-ilk      (:ilk node)}

                 [svg/rect (vec2 0 0) node-width height
                  {:class "bg"
                   :rx    1}]

                 [sparky item]

                 [svg/text
                  (vec2 4 12)
                  (str (:name item))
                  {:class       "node-text"
                   :text-anchor "left"}]

                 [svg/rect (vec2 0 0) node-width height
                  {:rx    1
                   :class "click-target"}]

                 ;[links scene-id node]
                 ])))


(defn node
      [room-id scene-id n item]
      (condp = (kw* (:ilk n))
             :signal [signal-node room-id scene-id n item]
             :light [light-node room-id scene-id n item]
             :color [color-node room-id scene-id n item]
             ;:output [output-node n]
             [default-node n]))

(defn nodes
      "Renders all nodes into the editor"
      []
      (fn [room-rctn scene-rctn]
          [svg/group
           {:id "nodes"}
           (doall (for [n (vals (:nodes @scene-rctn))]
                       (let [ilk (keyword (:ilk n))
                             item-rctn (subscribe [:pd/node-item {:ilk ilk :id (:item-id n)}])]
                            ^{:key (str "n-" (:id n))}
                            [node (:id @room-rctn) (:id @scene-rctn) n @item-rctn])))]))


;//              _
;//   _ __  __ _| |_____
;//  | '  \/ _` | / / -_)
;//  |_|_|_\__,_|_\_\___|
;//



(defn- color-in-link [type name node-id]
       {:id        (str type "-" node-id)
        :type      type
        :name      name
        :ilk       "signal"
        :state     "normal"
        :direction "in"})

(defn- make-color-links
       "Helper function for creating the in/out links for a given color.
       The number input-links corresponds to the number of color channels (h,s,v)"
       [node-id color]
       (let [out-link [{:id        (str "out-" node-id)
                        :ilk       "color"
                        :state     "normal"
                        :name      "out"
                        :direction "out"}]
             in-links (condp = (:type color)
                             :v [(color-in-link "v" "brightness" node-id)]
                             :sv [(color-in-link "v" "brightness" node-id)
                                  (color-in-link "s" "saturation" node-id)]
                             :hsv [(color-in-link "v" "brightness" node-id)
                                   (color-in-link "s" "saturation" node-id)
                                   (color-in-link "h" "hue" node-id)]
                             :default (log/error (str "Unknown color type " (:type color) ". Must be either :v :sv or :hsv")))]
            (->> (map (fn [l] [(:id l) l]) (concat out-link in-links))
                 (into {}))))

(defmulti make-node
          (fn [ilk data db] ilk))

(defmethod make-node :light
           [_ {:keys [position] :as data} db]
           (let [node-id (str (random-uuid))
                 link-id (str (random-uuid))
                 item-id (find-unused-light data db)]
                {:id       node-id
                 :ilk      "light"
                 :item-id  item-id
                 :position (vec-map position)
                 :links    {(keyword link-id)
                            {:id        link-id
                             :ilk       "color"
                             :state     "normal"
                             :direction "in"}}}))

(defmethod make-node :color
           [_ {:keys [position] :as data} db]
           (let [node-id (str (random-uuid))
                 color* (make-color)
                 links (make-color-links node-id color*)]
                (dispatch [:color/make color*])
                {:id       node-id
                 :ilk      "color"
                 :position (vec-map position)
                 :item-id  (:id color*)
                 :links    links}))

(defmethod make-node :signal
           [_ {:keys [position] :as data} db]
           (let [node-id (str (random-uuid))
                 link-id (str (random-uuid))
                 item-id (find-unused-signal data db)]
                {:id       node-id
                 :ilk      "signal"
                 :position (vec-map position)
                 :item-id  item-id
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

