(ns guadalete-ui.views.sections
  (:require
    [re-frame.core :refer [dispatch subscribe]]
    [guadalete-ui.views.segments :refer [segment]]
    [guadalete-ui.views.menu :refer [main-menu secondary-menu]]
    [guadalete-ui.console :as log]
    [guadalete-ui.util :refer [pretty]]))


;// BLANK
;// ********************************
(defn- blank [] [:div#blank])



;// DASH
;// ********************************
(defn- dash []
  (fn []
    (let [unconfirmed-lights-rctn (subscribe [:light/unconfirmed])]
      [:div#dash.flex-container.full-height
       [:h3.margins.margin-top "dash…"]
       (when (> (count @unconfirmed-lights-rctn) 0)
         [:div#unconfirmed-lights
          [:p.small.side-margins
           (if (= (count @unconfirmed-lights-rctn) 1)
             "A new light has been found:"
             "New lights have been found:")]

          (doall
            (for [light @unconfirmed-lights-rctn]
              ^{:key (str "s-" (:id light))}
              [:div.ui.message.side-margins.no-top-margin
               {:on-click #(dispatch [:light/edit (:id light)])}
               [:div.content.flex-row-container
                [:div.align.right.margin-right
                 [:ul
                  [:li.small "name:"]]]
                [:div
                 [:ul
                  [:li (:name light)]]]
                [:div.align.right.flexing
                 [:button.ui.mini.circular.icon.button.item
                  {:on-click #(dispatch [:light/edit (:id light)])}
                  [:i.mini.edit.icon]]]]]))])])))

;// ROOM
;// ********************************
(defn- room []
  (fn []
    (let [room-rctn (subscribe [:view/room {:assemble? true}])
          segment-rctn (subscribe [:view/segment])]
      [:div.room.flex-container.full-height
       [:div#header.margins
        [:h4.floating (:name @room-rctn)]
        ;[:h4 (:name @room-rctn)]
        [:button.ui.right.floated.mini.circular.trash.icon.button
         {:on-click #(dispatch [:room/prepare-trash (:id @room-rctn)])}
         [:i.mini.trash.icon]]
        ]
       [:div#menus.side-margins
        [main-menu room-rctn segment-rctn]
        [secondary-menu room-rctn segment-rctn]
        ]
       [:div.flexing.relative
        (segment @segment-rctn room-rctn)]])))

(defmulti section identity)
(defmethod section :blank [] [blank])
(defmethod section :room [] [room])
(defmethod section :dash [] [dash])
