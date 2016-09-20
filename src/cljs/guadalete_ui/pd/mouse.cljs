(ns guadalete-ui.pd.mouse
  (:require-macros [reagent.ratom :refer [reaction]]
                   [schema.macros])
  (:require [clojure.set :refer [difference]]
            [re-frame.core :refer [dispatch]]
            [thi.ng.geom.core :as g]
            [thi.ng.geom.core.vector :refer [vec2]]
            [guadalete-ui.util :refer [pretty validate! vec->map in?]]
            [guadalete-ui.console :as log]
            [guadalete-ui.pd.util :refer [pd-screen-offset]]
            [schema.core :as s]
            [guadalete-ui.schema :as gs]))


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
       "Return the data required for retrieving a link"
       [id]
       (let [jq (js/$ (str "#" id))
             scene-id* (.attr jq "data-scene-id")
             node-id* (.attr jq "data-node-id")
             link-id* (.attr jq "data-link-id")]
            {:scene-id scene-id*
             :node-id  node-id*
             :id       link-id*
             :type     :link}))

(defn- load-node
       "Return the data required for retrieving a node"
       [id]
       (let [jq (js/$ (str "#" id))
             scene-id* (.attr jq "data-scene-id")
             ilk* (keyword (.attr jq "data-ilk"))]
            {:scene-id scene-id*
             :id       id
             :ilk      ilk*
             :type     :node}))

(defn- flow-reference
       "Return the data required for retrieving a flow"
       [id]
       (let [jq (js/$ (str "#" id))
             scene-id (.attr jq "data-scene-id")]
            {:scene-id scene-id
             :id       id
             :type     :flow}))



(defn event-target
      "Returns a map containing information to access the specific item type.
      E.g. if a 'link' is the target the node id and link type will be attached"
      [ev]
      (let [target* (.-target ev)
            type (target-type target*)
            id (target-id target*)]
           (condp = type
                  :pd {:type type}
                  :node (load-node id)
                  :link (load-link id)
                  :flow (flow-reference id)
                  {:type type})))

(defn event-buttons
      "Get the button state of a mouse event"
      [ev]
      {:buttons (.-buttons ev)})

(defn event-modifiers
      "Get the modifier keys of a mouse event"
      [ev]
      (let [ev* (.-nativeEvent ev)
            alt (.-altKey ev*)
            shift (.-shiftKey ev*)
            ]
           {:modifiers {:alt alt :shift shift}}))

(defn event-position
      "Calculates the position of the mouse event with respect to
      the offset of the svg frame in relation to the screen."
      [ev]
      (let [ev* (.-nativeEvent ev)
            pos (vec2 (int (.-x ev*)) (int (.-y ev*)))
            offset (pd-screen-offset)]
           {:position (g/- pos offset)}))

;(s/defn ^:always-validate event-data :- gs/MouseEventData
(s/defn event-data :- gs/MouseEventData
        "Creates MouseEvent data to be dispatched from a DOM mouse-event
        and the information about the room and scene we're workin on"
        [ev
         scene-data :- gs/SceneData]

        (validate! gs/SceneData scene-data :silent)

        (let [target (event-target ev)
              buttons (event-buttons ev)
              modifiers (event-modifiers ev)
              position (event-position ev)]
             (merge scene-data target position buttons modifiers)))
