(ns guadalete-ui.views.modal
  (:require-macros
    [cljs.core.async.macros :refer [go go-loop]]
    [reagent.ratom :refer [reaction]])
  (:require
    [clojure.set :refer [difference]]
    [clojure.set :refer [union]]
    [cljs.core.async :refer [<! >! put! chan close!]]
    [reagent.core :refer [create-class dom-node]]
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




(defmulti pd-node-modal* (fn [type node-rctn item-rctn] type))


(defmethod pd-node-modal* :light
  [_ node-rctn]
  (log/debug "pd-node-modal* :light")
  )

(defmethod pd-node-modal* :default
  [_ node-rctn]
  [:h1 ":default"])



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
  )


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

(defn- pd-select-change [ev data]
  (let [new-id (-> ev (.-currentTarget) (.-value))
        data* (assoc data :new-id new-id)
        data** (assoc data :item-id new-id)]
    (dispatch [:pd/register-node data*])
    (dispatch [:modal/update data**])))


(defn- color-type-change [ev data]
  (let [new-type (-> ev (.-currentTarget) (.-value))
        data* (assoc data :new-type new-type)]
    (dispatch [:color/change-type data*])))

;//      _                _
;//   ____)__ _ _ _  __ _| |
;//  (_-< / _` | ' \/ _` | |
;//  /__/_\__, |_||_\__,_|_|
;//       |___/
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
            [:div.header.centred.margin-bottom
             [:h2 "Select signal"]]
            [:div.content.ui.form.centred
             [:select.ui.dropdown
              {:id        id
               :name      "item"
               :on-change #(pd-select-change % @data-rctn)
               :value     (:item-id @data-rctn)}
              (doall (for [an-option (vals @options-rctn)]
                       ^{:key (str "select-" (:id an-option))}
                       [:option
                        {:value (:id an-option)}
                        (:name an-option)]))]]]))})))

;//          _
;//   __ ___| |___ _ _
;//  / _/ _ \ / _ \ '_|
;//  \__\___/_\___/_|
;//
(defn- pd-color []
  (let [channel (chan)
        id (str (random-uuid))
        data-rctn (subscribe [:modal/data])
        node-rctn (subscribe [:modal/node])
        item-rctn (subscribe [:modal/item])]

    (create-class
      {
       :component-did-mount
       (fn [_]
         (-> (str "#" id) (js/$) (.dropdown)))
       ;(fn [_]
       ;(go-loop []
       ;         (let [c (<! channel)
       ;               color-id (color/make-id c (:type @item-rctn))]
       ;           (if (not= (:item-id @node-rctn) color-id)
       ;             (dispatch [:modal/register-node {:item-id color-id}])))
       ;         (recur)))
       :reagent-render
       (fn []
         (let [color "#3B3F44"]
           [:div#color-modal
            [:div.header.centred.margin-bottom
             [:h2 "Adjust color"]
             [:pre.code (pretty @item-rctn)]]
            [:div.content.centred
             [:div.color-indicator
              {:style {:background-color color}}]
             [:div.flex-row-container
              [:div.ui.form.flexing
               [:select.ui.dropdown
                {:id        id
                 :name      "color"
                 :value     (:type @item-rctn)
                 :on-change #(color-type-change % @data-rctn)}
                [:option {:value :v} "White"]
                [:option {:value :sv} "Two tone white"]
                [:option {:value :hsv} "RGB"]]]]
             ;[color-widget item-rctn channel]
             ]]))})))

;//   _ _      _   _
;//  | (_)__ _| |_| |_
;//  | | / _` | ' \  _|
;//  |_|_\__, |_||_\__|
;//      |___/

(defn- pd-light []
  (let [id (str (random-uuid))]
    (create-class
      {:component-did-mount
       (fn [_] (-> (str "#" id) (js/$) (.dropdown)))

       :reagent-render
       (fn []
         (let [data-rctn (subscribe [:modal/data])
               item-rctn (subscribe [:modal/item])
               ;; we want to show only those lights as select-options, which are not yet being used in the scene.
               ;; for that we first get the ids of all lights belonngiong to that room and we're gonna subtract
               ;; from these the ids of those lights alread used in the scene
               options-rctn (subscribe [:light/unused-by-scene (:room-id @data-rctn) (:scene-id @data-rctn)])
               options* (->> (conj @options-rctn @item-rctn)
                             (sort-by :name))
               ]
           [:div#pd-light-modal
            [:div.header.centred.margin-bottom
             [:h2 "Select light"]]
            [:div.content.ui.form.centred
             [:select.ui.dropdown
              {:id        id
               :name      "item"
               ;; obacht!
               :on-change #(pd-select-change % @data-rctn)
               :value     (:item-id @data-rctn)}
              (doall (for [an-option options*]
                       ^{:key (str "select-" (:id an-option))}
                       [:option
                        {:value (:id an-option)}
                        (:name an-option)]))]]]))})))

(defn modal []
  (fn []
    (let [data-rctn (subscribe [:modal/data])
          modal-type (:modal-type @data-rctn)]
      [:div#modal.thing.ui.basic.modal.small
       [:div.content
        (condp = modal-type
          :pd/signal [pd-signal]
          :pd/color [pd-color]
          :pd/light [pd-light]
          :light [light-modal]
          [:div.empty])]
       [:div.actions
        [:div.ui.button.cancel "close"]]])))