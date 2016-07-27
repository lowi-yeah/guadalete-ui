(ns guadalete-ui.views.segments.light
  (:require
    [clojure.string :refer [lower-case]]
    [re-frame.core :refer [dispatch subscribe]]
    [guadalete-ui.items :refer [light-type]]
    ))

(defn- color-indicator []
  (fn [light]
    (let [color-item {:color (:color light)}])
    [:div.color-indicator]))

(defn- color-string
  "Returns a human readable representation of the state"
  [light]
  (let [color (:color light)]
    (condp = (:num-channels light)
      1 (str "brightness: " (:v color))
      2 (str "brightness: " (:v color) ", tint: " (:s color))
      (str "[ h: " (:h color) ", s: " (:s color) ", v: " (:v color) " ]"))))

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