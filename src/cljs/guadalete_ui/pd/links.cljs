(ns guadalete-ui.pd.links
  (:require-macros
    [thi.ng.math.macros :as mm])
  (:require
    [reagent.core :as r]
    [re-frame.core :refer [dispatch subscribe]]
    [thi.ng.geom.core :as g]
    [thi.ng.geom.svg.core :as svg]
    [thi.ng.geom.core.vector :refer [vec2]]
    [guadalete-ui.console :as log]
    [guadalete-ui.util :refer [pretty abs]]
    [guadalete-ui.pd.util :refer [target-id target-type]]
    [thi.ng.math.core :as math :refer [PI HALF_PI TWO_PI]]))


(defn- get-node-position [key node-id layout]
       (let [node-position (->> layout
                                (:nodes)
                                (filter #(= (:id %) node-id))
                                (first)
                                (:position)
                                (vec2))
             offset (condp = key
                           :from (vec2 18 36)
                           :to (vec2 18 0)
                           (vec2))]
            (g/+ node-position offset)))

(defn- get-position [key link layout]
       (let [v (get link key)]
            (if (= :mouse v)
              (let [mouse-pos (or (:mouse layout) [0 0])]
                   (vec2 mouse-pos))
              (get-node-position key v layout))))

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



(defn links
      "Renders the links between nodes"
      []
      (fn [room-id scene]
          (let [layout (:layout scene)
                link (:link layout)]
               ^{:key "link-group"}
               [svg/group
                {:id "links"}
                (when link
                      (let [from (get-position :from link layout)
                            to (get-position :to link layout)
                            bezier-string (svg-cubic-bezier from to)]
                           ^{:key "mouse-link"}
                           [:path
                            {:id    "mouse-link"
                             :class "link"
                             :d     bezier-string}]))])))

(defn link-mouse [db scene-id layout position node-id type]
      (log/debug "link-mouse" (str type) node-id)
      (let [scene (get-in db [:scene scene-id])
            layout (:layout scene)
            layout* (assoc layout :mouse {:x (:x position) :y (:y position)})
            scene* (assoc scene :layout layout*)]
           (assoc-in db [:scene scene-id] scene*)))

(defn make-link [db scene-id layout position node-id type]
      (log/debug "make-link" (str type) node-id)
      (let [scene (get-in db [:scene scene-id])
            layout (:layout scene)
            layout* (assoc layout :mouse {:x (:x position) :y (:y position)})
            scene* (assoc scene :layout layout*)]
           (assoc-in db [:scene scene-id] scene*)))