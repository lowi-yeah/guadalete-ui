(ns guadalete-ui.pd.flow
  (:require-macros
    [thi.ng.math.macros :as mm])
  (:require
    [reagent.core :as r]
    [re-frame.core :refer [dispatch subscribe]]
    [thi.ng.geom.core :as g]
    [thi.ng.geom.svg.core :as svg]
    [thi.ng.geom.core.vector :refer [vec2]]
    [guadalete-ui.console :as log]
    [guadalete-ui.util :refer [pretty abs vec-map kw*]]
    [thi.ng.math.core :as math :refer [PI HALF_PI TWO_PI]]
    [guadalete-ui.pd.link :as link]))

;//      _
;//   __| |_ _ __ ___ __ __
;//  / _` | '_/ _` \ V  V /
;//  \__,_|_| \__,_|\_/\_/
;//
(defn- get-node-position [key flow scene]
       (let [
             node (get-in scene [:nodes (keyword (:node-id flow))])
             node-position (-> node (:position) (vec2))
             offset (condp = key
                           :from (vec2 18 36)
                           :to (vec2 18 0)
                           (vec2))]
            (g/+ node-position offset)))

(defn- get-position [key flow scene]
       (let [v (get flow key)]
            (if (= :mouse v)
              (let [mouse-pos (or (:mouse/position scene) [0 0])]
                   (vec2 mouse-pos))
              (get-node-position key v scene))))

(defn- svg-cubic-bezier [from to]
       (let [delta-y (-> to
                         (g/- from)
                         (:y)
                         (/ 2)
                         (abs)
                         (max 32))
             c0 (g/+ from (vec2 0 delta-y))
             c1 (g/- to (vec2 0 delta-y))]
            (str "M" (:x from) "," (:y from) " C" (:x c0) "," (:y c0) " " (:x c1) "," (:y c1) " " (:x to) "," (:y to))))

(defn mouse
      "Renders the temporary mouse-flow (if it exists"
      []
      (fn [scene]
          (when (:flow/mouse scene)
                (let [from (get-position :from (:flow/mouse scene) scene)
                      to (get-position :to (:flow/mouse scene) scene)
                      bezier-string (svg-cubic-bezier from to)]
                     ^{:key "mouse-flow"}
                     [:path
                      {:id    "mouse-flow"
                       :class "flow"
                       :d     bezier-string}]))))

(defn flows
      "Renders the connections between nodes (flows that is…)"
      []
      (fn [room-id scene]
          ^{:key "flow-group"}
          [svg/group
           {:id "flows"}
           [mouse scene]

           ]))


;//              _
;//   _ __  __ _| |_____
;//  | '  \/ _` | / / -_)
;//  |_|_|_\__,_|_\_\___|
;//



(defn- decorate-link
       "Decorates a given link with data required for properly renderingit .
       Called during 'begin'"
       [db scene-id node-id link-id link-type]
       (let [link (link/->get db scene-id node-id link-id link-type)
             link* (assoc link :state :active)]
            (link/->update db scene-id node-id link-id link-type link*)))

(defn- to-mouse
       "Internal function for creating a flow from an out-flow to the mouse"
       [{:keys [scene-id node-id id link-type position db] :as data}]
       (let [flow {:from {:node-id node-id :id id} :to :mouse}]
            (-> db
                (assoc-in [:scene scene-id :flow/mouse] flow)
                (assoc-in [:scene scene-id :mouse/position] (vec-map position))
                (assoc-in [:scene scene-id :mode] :link)
                (decorate-link scene-id node-id id link-type)
                )))

(defn- from-mouse
       "Internal function for creating a flow from the mouse into an in-flow"
       [{:keys [scene-id node-id id link-type position db] :as data}]
       (let [flow {:from :mouse :to {:node-id node-id :id id}}]
            (-> db
                (assoc-in [:scene scene-id :flow/mouse] flow)
                (assoc-in [:scene scene-id :mouse/position] (vec-map position))
                (assoc-in [:scene scene-id :mode] :link)
                (decorate-link scene-id node-id id link-type)
                )))

(defn begin [{:keys [link-type db] :as data}]
      (condp = (keyword link-type)
             :in (from-mouse data)
             :out (to-mouse data)
             (log/error "Cannot begin link. Dunno the link-type" link-type)))

;(defn- update-link [scene node-id type id] scene)

(defn move
      "Called when moving the mouse during link creation.
      Updates the mouse mosition (for rendering the temporary flow.
      Also Checks the current target and sets appropriae values in the db"
      [{:keys [db scene-id position node-id type id link-type] :as data}]
      (let [scene (get-in db [:scene scene-id])
            position* (g/- position (vec2 (:translation scene)))
            scene* (assoc scene :mouse/position (vec-map position*))]
           (if (= :link (:type data))
             (-> db
                 (assoc-in [:scene scene-id] scene*)
                 (decorate-link scene-id node-id id link-type))
             (assoc-in db [:scene scene-id] scene*))))




;"buttons": 1,
;"scene-id": "scene2",
;"link-type": "out",
;"type": "link",
;"node-id": "4974f288-fcf8-45a2-aeb3-0921ab31dfb7",
;"id": "c73af9c0-37c5-4b48-a150-242101525339",
;"room-id": "w00t",
;"position": [ 151, 85 ]


;
;(defn make-link [db scene-id position node-id type]
;      (let [scene (get-in db [:scene scene-id])
;            scene* (assoc scene :mouse (vec-map position))]
;           (assoc-in db [:scene scene-id] scene*)))
;
;
;(defn link-state
;      "Returns a string describing the state of the links of a node"
;      [node]
;      "none")