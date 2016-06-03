(ns guadalete-ui.pd.mouse
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [clojure.set :refer [difference]]
            [guadalete-ui.util :refer [pretty]]
            [re-frame.core :refer [dispatch]]
            [thi.ng.geom.core :as g]
            [thi.ng.geom.core.vector :as v]
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

(defn- make-link-start [scene-id node-id position layout db]
       (log/debug "make-link-start")
       (let [scene (get-in db [:scene scene-id])
             nodes (:nodes layout)
             node (->> nodes
                       (filter #(= (:id %) node-id))
                       (first))
             ;node-0 (assoc node
             ;              :selected true
             ;              :pos-0 (:position node))
             ;
             ;nodes-0 (->> nodes
             ;             (unselect-all)
             ;             (remove #(= (:id %) node-id)))
             ;nodes-1 (conj nodes-0 node-0)
             ;
             layout-0 (assoc layout
                             :mode :link
                             :pos-0 position)
             scene-0 (assoc scene :layout layout-0)

             ]
            (log/debug "scene-0" (pretty scene-0))

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

(defmethod mouse-down* :light [_type scene-id node-id position layout db]
           (move-node-start scene-id node-id position layout db))

(defmethod mouse-down* :color [_type scene-id node-id position layout db]
           (move-node-start scene-id node-id position layout db))

(defmethod mouse-down* :outlet [_type scene-id node-id position layout db]
           (make-link-start scene-id node-id position layout db))

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
(defn up [{:keys [type scene-id node-id position layout] :as data} db]
      (let [scene (get-in db [:scene scene-id])
            layout-0 (assoc layout :mode :none)
            nodes-0 (unselect-all (:nodes layout-0))
            layout-1 (assoc layout-0 :nodes nodes-0)
            layout-2 (dissoc layout-1 :pos-0 :pos-1)
            scene-0 (assoc scene :layout layout-2)]
           (dispatch [:scene/update scene-0])
           (assoc-in db [:scene scene-id] scene-0)))

;//
;//   _ __  ___ _  _ ______   _ __  _____ _____
;//  | '  \/ _ \ || (_-< -_) | '  \/ _ \ V / -_)
;//  |_|_|_\___/\_,_/__\___| |_|_|_\___/\_/\___|
;//
(defmulti mouse-move* (fn [params] (:mode params)))

(defmethod mouse-move* :none [{:keys [id editor]}]
           (if (not (or (= id "zoom-group") (= id "pd-svg") (= id "artboard")))
             (let [nodes (:node editor)
                   node (get nodes id)
                   node-0 (assoc node :selected true)
                   nodes-0 (assoc (unselect-all nodes) id node-0)]
                  (assoc editor :node nodes-0))
             (let [nodes (:node editor)
                   nodes-0 (unselect-all nodes)]
                  (assoc editor :node nodes-0))))

(defmethod mouse-move* :move [{:keys [editor pos]}]
           (let [s-nodes (selected-nodes editor)
                 all-nodes (:node editor)
                 pos-0 (:pos-0 editor)
                 δ (g/- pos pos-0)
                 s-nodes-0 (into {} (map #(move-node % δ) s-nodes))
                 all-nodes-0 (merge all-nodes s-nodes-0)]
                (assoc editor :node all-nodes-0)))

(defmethod mouse-move* :pan [{:keys [editor pos]}]
           (let [pos-0 (:pos-0 editor)                      ; the reference point for pan. set durin' :start-pan
                 pos-1 (:pos-1 editor)                      ; the translation at :start-pan
                 δ (g/- pos pos-0)]                         ; the delta between where we are now and where we started
                (assoc editor :translation (g/+ pos-1 δ))))

(defmethod mouse-move* :default [params]
           (log/error (str "mouse-move: I don't know the type: " (:mode params)))
           (:editor params))

(defn mouse-move [type id pos editor]
      (mouse-move* {:mode   (:mode editor)
                    :id     id
                    :pos    pos
                    :editor editor}))