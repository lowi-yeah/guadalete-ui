(ns guadalete-ui.pd.mouse
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [clojure.set :refer [difference]]
            [guadalete-ui.util :refer [pretty]]
            [re-frame.core :refer [dispatch]]
            [thi.ng.geom.core :as g]
            [thi.ng.geom.core.vector :refer [vec2]]
            [guadalete-ui.pd.links :refer [link-mouse]]
            [guadalete-ui.console :as log]))

;//   _        _
;//  | |_  ___| |_ __ ___ _ _ ___
;//  | ' \/ -_) | '_ \ -_) '_(_-<
;//  |_||_\___|_| .__\___|_| /__/
;//             |_|
(defn- in?
       "true if seq contains elm"
       [seq elm]
       (some #(= elm %) seq))

(defn- unselect-all
       "Sets :seleted to false for all given nodes"
       [nodes]
       ;(into {} (map (fn [[k v]] [k (-> v (assoc :selected false) (dissoc :pos-0))])) nodes))
       (into [] (map (fn [n] (-> n (assoc :selected false) (dissoc :pos-0)))) nodes))

(defn- selected-nodes
       "Gets all selected nodes form the editor"
       [editor]
       (into [] (filter (fn [[_id n]] (true? (:selected n)))) (:node editor)))

(defn- move-node [[id n] δ]
       "Move a node by the specified centre."
       (let [offset (g/+ (:pos-0 n) δ)]
            [id (assoc n :pos {:x (:x offset) :y (:y offset)})]))

(defn- move-node-start [scene-id node-id position layout db]
       (log/debug "move-node-start" (pretty position))
       (let [scene (get-in db [:scene scene-id])
             nodes (:nodes layout)
             node (->> nodes
                       (filter #(= (:id %) node-id))
                       (first))
             node-0 (assoc node
                           :selected true
                           :pos-0 (:position node))

             nodes-0 (->> nodes
                          (unselect-all)
                          (remove #(= (:id %) node-id)))
             nodes-1 (conj nodes-0 node-0)

             layout-0 (assoc layout
                             :mode :move
                             :pos-0 position
                             :nodes nodes-1)
             scene-0 (assoc scene :layout layout-0)]
            (assoc-in db [:scene scene-id] scene-0)))

(defn- make-link-start [scene-id node-id position layout db anchor]
       (log/debug "make-link-start" anchor)
       (let [scene (get-in db [:scene scene-id])
             link (if (= anchor :from)
                    {:from node-id :to :mouse}
                    {:from :mouse :to node-id})
             layout-0 (assoc layout
                             :mode :link
                             :link link
                             :mouse {:x (:x position) :y (:y position)})
             scene-0 (assoc scene :layout layout-0)]
            (assoc-in db [:scene scene-id] scene-0)))

;//                              _
;//   _ __  ___ _  _ ______   __| |_____ __ ___ _
;//  | '  \/ _ \ || (_-< -_) / _` / _ \ V  V / ' \
;//  |_|_|_\___/\_,_/__\___| \__,_\___/\_/\_/|_||_|
;//
(defmulti mouse-down*
          (fn [type scene-id node-id position layout db] type))

(defmethod mouse-down* :pd [_type scene-id node-id position layout db]
           (let [scene (get-in db [:scene scene-id])
                 nodes-0 (unselect-all (:nodes layout))
                 layout-0 (assoc layout
                                 :mode :pan
                                 :pos-0 position
                                 :pos-1 (:translation layout)
                                 :nodes nodes-0)
                 scene-0 (assoc scene :layout layout-0)]
                (assoc-in db [:scene scene-id] scene-0)))

(defmethod mouse-down* :node/light [_type scene-id node-id position layout db]
           (move-node-start scene-id node-id position layout db))

(defmethod mouse-down* :node/color [_type scene-id node-id position layout db]
           (move-node-start scene-id node-id position layout db))

(defmethod mouse-down* :outlet/color [_type scene-id node-id position layout db]
           (make-link-start scene-id node-id position layout db :from))

(defmethod mouse-down* :inlet/color [_type scene-id node-id position layout db]
           (make-link-start scene-id node-id position layout db :to))

(defmethod mouse-down* :default [type _ _ _ _ db]
           (log/error (str "mouse-down: I don't know the type: " type))
           db)

(defn down [{:keys [type scene-id node-id position layout] :as data} db]
      (mouse-down* type scene-id node-id position layout db))

;//
;//   _ __  ___ _  _ ______   _  _ _ __
;//  | '  \/ _ \ || (_-< -_) | || | '_ \
;//  |_|_|_\___/\_,_/__\___|  \_,_| .__/
;//                               |_|

(defn- default-up*
       "This is the standard behaviour upon mouse up.
       Canceles everything that might have been going on during move.
       Called by modes [:none :pd :move]"
       [type scene-id node-id position layout db]
       (let [scene (get-in db [:scene scene-id])
             layout-0 (assoc layout :mode :none)
             nodes-0 (unselect-all (:nodes layout-0))
             layout-1 (assoc layout-0 :nodes nodes-0)
             layout-2 (dissoc layout-1 :pos-0 :pos-1 :link)
             scene-0 (assoc scene :layout layout-2)]
            (dispatch [:scene/update scene-0])
            (assoc-in db [:scene scene-id] scene-0)))

(defn- inlet-up*
       [type scene-id node-id position layout db]
       (log/debug "inlet-up*:" type)
       (let [scene (get-in db [:scene scene-id])
             layout-0 (assoc layout :mode :none)
             nodes-0 (unselect-all (:nodes layout-0))
             layout-1 (assoc layout-0 :nodes nodes-0)
             layout-2 (dissoc layout-1 :pos-0 :pos-1 :link)
             scene-0 (assoc scene :layout layout-2)]
            (dispatch [:scene/update scene-0])
            (assoc-in db [:scene scene-id] scene-0)))

(defn- outlet-up*
       [type scene-id node-id position layout db]
       (log/debug "outlet-up*:" type)
       (default-up* type scene-id node-id position layout db))

(defmulti up*
          (fn [type scene-id node-id position layout db] type))

(defmethod up* :pd [_ scene-id node-id position layout db]
           (default-up* :pd scene-id node-id position layout db))

(defmethod up* :node/light [_ scene-id node-id position layout db]
           (default-up* :light scene-id node-id position layout db))

(defmethod up* :node/color [_ scene-id node-id position layout db]
           (default-up* :color scene-id node-id position layout db))

(defmethod up* :outlet/color [_ scene-id node-id position layout db]
           (outlet-up* :outlet scene-id node-id position layout db))

(defmethod up* :inlet/color [_ scene-id node-id position layout db]
           (inlet-up* :inlet/color scene-id node-id position layout db))

(defmethod up* :default [type _ _ _ _ db]
           (log/error (str "mouse UP: I don't know the type: " type))
           db)

(defn up [{:keys [type scene-id node-id position layout] :as data} db]
      (up* type scene-id node-id position layout db))

;//
;//   _ __  ___ _  _ ______   _ __  _____ _____
;//  | '  \/ _ \ || (_-< -_) | '  \/ _ \ V / -_)
;//  |_|_|_\___/\_,_/__\___| |_|_|_\___/\_/\___|
;//

(defmulti move*
          (fn [mode type scene-id node-id position layout db] mode))

(defmethod move* :move [mode type scene-id node-id position layout db]
           (let [scene (get-in db [:scene scene-id])
                 δ (g/- (vec2 position) (vec2 (:pos-0 layout)))
                 nodes (:nodes layout)
                 node (->> nodes
                           (filter #(:selected %))
                           (first))
                 node-position (vec2 (:pos-0 node))
                 node-position-0 (g/+ node-position δ)
                 node-0 (assoc node :position {:x (:x node-position-0) :y (:y node-position-0)})
                 nodes-0 (remove #(= (:id %) (:id node-0)) nodes)
                 nodes-1 (conj nodes-0 node-0)
                 layout-0 (assoc layout :nodes nodes-1)
                 scene-0 (assoc scene :layout layout-0)]
                (assoc-in db [:scene scene-id] scene-0)))

(defmethod move* :pan [mode type scene-id node-id position layout db]
           (let [scene (get-in db [:scene scene-id])
                 δ (g/- (vec2 position) (vec2 (:pos-0 layout)))
                 translation-0 (g/+ (vec2 (:pos-1 layout)) δ)
                 layout-0 (assoc layout :translation translation-0)
                 scene-0 (assoc scene :layout layout-0)]
                (assoc-in db [:scene scene-id] scene-0)))

(defmethod move* :link [mode type scene-id node-id position layout db]
           (link-mouse db scene-id layout position node-id type))

(defmethod move* :none [_ _ _ _ _ _ db] db)

(defn move [{:keys [type scene-id node-id position layout]} db]
      (move* (:mode layout) type scene-id node-id position layout db))