(ns guadalete-ui.events.render
  (:require
    [re-frame.core :refer [dispatch def-event def-event-fx def-fx]]
    [guadalete-ui.console :as log]))


(def-event
  :render/login
  (fn [db ]
    (log/debug ":render/login" (:user/role db))
    ;])
    db
    ))