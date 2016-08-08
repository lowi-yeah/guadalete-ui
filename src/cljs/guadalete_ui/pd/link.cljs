(ns guadalete-ui.pd.link
  (:require-macros
    [reagent.ratom :refer [reaction]])
  (:require
    [clojure.string]
    [reagent.core :as r]
    [re-frame.core :refer [dispatch subscribe]]
    [thi.ng.geom.core :as g]
    [thi.ng.geom.svg.core :as svg]
    [thi.ng.geom.core.vector :refer [vec2]]
    [thi.ng.color.core :as color]
    [guadalete-ui.console :as log]
    [guadalete-ui.util :refer [pretty vec-map kw*]]
    [guadalete-ui.pd.mouse :as mouse]
    [guadalete-ui.pd.color :refer [render-color]]
    [guadalete-ui.pd.layout
     :refer [node-width line-height node-height handle-width handle-height handle-text-padding]]))

(def link-radius 6)

;//                   _
;//   _ _ ___ _ _  __| |___ _ _
;//  | '_/ -_) ' \/ _` / -_) '_|
;//  |_| \___|_||_\__,_\___|_|
;//
(defn- link-handle []
  (fn [l direction scene-id node-id position]
    [svg/rect position handle-width handle-height
     {:id            (:id l)
      :class         (str "link " (:direction l))
      :data-scene-id scene-id
      :data-node-id  node-id
      :data-type     "link"
      :data-state    (name (:state l))
      ;:on-mouse-enter #(dispatch [:link/mouse-enter (merge {:scene-id scene-id} (mouse/dispatch-data %))])
      ;:on-mouse-leave #(dispatch [:link/mouse-exit (merge {:scene-id scene-id} (mouse/dispatch-data %))])
      }]))

(defn- in* []
  (fn [links scene-id node-id offset]
    [svg/group
     {:class "in-links"}
     (doall
       (for [index (-> links count range)]
         (let [l (nth links index)
               text-position (vec2 handle-text-padding (* (+ index offset) line-height))
               handle-position (vec2 (* -1 handle-width) (* (+ index (- offset 0.64)) line-height))]
           ^{:key (str "link-" (:id l))}
           [svg/group {}
            [link-handle l :in scene-id node-id handle-position]
            [svg/text
             text-position
             (str (:name l))]])))]))

(defn- out* []
  (fn [links scene-id node-id offset]
    [svg/group
     {:class "out-links"}
     (doall
       (for [index (-> links count range)]
         (let [l (nth links index)
               text-position (vec2 (- node-width handle-text-padding) (* (+ index offset) line-height))
               handle-position (vec2 node-width (* (+ index (- offset 0.64)) line-height))]
           ^{:key (str "link-" (:id l))}
           [svg/group {}
            [link-handle l :out scene-id node-id handle-position]
            [svg/text
             text-position
             (str (:name l))
             {:text-anchor :end}]])))]))

(defn- filter-direction [links direction]
  (let [filter-fn (fn [[_id link]] (= direction (:direction link)))]
    (->> links
         (filter filter-fn)
         (into {})
         (vals))))

(defn links
  "Draws the in- & out-links of a given node"
  []
  (fn [scene-id node offset]
    (let [in-links (filter-direction (:links node) "in")
          out-links (filter-direction (:links node) "out")]
      ^{:key (str "links-" (:id node))}
      [svg/group
       {}
       (if (not-empty in-links) [in* in-links scene-id (:id node) offset])
       (if (not-empty out-links) [out* out-links scene-id (:id node) (+ offset (count in-links))])])))



(defn ->get [db scene-id node-id link-id]
  (let [link (get-in db [:scene scene-id :nodes (kw* node-id) :links (kw* link-id)])]
    (assoc link :node-id node-id :scene-id scene-id)))

(defn ->update [db scene-id node-id link-id link*]
  (assoc-in db
            [:scene scene-id :nodes (kw* node-id) :links (kw* link-id)]
            (dissoc link* :node-id :scene-id)))

(defn- ->reset [[id link]]
  [id (assoc link :state :normal)])

(defn reset-all [links] (into {} (map ->reset links)))
