(ns guadalete-ui.views.modal
  (:require-macros
    [cljs.core.async.macros :refer [go go-loop]])
  (:require
    [clojure.string :as string]
    [clojure.set :refer [union]]
    [cljs.core.async :refer [<! >! put! chan close!]]
    [reagent.core :as reagent :refer [create-class dom-node]]
    [re-frame.core :refer [dispatch subscribe]]
    [thi.ng.color.core :refer [as-css]]
    [guadalete-ui.views.modal.light :refer [light-modal]]
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
          {:name  "item"
           ;; obacht!
           ;:on-change #(light-modal-change % @node-rctn)
           :value (:item-id @node-rctn)}

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
     ;[new-room-modal]
     [light-modal]
     ;[confirm-trash-modal]
     ;[pd-light-modal]
     ;[pd-signal-modal]
     ;[pd-color-modal]
     ]))





;//
;//   _ _  _____ __ __  _ _  _____ __ __  _ _  _____ __ __
;//  | ' \/ -_) V  V / | ' \/ -_) V  V / | ' \/ -_) V  V /
;//  |_||_\___|\_/\_/  |_||_\___|\_/\_/  |_||_\___|\_/\_/
;//

(defn- signal-change [ev data]

  (let [new-signal-id (-> ev
                (.-currentTarget)
                (.-value))
        data* (assoc data :new-id new-signal-id)]
    (dispatch [:pd/register-node data*])))

(defn- pd-signal
  "Modal for pd signal nodes"
  []
  (let [id (str (random-uuid))]
    (create-class
      {:component-did-mount
       (fn [_]
         (-> (str "#" id) (js/$) (.dropdown)))
       :reagent-render
       (fn []
         (let [data-rctn (subscribe [:modal/data])
               options-rctn (subscribe [:modal/same-ilk-items])]
           [:div#signal-modal
            [:div.header "Signal"]
            [:div.content.ui.form
             [:select.ui.dropdown
              {:id        id
               :name      "item"
               ;; obacht!
               :on-change #(signal-change % @data-rctn)
               :value     (:item-id @data-rctn)}
              (doall (for [an-option (vals @options-rctn)]
                       ^{:key (str "select-" (:id an-option))}
                       [:option
                        {:value (:id an-option)}
                        (:name an-option)]))]]]))})))

(defn- pd-color []
  (fn []
    [:h2 "Kolor!"]))

(defn modal []
  (fn []
    (let [data-rctn (subscribe [:modal/data])
          modal-type (:modal-type @data-rctn)
          data-rctn (subscribe [:modal/data])]
      [:div#modal.thing.ui.basic.modal.small
       [:div.content
        (condp = modal-type
          :pd/signal [pd-signal]
          :pd/color [pd-color]
          [:div.empty])

        [:pre.code (pretty @data-rctn)]
        ]
       [:div.actions
        [:div.ui.button.cancel "close"]]])))