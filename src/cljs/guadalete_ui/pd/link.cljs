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
    [guadalete-ui.pd.color :refer [render-color]]))

(def node-height 28)
(def link-height 8)
(def link-offset 8)
(def line-height 14)
(def node-width 92)
(def link-radius 6)

;//                   _
;//   _ _ ___ _ _  __| |___ _ _
;//  | '_/ -_) ' \/ _` / -_) '_|
;//  |_| \___|_||_\__,_\___|_|
;//
(defn- link-handle []
       (fn [l direction scene-id node-id position]
           [svg/circle position link-radius
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
       (fn [links scene-id node-id position offset]
           [svg/group
            {:class "in-links"}
            (doall
              (for [l links]
                   (let [position* (vec2 0 (* offset line-height))]
                        ^{:key (str "link-" (:id l))}
                        [svg/group {}
                         [link-handle l :in scene-id node-id position*]
                         [svg/text
                          (g/+ position* (vec2 (+ 2 link-radius) 3))
                          (str (:name l))]])))]))

(defn- out* []
       (fn [links scene-id node-id position offset]
           (log/debug "offset" (str offset))
           [svg/group
            {:class "out-links"}
            (doall
              (for [l links]
                   (let [position* (vec2 node-width (* offset line-height))]
                        ^{:key (str "link-" (:id l))}
                        [svg/group {}
                         [link-handle l :out scene-id node-id position*]
                         [svg/text
                          (g/+ position* (vec2 (* -1 (+ 2 link-radius)) 3))
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
      (fn [scene-id node]
          (let [in-links (filter-direction (:links node) "in")
                out-links (filter-direction (:links node) "out")
                position (vec2 (:position node))

                base-offset 2]

               (log/debug "out-links" (str out-links))
               (log/debug "in-links" (str in-links))
               (log/debug "position" (str position))

               ^{:key (str "links-" (:id node))}
               [svg/group
                {}
                (if (not-empty out-links) [out* out-links scene-id (:id node) position base-offset])
                (if (not-empty in-links) [in* in-links scene-id (:id node) position (+ base-offset (count out-links))])])))



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
