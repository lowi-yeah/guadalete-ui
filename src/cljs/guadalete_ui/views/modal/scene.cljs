(ns guadalete-ui.views.modal.scene
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

(defn- change-name [ev scene-rctn]
  (let [name* (-> ev .-target .-value)]
    (log/debug "new scene name:" name*)
    (dispatch [:scene/update (assoc @scene-rctn :name name*)])))


(defn scene-modal []
  (fn []
    (let [scene-rctn (subscribe [:modal/item])
          siblings-rctn (subscribe [:scene/sibling-ids (:id @scene-rctn)])
          ]
      [:div#scene-modal
       [:div.content.ui.form
        [:label "Name"]
        [:div.ui.input.margin-bottom.margin-right
         [:input {:type      "text"
                  :value     (:name @scene-rctn)
                  :on-change #(change-name % scene-rctn)}]]


        [:button.circular.ui.icon.button.trash.margin-sides
         {:on-click #(dispatch [:scene/trash (:id @scene-rctn)])
          :disabled (empty? @siblings-rctn)}
         [:i.trash.outline.icon]]]
       [:div.actions
        [:div.ui.button.cancel
         "close"]]
       ])))
