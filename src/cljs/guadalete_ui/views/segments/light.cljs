(ns guadalete-ui.views.segments.light
  (:require
    [clojure.string :refer [lower-case]]
    [re-frame.core :refer [dispatch subscribe]]
    [guadalete-ui.items :refer [light-type]]
    [guadalete-ui.pd.color :refer [render-color]]
    [guadalete-ui.console :as log]))

(defn- color-indicator []
       (fn [light]
           (let [color (:color light)
                 c (render-color (assoc color :type (:type light)))]
                [:div.small-color-indicator
                 {:style {:background c}}])))

(defn- color-string
       "Returns a human readable representation of the state"
       [light]
       (let [color (:color light)
             b (:brightness color)
             s (:saturation color)
             h (:hue color)]
            (condp = (:type light)
                   :v (str "brightness: " b)
                   :sv (str "brightness: " b ", tint: " s)
                   :hsv (str "[ h: " h ", s: " s ", v: " b " ]")
                   (str "Unknown color type: " (:type light)))))

(defn light-segment
      "Segement for rendering the light pane of a room."
      [room-rctn]
      (let [lights (sort
                     #(compare
                       (-> %1 (get :name) (lower-case))
                       (-> %2 (get :name) (lower-case)))
                     (:light @room-rctn))]
           [:div.side-margins.margin-bottom
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
             {:on-click #(dispatch [:light/make (:id @room-rctn)])}
             [:i.plus.outline.icon] "Make light"]]))