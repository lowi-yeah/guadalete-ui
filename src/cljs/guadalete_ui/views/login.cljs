(ns guadalete-ui.views.login
  "The login screen."
  (:require [re-frame.core :as re-frame]
            [guadalete-ui.console :as log]
            [clojure.string :as string]
            [reagent.core :as reagent]))

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

(defn login-panel
      "Component for login"
      []
      (let [name (reagent/atom "lowi")
            password (reagent/atom "sfx123")]
           (fn []
               [:div#login
                [:div#login-bg]
                [:div.ui.middle.aligned.center.aligned.grid
                 [:div.column
                  [:form.ui.large.form
                   [:div.ui.segment
                    [:div.field
                     [:div.ui.left.icon.input
                      [:i.user.icon]
                      [initial-focus-wrapper
                       [name-form name]]]]
                    [:div.field
                     [:div.ui.left.icon.input
                      [:i.lock.icon]
                      [password-form password]]]
                    [:div.ui.fluid.large.submit.button
                     {:on-click #(re-frame/dispatch [:login @name @password])}
                     "Login"]
                    ]]]]])))