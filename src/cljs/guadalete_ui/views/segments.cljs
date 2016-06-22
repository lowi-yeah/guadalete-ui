(ns guadalete-ui.views.segments
  "The segments of a room: Scenes, lights, switches, maybe sensors, maybe more…?"
  (:require [re-frame.core :refer [dispatch subscribe]]
            [clojure.string :as string]
            [reagent.core :as reagent]
            [guadalete-ui.items :refer [light-type]]
            [guadalete-ui.pd.core :refer [pd]]
            [guadalete-ui.console :as log]
            [guadalete-ui.dmx :refer [dmx]]
            [guadalete-ui.util :refer [pretty]]
            [guadalete-ui.pd.color :refer [render-color]]
            ))
;(defn scenes
;      "Scene section"
;      []
;      (fn []
;          (let []
;               [:div#login
;                [:div#login-bg]
;                [:div.ui.middle.aligned.center.aligned.grid
;                 [:div.column
;                  [:form.ui.large.form
;                   [:div.ui.segment
;                    [:div.field
;                     [:div.ui.left.icon.input
;                      [:i.user.icon]
;                      [initial-focus-wrapper
;                       [name-form name]]]]
;                    [:div.field
;                     [:div.ui.left.icon.input
;                      [:i.lock.icon]
;                      [password-form password]]]
;                    [:div.ui.fluid.large.submit.button
;                     {:on-click #(re-frame/dispatch [:login @name @password])}
;                     "Login"]
;                    ]]]]])))

(defmulti segment
          (fn [type room-rctn]
              type))

(defmethod segment :scene
           [_ room-rctn]
           (let [scenes (:scene @room-rctn)
                 scene-rctn (subscribe [:current/scene])]
                [:div#scenes.ui.flexing.relative

                 [:div.side-margins.margin-bottom
                  [:div.ui.pointing.menu.inverted.secondary
                   (doall
                     (for [s scenes]
                          (let [scene-link (str "#/room/" (:id @room-rctn) "/scene/" (:id s))
                                current? (= (:id s) (:id @scene-rctn))]
                               ^{:key (str "s-" (:id s))}
                               [:a.item
                                {:href  scene-link
                                 :class (if current? "active")}
                                (:name s)])))]]
                 [pd room-rctn @scene-rctn]]))

(defn- color-type [num-channels]
       (condp = num-channels
              1 "w"
              2 "ww"
              3 "rgb"
              4 "rgbw"
              "unknown"))

(defn- color-string
       "Returns a human readable representation of the state"
       [light]
       (let [color (:color light)]
            (condp = (:num-channels light)
                   1 (str "brightness: " (:v color))
                   2 (str "brightness: " (:v color) ", tint: " (:s color))
                   (str "[ h: " (:h color) ", s: " (:s color) ", v: " (:v color) " ]")
                   )))

(defn- color-indicator []
       (fn [light]
           (let [color-item {:color (:color light)}

                 ])
           [:div.color-indicator]
           ))

(defmethod segment :light
           [_ room-rctn]
           (let [lights (:light @room-rctn)]
                [:div.side-margins
                 [:table.ui.celled.table.inverted
                  [:thead
                   [:tr
                    [:th "name"]
                    [:th "type"]
                    [:th "color"]
                    [:th "channels"]]]
                  [:tbody
                   (if (not-empty lights)
                     (doall
                       (for [light lights]
                            ^{:key (str "l-" (:id light))}
                            [:tr.light
                             {:on-click #(dispatch [:light/edit (:id light)])}
                             [:td (:name light)]
                             [:td (light-type light)]
                             [:td.color
                              [color-indicator light]
                              [:span (color-string light)]]
                             [:td (str (:channels light))]
                             ]
                            ))
                     (do
                       ^{:key (str "no-lights")}
                       [:tr
                        [:td "No lights have been registered."]
                        [:td]
                        [:td]
                        [:td]]))]]

                 [:div.ui.button.add

                  {:on-click #(dispatch [:light/prepare-new (:id @room-rctn)])}
                  [:i.plus.outline.icon] "Make light"]
                 ]))

(defmethod segment :switch
           [_ room-rctn]
           [:div.side-margins
            [:h1 "debug"]
            [:pre.code (pretty @room-rctn)]
            ])

(defmethod segment :dmx
           [_]
           (let [dmx-rctn (subscribe [:dmx/all])]
                [:div#dmx.ui.flexing.relative
                 [dmx dmx-rctn]]))
