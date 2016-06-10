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
            [guadalete-ui.console :as log]
            [guadalete-ui.pd.util :refer [pd-screen-offset]]))

;//   _        _
;//  | |_  ___| |_ __ ___ _ _ ___
;//  | ' \/ -_) | '_ \ -_) '_(_-<
;//  |_||_\___|_| .__\___|_| /__/
;//             |_|
(defn- in?
       "true if seq contains elm"
       [seq elm]
       (some #(= elm %) seq))


(defn- move-node [[id n] δ]
       "Move a node by the specified centre."
       (let [offset (g/+ (:pos-0 n) δ)]
            [id (assoc n :pos (vec-map offset))]))



;//                              _
;//   _ __  ___ _  _ ______   __| |_____ __ ___ _
;//  | '  \/ _ \ || (_-< -_) / _` / _ \ V  V / ' \
;//  |_|_|_\___/\_,_/__\___| \__,_\___/\_/\_/|_||_|
;//
(defmulti down* (fn [type data] type))

(defmethod down* :pd [_ data] (dispatch [:pd/mouse-down data]))
(defmethod down* :node [_ data] (dispatch [:node/mouse-down data]))
(defmethod down* :link [_ data] (dispatch [:flow/mouse-down data]))
(defmethod down* :default [type _] (log/error (str "mouse-down: I don't know the type: " type)))

(s/defn down :- DB
        [{:keys [type] :as data} :- MouseEventData
         db :- DB]
        (down* type data)
        db)

;//
;//   _ __  ___ _  _ ______   _  _ _ __
;//  | '  \/ _ \ || (_-< -_) | || | '_ \
;//  |_|_|_\___/\_,_/__\___|  \_,_| .__/
;//                               |_|
(defn default-up
      "This is the standard behaviour upon mouse up.
      Canceles everything that might have been going on during move.
      Called by modes [:none :pd :move]"
      [{:keys [type scene-id node-id position]} db]
      (let [scene (get-in db [:scene scene-id])
            scene* (dissoc scene :pos-0 :pos-1 :flow/mouse :mode)]
           (dispatch [:scene/update scene*])
           (dispatch [:node/reset-all scene-id])
           db))


(defmulti up* (fn [type data] type))

(defmethod up* :pd [_ data] (dispatch [:mouse/default-up data]))
(defmethod up* :node [_ data] (dispatch [:mouse/default-up data]))
(defmethod up* :link [_ data] (dispatch [:flow/mouse-up data]))
;(defmethod up* :flow [_ data] (dispatch [:flow/mouse-up data]))
(defmethod up* :default [type _] (log/error (str "mouse up: I don't know the type: " type)))

(s/defn up :- DB
        [{:keys [type] :as data} :- MouseEventData
         db :- DB]
        (up* type data)
        db)


;//
;//   _ __  ___ _  _ ______   _ __  _____ _____
;//  | '  \/ _ \ || (_-< -_) | '  \/ _ \ V / -_)
;//  |_|_|_\___/\_,_/__\___| |_|_|_\___/\_/\___|
;//

(defn- selected-node
       "Returns the first node ith a selected attribute."
       [nodes]
       (let [[_ node] (first (filter (fn [[k v]] (:selected v)) nodes))]
            node))

(defmulti move* (fn [type data] type))

(defmethod move* nil [_ data] (comment "no nothing"))
(defmethod move* :none [_ data] (comment "no nothing"))
(defmethod move* :pan [_ data] (dispatch [:pd/mouse-move data]))
(defmethod move* :move [_ data] (dispatch [:node/mouse-move data]))
(defmethod move* :link [_ data] (dispatch [:flow/mouse-move data]))
(defmethod move* :default [type _] (log/error (str "mouse move: I don't know the type: " type)))

(s/defn move :- DB
        [data :- MouseEventData
         db :- DB]
        (let [mode (get-in db [:scene (:scene-id data) :mode])]
             (move* mode data)
             db))


;//                   _
;//   _____ _____ _ _| |_
;//  / -_) V / -_) ' \  _|
;//  \___|\_/\___|_||_\__|
;//
(defn- target-type [target]
       "Return the targets data-type, or – in case it has none - recursively walk up the dom to find the first ancestor with a data-type."
       (let [type (.attr (js/$ target) "data-type")]
            (if (nil? type)
              (target-type (.parent (js/$ target)))
              (keyword type))))

(defn- target-id [target]
       "Return the targets id, or – in case it has none - recursively walk up the dom to find the first ancestor with an id."
       (let [id (.attr (js/$ target) "id")]
            (if (nil? id)
              (target-id (.parent (js/$ target)))
              id)))


(defn- node-id*
       "Recursive helper for node-id"
       [jq flow]
       (let [type (keyword (.attr jq "data-type"))
             id (.attr jq "id")]
            (if (not (= type :node))
              (node-id* (.parent jq) flow)
              (merge flow {:node-id id}))))

(defn- load-link
       "Return the id of the node to which the given link belongs"
       [id]
       (let [jq (js/$ (str "#" id))
             scene-id* (.attr jq "data-scene-id")
             node-id* (keyword (.attr jq "data-node-id"))]
            {:scene-id scene-id*
             :node-id  node-id*
             :id       id
             :type     :link}))

(defn- ->page
       "Helper function for getting the event position relative to the page."
       [ev]
       (vec2 (.-pageX ev) (.-pageY ev)))

(defn event-target
      "Returns a map containing information to access the specific item type.
      E.g. if a 'link' is the target the node id and link type will be attached"
      [ev]
      (let [target* (.-target ev)
            type (target-type target*)
            id (target-id target*)]
           (condp = type
                  :pd {:type type}
                  :node {:type type :id id}
                  :link (load-link id)
                  {:type type}
                  )))

(defn event-buttons
      "Get the button state of a mouse event"
      [ev]
      {:buttons (.-buttons ev)})

(defn event-position
      "Calculates the position of the mouse event with respect to
      the offset of the svg frame in relation to the screen."
      [ev]
      (let [ev* (.-nativeEvent ev)
            pos (vec2 (int (.-x ev*)) (int (.-y ev*)))
            offset (pd-screen-offset)]
           {:position (g/- pos offset)}))


(defn- event-data
       [ev data]
       (let [target (event-target ev)
             buttons (event-buttons ev)
             position (event-position ev)]
            (merge data target position buttons)))
