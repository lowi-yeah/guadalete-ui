(ns guadalete-ui.views
  (:require
    [re-frame.core :as re-frame]
    [reagent.core :as reagent]
    [clojure.string :as string]

    ; ---- mine ----
    ;[guadalete-ui.console :as c]
    ;[guadalete-ui.views.login :refer [login-form]]
    ;[guadalete-ui.views.admin :as admin]
    ;[guadalete-ui.views.user :as user]
    ;[guadalete-ui.views.sensor :as sensor]
    ;[guadalete-ui.views.signals :as signals]
    ;[guadalete-ui.views.debug :as debug]
    ;[guadalete-ui.views.nav :as nav]
    [guadalete-ui.console :as log]
    ;[guadalete-ui.socket :as socket]
    ))

(defn blank-panel [] [:div#blank])

;(defn root-panel
;      "the panel for the root url '/'"
;      []
;      (let [role (re-frame/subscribe [:user/role])]
;           (fn []
;               (condp = @role
;                      :none [blank-panel]
;                      :anonymous [login-form]
;                      :user [user/root-panel]
;                      :admin [admin/r00t-panel]))))

(defmulti panels identity)
(defmethod panels :root-panel [] [blank-panel])
;(defmethod panels :room-panel [] [room-panel])
;(defmethod panels :room-switches-panel [] [room-switches-panel])
;(defmethod panels :room-lights-panel [] [room-lights-panel])
;(defmethod panels :room-scene-panel [] [room-scene-panel])
;(defmethod panels :user-panel [] [user-panel])
;(defmethod panels :sensor-panel [] [sensor-panel])
;(defmethod panels :signal-panel [] [signal-panel])
;(defmethod panels :debug-panel [] [debug-panel])
(defmethod panels :default [] [blank-panel])

;//              _
;//   _ __  __ _(_)_ _
;//  | '  \/ _` | | ' \
;//  |_|_|_\__,_|_|_||_|
;//
(defn main-panel []
      (let [active-panel (re-frame/subscribe [:active-panel])]
           (fn []  (panels @active-panel))))


;(defn input
;      []
;      [:input {:type        "text"
;               :placeholder "Write here."
;               :on-change   #(re-frame/dispatch [:test/send (-> % .-target .-value)])}])
;
;(defn message
;      []
;      (let [message (re-frame/subscribe [:message])]
;           [:div
;            [:span "> "]
;            [:span @message]]))
;
;(defn main-panel []
;      (let [name (re-frame/subscribe [:name])]
;           (reagent/create-class
;             {:component-will-mount socket/event-loop
;              :reagent-render       (fn []
;                                        [:div
;                                         [:div "Hello from " @name]
;                                         [input]
;                                         [message]])})))
