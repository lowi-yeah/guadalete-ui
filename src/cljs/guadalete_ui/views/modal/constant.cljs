(ns guadalete-ui.views.modal.constant
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


(defn- change-value [value constant-rctn]
       (dispatch [:constant/update (assoc @constant-rctn :value (/ value 100.0))]))

;//                _      _
;//   _ __  ___ __| |__ _| |
;//  | '  \/ _ \ _` / _` | |
;//  |_|_|_\___\__,_\__,_|_|
;//
(defn constant-modal []
      (let [constant-rctn (subscribe [:modal/item])]
           (create-class
             {:component-did-mount
              (fn [this]

                  (-> (js/$ "#constant-range")
                      (.range
                        (js-obj "onChange" #(change-value % constant-rctn)
                                "min" 0
                                "max" 100
                                "start" (int (* 100 (:value @constant-rctn)))))))
              :reagent-render
              (fn []
                  [:div#pd-constant-modal
                   [:div.content.ui.form
                    [:h3.centred "Edit constant value"]
                    [:h1.value (int (* 100 (:value @constant-rctn)))]
                    [:div#constant-range.ui.range.margin-bottom]
                    [:div.flex-row-container.right.actions
                     [:div.ui.button.approve
                      [:i.ui.check.icon]
                      "ok"]]]

                   [:pre.tiny.code (pretty @constant-rctn)]
                   ])})))
