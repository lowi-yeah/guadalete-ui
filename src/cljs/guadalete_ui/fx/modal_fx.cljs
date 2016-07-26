(ns guadalete-ui.fx.modal-fx
  (:require
    [re-frame.core :refer [def-fx dispatch]]
    [guadalete-ui.socket :refer [chsk-send! chsk-state chsk-reconnect! cb-success?]]
    [guadalete-ui.console :as log]
    [guadalete-ui.util :refer [pretty]]))

(def-fx
  :modal
  (fn modal-effect [[type id]]
    (dispatch [:modal/open type id])))
