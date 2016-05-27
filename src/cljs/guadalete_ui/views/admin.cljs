(ns guadalete-ui.views.admin
  "The login screen."
  (:require
    [re-frame.core :as re-frame]
    [reagent.core :as reagent]
    [guadalete-ui.console :as log]
    [guadalete-ui.util :refer [pretty]]
    [guadalete-ui.views.modal :refer [new-room-modal]]
    [guadalete-ui.views.segments :refer [segment]]
    ))


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
(defn- active-segment? [active current]
       (if (= active current) "active" ""))

(defn room-view []
      (fn []
          (let [room-rctn (re-frame/subscribe [:current/room {:assemble true}])
                segment-rctn (re-frame/subscribe [:current/segment])
                db-rctn (re-frame/subscribe [:db])
                scene-link (str "#/room/" (:id @room-rctn) "/scene") ; OBACHT the '#' ist important, since without it the whole page gets relaoded
                light-link (str "#/room/" (:id @room-rctn) "/light")
                switch-link (str "#/room/" (:id @room-rctn) "/switch")]
               [:div.room.flex-container.full-height
                [:div.margins
                 [:h3 (:name @room-rctn)]
                 [:div.ui.pointing.menu.inverted
                  [:a.item {:href scene-link :class (active-segment? @segment-rctn :scene)} "scenes"]
                  [:a.item {:href light-link :class (active-segment? @segment-rctn :light)} "lights"]
                  [:a.item {:href switch-link :class (active-segment? @segment-rctn :switch)} "switches"]]]
                (segment @segment-rctn room-rctn)
                ])))

(defmulti view identity)
(defmethod view :blank [] [blank-view])
(defmethod view :room [] [room-view])

;//               _
;//   _ _ ___ ___| |_
;//  | '_/ _ \ _ \  _|
;//  |_| \___\___/\__|
;//
(defn root-panel
      "root component for :role/admin"
      []
      (fn []
          (let [view-rctn (re-frame/subscribe [:current/view])
                rooms-rctn (re-frame/subscribe [:rooms])]
               [(with-meta identity
                           {:component-did-mount (fn [this]
                                                     (.sidebar (js/$ "#nav")
                                                               (js-obj "context" (js/$ "#root")
                                                                       "closable" false
                                                                       "dimPage" false))
                                                     (.sidebar (js/$ "#nav") "push page"))})
                [:div#root
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
                 [:div#modals
                  [new-room-modal]]
                 ]])))
