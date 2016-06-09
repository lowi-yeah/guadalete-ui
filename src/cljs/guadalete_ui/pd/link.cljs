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
    [guadalete-ui.pd.color :refer [render-color]]))

(def node-height 36)
(def link-height 8)
(def link-offset 8)

;//                   _
;//   _ _ ___ _ _  __| |___ _ _
;//  | '_/ -_) ' \/ _` / -_) '_|
;//  |_| \___|_||_\__,_\___|_|
;//


;:on-mouse-enter  #(dispatch-mouse :pd/mouse-enter % mouse-event-data)
;:on-mouse-leave  #(dispatch-mouse :pd/mouse-leave % mouse-event-data)


(defn- link* []
       (fn [l type position]
           [svg/rect position 0 0                           ; dimesions are being set via css
            {:id             (:id l)
             :class          (str "link " (name type))
             :data-type      "link"
             :data-state     (:state l)
             :data-link      (name type)
             :on-mouse-enter #(log/debug ":link/mouse-enter" %)
             :on-mouse-leave #(log/debug ":link/mouse-leave" %)
             }]))

(defn- in* []
       (fn [links position]
           [svg/group
            {:class "in-links"}
            (doall
              (for [l links]
                   (let [position (vec2 link-offset (* -1 link-offset))]
                        ^{:key (str "link-" (:id l))}
                        [link* l :in position])))]))

(defn- out* []
       (fn [links position]
           [svg/group
            {:class "out-links"}
            (doall
              (for [l links]
                   (let [position (vec2 link-offset node-height)]
                        ^{:key (str "link-" (:id l))}
                        [link* l :out position])))]))

(defn links
      "Draws the in- & out-links of a given node"
      []
      (fn [node]
          (let [in-links (into [] (vals (get-in node [:links :in])))
                out-links (into [] (vals (get-in node [:links :out])))
                position (vec2 (:position node))]
               ^{:key (str "links-" (:id node))}
               [svg/group
                {}
                (if (not-empty in-links) [in* in-links position])
                (if (not-empty out-links) [out* out-links position])
                ])))



(defn ->get [db scene-id node-id link-id link-type]
      (get-in db [:scene scene-id :nodes (kw* node-id) :links link-type (kw* link-id)]))

(defn ->update [db scene-id node-id link-id link-type link*]
      (assoc-in db [:scene scene-id :nodes (kw* node-id) :links link-type (kw* link-id)] link*))

(defn- ->reset [[id link]]
       [id (assoc link :state :normal)])

(defn reset-all [links]
      (let [ins* (into {} (map ->reset (:in links)))
            outs* (into {} (map ->reset (:out links)))]
           (-> links
               (assoc :in ins*)
               (assoc :out outs*))))


;(defn- begin-link*
;       "Internal function for staring the creation of a link.
;       Checks whether it is ok to create a link from the given outlet-inlet"
;
;       [scene-id node-id link position db]
;       (let [scene (get-in db [:scene scene-id])
;
;             from (get-outlet (:from link) scene)
;             to (get-inlet (:to link) scene)
;
;             from* (check-outlet from)
;             to* (check-inlet to)
;             position* (g/- position (vec2 (:translation scene)))
;
;             scene* (set-outlet from* scene node-id)
;             scene* (set-inlet to* scene* node-id)
;             scene* (assoc scene*
;                           :mode :link
;                           :link link
;                           :mouse (vec-map position*))]
;
;            (assoc-in db [:scene scene-id] scene*)))

