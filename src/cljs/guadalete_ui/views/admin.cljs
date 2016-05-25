(ns guadalete-ui.views.admin
  "The login screen."
  (:require
    [re-frame.core :as re-frame]
    ;[clojure.string :as string]
    [reagent.core :as reagent]
    [guadalete-ui.console :as log]
    [guadalete-ui.util :refer [pretty]]))

(defn- input-element
       "An input element which updates its value and on focus parameters on change, blur, and focus"
       [id name type value-atom placeholder]
       [:input {:id          id
                :name        name
                :type        type
                :placeholder placeholder
                :value       @value-atom
                :on-change   #(reset! value-atom (-> % .-target .-value))}])

(defn- name-form [name-atom]
       (input-element "name"                                ;id
                      "name"                                ;name
                      "text"                                ;type
                      name-atom                             ;state
                      "Tu nombre"                           ;placeholder
                      ))

(defn- password-form [name-atom]
       (input-element "pwd"                                 ;id
                      "pwd"                                 ;name
                      "password"                            ;type
                      name-atom                             ;state
                      "Tu contraseña"                       ;placeholder
                      ))

(def initial-focus-wrapper
  (with-meta identity
             {:component-did-mount #(.focus (reagent/dom-node %))}))

(defn root-panel
      "root component for :role/admin"
      []
      (fn []
          (let [
                ;sensors-rctn (re-frame/subscribe [:sensors {:filter? true}])
                rooms-rctn (re-frame/subscribe [:rooms])
                ]

               ;$('.context.example .ui.sidebar')

               ;.sidebar('attach events', '.context.example .menu .item')

               [(with-meta identity
                           {:component-did-mount (fn [this]
                                                     (.sidebar (js/$ "#nav")
                                                               (js-obj "context" (js/$ "#root")
                                                                       "closable" false
                                                                       "dimPage" false))
                                                     (.sidebar (js/$ "#nav") "push page")
                                                     )})
                [:div#root
                 [:div#nav.ui.visible.sidebar.inverted.vertical.menu
                  [:a.item "f00"]
                  [:a.item "w00t"]
                  [:a.item "bam"]]
                 [:div.ui.top.fixed.menu
                  [:h1 "f00"]]
                 ;<div class="ui top fixed menu">
                 [:div.pusher
                  [:h1 "ROOT"]
                  [:pre.code (pretty @rooms-rctn)]]]])))