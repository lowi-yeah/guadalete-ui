(ns guadalete-ui.views.segments
  "The segments of a room: Scenes, lights, switches, maybe sensors, maybe more…?"
  (:require [re-frame.core :refer [dispatch subscribe]]
            [clojure.string :as string]
            [reagent.core :as reagent]

            [guadalete-ui.pd.core :refer [pd]]
            [guadalete-ui.console :as log]
            [guadalete-ui.dmx :refer [dmx]]
            [guadalete-ui.util :refer [pretty]]
            [guadalete-ui.pd.color :refer [render-color]]
            [guadalete-ui.views.widgets :refer [signal-sparkline]]
            [guadalete-ui.views.segments.light :refer [light-segment]]
            [guadalete-ui.views.editor :refer [editor]]))

;// ROOM
;// ********************************
(defn- scene* []
  (fn [room-rctn]
    (let [scene-rctn (subscribe [:view/scene])]
      [pd room-rctn scene-rctn])))

(defmulti segment (fn [type _] type))

(defmethod segment :scene
  [_ room-rctn]
  [scene* room-rctn])

(defmethod segment :dash
  [_ room-rctn]
  (let []
    [:div#dash
     [:h2 "dash!"]
     [:pre.code (pretty @room-rctn)]]))

(defn- color-type [num-channels]
  (condp = num-channels
    1 "w"
    2 "ww"
    3 "rgb"
    4 "rgbw"
    "unknown"))

(defmethod segment :light
  [_ room-rctn]
  [light-segment room-rctn])

(defmethod segment :switch
  [_ room-rctn]
  [:div#db.ui.flexing.relative
   [editor @room-rctn]])

(defmethod segment :dmx
  [_]
  (let [dmx-rctn (subscribe [:dmx/all])]
    [:div#dmx.ui.flexing.relative
     [dmx dmx-rctn]]))


(defmethod segment :signal
  [_]
  (let [signals-rctn (subscribe [:signal/all])]
    [:div#signals.ui.flexing.relative.side-margins
     [:table.ui.celled.table.inverted
      [:thead
       [:tr
        [:th "name"]
        [:th "type"]
        [:th "values"]]]
      [:tbody
       (doall
         (for [signal (vals @signals-rctn)]
           (do
             ^{:key (str "s-" (:id signal))}
             [:tr.signal
              [:td (:name signal)]
              [:td (:type signal)]
              [:td
               [signal-sparkline signal]
               ]])))]]]))

(defmethod segment :debug
  [_]
  (let [db-rctn (subscribe [:db])]
    [:div#db.ui.flexing.relative
     [editor @db-rctn]]))