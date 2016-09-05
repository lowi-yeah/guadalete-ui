(ns guadalete-ui.pd.palette
  (:require-macros
    [thi.ng.math.macros :as mm]
    [reagent.ratom :refer [reaction]])
  (:require
    [clojure.set :refer [difference]]
    [reagent.core :as r]
    [re-frame.core :refer [dispatch subscribe]]
    [cognitect.transit :as t]
    [thi.ng.geom.core :as g]
    [thi.ng.geom.core.vector :as v :refer [vec2 vec3]]
    [guadalete-ui.console :as log]
    [guadalete-ui.util :refer [pretty kw* mappify]]
    ))


;     _                           _
;  __| |_ _ __ _ __ _   _ _    __| |_ _ ___ _ __
; / _` | '_/ _` / _` | | ' \  / _` | '_/ _ \ '_ \
; \__,_|_| \__,_\__, | |_||_| \__,_|_| \___/ .__/
;               |___/                      |_|

;; because DnD in HTMl5 is crazy...
(defn allow-drop [e] (.preventDefault e))

(defn drop* [ev]
  (.preventDefault ev)
  (let [ev (.-nativeEvent ev)
        r (t/reader :json)
        transit-data (.getData (.-dataTransfer ev) "Text")
        js-data (t/read r transit-data)
        data (js->clj js-data)
        ev-pos (v/vec2 (.-offsetX ev) (.-offsetY ev))
        ilk (keyword (get data "ilk"))
        room-id (get data "room")
        scene-id (get data "scene")
        offset (vec2 (get data "offset"))
        position (g/- ev-pos offset)]
    (dispatch [:node/make {:room-id  room-id
                           :scene-id scene-id
                           :ilk      ilk
                           :position position}])))

(defn- start-drag [ev room-id scene-id ilk]
  (let [ev (.-nativeEvent ev)
        w (t/writer :json)
        offset (v/vec2 (.-offsetX ev) (.-offsetY ev))
        data {:ilk    ilk
              :room   room-id
              :scene  scene-id
              :offset offset}
        js-data (clj->js data)
        transit-data (t/write w js-data)]
    (.setData (.-dataTransfer ev) "Text" transit-data)))

;//              _
;//   _ __  __ _(_)_ _
;//  | '  \/ _` | | ' \
;//  |_|_|_\__,_|_|_||_|
;//
(defn- unused-lights
  "Finds a light inside a room which is not yet in use by the given scene."
  [room scene]
  (let [all-light-ids (->> (:light room)
                           (map (fn [l] (:id l)))
                           (into []))
        used-light-ids (->> (:nodes scene)
                            (filter (fn [[id l]] (= :light (kw* (:ilk l)))))
                            (map (fn [[id l]] (:item-id l)))
                            (filter (fn [id] id))
                            (into #{}))]
    (if (< 0 (count all-light-ids))
      (into [] (difference (set all-light-ids) used-light-ids))
      [])))

(defn palette
  "the toolbar for the editor."
  []
  (fn [room-rctn scene-rctn]
    (let [unused-lights? (> (count (unused-lights @room-rctn @scene-rctn)) 0)]
      [:div#palette
       [:div.ui.list

        ; SIGNAL
        ; ****************
        [:div#palette-signal.item
         [:button.ui.circular.icon.button
          {:on-drag-start #(start-drag % (:id @room-rctn) (:id @scene-rctn) :signal)
           :draggable     true}
          [:i.icon.gdlt.signal]]]

        ; SIGNAL-MIXER
        ; ****************
        [:div#palette-mixer.item
         [:button.ui.circular.icon.button
          {:on-drag-start #(start-drag % (:id @room-rctn) (:id @scene-rctn) :mixer)
           :draggable     true}
          [:i.icon.gdlt.mixer]]]

        ; COL0R
        ; ****************
        [:div#palette-color.item
         [:button.ui.circular.icon.button
          {:on-drag-start #(start-drag % (:id @room-rctn) (:id @scene-rctn) :color)
           :draggable     true}
          [:i.icon.gdlt.color]]]

        ; LIGHT
        ; ****************
        [:div#palette-light.item
         [:button.ui.circular.icon.button
          {:on-drag-start #(start-drag % (:id @room-rctn) (:id @scene-rctn) :light)
           :draggable     unused-lights?
           :disabled      (not unused-lights?)}
          [:i.icon.gdlt.bulb]
          ]]
        ]]
      )
    ))

