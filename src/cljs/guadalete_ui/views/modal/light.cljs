(ns guadalete-ui.views.modal.light
  (:require
    [clojure.set :refer [union]]
    [clojure.string :as string]
    [reagent.core :refer [dom-node create-class]]
    [re-frame.core :refer [dispatch subscribe]]
    [thi.ng.color.core :refer [as-css]]
    [guadalete-ui.util :refer [pretty kw*]]
    [guadalete-ui.console :as log]))

;//   _        _
;//  | |_  ___| |_ __ ___ _ _ ___
;//  | ' \/ -_) | '_ \ -_) '_(_-<
;//  |_||_\___|_| .__\___|_| /__/
;//             |_|

(defn- dropdown-id [light-rctn index]
       (str "drop-" (:id @light-rctn) "-" index))

(defn- change-light-name [ev light-rctn]
       (let [name* (-> ev .-target .-value)]
            (log/debug "change-light-name" name*)
            (dispatch [:light/update (assoc @light-rctn :name name*)])))

(defn- light-channel-change [value-strings new-light-rctn index]
       (let [values (->> (string/split value-strings #",")
                         (map #(int %))
                         (remove #(= 0 %))
                         (into []))
             channels (:channels @new-light-rctn)
             channels* (assoc channels index values)]
            (dispatch [:light/update (assoc @new-light-rctn :channels channels*)])))

(defn- init-dropdown
       "Internal helper for initializing a semantic-ui dropdown"
       [this]
       (-> this
           (dom-node)
           (js/$)
           (.dropdown)))

(defn- init-channel-dropdown [_this light-rctn index]
       (-> (js/$ (str "#" (dropdown-id light-rctn index)))
           (.dropdown
             (js-obj "onChange" #(light-channel-change % light-rctn index)))))

(defn- change-light-type [ev new-light-rctn]
       (let [num-channels (-> ev .-target .-value int)]
            (log/debug "change-light-type" num-channels)
            (let [new-type (condp = num-channels
                                  1 :v
                                  2 :sv
                                  3 :hsv
                                  4 :hsv)
                  light* (assoc @new-light-rctn
                                :num-channels num-channels
                                :type new-type)]
                 (dispatch [:light/update light*]))))

(defn- change-room [ev light-rctn]
       (let [room-id (-> ev .-target .-value)
             light* (-> @light-rctn
                        (assoc :room-id room-id)
                        (assoc :accepted? true))]
            (dispatch [:light/update light*])))

;// re-frame
;//   / _|_ _ ___ _ __  ___
;//  |  _| '_/ _ \ '  \(_-<
;//  |_| |_| \___/_|_|_/__/
;//

(defn- channel-selector []
       (fn [new-light-rctn index label available-dmx-rctn]
           (create-class
             {:component-did-mount
              #(init-channel-dropdown % new-light-rctn index)
              :reagent-render
              (fn [light-rctn index label available-dmx-rctn]
                  (let [channel-dmx (set (get-in @light-rctn [:channels index]))
                        dmx-options (sort (into [] (union channel-dmx @available-dmx-rctn)))
                        light-channels (get-in @light-rctn [:channels index])]
                       [:div.flex-row-container
                        [:label {:class label} label]
                        [:div.ui.input.margin-bottom.flexing
                         [:div.ui.fluid.multiple.search.selection.dropdown
                          {:id (dropdown-id light-rctn index)}
                          [:input {:value (string/join "," light-channels)
                                   :name  "dmxs"
                                   :type  "hidden"}]
                          [:i.dropdown.icon]
                          [:div.default.text ""]
                          [:div.menu
                           (doall (for [dmx-index dmx-options]
                                       ^{:key (str "channel-" dmx-index)}
                                       [:div.item {:data-value dmx-index} dmx-index]))]]]]))})))

(defn- channel-selectors []
       (fn [new-light-rctn available-dmx-rctn]
           [:div#channel-selectors
            (when (= 1 (:num-channels @new-light-rctn))
                  [:div.one.channel
                   [channel-selector new-light-rctn 0 "White" available-dmx-rctn]])

            (when (= 2 (:num-channels @new-light-rctn))
                  [:div.two.channels
                   [channel-selector new-light-rctn 0 "Cool" available-dmx-rctn]
                   [channel-selector new-light-rctn 1 "Warm" available-dmx-rctn]])

            (when (= 3 (:num-channels @new-light-rctn))
                  [:div.three.channels
                   [channel-selector new-light-rctn 0 "Red" available-dmx-rctn]
                   [channel-selector new-light-rctn 1 "Green" available-dmx-rctn]
                   [channel-selector new-light-rctn 2 "Blue" available-dmx-rctn]])

            (when (= 4 (:num-channels @new-light-rctn))
                  [:div.four.channels
                   [channel-selector new-light-rctn 0 "Red" available-dmx-rctn]
                   [channel-selector new-light-rctn 1 "Green" available-dmx-rctn]
                   [channel-selector new-light-rctn 2 "Blue" available-dmx-rctn]
                   [channel-selector new-light-rctn 3 "White" available-dmx-rctn]])]))


;//      _
;//   __| |_ __ __ __
;//  / _` | '  \\ \ /
;//  \__,_|_|_|_/_\_\
;//
(defn- transport-dmx
       "Component for configuring the DMX channels of a light
       Using a Form-3 component here, as an eternal js library (semantic-ui) has to be called.
       @see: https://github.com/Day8/re-frame/wiki/Creating%20Reagent%20Components"
       []
       (fn [new-light-rctn available-dmx-rctn]
           (create-class
             {:component-did-mount
              init-dropdown
              :reagent-render
              (fn []
                  [:div#dmx-config
                   [:div.flex-row-container
                    [:label "Type"]
                    [:select.ui.dropdown.margin-bottom.flexing
                     {:name      "type"
                      :on-change #(change-light-type % new-light-rctn)
                      :value     (or (:num-channels @new-light-rctn) "")}
                     [:option {:value 1} "White"]
                     [:option {:value 2} "Two-tone"]
                     [:option {:value 3} "RGB"]
                     [:option {:value 4} "RGBW"]]]

                   ;[channel-selectors new-light-rctn available-dmx-rctn]
                   ])})))

;//              _   _
;//   _ __  __ _| |_| |_
;//  | '  \/ _` |  _|  _|
;//  |_|_|_\__, |\__|\__|
;//           |_|
(defn- transport-mqtt
       "Component for configuring the mqtt lights
       Using a Form-3 component here, as an eternal js library (semantic-ui) has to be called.
       @see: https://github.com/Day8/re-frame/wiki/Creating%20Reagent%20Components"
       []
       (fn [light-rctn rooms-rctn]
           (create-class
             {:component-did-mount
              init-dropdown
              :reagent-render
              (fn []
                  [:div#mqtt-config
                   [:div.flex-row-container
                    [:label "Room"]
                    [:select.ui.dropdown.margin-bottom.flexing
                     {:name      "room"
                      :on-change #(change-room % light-rctn)
                      :value     (or (:room-id @light-rctn) "-")}

                     (if (nil? (:room-id @light-rctn))
                       [:option
                        {:value "-"}
                        (:name "-")])

                     (doall (for [room @rooms-rctn]
                                 ^{:key (str "select-" (:id room))}
                                 [:option
                                  {:value (:id room)}
                                  (:name room)]))]]])})))

;//                _      _
;//   _ __  ___ __| |__ _| |
;//  | '  \/ _ \ _` / _` | |
;//  |_|_|_\___\__,_\__,_|_|
;//
(defn light-modal []
      (create-class
        {:component-did-mount
         (fn [_] ())
         :reagent-render
         (fn []
             (let [light-rctn (subscribe [:modal/item])
                   available-dmx-rctn (subscribe [:dmx/available])
                   rooms-rctn (subscribe [:rooms])]
                  [:div#pd-light-modal
                   [:div.content.ui.form

                    [:h3.centred "Edit light"]
                    [:pre.code (pretty @light-rctn)]

                    ;; name
                    ;; ----------------
                    [:div.flex-row-container
                     [:label "Name"]
                     [:div.ui.input.margin-bottom.flexing
                      [:input {:type      "text"
                               :value     (:name @light-rctn)
                               :on-change #(change-light-name % light-rctn)}]]]

                    ;; transport specific
                    ;; ----------------
                    (condp = (:transport @light-rctn)
                           :dmx [transport-dmx light-rctn available-dmx-rctn]
                           :mqtt [transport-mqtt light-rctn rooms-rctn]
                           (comment "do nothing."))

                    [:div.flex-row-container.right.actions

                     (if (= :mqtt (:transport @light-rctn))
                       [:div.ui.button.block
                        {:on-click #(dispatch [:light/block (:id @light-rctn)])}
                        [:i.ui.ban.icon]
                        "block"]
                       )

                     [:div.ui.button.trash.cancel
                      {:on-click #(dispatch [:light/trash (:id @light-rctn)])}
                      [:i.trash.outline.icon]
                      "trash"]

                     [:div.ui.button.approve
                      {:on-click #(dispatch [:light/ok (:id @light-rctn)])}
                      [:i.ui.check.icon]
                      "ok"]]
                    ]
                   ]))}))
