(ns guadalete-ui.views.admin
  "The login screen."
  (:require
    [cljs-utils.core :refer [by-id]]
    [reagent.core :refer [create-class]]
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as reagent]
    [thi.ng.geom.core.vector :refer [vec2]]
    [guadalete-ui.console :as log]
    [guadalete-ui.util :refer [pretty dimensions]]
    [guadalete-ui.views.modal :refer [modal]]
    [guadalete-ui.views.segments :refer [segment]]
    [guadalete-ui.views.sections :refer [section]]
    [guadalete-ui.views.menu :refer [main-menu secondary-menu]]))


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
  (let [section-rctn (subscribe [:view/section])
        rooms-rctn (subscribe [:rooms])]
    (create-class
      {:component-did-mount
       (fn [_]
         ;; init the sidebar upon mount
         (let [jq-root (js/$ "#root")
               jq-nav (js/$ "#nav")
               jq-view (js/$ "#view")
               jq-header (js/$ "#header")]
           (.sidebar jq-nav
                     (js-obj "context" jq-root
                             "closable" false
                             "dimPage" false))
           (.sidebar jq-nav "setting" "transition" "overlay")
           (.sidebar jq-nav "push page")

           (dispatch [:view/dimensions {:root   (dimensions jq-root)
                                        :view   (dimensions jq-view)
                                        :header (dimensions jq-header)}])))

       ;; for more helpful warnings & errors
       :display-name
       "root/admin"

       :reagent-render
       (fn []
         [:div#root.attached.segment.pushable
          [:div#nav.ui.visible.thin.sidebar.inverted.vertical.menu
           [:img {:src "images/logo.svg"}]
           (doall
             (for [room @rooms-rctn]
               (let
                 ; OBACHT the '#' is important, since without it the whole page gets relaoded
                 [room-link (str "#/room/" (:id room))]
                 ^{:key (str "r-" (:id room))}
                 [:a.item {:href room-link} (:name room)])))
           [:a.item {:on-click #(.modal (js/$ "#new-room.modal") "show")}
            [:i.add.circle.icon]]]
          [:div#view.pusher
           (section @section-rctn)
           ]
          [modal]
          ])})))