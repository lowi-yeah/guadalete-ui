(ns guadalete-ui.fx.modal-fx
  (:require
    [re-frame.core :refer [def-fx dispatch]]
    [guadalete-ui.socket :refer [chsk-send! chsk-state chsk-reconnect! cb-success?]]
    [guadalete-ui.console :as log]
    [guadalete-ui.util :refer [pretty]]))

(def-fx
  :modal
  (fn modal-effect [command]
    (let [jq-node (js/$ (str "#modal"))
          options {
                   ;:onDeny        deny-modal
                   ;:onApprove     #(approve-modal modal-id)
                   :closable      true
                   :dimPage       true
                   :detachable    false
                   :context       "#root"
                   :allowMultiple false}
          js-options (clj->js options)]
      (.modal jq-node js-options)
      (.modal jq-node (name command)))))
