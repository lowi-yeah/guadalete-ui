(ns guadalete-ui.views.admin
  "The login screen."
  (:require
    [cljs-utils.core :refer [by-id]]
    [reagent.core :refer [create-class]]
    [re-frame.core :refer [subscribe]]
    [reagent.core :as reagent]
    [guadalete-ui.console :as log]
    [guadalete-ui.util :refer [pretty]]
    [guadalete-ui.views.modal :refer [modals]]
    [guadalete-ui.views.segments :refer [segment]]
    [guadalete-ui.views.menu :refer [main-menu secondary-menu]]))


;//       _
;//  __ ___)_____ __ _____
;//  \ V / / -_) V  V (_-<
;//   \_/|_\___|\_/\_//__/
;//

;// BLANK
;// ********************************
(defn blank-view [] [:div#blank])


;// ROOM
;// ********************************

(defn room-view []
  (fn []
    (let [room-rctn (subscribe [:current/room {:assemble true}])
          segment-rctn (subscribe [:current/segment])]
      [:div.room.flex-container.full-height
       [:div#header.margins.flex-row-container
        [:h2 (:name @room-rctn)]
        [:div#menus.flexing
         [main-menu room-rctn segment-rctn]
         [secondary-menu room-rctn segment-rctn]]]
       [:div.flexing.relative
        (segment @segment-rctn room-rctn)
        ]])))

(defmulti view identity)
(defmethod view :blank [] [blank-view])
(defmethod view :room [] [room-view])

;//               _
;//   _ _ ___ ___| |_
;//  | '_/ _ \ _ \  _|
;//  |_| \___\___/\__|
;//

(defn root-panel
  "Root component for :admin.
  Using a Form-3 component here, as an eternal js library (semantic-ui) has to be initialized.
  @see: https://github.com/Day8/re-frame/wiki/Creating%20Reagent%20Components"
  []
  (let [view-rctn (subscribe [:current/view])
        rooms-rctn (subscribe [:rooms])]
    (create-class
      {:component-did-mount
       (fn [_]
         (let [jq-root (js/$ "#root")
               jq-nav (js/$ "#nav")]
           (.sidebar jq-nav
                     (js-obj "context" jq-root
                             "closable" false
                             "dimPage" false))
           (.sidebar jq-nav "setting" "transition" "overlay")
           (.sidebar jq-nav "push page")))

       ;; for more helpful warnings & errors
       :display-name
       "root/admin"

       :reagent-render
       (fn []
         [:div#root.attached.segment.pushable
          [:div#nav.ui.visible.thin.sidebar.inverted.vertical.menu
           (doall
             (for [room @rooms-rctn]
               (let
                 ; OBACHT the '#' is important, since without it the whole page gets relaoded
                 [room-link (str "#/room/" (:id room))]
                 ^{:key (str "r-" (:id room))}
                 [:a.item {:href room-link} (:name room)])))
           [:a.item {:on-click #(.modal (js/$ "#new-room.modal") "show")}
            [:i.large.add.circle.icon]]]
          [:div#view.pusher
           (view @view-rctn)]
          ;[modals]
          ])})))