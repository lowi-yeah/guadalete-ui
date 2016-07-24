(ns guadalete-ui.views
  (:require
    [re-frame.core :as re-frame]
    [reagent.core :as reagent]
    [clojure.string :as string]

    ; ---- mine ----
    [guadalete-ui.views.login :refer [login-panel]]
    [guadalete-ui.views.admin :refer [root-panel]]
    [guadalete-ui.console :as log]
    ))

(defn blank-panel [] [:div#blank])

(defmulti panels identity)
(defmethod panels :blank-panel [] [blank-panel])
(defmethod panels :root-panel [] [root-panel])
(defmethod panels :login-panel [] [login-panel])
(defmethod panels :default [] [blank-panel])

;//              _
;//   _ __  __ _(_)_ _
;//  | '  \/ _` | | ' \
;//  |_|_|_\__,_|_|_||_|
;//
(defn main-panel []
  (fn []
    (let [main-panel-rctn (re-frame/subscribe [:main-panel])]
      (panels @main-panel-rctn))))