(ns guadalete-ui.views.modal)

(defn new-room-modal []
      (fn []
          [:div#new-room.ui.modal
           [:i.close.icon]
           [:div.header "Create a new room"]
           [:div.actions
            [:div.ui.button "cancel"]
            [:div.ui.button "ok"]]]))
