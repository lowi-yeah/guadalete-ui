(ns guadalete-ui.pd.nodes.link
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
    [guadalete-ui.util :refer [pretty vec->map kw*]]
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
  (fn [l scene-id node-id position]
    [svg/rect position handle-width handle-height
     {:id            (str node-id "-" (:id l))
      :class         (str "link " (name (:direction l)))
      :data-scene-id scene-id
      :data-node-id  node-id
      :data-link-id  (:id l)
      :data-type     "link"}]))

(defn- y-pos [offset index]
  [(* (+ index offset) line-height)
   (* (+ index (- offset 0.64)) line-height)])

(defn- in* []
  (fn [links scene-id node-id offset]
    [svg/group
     {:class "in-links"}
     (doall
       (for [l links]
         (let [index (:index l)
               text-position (vec2 handle-text-padding (* (+ index offset) line-height))
               handle-position (vec2 (* -1 handle-width) (* (+ index (- offset 0.64)) line-height))]
           ^{:key (str "link-" (:id l))}
           [svg/group {}
            [link-handle l scene-id node-id handle-position]
            [svg/text
             text-position
             (str (:name l))
             ]])))]))

(defn- out* []
  (fn [links scene-id node-id offset]
    [svg/group
     {:class "out-links"}
     (doall
       (for [l links]
         (let [index (:index l)
               [text-y handle-y] (y-pos offset index)
               text-position (vec2 (- node-width handle-text-padding) text-y)
               handle-position (vec2 node-width handle-y)]
           ^{:key (str "link-" (:id l))}
           [svg/group {}
            [link-handle l scene-id node-id handle-position]
            [svg/text
             text-position
             (str (:name l))
             ;(str (:index l) " - " (:name l))
             {:text-anchor :end}]])))]))


(defn- group-by-direction [links]
  (group-by :direction links))

(defn- sort-by-index [{:keys [in out]}]
  (let [sorted-in (sort-by :index in)
        sorted-out (sort-by :index out)]
    {:in  sorted-in
     :out sorted-out}))

(defn links
  "Draws the in- & out-links of a given node"
  []
  (fn [scene-id node offset]

    (let [links (->> (:links node) (group-by-direction) (sort-by-index))]
      ^{:key (str "links-" (:id node))}
      [svg/group {}
       (if (not-empty (:in links))
         [in* (:in links) scene-id (:id node) offset])
       (if (not-empty (:out links))
         [out* (:out links) scene-id (:id node) offset])])))

(defn ->get [db scene-id node-id link-id]
  (let [links (get-in db [:scene scene-id :nodes (kw* node-id) :links])]
    (->> links
         (filter #(= link-id (:id %)))
         (first))))

(defn ->update [db scene-id node-id link-id link*]
  (assoc-in db
            [:scene scene-id :nodes (kw* node-id) :links (kw* link-id)]
            (dissoc link* :node-id :scene-id)))


(defn- ->reset [[id link]]
  [id (assoc link :state :normal)])

(defn reset-all [links] (into {} (map ->reset links)))
