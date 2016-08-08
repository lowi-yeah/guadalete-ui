(ns guadalete-ui.events.color
  (:require
    [re-frame.core :refer [dispatch def-event def-event-fx def-fx]]
    [guadalete-ui.util :refer [pretty kw*]]
    [guadalete-ui.console :as log]
    [differ.core :as differ]))

;// re-frame_  __        _
;//   ___ / _|/ _|___ __| |_ ___
;//  / -_)  _|  _/ -_) _|  _(_-<
;//  \___|_| |_| \___\__|\__/__/
;//

(defn- new-color-effect
  "Creates a sente-effect for syncing the new light with the backend"
  [color]
  {:topic      :color/make
   :data       color
   :on-success [:success-color-make]
   :on-failure [:failure-color-make]})

(defn- update-color-effect
  "Creates a sente-effect for syncing the new light with the backend"
  [old new]
  (let [id (:id new)
        patch (differ/diff old new)]
    {:topic      :color/update
     :data       [id patch :patch]
     :on-success [:success-color-update]
     :on-failure [:failure-color-update]}))


;// re-frame          _     _                 _ _
;//   _____ _____ _ _| |_  | |_  __ _ _ _  __| | |___ _ _ ___
;//  / -_) V / -_) ' \  _| | ' \/ _` | ' \/ _` | / -_) '_(_-<
;//  \___|\_/\___|_||_\__| |_||_\__,_|_||_\__,_|_\___|_| /__/
;//

;; MAKE
;; ********************************
(def-event-fx
  ;; create a new light (a map) and sync it via sente
  :color/make
  (fn [{:keys [db]} [_ color]]
    (let [sente (new-color-effect color)]
      {:db    db
       :sente sente})))

(def-event-fx
  ;; Handler called after the light has been updated sucessfully
  :success-color-make
  (fn [{:keys [db]} [_ _response]]
    {:db db}))

(def-event-fx
  :failure-color-make
  (fn [{:keys [db]} [_ response]]
    (let []
      (log/warn "make color failed" response)
      {:db db})))

(defn update-color-type
  "Helper function which updates the color type after a type-change."
  [color new-type]
  (let [color* (condp = (kw* new-type)
                 :v (dissoc color :s :h)
                 :sv (-> color
                         (dissoc :h)
                         (assoc :s (or (:s color) 0)))
                 :hsv (-> color
                          (assoc :s (or (:s color) 1))
                          (assoc :h (or (:h color) 0)))
                 color)
        color** (assoc color* :type (kw* new-type))]
    color**))

(def-event-fx
  :color/change-type
  (fn [{:keys [db]} [_ {:keys [item-id new-type] :as data}]]
    (let [color (get-in db [:color item-id])
          color* (update-color-type color new-type)
          sente (update-color-effect color color*)]
      {:db       (assoc-in db [:color item-id] color*)
       :sente    sente
       :dispatch [:node/update-color data]})))

(def-event-fx
  :success-color-update
  (fn [{:keys [db]} [_ response]]
    {:db db}))

(def-event-fx
  :failure-color-update
  (fn [{:keys [db]} [_ response]]
    (let []
      (log/warn "update color failed" response)
      {:db db})))