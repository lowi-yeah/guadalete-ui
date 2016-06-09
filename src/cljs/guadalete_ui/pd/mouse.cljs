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
            [guadalete-ui.pd.flow :as flow]
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


(defmulti check-outlet* (fn [type outlet] type))

(defmethod check-outlet* :signal [_ outlet] outlet)

(defmethod check-outlet* :color [_ outlet]
           (assoc outlet :state "active"))

(defn- check-outlet
       "Checks whether is is ok to use the given outlet bsed on its type."
       [outlet]
       (if (= :mouse outlet)
         :mouse
         (let [type (keyword (:type outlet))]
              (check-outlet* type outlet))))


(defmulti check-inlet* (fn [type inlet] type))


(defmethod check-inlet* :signal [_ inlet] inlet)

(defmethod check-inlet* :color [_ inlet]
           (assoc inlet :state "active"))

(defn- check-inlet
       "Checks whether is is ok to use the given outlet based on its type."
       [inlet]
       (if (= :mouse inlet)
         :mouse
         (let [type (keyword (:type inlet))]
              (log/debug "check-inlet" inlet type)
              (check-inlet* type inlet))))

(defn- get-outlet [from scene]
       (if (= :mouse from)
         :mouse
         (let [node (->> (:nodes scene)
                         (filter #(= (:id %) (:node-id from)))
                         (first))
               outlet (->> (:outlets node)
                           (filter #(= (:id %) (:id from)))
                           (first))]
              outlet)))

(defn- set-outlet [outlet scene node-id]
       (if (= :mouse outlet)
         scene
         (let [
               nodes (:nodes scene)
               node (->> nodes
                         (filter #(= (:id %) node-id))
                         (first))
               outlets (:outlets node)
               outlets* (remove #(= (:id %) (:id outlet)) outlets)
               outlets* (conj outlets* outlet)
               node* (assoc node :outlets outlets*)
               nodes* (remove #(= (:id %) node-id) nodes)
               nodes* (conj nodes* node*)]
              (assoc scene :nodes nodes*))))

(defn- set-inlet [inlet scene node-id]
       (if (= :mouse inlet)
         scene
         (let [
               nodes (:nodes scene)
               node (->> nodes
                         (filter #(= (:id %) node-id))
                         (first))
               inlets (:inlets node)
               inlets* (remove #(= (:id %) (:id inlet)) inlets)
               inlets* (conj inlets* inlet)
               node* (assoc node :inlets inlets*)
               nodes* (remove #(= (:id %) node-id) nodes)
               nodes* (conj nodes* node*)]
              (assoc scene :nodes nodes*))))

(defn- get-inlet [to scene]
       (if (= :mouse to)
         :mouse
         (let [node (->> (:nodes scene)
                         (filter #(= (:id %) (:node-id to)))
                         (first))
               inlet (->> (:inlets node)
                          (filter #(= (:id %) (:id to)))
                          (first))]
              inlet)))


(defn- reset-outlets* [node]
       (assoc node :outlets (->> (:outlets node)
                                 (map #(assoc % :state "normal"))
                                 (into []))))
(defn- reset-outlets [nodes]
       (->> nodes
            (map reset-outlets*)
            (into [])))


(defn- reset-inlets* [node]
       (assoc node :inlets (->> (:inlets node)
                                (map #(assoc % :state "normal"))
                                (into []))))
(defn- reset-inlets [nodes]
       (->> nodes
            (map reset-inlets*)
            (into [])))


;//                              _
;//   _ __  ___ _  _ ______   __| |_____ __ ___ _
;//  | '  \/ _ \ || (_-< -_) / _` / _ \ V  V / ' \
;//  |_|_|_\___/\_,_/__\___| \__,_\___/\_/\_/|_||_|
;//
(defmulti mouse-down*
          (fn [type data] type))

(defmethod mouse-down* :pd [_ {:keys [scene-id node-id position db] :as data}]
           (let [scene (get-in db [:scene scene-id])
                 scene* (assoc scene
                               :mode :pan
                               :pos-0 (vec-map position)
                               :pos-1 (vec-map (:translation scene)))]
                (assoc-in db [:scene scene-id] scene*)))

(defmethod mouse-down* :node [_ data]
           (node/start-move data))

(defmethod mouse-down* :link [_ data]
           (flow/begin data))

(defmethod mouse-down* :default [type {:keys [db]}]
           (log/error (str "mouse-down: I don't know the type: " type))
           db)

(s/defn down :- DB
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
       [type scene-id node-id position db]
       (let [scene (get-in db [:scene scene-id])
             nodes* (node/reset-all (:nodes scene))
             scene* (assoc scene :mode :none :nodes nodes*)
             scene* (dissoc scene* :pos-0 :pos-1 :flow/mouse)]
            (dispatch [:scene/update scene*])
            (assoc-in db [:scene scene-id] scene*)))


(defmulti up* (fn [type data] type))

(defmethod up* :pd [_ {:keys [scene-id node-id position db]}]
           (default-up* :pd scene-id node-id position db))

(defmethod up* :node [_ {:keys [scene-id node-id position db]}]
           (default-up* :light scene-id node-id position db))

(defmethod up* :link [_ {:keys [scene-id node-id position db]}]
           (default-up* :light scene-id node-id position db))

(defmethod up* :flow [_ data]
           (log/debug "flow-up*" (pretty (dissoc data :db)))
           (:db data))

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

(defn- selected-node
       "Returns the first node ith a selected attribute."
       [nodes]
       (let [[_ node] (first (filter (fn [[k v]] (:selected v)) nodes))]
            node))

(defmulti move*
          (fn [mode data] mode))

(defmethod move* :move [_ {:keys [scene-id id position db] :as data}]
           (node/move data))

(defmethod move* :pan [_ {:keys [scene-id position db]}]
           (let [scene (get-in db [:scene scene-id])
                 δ (g/- (vec2 position) (vec2 (:pos-0 scene)))
                 translation* (g/+ (vec2 (:pos-1 scene)) δ)
                 scene* (assoc scene :translation (vec-map translation*))]
                (assoc-in db [:scene scene-id] scene*)))

(defmethod move* :link [_ data] (flow/move data))

(defmethod move* :none [_ {:keys [db]}] db)

(defmethod move* :default [_ {:keys [db]}] db)

(s/defn move :- DB
        [{:keys [scene-id] :as data} :- MouseEventData
         db :- DB]
        (let [mode (get-in db [:scene scene-id :mode])]
             (move* mode (assoc data :db db))))


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
       [{:keys [id] :as link}]
       (let [jq (js/$ (str "#" id))
             link-type (keyword (.attr jq "data-link"))]
            (node-id* jq (assoc link :link-type link-type))))

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
                  :link (load-link {:type type :id id})
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



(defn dispatch-data
      [msg ev mouse-event-data]
      (let [target (event-target ev)
            buttons (event-buttons ev)
            position (event-position ev)]
           (merge mouse-event-data target position buttons)))
