(ns guadalete-ui.views.modal
  (:require-macros
    [cljs.core.async.macros :refer [go go-loop]])
  (:require
    [cljs.core.async :refer [<! >! put! chan close!]]
    [reagent.core :refer [dom-node]]
    [re-frame.core :refer [dispatch subscribe]]
    [thi.ng.color.core :refer [as-css]]
    [guadalete-ui.util :refer [pretty]]
    [guadalete-ui.console :as log]
    [guadalete-ui.pd.color :as color :refer [color-widget render-color]]))


(defn- approve-modal [_]
       (let [])
       (log/debug "approve-modal")
       (dispatch [:pd/modal-approve])
       true)

(defn- deny-modal [_]
       (log/debug "deny-modal")
       (dispatch [:pd/modal-deny])
       true)

(defn- open [modal-id]
       (let [jq-node (js/$ (str "#" (name modal-id) ".modal"))
             options {:onDeny     deny-modal
                      :onApprove  approve-modal
                      :closable   false
                      :dimPage    true
                      :detachable false
                      :context    "#modals"
                      }
             js-options (clj->js options)]
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
            [:div.ui.button "cancel"]
            [:div.ui.button "ok"]]]))

(defn- light-modal-change [ev node]
       (let [item-id (-> ev
                         (.-currentTarget)
                         (.-value))]
            (dispatch [:pd/link-modal-node {:item-id item-id}])
            (close :pd-light-node)))

(defn- color-type-change [ev node]
       (let [new-type (-> ev
                          (.-currentTarget)
                          (.-value))
             node-color (color/from-id (:item-id node))
             node-color* (assoc node-color :type new-type)
             color-id* (color/make-id (:color node-color*) (:type node-color*))]
            (dispatch [:pd/link-modal-node {:item-id color-id*}])))



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
                 [:select.ui.dropdown
                  {:name      "item"
                   :on-change #(light-modal-change % @node-rctn)}
                  (if (nil? @item-rctn)
                    ^{:key (str "light-select-nil")}
                    [:option {:value "nil"} "-"])

                  (doall (for [an-option @select-options-rctn]
                              ^{:key (str "select-" (:id an-option))}
                              [:option
                               {:value (:id an-option)}
                               (:name an-option)]))]]
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
                [:div.header
                 [:div.color-indicator
                  {:style {:background-color color}}]]
                [:div.content
                 [:div.ui.form
                  [:select.ui.dropdown
                   {:name      "color"
                    :value     (:type @item-rctn)
                    :on-change #(color-type-change % @node-rctn)}
                   [:option {:value "w"} "White"]
                   [:option {:value "ww"} "Two tone white"]
                   [:option {:value "rgb"} "RGB"]]]

                 [(with-meta identity
                             {:component-did-mount
                              (fn [_]
                                  (go-loop []
                                           (let [c (<! channel)
                                                 color-id (color/make-id c (:type @item-rctn))]
                                                (if (not= (:item-id @node-rctn) color-id)
                                                  (dispatch [:pd/link-modal-node {:item-id color-id}])))
                                           (recur)))
                              ;:component-will-unmount #(close! channel)
                              })
                  [color-widget item-rctn channel]]]
                [:div.actions
                 [:div.ui.button.deny
                  "close"]]])))


(defn modals []
      (fn []
          [:div#modals.ui.modals.dimmer.page
           [new-room-modal]
           [pd-light-modal]
           [pd-color-modal]
           ]))