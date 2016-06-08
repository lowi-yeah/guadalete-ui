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
    [guadalete-ui.util :refer [pretty abs vec-map]]
    [thi.ng.math.core :as math :refer [PI HALF_PI TWO_PI]]))

;//      _
;//   __| |_ _ __ ___ __ __
;//  / _` | '_/ _` \ V  V /
;//  \__,_|_| \__,_|\_/\_/
;//
(defn- get-node-position [key link scene]
       (let [
             node (get-in scene [:nodes (:node-id link)])
             node-position (-> node (:position) (vec2))
             offset (condp = key
                           :from (vec2 18 36)
                           :to (vec2 18 0)
                           (vec2))]
            (g/+ node-position offset)))

(defn- get-position [key link scene]
       (let [v (get link key)]
            (if (= :mouse v)
              (let [mouse-pos (or (:mouse scene) [0 0])]
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



(defn flows
      "Renders the connections between nodes (links that is…)"
      []
      (fn [room-id scene]
          (let [link (:link scene)]
               ^{:key "link-group"}
               [svg/group
                {:id "links"}
                (when link
                      (let [from (get-position :from link scene)
                            to (get-position :to link scene)
                            bezier-string (svg-cubic-bezier from to)]
                           ^{:key "mouse-link"}
                           [:path
                            {:id    "mouse-link"
                             :class "link"
                             :d     bezier-string}]))])))


;//              _
;//   _ __  __ _| |_____
;//  | '  \/ _` | / / -_)
;//  |_|_|_\__,_|_\_\___|
;//

(defn- to-mouse
       "Internal function for creating a flow from an out-flow to the mouse"
       [{:keys [db] :as data}]
       (let [
             ;link {:from {:node-id node-id :id id} :to :mouse}
             ]
            (log/debug "flow/begin" (pretty (dissoc data :db)))
            db
            )
       )

(defn- from-mouse
       "Internal function for creating a flow from the mouse into an in-flow"
       [{:keys [db] :as data}]
       (log/debug "from-mouse")

       db
       )

(defn- begin [{:keys [link-type db] :as data}]
       (condp = (keyword link-type)
              :in (from-mouse data)
              :out (to-mouse data)
              (log/error "Cannot begin link. Dunno the link-type" link-type)))


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