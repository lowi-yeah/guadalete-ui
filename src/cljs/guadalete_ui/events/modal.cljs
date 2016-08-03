(ns guadalete-ui.events.modal
  (:require
    [re-frame.core :refer [dispatch def-event def-event-fx def-fx]]
    [differ.core :as differ]
    [guadalete-ui.util :refer [pretty]]
    [guadalete-ui.console :as log]
    [guadalete-ui.views.modal :as modal]))

;//                _      _
;//   _ __  ___ __| |__ _| |
;//  | '  \/ _ \ _` / _` | |
;//  |_|_|_\___\__,_\__,_|_|
;//

(def-event
  :modal/update
  (fn [db [_ new-data]]
    (assoc db :modal new-data)))



(def-event-fx
  :modal/close
  (fn [{:keys [db]} [_ type]]
    {:db    (dissoc db :modal)
     :modal :close}))
;
;(def-event
;  :modal/approve
;  (fn [db [_ modal-id]]
;    (log/debug ":modal/approve" modal-id)
;    (condp = modal-id
;      ;:new-light (do (dispatch [:light/make]))
;      (comment "nothing yet"))
;    db))
;
(def-event
  :modal/deny
  (fn [db _]
    (dissoc db :modal/item)))
;
;(def-event
;  :modal/register-node
;  (fn [db [_ {:keys [item-id]}]]
;    (if (= item-id "nil")
;      db
;      (let [scene (modal-scene db)
;            nodes (get scene :nodes)
;            node (modal-node db)
;            type (keyword (:type node))
;            item (get-in db [type item-id])
;            scene-items (get scene type)
;            node* (assoc node :item-id item-id)
;            nodes* (assoc nodes (kw* (:id node*)) node*)
;            scene* (assoc scene :nodes nodes*)
;            db* (assoc-in db [:scene (:id scene)] scene*)]
;        (dispatch [:scene/update scene*])
;        db*))))
