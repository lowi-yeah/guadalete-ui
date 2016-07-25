(ns guadalete-ui.pd.flow
  (:require-macros
    [thi.ng.math.macros :as mm])
  (:require
    [reagent.core :as r]
    [re-frame.core :refer [dispatch]]
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

(defn- flow
       "internal helper for rendering a single flow (ie. bezier curve between tow links"
       []
       (fn [f scene]
           (let [from (get-position :from f scene)
                 to (get-position :to f scene)
                 bezier-string (svg-cubic-bezier from to)]
                ^{:key "mouse-flow"}
                [:path
                 {:id    "mouse-flow"
                  :class "flow"
                  :d     bezier-string}])
           ))

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

(defn flows*
      "Internal helper for rendering all flows"
      []
      (fn [scene]
          [svg/group {}
           (doall (for [f (vals (:flows scene))]
                       (let []
                            ^{:key (str "f-" (:id f))}
                            [flow f scene])))
           ]))

(defn flows
      "Renders the connections between nodes (flows that is…)"
      []
      (fn [scene]
          ^{:key "flow-group"}
          [svg/group
           {:id "flows"}
           [mouse scene]
           [flows* scene]

           ]))


;//              _
;//   _ __  __ _| |_____
;//  | '  \/ _` | / / -_)
;//  |_|_|_\__,_|_\_\___|
;//



(defn- decorate-link
       "Decorates a given link with data required for properly renderingit .
       Called during 'begin'"
       [db scene-id node-id link-id]
       (let [link (link/->get db scene-id node-id link-id)
             link* (assoc link :state :active)]
            (link/->update db scene-id node-id link-id link*)))

(defn- to-mouse
       "Internal function for creating a flow from an out-flow to the mouse"
       [{:keys [scene-id node-id id position] :as data} db]
       (let [flow {:from {:node-id node-id :id id} :to :mouse}
             scene (get-in db [:scene scene-id])
             position* (g/- position (vec2 (:translation scene)))]
            (-> db
                (assoc-in [:scene scene-id :flow/mouse] flow)
                (assoc-in [:scene scene-id :mouse/position] (vec-map position*))
                (assoc-in [:scene scene-id :mode] :link)
                (decorate-link scene-id node-id id)
                )))

(defn- from-mouse
       "Internal function for creating a flow from the mouse into an in-flow"
       [{:keys [scene-id node-id id position] :as data} db]
       (let [flow {:from :mouse :to {:node-id node-id :id id}}
             scene (get-in db [:scene scene-id])
             position* (g/- position (vec2 (:translation scene)))]
            (-> db
                (assoc-in [:scene scene-id :flow/mouse] flow)
                (assoc-in [:scene scene-id :mouse/position] (vec-map position*))
                (assoc-in [:scene scene-id :mode] :link)
                (decorate-link scene-id node-id id)
                )))

(defn begin [{:keys [scene-id node-id id position] :as data} db]
      (let [link (link/->get db scene-id node-id id)]
           (condp = (kw* (:direction link))
                  :in (from-mouse data db)
                  :out (to-mouse data db)
                  (do
                    (log/error "Cannot begin link. Dunno the link-type" (:direction link))
                    db))))

;(defn- update-link [scene node-id type id] scene)

(defn move
      "Called when moving the mouse during link creation.
      Updates the mouse mosition (for rendering the temporary flow.
      Also Checks the current target and sets appropriae values in the db"
      [{:keys [scene-id position node-id type id link-type] :as data} db]
      (let [scene (get-in db [:scene scene-id])
            position* (g/- position (vec2 (:translation scene)))
            scene* (assoc scene :mouse/position (vec-map position*))]
           (if (= :link type)
             (dispatch [:flow/check-connection data])
             (dispatch [:flow/reset-target data]))
           (assoc-in db [:scene scene-id] scene*)))

(defn- non-mouse-link
       "Helper to retrieve the non-mouse side of a 'mouse-flow'"
       [scene-id db]
       (let [mouse-flow (get-in db [:scene scene-id :flow/mouse])
             flow-link (if (= (:from mouse-flow) :mouse)
                         (:to mouse-flow)
                         (:from mouse-flow))
             link (link/->get db scene-id (:node-id flow-link) (:id flow-link))]
            (assoc link :scene-id scene-id)))

(defn- valid-connection?
       [{:keys [scene-id node-id id]} db]
       (let [candidate (link/->get db scene-id node-id id)
             nml (non-mouse-link scene-id db)
             same-id? (= id (:id nml))
             same-direction? (= (:direction candidate) (:direction nml))
             same-ilk? (= (:ilk candidate) (:ilk nml))
             accepts-link? true]
            (and (not same-id?) (not same-direction?) same-ilk? accepts-link?)))


(defn check-connection [{:keys [scene-id node-id id] :as data} db]
      (if (valid-connection? data db)
        (let [mouse-flow (get-in db [:scene scene-id :flow/mouse])]
             (-> db
                 (assoc-in [:scene scene-id :flow/mouse :candidate] {:node-id node-id :id id})
                 (decorate-link scene-id node-id id)))
        db))

(defn reset-target [{:keys [scene-id node-id id] :as data} db]
      (let [mouse-flow (get-in db [:scene scene-id :flow/mouse])
            candidate-data (:candidate mouse-flow)]
           (if candidate-data
             (let [candidate (link/->get db scene-id (:node-id candidate-data) (:id candidate-data))
                   candidate* (assoc candidate :state :none)
                   mouse-flow* (dissoc mouse-flow :candidate)
                   ]
                  (-> db
                      (link/->update scene-id (:node-id candidate-data) (:id candidate-data) candidate*)
                      (assoc-in [:scene scene-id :flow/mouse] mouse-flow*)))
             db)))

(defn- get-from [link-0 link-1]
       (let [link* (if (= :out (kw* (:direction link-0)))
                     link-0
                     link-1)]
            {:node-id (:node-id link*)
             :id      (:id link*)}))

(defn- get-to [link-0 link-1]
       (let [link* (if (= :in (kw* (:direction link-0)))
                     link-0
                     link-1)]
            {:node-id (:node-id link*)
             :id      (:id link*)}))

(defn- make-flow
       "internal function that actually creates a flow"
       [{:keys [scene-id node-id id] :as data} db]
       (let [nml (non-mouse-link scene-id db)
             new-link (link/->get db scene-id node-id id)
             from (get-from nml new-link)
             to (get-to nml new-link)
             new-flow-id (str (random-uuid))
             new-flow {:id   new-flow-id
                       :from from
                       :to   to}
             scene (get-in db [:scene scene-id])
             flows (:flows scene)
             flows* (assoc flows (kw* new-flow-id) new-flow)
             scene* (-> scene
                        (assoc :flows flows*)
                        (dissoc :flow/mouse))]
            (dispatch [:scene/update scene*])
            (dispatch [:node/reset-all scene-id])
            (assoc-in db [:scene scene-id] scene*)))

(defn end [data db]
      (if (valid-connection? data db)
        (make-flow data db)))
