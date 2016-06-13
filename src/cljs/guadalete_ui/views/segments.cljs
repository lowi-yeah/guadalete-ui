(ns guadalete-ui.views.segments
  "The segments of a room: Scenes, lights, switches, maybe sensors, maybe more…?"
  (:require [re-frame.core :refer [dispatch subscribe]]
            [clojure.string :as string]
            [reagent.core :as reagent]
            [guadalete-ui.items :refer [light-type]]
            [guadalete-ui.pd.core :refer [pd]]
            [guadalete-ui.console :as log]
            [guadalete-ui.dmx :refer [dmx]]
            [guadalete-ui.util :refer [pretty]]))
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
                 scene (first scenes)]
                [:div#scenes.ui.flexing.relative
                 ;[:div.ui.pointing.menu.inverted.secondary
                 ; (doall
                 ;   (for [scene scenes]
                 ;        (let [scene-link (str "#/room/" (:id @room-rctn) "/scene/" (:id scene))]
                 ;             ^{:key (str "s-" (:id scene))}
                 ;             [:a.item {:href scene-link} (:name scene)])))]
                 [pd room-rctn scene]
                 ]))

(defmethod segment :light
           [_ room-rctn]
           (let [lights (:light @room-rctn)]
                [:div.side-margins
                 [:table.ui.celled.table.inverted
                  [:thead
                   [:tr
                    [:th "name"]
                    [:th "type"]
                    [:th "state"]]]
                  [:tbody
                   (if (not-empty lights)
                     (doall
                       (for [light lights]
                            ^{:key (str "l-" (:id light))}
                            [:tr
                             [:td (:name light)]
                             [:td (light-type light)]
                             [:td (get-in light [:state :brightness])]
                             ]))
                     (do
                       ^{:key (str "no-lights")}
                       [:tr
                        [:td "No lights have been registered."]
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
