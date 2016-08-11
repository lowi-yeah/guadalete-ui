(ns guadalete-ui.pd.nodes
  (:require-macros
    [reagent.ratom :refer [reaction]])
  (:require
    [clojure.string :as string]
    [reagent.core :refer [create-class dom-node]]
    [re-frame.core :refer [dispatch subscribe]]
    [thi.ng.geom.core :as g]
    [thi.ng.geom.svg.core :as svg]
    [thi.ng.geom.core.vector :refer [vec2]]
    [thi.ng.color.core :as color]
    [guadalete-ui.console :as log]
    [guadalete-ui.util :refer [pretty kw* vec-map in?]]
    [guadalete-ui.pd.color :refer [make-color render-color]]
    [guadalete-ui.items :refer [find-unused-light find-unused-signal]]
    [guadalete-ui.pd.link :as link :refer [links]]
    [guadalete-ui.views.widgets :refer [sparky]]
    [guadalete-ui.pd.layout :refer [node-width line-height node-height]]))

;//      _
;//   __| |_ _ __ ___ __ __
;//  / _` | '_/ _` \ V  V /
;//  \__,_|_| \__,_|\_/\_/
;//
(defn- node-title []
  (fn [title]
    [svg/group
     {:class "title"}
     [svg/rect (vec2 0 0) node-width line-height
      {:class "bg"
       :rx    1}]
     [svg/text
      (vec2 4 10)
      title
      {:class       "node-title"
       :text-anchor "left"}]
     ]))

(defn- default-node
  []
  (fn [n selected?]
    (let [position (:position n)]
      [svg/group
       {:id        (:id n)
        :class     (if selected? "node selected" "node")
        :transform (str "translate(" (:x position) " " (:y position) ")")
        :data-type "node"
        :data-ilk  "none"
        }
       [svg/rect (vec2) 96 32 {:rx 2 :class "bg"}]
       [svg/text (vec2 8 21) (:name n) {}]])))

(defn- split-name*
  "recursice helper for split name"
  [words lines]
  (if (= 0 (count words))
    (do
      (reverse lines))
    (let [max-length 14
          [line & lines*] lines
          [word & words*] words
          joined (string/trim (str line " " word))]
      (if (<= max-length (count joined))
        ;; too long. keep seperate
        (split-name* words* (conj lines word))
        ;; short enough. join
        (split-name* words* (conj lines* joined))))))

(defn- split-name [name]
  (split-name* (string/split name #" ") [""]))

(defn- light-name []
  (fn [name]
    (let [lines (split-name name)]
      [svg/group
       {:transform (str "translate(4 " line-height ")")}
       [:text
        {:x           0
         :y           0
         :class       "node-text"
         :text-anchor "left"}
        (doall
          (for [index (range (count lines))
                :let [line (nth lines index)
                      offset "1rem"]]
            ^{:key (str (random-uuid))}
            [:tspan {:x "0" :dy offset} line]))]])))

(defn- light-node
  [scene-id node item selected?]
  (let [id (:id node)]
    (create-class
      {:component-did-mount
       (fn [_this]
         (let [offset-top (* 2 line-height)
               text-height (-> (str "#" id " .node-text") (js/$) (.height))
               height (+ offset-top text-height)]
           (-> (str "#" id " > .bg")
               (js/$)
               (.height height))
           (-> (str "#" id " .click-target")
               (js/$)
               (.height height))))

       :reagent-render
       (fn [scene-id node item selected?]
         (let [position (:position node)
               link-offset 2]
           [svg/group
            {:id            id
             :class         (if selected? "light node selected" "light node")
             :transform     (str "translate(" (:x position) " " (:y position) ")")
             :data-type     "node"
             :data-ilk      (:ilk node)
             :data-scene-id scene-id}

            [svg/rect (vec2) node-width node-height
             {:rx    2
              :class "bg"}]

            [node-title "Light"]

            [svg/group
             {:class "node-content"}
             [light-name (:name item)]]

            [svg/rect (vec2 0 0) node-width node-height
             {:rx    1
              :class "click-target"}]

            [links scene-id node link-offset]]))})))

(defn- color-node
  []
  (fn [scene-id node item selected?]
    (let [
          id (:id node)
          position (:position node)
          height (* line-height (+ 3 (count (name (:type item)))))
          ;hackedy hack:
          ;to make the rendering a bit nicer, take the brightness value and set it as the alpha channel
          ;that way not a black splot gets rendered when the birightness is low, but rather a transparent one
          hacked-color (render-color item)
          link-offset 2.5
          ]
      [svg/group
       {:id            id
        :class         (if selected? "color node selected" "color node")
        :transform     (str "translate(" (:x position) " " (:y position) ")")
        :data-type     "node"
        :data-scene-id scene-id
        :data-ilk      (:ilk node)}

       [svg/rect (vec2 0 0) node-width height
        {:class "bg"
         :rx    1}]

       [node-title (str "Color: " (:type item))]

       [svg/rect (vec2 0 line-height) node-width (/ line-height 2)
        {:fill  hacked-color
         :class "indicator"
         :rx    1}]

       [svg/rect (vec2 0 0) node-width height
        {:rx    1
         :class "click-target"}]

       [links scene-id node link-offset]])))

(defn- signal-node []
  (fn [scene-id node item selected?]
    (let [outlet (first (:outlets node))
          outlet-size (vec2 18 8)
          id (:id node)
          position (:position node)
          height (* line-height 4)
          link-offset 3.5
          ]

      [svg/group
       {:id            id
        :class         (if selected? "signal node selected" "signal node")
        :transform     (str "translate(" (:x position) " " (:y position) ")")
        :data-type     "node"
        :data-scene-id scene-id
        :data-ilk      (:ilk node)}

       [svg/rect (vec2 0 0) node-width height
        {:class "bg"
         :rx    1}]

       [node-title "Signal"]

       [sparky item
        {:position  (vec2 0 (* line-height 2))
         :dimension (vec2 node-width line-height)}]

       [svg/text
        (vec2 4 (+ 12 line-height))
        (str (:name item))
        {:class       "node-text"
         :text-anchor "left"}]

       [svg/rect (vec2 0 0) node-width height
        {:rx    1
         :class "click-target"}]

       [links scene-id node link-offset]])))


(defn node*
  [scene-id node item selected?]
  (condp = (kw* (:ilk node))
    :signal [signal-node scene-id node item selected?]
    :light [light-node scene-id node item selected?]
    :color [color-node scene-id node item selected?]
    ;:output [output-node n]
    [default-node node selected?]))

(defn nodes
  "Renders all nodes into the editor"
  []
  (fn [scene-rctn]
    [svg/group
     {:id "nodes"}
     (let [selected-node-ids-rctn (subscribe [:pd/selected-nodes])]
       (doall (for [node (vals (:nodes @scene-rctn))]
                (let [ilk (kw* (:ilk node))
                      item-rctn (subscribe [:pd/node-item {:ilk ilk :id (:item-id node)}])
                      selected? (in? @selected-node-ids-rctn (:id node))]
                  ^{:key (str "n-" (:id node))}
                  [node* (:id @scene-rctn) node @item-rctn selected?]))))]))

;//              _
;//   _ __  __ _| |_____
;//  | '  \/ _` | / / -_)
;//  |_|_|_\__,_|_\_\___|
;//
(defn- color-channel-link [channel name node-id]
  {:id        (str channel "-" node-id)
   :type      channel
   :name      name
   :ilk       "value"
   :direction "in"})

(defn- make-color-links
  "Helper function for creating the in/out links for a given color.
  The number input-links corresponds to the number of color channels (h,s,v)"
  [node-id color]
  (let [out-link [{:id        (str "out-" node-id)
                   :ilk       "color"
                   :name      "out"
                   :direction "out"}]
        in-links (condp = (:type color)
                   :v [(color-channel-link "v" "brightness" node-id)]
                   :sv [(color-channel-link "v" "brightness" node-id)
                        (color-channel-link "s" "saturation" node-id)]
                   :hsv [(color-channel-link "v" "brightness" node-id)
                         (color-channel-link "s" "saturation" node-id)
                         (color-channel-link "h" "hue" node-id)]
                   :default (log/error (str "Unknown color type " (:type color) ". Must be either :v :sv or :hsv")))]
    (->> (map (fn [l] [(:id l) l]) (concat out-link in-links))
         (into {}))))

(defmulti make
          (fn [ilk _ _] ilk))

(defmethod make :light
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
                 :direction "in"}}}))

(defmethod make :color
  [_ {:keys [position color]} db]
  (let [node-id (str (random-uuid))
        links (make-color-links node-id color)]
    {:id       node-id
     :ilk      "color"
     :position (vec-map position)
     :item-id  (:id color)
     :links    links}))

(defmethod make :signal
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
                 :ilk       "value"
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


(defn move [{:keys [scene-id id position] :as data} db]
  (let [scene (get-in db [:scene scene-id])
        nodes (get-in db [:scene scene-id :nodes])
        node (selected-node nodes)
        δ (g/- (vec2 position) (vec2 (:pos-0 scene)))
        node-position* (g/+ (vec2 (:pos-0 node)) δ)
        node* (assoc node :position (vec-map node-position*))
        scene* (assoc-in scene [:nodes (keyword (:id node))] node*)]
    (assoc-in db [:scene scene-id] scene*)))

