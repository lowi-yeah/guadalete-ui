(ns guadalete-ui.pd.mouse
  (:require-macros [reagent.ratom :refer [reaction]]
                   [schema.macros])
  (:require [clojure.set :refer [difference]]
            [schema.core :as s]
            [re-frame.core :refer [dispatch]]
            [thi.ng.geom.core :as g]
            [thi.ng.geom.core.vector :refer [vec2]]
            [guadalete-ui.schema.core :refer [DB MouseEventData]]
            [guadalete-ui.util :refer [pretty vec-map]]
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
            [id (assoc n :pos (vec-map offset))]))

(defn- move-node-start [scene-id node-id position layout db]
       (let [scene (get-in db [:scene scene-id])
             nodes (:nodes layout)
             node (->> nodes
                       (filter #(= (:id %) node-id))
                       (first))
             node* (assoc node
                          :selected true
                          :pos-0 (vec-map (:position node)))

             nodes* (->> nodes
                         (unselect-all)
                         (remove #(= (:id %) node-id)))
             nodes* (conj nodes* node*)

             layout* (assoc layout
                            :mode :move
                            :pos-0 (vec-map position)
                            :nodes nodes*)
             scene* (assoc scene :layout layout*)]

            (assoc-in db [:scene scene-id] scene*)))

(defn- make-link-start [scene-id node-id position layout db anchor]
       (let [scene (get-in db [:scene scene-id])
             link (if (= anchor :from)
                    {:from node-id :to :mouse}
                    {:from :mouse :to node-id})
             position* (g/- position (vec2 (:translation layout)))
             layout-0 (assoc layout
                             :mode :link
                             :link link
                             :mouse (vec-map position*))
             scene-0 (assoc scene :layout layout-0)]
            (assoc-in db [:scene scene-id] scene-0)))

;//                              _
;//   _ __  ___ _  _ ______   __| |_____ __ ___ _
;//  | '  \/ _ \ || (_-< -_) / _` / _ \ V  V / ' \
;//  |_|_|_\___/\_,_/__\___| \__,_\___/\_/\_/|_||_|
;//
(defmulti mouse-down*
          (fn [type data] type))

(defmethod mouse-down* :pd [_ {:keys [scene-id node-id position layout db]}]
           (let [scene (get-in db [:scene scene-id])
                 nodes* (unselect-all (:nodes layout))
                 layout* (assoc layout
                                :mode :pan
                                :pos-0 (vec-map position)
                                :pos-1 (vec-map (:translation layout))
                                :nodes nodes*)
                 scene* (assoc scene :layout layout*)]
                (assoc-in db [:scene scene-id] scene*)))

(defmethod mouse-down* :node/light [_ {:keys [scene-id node-id position layout db]}]
           (move-node-start scene-id node-id position layout db))

(defmethod mouse-down* :node/color [_ {:keys [scene-id node-id position layout db]}]
           (move-node-start scene-id node-id position layout db))

(defmethod mouse-down* :outlet/color [_ {:keys [scene-id node-id position layout db]}]
           (make-link-start scene-id node-id position layout db :from))

(defmethod mouse-down* :inlet/color [_ {:keys [scene-id node-id position layout db]}]
           (make-link-start scene-id node-id position layout db :to))

(defmethod mouse-down* :default [type {:keys [db]}]
           (log/error (str "mouse-down: I don't know the type: " type))
           db)

(s/defn down :- s/Any
        [{:keys [type] :as data} :- MouseEventData
         db :- DB]
        (mouse-down* type (assoc data :db db)))

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


(defmulti up* (fn [type data] type))

(defmethod up* :pd [_ {:keys [scene-id node-id position layout db]}]
           (default-up* :pd scene-id node-id position layout db))

(defmethod up* :node/light [_ {:keys [scene-id node-id position layout db]}]
           (default-up* :light scene-id node-id position layout db))

(defmethod up* :node/color [_ {:keys [scene-id node-id position layout db]}]
           (default-up* :color scene-id node-id position layout db))

(defmethod up* :outlet/color [_ {:keys [scene-id node-id position layout db]}]
           (outlet-up* :outlet scene-id node-id position layout db))

(defmethod up* :inlet/color [_ {:keys [scene-id node-id position layout db]}]
           (inlet-up* :inlet/color scene-id node-id position layout db))

(defmethod up* :default [type {:keys [db]}]
           (log/error (str "mouse UP: I don't know the type: " type))
           db)

;(s/defn up :- DB
;        [{:keys [type] :as data} :- MouseEventData
;         db :- DB]
;        (up* type (assoc data :db db)))

(s/defn up :- s/Any
        [{:keys [type] :as data} :- s/Any
         db :- s/Any]
        (up* type (assoc data :db db)))


;//
;//   _ __  ___ _  _ ______   _ __  _____ _____
;//  | '  \/ _ \ || (_-< -_) | '  \/ _ \ V / -_)
;//  |_|_|_\___/\_,_/__\___| |_|_|_\___/\_/\___|
;//

(defmulti move*
          (fn [mode data] mode))

(defmethod move* :move [_ {:keys [scene-id node-id position layout db]}]
           (let [scene (get-in db [:scene scene-id])
                 δ (g/- (vec2 position) (vec2 (:pos-0 layout)))
                 nodes (:nodes layout)
                 node (->> nodes
                           (filter #(:selected %))
                           (first))
                 node-position (vec2 (:pos-0 node))
                 node-position* (g/+ node-position δ)
                 node* (assoc node :position (vec-map node-position*))
                 nodes* (remove #(= (:id %) (:id node*)) nodes)
                 nodes* (conj nodes* node*)
                 layout* (assoc layout :nodes nodes*)
                 scene* (assoc scene :layout layout*)]
                (assoc-in db [:scene scene-id] scene*)))

(defmethod move* :pan [_ {:keys [scene-id node-id position layout db]}]
           (let [scene (get-in db [:scene scene-id])
                 δ (g/- (vec2 position) (vec2 (:pos-0 layout)))
                 translation* (g/+ (vec2 (:pos-1 layout)) δ)
                 layout* (assoc layout :translation (vec-map translation*))
                 scene* (assoc scene :layout layout*)]
                (assoc-in db [:scene scene-id] scene*)))

(defmethod move* :link [_ {:keys [scene-id node-id position layout db type]}]
           (link-mouse db scene-id layout position node-id type))

(defmethod move* :none [_ {:keys [db]}] db)

(defmethod move* :default [_ {:keys [db]}] db)

(s/defn move :- DB
        [data :- MouseEventData
         ;db :- DB
         db :- s/Any
         ]
        (let [mode (get-in data [:layout :mode])]
             (move* mode (assoc data :db db))))

