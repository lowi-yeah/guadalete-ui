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
    [guadalete-ui.util :refer [pretty vec-map]]
    [guadalete-ui.pd.color :refer [render-color]])
  )

(def node-height 36)
(def link-height 8)
(def link-offset 8)

;//                   _
;//   _ _ ___ _ _  __| |___ _ _
;//  | '_/ -_) ' \/ _` / -_) '_|
;//  |_| \___|_||_\__,_\___|_|
;//
(defn- in* []
       (fn [links position]
           [svg/group
            {:class "in-links"}
            (doall
              (for [l links]
                   (let [position (vec2 link-offset (* -1 link-offset))]
                        ^{:key (str "link-" (:id l))}
                        [svg/rect position 0 0              ; dimesions are being set via css
                         {:id         (:id l)
                          :class      "link in"
                          :data-type  "link"
                          :data-state (:state l)
                          :data-link  "in"}])))]))

(defn- out* []
       (fn [links position]
           [svg/group
            {:class "out-links"}
            (doall
              (for [l links]
                   (let [position (vec2 link-offset node-height)]
                        ^{:key (str "link-" (:id l))}
                        [svg/rect position 0 0              ; dimesions are being set via css
                         {:id         (:id l)
                          :class      "link out"
                          :data-type  "link"
                          :data-state (:state l)
                          :data-link  "out"}])))]))

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


;//              _
;//   _ __  __ _| |_____
;//  | '  \/ _` | / / -_)
;//  |_|_|_\__,_|_\_\___|
;//

(defn reset-all [links]
      ;(log/debug "reseetting all links" (pretty links))
      links
      )


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

