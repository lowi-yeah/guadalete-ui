(ns guadalete-ui.pd.palette
  (:require-macros
    [thi.ng.math.macros :as mm]
    [reagent.ratom :refer [reaction]])
  (:require
    [reagent.core :as r]
    [re-frame.core :refer [dispatch]]
    [cognitect.transit :as t]
    [thi.ng.geom.core :as g]
    [thi.ng.geom.core.vector :as v :refer [vec2 vec3]]
    [guadalete-ui.console :as log]
    ))


;     _                           _
;  __| |_ _ __ _ __ _   _ _    __| |_ _ ___ _ __
; / _` | '_/ _` / _` | | ' \  / _` | '_/ _ \ '_ \
; \__,_|_| \__,_\__, | |_||_| \__,_|_| \___/ .__/
;               |___/                      |_|



(defn allow-drop [e]
      (.preventDefault e)
      )                                                     ;; because DnD in HTMl5 is crazy...

(defn drop* [ev]
      (.preventDefault ev)
      (let [ev (.-nativeEvent ev)
            r (t/reader :json)
            transit-data (.getData (.-dataTransfer ev) "Text")
            js-data (t/read r transit-data)
            data (js->clj js-data)
            ev-pos (v/vec2 (.-offsetX ev) (.-offsetY ev))
            type (keyword (get data "type"))
            room-id (get data "room")
            scene-id (get data "scene")
            offset (vec2 (get data "offset"))
            node-pos (g/- ev-pos offset)
            ]
           (dispatch [:node/make [room-id scene-id type node-pos]])
           ))



(defn- start-drag [ev room-id scene-id type]
       (let [ev (.-nativeEvent ev)
             w (t/writer :json)
             offset (v/vec2 (.-offsetX ev) (.-offsetY ev))
             data {:type   type
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
(defn palette
      "the toolbar for the editor."
      []
      (fn [room-id scene-id]
          [:div#palette
           [:div.ui.list

            ; SIGNAL
            ; ****************
            [:div#palette-signal.item
             [:button.ui.circular.icon.button
              {:on-drag-start #(start-drag % room-id scene-id :sgnl)
               :draggable     true}
              [:i.icon.gdlt.signal]]]

            ; COL0R
            ; ****************
            [:div#palette-color.item
             [:button.ui.circular.icon.button
              {:on-drag-start #(start-drag % room-id scene-id :color)
               :draggable     true}
              [:i.icon.gdlt.color]]]

            ; LIGHT
            ; ****************
            [:div#palette-light.item
             [:button.ui.circular.icon.button
              {:on-drag-start #(start-drag % room-id scene-id :light)
               :draggable     true}
              [:i.icon.gdlt.bulb]
              ]]
            ]]))
