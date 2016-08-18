(ns guadalete-ui.views.menu
  (:require
    [reagent.core :refer [create-class]]
    [re-frame.core :refer [subscribe dispatch]]
    [guadalete-ui.console :as log]
    [guadalete-ui.util :refer [pretty]]))

(defn- active-segment? [active current]
  (if (= active current) "active" ""))


(defn- init-dropdown [dropdown-id]
  (-> (js/$ (str "#" dropdown-id))
      (.dropdown
        (js-obj
          ;"onChange" #(light-channel-change % light-rctn index)
          ;"direction" "upward"
          "showOnFocus" false
          "forceSelection" false
          "keepOnScreen" false
          "action" "hidden"
          "debug" true
          ;"silent" false
          "tabbable" false
          )
        )))

(defn main-menu []
  (fn [room-rctn segment-rctn]
    (let [
          room-id (:id @room-rctn)
          ;; OBACHT the '#' ist important, since without it the whole page gets relaoded
          dash-link (str "#/room/" room-id "/dash")
          scene-id (-> @room-rctn (get :scene) first (get :id))
          scene-link (str "#/room/" room-id "/scene/" scene-id)
          light-link (str "#/room/" room-id "/light")
          switch-link (str "#/room/" room-id "/switch")
          dmx-link (str "#/room/" room-id "/dmx")
          debug-link (str "#/room/" room-id "/debug")
          signal-link (str "#/room/" room-id "/signal")
          scenes (:scene @room-rctn)]
      [:div.ui.menu.inverted
       [:a.item {:href dash-link :class (active-segment? @segment-rctn :dash)} "dash"]
       [:a.item {:href scene-link :class (active-segment? @segment-rctn :scene)} "scenes"]
       [:a.item {:href light-link :class (active-segment? @segment-rctn :light)} "lights"]
       [:a.item {:href switch-link :class (active-segment? @segment-rctn :switch)} "switches"]
       ;[:a.item {:href dmx-link :class (active-segment? @segment-rctn :dmx)} "dmx"]
       [:a.item {:href signal-link :class (active-segment? @segment-rctn :signal)} "signals"]
       [:a.item {:href debug-link :class (active-segment? @segment-rctn :debug)} "debug"]])))

(defn- scene-menu []
  (fn [room-rctn]
    (let [scenes (:scene @room-rctn)
          scene-rctn (subscribe [:view/scene])]

      [:div#scene-menu
       [:div.ui.secondary.inverted.menu.pointing.floating
        (doall
          (for [s scenes]
            (let [scene-link (str "#/room/" (:id @room-rctn) "/scene/" (:id s))
                  current? (= (:id s) (:id @scene-rctn))]
              ^{:key (str "s-" (:id s))}
              [:a.item
               {:href  scene-link
                :class (if current? "active")}
               (:name s)])))
         [:button#make-scene.ui.mini.circular.icon.button.item
          {:on-click #(dispatch [:scene/make (:id @room-rctn)])}
          [:i.mini.add.circle.icon]]
        ]
       [:button#edit-scene.ui.right.floated.mini.circular.icon.button
        {:on-click #(dispatch [:scene/edit (:id @scene-rctn)])}
        [:i.mini.edit.icon]]
       ]
      )))

(defn secondary-menu []
  (fn [room-rctn segment-rctn]
    (condp = @segment-rctn
      :scene [scene-menu room-rctn]
      ;:light (log/debug "light!")
      ;:switch (log/debug "switch!")
      ;:signal (log/debug "signal!")
      (do
        ;(log/error (str "Sorry, I don't know the segment type " @segment-rctn))
        [:div.invisible]))))