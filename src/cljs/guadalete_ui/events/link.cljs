(ns guadalete-ui.events.link

  (:require
    [re-frame.core :refer [dispatch def-event def-event-fx def-fx]]
    [thi.ng.geom.core :as g]
    [thi.ng.geom.core.vector :refer [vec2]]
    [guadalete-ui.util :refer [pretty validate! vec->map]]
    [guadalete-ui.pd.nodes.link :as link]
    [guadalete-ui.console :as log]
    [guadalete-ui.events.scene :as scene]
    ; schema
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
         (s/validate gs/Flow flow)
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


        (log/debug "mouse down")
        (log/debug "MouseEventData" data)
        (validate! gs/MouseEventData data)

        (let [scene (get-in db [:scene scene-id])
              node-link (link/->get db scene-id node-id id)
              flow-reference (condp = (keyword (:direction node-link))
                                    :in {:from :mouse :to {:scene-id scene-id :node-id node-id :id id}}
                                    :out {:from {:scene-id scene-id :node-id node-id :id id} :to :mouse}
                                    nil)
              db* (-> db
                      (assoc-in [:tmp :flow] flow-reference)
                      (assoc-in [:tmp :start-pos] (vec->map position))
                      (assoc-in [:tmp :mouse-pos] (vec->map position))
                      (assoc-in [:tmp :mode] :link)
                      (assoc-in [:tmp :scene] scene))]
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
       (let [tmp (:tmp db)
             tmp* (-> tmp
                      (assoc :mode :none)
                      (dissoc :scene)
                      (dissoc :start-pos)
                      (dissoc :mouse-pos)
                      (dissoc :flow))]
            (assoc db :tmp tmp*)))

(s/defn ^:always-validate from-mouse-and-data :- gs/FlowReference
        "Internal helper generating a FlowReference from the temporary mouse-flow and the data given by the mouse event"
        [db :- gs/DB
         {:keys [scene-id node-id id] :as data} :- gs/MouseEventData]
        (let [mouse-flow (get-in db [:tmp :flow])
              reference {:scene-id scene-id
                         :node-id  node-id
                         :id       id}]
             (if (= :mouse (:from mouse-flow))
               (assoc mouse-flow :from reference)
               (assoc mouse-flow :to reference))))


(s/defn ^:always-validate from-reference
        "Internal helper generating a Flow a given flow reference"
        [db :- gs/DB
         reference :- gs/FlowReference]
        ;(log/debug "from-reference" reference)
        (let [{:keys [from to]} reference
              from-links (get-in db [:scene (:scene-id from) :nodes (keyword (:node-id from)) :links])
              from-link (->> from-links
                             (filter #(= (:id from) (:id %)))
                             (first))
              ;_ (log/debug "from-link" from-link)
              from* (merge from from-link)
              to-links (get-in db [:scene (:scene-id to) :nodes (keyword (:node-id to)) :links])
              to-link (->> to-links
                           (filter #(= (:id to) (:id %)))
                           (first))
              ;_ (log/debug "to-link" to-link)
              to* (merge to to-link)
              flow {:from from* :to to*}]
             flow))

(s/defn ^:always-validate make-flow :- gs/Effect
        "Internal helper creating a flow after mouse-up.
        Also checks validity and resets! if necessary"
        [db :- gs/DB
         {:keys [scene-id] :as data}]
        (let [mouse-flow (get-in db [:tmp :flow])]
             (if (= :valid (:valid? mouse-flow))
               (let [scene (get-in db [:scene scene-id])
                     flows (:flows scene)
                     flow-id (str (random-uuid))
                     reference (->
                                 (from-mouse-and-data db data)
                                 (assoc :id flow-id)
                                 (dissoc :valid?))
                     flows* (assoc flows (keyword flow-id) reference)
                     scene* (-> scene (assoc :flows flows*))]
                    {:db    (-> db
                                (abort)
                                (assoc-in [:scene scene-id] scene*))
                     :sente (scene/sync-effect {:old scene :new scene*})})
               ;else
               {:db (abort db)})
             ))

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
             :link (let [flow-reference (from-mouse-and-data db data)
                         different-nodes? (not= (get-in flow-reference [:from :node-id])
                                                (get-in flow-reference [:to :node-id]))
                         flow (from-reference db flow-reference)]
                        (if different-nodes?
                          (assoc-in db [:tmp :flow :valid?] (validate-flow flow))
                          db))
             ;; condp :else
             db)))

(def-event
  :link/reset-connection
  (fn [db [_ _data]]
      (let [flow (get-in db [:tmp :flow])
            flow* (dissoc flow :valid?)]
           (assoc-in db [:tmp :flow] flow*))))

