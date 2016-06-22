(ns guadalete-ui.views.admin
  "The login screen."
  (:require
    [re-frame.core :refer [subscribe]]
    [reagent.core :as reagent]
    [guadalete-ui.console :as log]
    [guadalete-ui.util :refer [pretty]]
    [guadalete-ui.views.modal :refer [modals]]
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
          (let [room-rctn (subscribe [:current/room {:assemble true}])
                segment-rctn (subscribe [:current/segment])
                db-rctn (subscribe [:db])
                scene-link (str "#/room/" (:id @room-rctn) "/scene") ; OBACHT the '#' ist important, since without it the whole page gets relaoded
                light-link (str "#/room/" (:id @room-rctn) "/light")
                switch-link (str "#/room/" (:id @room-rctn) "/switch")
                dmx-link (str "#/room/" (:id @room-rctn) "/dmx")]
               [:div.room.flex-container.full-height
                [:div#header.margins
                 [:h3 (:name @room-rctn)]
                 [:div.ui.pointing.menu.inverted
                  [:a.item {:href scene-link :class (active-segment? @segment-rctn :scene)} "scenes"]
                  [:a.item {:href light-link :class (active-segment? @segment-rctn :light)} "lights"]
                  [:a.item {:href switch-link :class (active-segment? @segment-rctn :switch)} "switches"]
                  [:a.item {:href dmx-link :class (active-segment? @segment-rctn :dmx)} "dmx"]
                  ]]
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
          (let [view-rctn (subscribe [:current/view])
                rooms-rctn (subscribe [:rooms])
                selected-nodes-rctn (subscribe [:selected])]
               [(with-meta identity
                           {:component-did-mount
                            (fn [this]
                                (let [options {:context  (js/$ "#root")
                                               :closable false
                                               :dimPage  false}
                                      js-options (clj->js options)
                                      sidebar-ids ["#nav" "#detail"]]

                                     (doall (for [sid sidebar-ids]
                                                 (let [jq (js/$ sid)]
                                                      (.sidebar jq js-options)
                                                      (.sidebar jq "setting" "transition" "overlay")
                                                      (.sidebar jq "push page"))))))})
                [:div#root.pushable

                 [:div#detail.ui.inverted.bottom.sidebar
                  (when (not-empty @selected-nodes-rctn)
                        [:pre.code (pretty @selected-nodes-rctn)])]

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

                 [modals]

                 ]])))
