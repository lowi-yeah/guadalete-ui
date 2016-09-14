(ns guadalete-ui.views.sections
  (:require
    [re-frame.core :refer [dispatch subscribe]]
    [guadalete-ui.views.segments :refer [segment]]
    [guadalete-ui.views.menu :refer [main-menu secondary-menu]]
    [guadalete-ui.console :as log]))


;// BLANK
;// ********************************
(defn- blank [] [:div#blank])

;// DASH
;// ********************************
(defn- dash []
  [:div#dash
                [:h1 "dash…"]])

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
