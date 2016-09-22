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
    [guadalete-ui.console :as log]
    [guadalete-ui.util :refer [pretty vec->map in?]]

    [guadalete-ui.pd.layout :refer [node-width line-height node-height]]

    [guadalete-ui.pd.color :refer [make-color render-color]]

    [guadalete-ui.pd.nodes.signal :as signal]
    [guadalete-ui.pd.nodes.color :as color]
    [guadalete-ui.pd.nodes.mixer :as mixer]
    [guadalete-ui.pd.nodes.light :as light]
    [guadalete-ui.pd.nodes.link :as link]
    [guadalete-ui.pd.nodes.constant :as constant]
    ))

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


(defn node*
      [scene-id node item selected?]
      (condp = (keyword (:ilk node))
             :signal [signal/render-node scene-id node item selected?]
             :light [light/render-node scene-id node item selected?]
             :color [color/render-node scene-id node item selected?]
             :mixer [mixer/render-node scene-id node item selected?]
             :constant [constant/render-node scene-id node item selected?]
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
                            (let [ilk (:ilk node)
                                  item-rctn (subscribe [:pd/node-item {:ilk ilk :id (:item-id node)}])
                                  selected? (in? @selected-node-ids-rctn (:id node))]
                                 ^{:key (str "n-" (:id node))}
                                 [node* (:id @scene-rctn) node @item-rctn selected?]))))]))

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
