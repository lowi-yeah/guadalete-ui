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
       (into {} (map (fn [[k v]] [k (-> v (assoc :selected false) (dissoc :pos-0))])) nodes))

(defn- selected-nodes
       "Gets all selected nodes form the editor"
       [editor]
       (into [] (filter (fn [[_id n]] (true? (:selected n)))) (:node editor)))

(defn- move-node [[id n] δ]
       "Move a node by the specified centre."
       (log/debug "move node " (pretty id) (pretty n))
       (let [offset (g/+ (:pos-0 n) δ)]
            [id (assoc n :pos offset)]))

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
           (let [
                 ;nodes (:node editor)
                 ;node (get nodes id)
                 ;node-0 (assoc node
                 ;         :selected true
                 ;         :pos-0 (:pos node))
                 ;nodes-0 (assoc (unselect-all nodes) id node-0)

                 ]
                ;(assoc editor :mode :move
                ;              :pos-0 (:position data)
                ;              :node nodes-0)

                db))

(defmethod mouse-down* :default [type _ _ _ _ db]
           (log/error (str "I don't know the type: " (pretty type)))
           db)

(defn down [{:keys [type scene-id node-id position layout] :as data} db]
      (mouse-down* type scene-id node-id position layout db))

;//
;//   _ __  ___ _  _ ______   _  _ _ __
;//  | '  \/ _ \ || (_-< -_) | || | '_ \
;//  |_|_|_\___/\_,_/__\___|  \_,_| .__/
;//                               |_|
(defn mouse-up [type id pos editor]
      (if (= type :node) (dispatch [:update-node-position id pos]))
      (-> editor
          (assoc :mode :none)
          (dissoc :pos-0 :pos-1)))

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
           (log/error (str "I don't know the type: " (:mode params)))
           (:editor params))

(defn mouse-move [type id pos editor]
      (mouse-move* {:mode   (:mode editor)
                    :id     id
                    :pos    pos
                    :editor editor}))