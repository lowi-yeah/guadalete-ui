(ns guadalete-ui.pd.flow
  (:require-macros
    [thi.ng.math.macros :as mm])
  (:require
    [reagent.core :as r]
    [reagent.core :refer [create-class dom-node]]
    [re-frame.core :refer [dispatch subscribe]]
    [thi.ng.geom.core :as g]
    [thi.ng.geom.svg.core :as svg]
    [thi.ng.geom.core.vector :refer [vec2]]
    [guadalete-ui.console :as log]
    [guadalete-ui.util :refer [pretty abs vec-map kw* in?]]
    [thi.ng.math.core :as math :refer [PI HALF_PI TWO_PI]]
    [guadalete-ui.pd.link :as link]
    [guadalete-ui.pd.layout
     :refer [link-offset node-width line-height node-height handle-width handle-height handle-text-padding]]))

;//      _
;//   __| |_ _ __ ___ __ __
;//  / _` | '_/ _` \ V  V /
;//  \__,_|_| \__,_|\_/\_/
;//
(defn- get-link-position [{:keys [node-id id] :as data} scene-rctn]
  (let [node (get-in @scene-rctn [:nodes (kw* node-id)])
        offset (link-offset node)
        node-vec (vec2 (:position node))
        link (get-in node [:links (kw* id)])
        link-vec (if (= (:direction link) "in")
                   (vec2 (* -0.5 handle-width) (+ (* (+ (:index link) (- offset 0.64)) line-height) (/ handle-height 2)))
                   (vec2 (+ (* 0.5 handle-width) node-width) (+ (/ handle-height 2) (* (+ (:index link) (- offset 0.64)) line-height))))
        ]
    (g/+ node-vec link-vec)))


(defn- svg-cubic-bezier [from to]
  (let [delta-x (-> to
                    (g/- from)
                    (:x)
                    (/ 2)
                    (abs)
                    (max 32))
        c0 (g/+ from (vec2 delta-x 0))
        c1 (g/- to (vec2 delta-x 0))]
    (str "M" (:x from) "," (:y from) " C" (:x c0) "," (:y c0) " " (:x c1) "," (:y c1) " " (:x to) "," (:y to))))

(defn- flow
  "internal helper for rendering a single flow (ie. bezier curve between two links)"
  []
  (fn [flow scene-rctn selected?]
    (let [id (:id flow)
          from-vec (get-link-position (:from flow) scene-rctn)
          to-vec (get-link-position (:to flow) scene-rctn)
          bezier-string (svg-cubic-bezier from-vec to-vec)
          ]
      ^{:key id}
      [:path
       {:id            id
        :class         (if selected? "flow selected" "flow")
        :data-type     "flow"
        :data-scene-id (:id @scene-rctn)
        :d             bezier-string}])))

(defn- mouse-flow [scene-rctn flow mouse-pos]
  (let [{:keys [from to]} flow
        mouse-pos* (g/- (vec2 mouse-pos) (vec2 (:translation @scene-rctn)))

        _ (log/debug "mouse-flow" flow)

        from-vec (if (= :mouse from)
                   mouse-pos*
                   (get-link-position from scene-rctn))
        to-vec (if (= :mouse to)
                 mouse-pos*
                 (get-link-position to scene-rctn))
        bezier-string (svg-cubic-bezier from-vec to-vec)]
    ^{:key "mouse-flow"}
    [:path
     {:id    "mouse-flow"
      :class (str "flow " (if (:valid? flow) (name (:valid? flow))))
      :d     bezier-string}]))

(defn flows
  "Renders the connections between nodes (flows that is…)"
  []
  (fn [scene-rctn]
    (let [tmp-rctn (subscribe [:pd/tmp])
          selected-flows (subscribe [:pd/selected-flows])]
      ^{:key "flow-group"}
      [svg/group
       {:id "flows"}
       (if (:flow @tmp-rctn) [mouse-flow scene-rctn (:flow @tmp-rctn) (:mouse-pos @tmp-rctn)])
       (doall (for [f (vals (:flows @scene-rctn))]
                (let [selected? (in? @selected-flows (:id f))]
                  ^{:key (str "f-" (:id f))}
                  [flow f scene-rctn selected?])))
       ])))


;//              _
;//   _ __  __ _| |_____
;//  | '  \/ _` | / / -_)
;//  |_|_|_\__,_|_\_\___|
;//

;(defn- update-link [scene node-id type id] scene)

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
          ;(decorate-link scene-id node-id id)
          ))
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
