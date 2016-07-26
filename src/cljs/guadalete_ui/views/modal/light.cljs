(ns guadalete-ui.views.modal.light
  (:require
    [clojure.string :as string]
    [reagent.core :refer [dom-node]]
    [re-frame.core :refer [dispatch subscribe]]
    [thi.ng.color.core :refer [as-css]]
    [guadalete-ui.util :refer [pretty kw*]]
    [guadalete-ui.console :as log]))

(defn- change-light-name [ev light-rctn]
  (let [name* (-> ev .-target .-value)]
    (log/debug "change-light-name" name*)
    (dispatch [:light/update (assoc @light-rctn :name name*)])))

(defn light-modal []
  (fn []
    (let [light-rctn (subscribe [:modal/item])
          available-dmx-rctn (subscribe [:dmx/available])]
      [:div#light-modal.thing.ui.basic.modal.small
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

        ;; transport
        ;; ----------------
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
        ;
        ;[transport-dmx light-rctn available-dmx-rctn]
        ;(condp = (:transport @light-rctn)
        ;  :dmx
        ;  :mqtt [transport-mqtt light-rctn]
        ;  (comment "do nothing."))
        ]
       [:div.actions
        [:div.ui.button.cancel "close"]
        ;[:div.ui.button.approve "make!"]
        ]
       [:pre.debug (pretty @light-rctn)]])))