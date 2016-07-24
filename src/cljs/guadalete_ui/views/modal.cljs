(ns guadalete-ui.views.modal
  (:require-macros
    [cljs.core.async.macros :refer [go go-loop]])
  (:require
    [clojure.string :as string]
    [clojure.set :refer [union]]
    [cljs.core.async :refer [<! >! put! chan close!]]
    [reagent.core :as reagent :refer [dom-node]]
    [re-frame.core :refer [dispatch subscribe]]
    [thi.ng.color.core :refer [as-css]]
    [guadalete-ui.util :refer [pretty kw*]]
    [guadalete-ui.console :as log]
    [guadalete-ui.pd.color :as color :refer [color-widget render-color]]))


(defn- approve-modal [modal-id]
  (dispatch [:modal/approve modal-id])
  true)

(defn- deny-modal [_]
  (dispatch [:modal/deny])
  true)

(defn open
  [modal-id options]
  (let [jq-node (js/$ (str "#" (name modal-id) ".modal"))
        options* (merge
                   {:onDeny        deny-modal
                    :onApprove     #(approve-modal modal-id)
                    :closable      false
                    :dimPage       true
                    :detachable    false
                    :context       "#modals"
                    :allowMultiple true}
                   options)
        js-options (clj->js options*)]
    (.modal jq-node js-options)
    (.modal jq-node "show")))


(defn- close [modal-id]
  (let [jq-node (js/$ (str "#" (name modal-id) ".modal"))]
    (.modal jq-node "hide")))

(defn new-room-modal []
  (fn []
    [:div#new-room.ui.modal
     [:i.close.icon]
     [:div.header "Create a new room"]
     [:div.actions
      [:div.ui.button.deny "cancel"]
      [:div.ui.button.approve "make!"]]]))


(defn- light-channel-change [value-strings new-light-rctn index]
  (let [values (->> (string/split value-strings #",")
                    (map #(int %))
                    (remove #(= 0 %))
                    (into []))
        channels (:channels @new-light-rctn)
        channels* (assoc channels index values)]
    (dispatch [:light/update (assoc @new-light-rctn :channels channels*)])))

(defn change-light-name [ev new-light-rctn]
  (let [name* (-> ev .-target .-value)]
    (log/debug "change-light-name" name*)
    (dispatch [:light/update (assoc @new-light-rctn :name name*)])))

(defn- change-light-transport [ev new-light-rctn]
  (let [transport* (-> ev .-target .-value kw*)]
    (dispatch [:light/update (assoc @new-light-rctn :transport transport*)])))

(defn- change-light-channel-count [ev new-light-rctn]
  (let [num-channels* (-> ev .-target .-value int)]
    (dispatch [:light/update (assoc @new-light-rctn :num-channels num-channels*)])))


(defn- init-dropdown [this]
  (-> this
      (reagent/dom-node)
      (js/$)
      (.dropdown)))

(defn- init-channel-dropdown [this new-light-rctn index]
  (-> this
      (reagent/dom-node)
      (js/$)
      (.dropdown
        (js-obj "onChange" #(light-channel-change % new-light-rctn index)))))

;function(text, value){}

(defn- channel-selector []
  (fn [new-light-rctn index label available-dmx-rctn]
    (let [channel-dmx (set (get-in @new-light-rctn [:channels index]))
          dmx-options (sort (into [] (union channel-dmx @available-dmx-rctn)))]
      [:div.flex-row-container
       [:label {:class label} label]
       [:div.ui.input.margin-bottom.flexing
        [(with-meta identity {:component-did-mount #(init-channel-dropdown % new-light-rctn index)})
         ;[:select.ui.fluid.multiple.selection.dropdown
         [:div.ui.fluid.multiple.search.selection.dropdown
          [:input {:value (string/join "," (get-in @new-light-rctn [:channels index]))
                   :name  "dmxs"
                   :type  "hidden"}]
          [:i.dropdown.icon]
          [:div.default.text ""]
          [:div.menu
           (doall (for [dmx-index dmx-options]
                    ^{:key (str "channel-" dmx-index)}
                    [:div.item {:data-value dmx-index} dmx-index]))]]]]])))

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
        [channel-selector new-light-rctn 0 "Red"]
        [channel-selector new-light-rctn 1 "Green"]
        [channel-selector new-light-rctn 2 "Blue"]
        [channel-selector new-light-rctn 3 "White"]])]))



(defn transport-dmx
  "Component for configuring the DMX channels of a light"
  []
  (fn [new-light-rctn available-dmx-rctn]
    (let []
      [:div#dmx-config
       [:div.flex-row-container
        [:label "Color"]
        [(with-meta identity {:component-did-mount init-dropdown})
         [:select.ui.dropdown.margin-bottom.flexing
          {:name         "num-channels"
           :verbose      true
           :keepOnScreen false
           :on-change    #(change-light-channel-count % new-light-rctn)
           :value        (:num-channels @new-light-rctn)}
          [:option {:value 1} "White"]
          [:option {:value 2} "Two-tone"]
          [:option {:value 3} "RGB"]
          [:option {:value 4} "RGBW"]]]]

       [channel-selectors new-light-rctn available-dmx-rctn]
       ])))

(defn transport-mqtt
  "Component for configuring the DMX channels of a light"
  []
  (fn [new-light-rctn]
    [:p "mqtt configuration goes here…"]))

(defn light-modal []
  (fn []
    (let [light-rctn (subscribe [:current/light])
          available-dmx-rctn (subscribe [:dmx/available])]
      [:div#edit-light.thing.ui.basic.modal.small
       [:div.content.ui.form

        [:div.flex-row-container.right
         [:div.circular.ui.icon.button.trash
          {:on-click #(dispatch [:light/prepare-trash (:id @light-rctn)])}
          [:i.trash.outline.icon]]]

        ; name
        ; ----------------
        [:div.flex-row-container
         [:label "Name"]
         [:div.ui.input.margin-bottom.flexing
          [:input {:type      "text"
                   :value     (:name @light-rctn)
                   :on-change #(change-light-name % light-rctn)}]]]

        ; transport
        ; ----------------
        ;[:div.flex-row-container
        ; [:label "Transport"]
        ; [:select.ui.dropdown.margin-bottom
        ;  {:name      "transport"
        ;   :on-change #(change-light-transport % new-light-rctn)
        ;   :value     (:transport @new-light-rctn)
        ;   }
        ;  [:option {:value "dmx"} "DMX"]
        ;  [:option {:value "mqtt"} "MQTT"]]
        ; ]

        [transport-dmx light-rctn available-dmx-rctn]
        ;(condp = (:transport @new-light-rctn)
        ;       :dmx
        ;       :mqtt [transport-mqtt new-light-rctn]
        ;       (comment "do nothing."))
        ]
       [:div.actions
        [:div.ui.button.cancel "close"]
        ;[:div.ui.button.approve "make!"]
        ]
       [:pre.debug (pretty @light-rctn)]])))

(defn- light-modal-change [ev node]
  (let [item-id (-> ev
                    (.-currentTarget)
                    (.-value))]
    (dispatch [:modal/register-node {:item-id item-id}])
    (close :pd-light-node)))

(defn- color-type-change [ev node]
  (let [new-type (-> ev
                     (.-currentTarget)
                     (.-value))
        node-color (color/from-id (:item-id node))
        node-color* (assoc node-color :type new-type)
        color-id* (color/make-id (:color node-color*) (:type node-color*))]
    (dispatch [:modal/register-node {:item-id color-id*}])))



(defmulti pd-node-modal* (fn [type node-rctn item-rctn] type))


(defmethod pd-node-modal* :light
  [_ node-rctn]
  (log/debug "pd-node-modal* :light")
  )

(defmethod pd-node-modal* :default
  [_ node-rctn]
  [:h1 ":default"])

(defn pd-light-modal
  "Modal for pd light nodes"
  []
  (fn []
    (let [node-rctn (subscribe [:pd/modal-node])
          item-rctn (subscribe [:pd/modal-item])
          select-options-rctn (subscribe [:pd/modal-select-options])]
      [:div#pd-light-node.ui.basic.modal.small
       [:div.header "Light"]
       [:div.content.ui.form
        [(with-meta identity
                    {:component-did-mount
                     (fn [this]
                       (-> this
                           (reagent/dom-node)
                           (js/$)
                           (.dropdown)))})
         [:select.ui.dropdown
          {:name      "item"
           :on-change #(light-modal-change % @node-rctn)
           :value     (:item-id @node-rctn)}

          ;(if (nil? @item-rctn)
          ;  ^{:key (str "light-select-nil")}
          ;  [:option {:value "nil"} "-"])

          (doall (for [an-option @select-options-rctn]
                   ^{:key (str "select-" (:id an-option))}
                   [:option
                    {:value (:id an-option)}
                    (:name an-option)]))]]

        ]
       [:div.actions
        [:div.ui.button.deny
         "close"]]])))

(defn pd-signal-modal
  "Modal for pd signal nodes"
  []
  (fn []
    (let [node-rctn (subscribe [:pd/modal-node])
          item-rctn (subscribe [:pd/modal-item])
          select-options-rctn (subscribe [:pd/modal-select-options])
          ]
      (log/debug "node" @node-rctn)
      (log/debug "item" @item-rctn)
      [:div#pd-signal-node.ui.basic.modal.small
       [:div.header "Signal"]
       [:pre.code (pretty @node-rctn)]
       [:pre.code (pretty @item-rctn)]
       [:div.actions
        [:div.ui.button.deny
         "close"]]])))

(defn pd-color-modal
  "Modal for pd color nodes"
  []
  (fn []
    (let [node-rctn (subscribe [:pd/modal-node])
          item-rctn (subscribe [:pd/modal-item])
          channel (chan)
          color (if @item-rctn
                  @(as-css (render-color @item-rctn))
                  "#3B3F44")]
      [:div#pd-color-node.ui.basic.modal.small
       [:div.content
        [:div.flex-row-container
         [:div.color-indicator
          {:style {:background-color color}}]
         [:div.ui.form.flexing
          [:select.ui.dropdown
           {:name      "color"
            :value     (:type @item-rctn)
            :on-change #(color-type-change % @node-rctn)}
           [:option {:value "w"} "White"]
           [:option {:value "ww"} "Two tone white"]
           [:option {:value "rgb"} "RGB"]]]]

        [(with-meta identity
                    {:component-did-mount
                     (fn [_]
                       (go-loop []
                                (let [c (<! channel)
                                      color-id (color/make-id c (:type @item-rctn))]
                                  (if (not= (:item-id @node-rctn) color-id)
                                    (dispatch [:modal/register-node {:item-id color-id}])))
                                (recur)))
                     ;:component-will-unmount #(close! channel)
                     })
         [color-widget item-rctn channel]]]

       [:div.actions
        [:div.ui.button.deny
         "close"]]])))


(defn confirm-trash-modal []
  (fn []
    [:div#trash-item.ui.basic.modal.small
     [:div.actions
      [:div.ui.button.approve
       {:on-click #(dispatch [:item/trash :bar "foo"])} "Trash"]
      ]]))


(defn modals []
  (fn []
    [:div#modals.ui.modals.dimmer.page
     [new-room-modal]
     [light-modal]
     [confirm-trash-modal]
     [pd-light-modal]
     [pd-signal-modal]
     [pd-color-modal]]))