(ns guadalete-ui.views.menu
  (:require
    [re-frame.core :refer [subscribe]]
    [guadalete-ui.console :as log]
    [guadalete-ui.util :refer [pretty]]))

(defn- active-segment? [active current]
  (if (= active current) "active" ""))


(defn main-menu []
  (fn [room-rctn segment-rctn]
    (let [room-id (:id @room-rctn)
          scene-link (str "#/room/" room-id "/scene")       ; OBACHT the '#' ist important, since without it the whole page gets relaoded
          light-link (str "#/room/" room-id "/light")
          switch-link (str "#/room/" room-id "/switch")
          dmx-link (str "#/room/" room-id "/dmx")
          signal-link (str "#/room/" room-id "/signal")]
      [:div.ui.pointing.menu.inverted.secondary
       [:a.item {:href scene-link :class (active-segment? @segment-rctn :scene)} "scenes"]
       [:a.item {:href light-link :class (active-segment? @segment-rctn :light)} "lights"]
       [:a.item {:href switch-link :class (active-segment? @segment-rctn :switch)} "switches"]
       [:a.item {:href dmx-link :class (active-segment? @segment-rctn :dmx)} "dmx"]
       [:a.item {:href signal-link :class (active-segment? @segment-rctn :signal)} "signals"]])))


(defn- scene-menu []
  (fn [room-rctn]
    (let [scenes (:scene @room-rctn)
          scene-rctn (subscribe [:current/scene])]
      [:div.ui.tier-3.inverted.menu
       (doall
         (for [s scenes]
           (let [scene-link (str "#/room/" (:id @room-rctn) "/scene/" (:id s))
                 current? (= (:id s) (:id @scene-rctn))]
             ^{:key (str "s-" (:id s))}
             [:a.item
              {:href  scene-link
               :class (if current? "active")}
              (:name s)])))])))

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