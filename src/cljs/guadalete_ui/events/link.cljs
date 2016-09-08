(ns guadalete-ui.events.link

  (:require
    [re-frame.core :refer [dispatch def-event def-event-fx def-fx]]
    [thi.ng.geom.core :as g]
    [thi.ng.geom.core.vector :refer [vec2]]
    [guadalete-ui.util :refer [pretty validate! vec->map]]
    [guadalete-ui.pd.link :as link]
    [guadalete-ui.console :as log]

    ; schema
    [schema.core :as s]
    [guadalete-ui.schema.pd :refer [Flow]]
    [guadalete-ui.events.scene :as scene]

    [schema.core :as s]
    [guadalete-ui.schema :as gs]))

;//   _        _
;//  | |_  ___| |_ __ ___ _ _ ___
;//  | ' \/ -_) | '_ \ -_) '_(_-<
;//  |_||_\___|_| .__\___|_| /__/
;//             |_|
(defn- validate-flow
  "Internal helper for validating a flow. I.e. whether both tips form a valid connection.
  Only checks for semantic validity (ie. whether the the :from and :to links match to form a valid connection."
  [flow]
  (try
    (s/validate Flow flow)
    :valid
    (catch js/Error e
      (log/debug "validation error!" e)
      :invalid)))


;//    __ _
;//   / _| |_____ __ __
;//  |  _| / _ \ V  V /
;//  |_| |_\___/\_/\_/
;//
;(s/defn ^:always-validate mouse-down :- gs/DB
(s/defn mouse-down :- gs/DB
  [db :- gs/DB
   {:keys [scene-id node-id id position] :as data} :- gs/MouseEventData]
  (let [scene (get-in db [:scene scene-id])
        _ (log/debug "mouse-down!\n scene" scene)
        _ (log/debug "data" data)
        node-link (link/->get db scene-id node-id id)
        _ (log/debug "node-link" node-link)
        flow (condp = (keyword (:direction node-link))
               :in {:from :mouse :to {:scene-id scene-id :node-id node-id :id id}}
               :out {:from {:scene-id scene-id :node-id node-id :id id} :to :mouse}
               nil)
        _ (log/debug "mouse flow" flow)
        db* (-> db
                (assoc-in [:tmp :flow] flow)
                (assoc-in [:tmp :start-pos] (vec->map position))
                (assoc-in [:tmp :mouse-pos] (vec->map position))
                (assoc-in [:tmp :mode] :link)
                (assoc-in [:tmp :scene] scene))]
    (validate! gs/DB db*)
    db*))

(def-event
  :link/mouse-down
  (fn [db [_ data]]
    (mouse-down db data)))


;; Called when moving the mouse during link creation.
;; Updates the mouse mosition (for rendering the temporary flow.
;; Also Checks the current target and sets appropriae values in the db.
(def-event-fx
  :link/mouse-move
  (fn [{:keys [db]} [_ {:keys [position type] :as data}]]
    (let [dispatch* (if (= :link (keyword type))
                      [:link/check-connection data]
                      [:link/reset-connection data])]
      {:db       (assoc-in db [:tmp :mouse-pos] (vec->map position))
       :dispatch dispatch*})))

(defn- abort
  "Internal helper for resetting the state when the flow-creation is being cancelled."
  [db]
  (-> db
      (assoc-in [:tmp :mode] :none)
      (assoc-in [:tmp :scene] nil)
      (assoc-in [:tmp :start-pos] nil)
      (assoc-in [:tmp :mouse-pos] nil)
      (assoc-in [:tmp :flow] nil)))

(s/defn ^:always-validate from-mouse-and-data :- gs/Flow
  "Internal helper generating a FlowReference from the temporary mouse-flow and the data given by the mouse event"
  [db :- gs/DB
   {:keys [scene-id node-id id] :as data} :- gs/MouseEventData]
  (log/debug "from mouse and data " data)
  (let [mouse-flow (get-in db [:tmp :flow])
        _ (log/debug "mouse-flow" mouse-flow)
        reference {:scene-id scene-id
                   :node-id  node-id
                   :id       id}
        _ (log/debug "reference" reference)]
    (if (= :mouse (:from mouse-flow))
      (assoc mouse-flow :from reference)
      (assoc mouse-flow :to reference))))

(defn- from-reference
  "Internal helper generating a Flow a fiven flow reference"
  [db reference]
  (let [{:keys [from to]} reference
        ;all-links (get-in db [:scene (:scene-id to) :nodes (:node-id to) :links])
        from-link (get-in db [:scene (:scene-id from) :nodes (:node-id from) :links (keyword (:id from))])
        to-link (get-in db [:scene (:scene-id to) :nodes (:node-id to) :links (keyword (:id to))])]
    {:from from-link :to to-link}))

(defn- make-flow
  "Internal helper creating a flow after mouse-up.
  Also checks validity and resets! if necessary"
  [db {:keys [scene-id] :as data}]
  (let [mouse-flow (get-in db [:tmp :flow])]
    (if (= :valid (:valid? mouse-flow))
      (let [scene (get-in db [:scene scene-id])
            flows (:flows scene)
            flow-id (str (random-uuid))
            reference (->
                        (from-mouse-and-data db data)
                        (assoc :id flow-id)
                        (dissoc :valid?))
            flows* (assoc flows flow-id reference)
            scene* (-> scene (assoc :flows flows*))]

        (log/debug "make flow" (pretty reference))

        {:db    (-> db
                    (abort)
                    (assoc-in [:scene scene-id] scene*))
         :sente (scene/sync-effect {:old scene :new scene*})})
      ;else
      {:db (abort db)})))

(def-event-fx
  :link/mouse-up
  (fn [{:keys [db]} [_ {:keys [type] :as data}]]
    (condp = type
      :link (make-flow db data)
      {:db (abort db)})))

(def-event
  ;; called during flow/mouse-move, when the mouse is above a nide link.
  ;; checks whether the first link tip matches the current link, so that a valid link would be created.
  :link/check-connection
  (fn [db [_ {:keys [type] :as data}]]
    (condp = type
      :link (let [
                  flow-reference (from-mouse-and-data db data)
                  different-nodes? (not= (get-in flow-reference [:from :node-id])
                                         (get-in flow-reference [:to :node-id]))
                  flow (from-reference db flow-reference)]
              (log/debug "flow-reference" flow-reference)
              (log/debug "check flow")
              (log/debug "from" (:from flow))
              (log/debug "to" (:to flow))
              (if different-nodes?
                (assoc-in db [:tmp :flow :valid?] (validate-flow flow))
                db))
      ;; condp :else
      db)))

(def-event
  :link/reset-connection
  (fn [db [_ _data]]
    (assoc-in db [:tmp :flow :valid?] nil)))

