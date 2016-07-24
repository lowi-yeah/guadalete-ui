(ns guadalete-ui.fx.sente-fx
  (:require
    [re-frame.core :refer [def-fx dispatch]]
    [guadalete-ui.socket :refer [chsk-send! chsk-state chsk-reconnect! cb-success?]]
    [guadalete-ui.console :as log]
    [guadalete-ui.util :refer [pretty]]))

(defn request->sente-options
  [{:as   request
    :keys [topic data timeout on-success on-failure]
    :or   {timeout    8000
           data       {}
           on-success [:sente-no-on-success]
           on-failure [:sente-no-on-failure]}}]
  (let [handler-fn (fn [reply]
                     (if (cb-success? reply)
                       (dispatch (conj on-success reply))
                       (dispatch (conj on-failure reply))))]
    ;return a parameter vector which will be applied to chsk-send!
    [[topic data] timeout handler-fn]))

(def-fx
  :sente
  (fn sente-effect [request]
    (cond
      (sequential? request) (doseq [each request] (sente-effect each))
      (map? request) (->>
                       request
                       request->sente-options
                       (apply chsk-send!))
      :else
      (log/error "sente-fx: expected request to be a list or vector or map, but got: " request))))
