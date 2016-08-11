(ns guadalete-ui.events.flow
  (:require
    [re-frame.core :refer [dispatch def-event def-event-fx def-fx]]
    [thi.ng.geom.core :as g]
    [thi.ng.geom.core.vector :refer [vec2]]
    [guadalete-ui.pd.flow :as flow]
    [guadalete-ui.util :refer [pretty kw* vec-map]]
    [guadalete-ui.pd.link :as link]
    [guadalete-ui.console :as log]

    ; schema
    [schema.core :as s]
    [guadalete-ui.schema.pd :refer [Flow ValueFlow ColorFlow FlowReference
                                    ColorOutLink ValueOutLink ColorInLink ValueInLink
                                    InLink OutLink Link]]))

;//   _        _
;//  | |_  ___| |_ __ ___ _ _ ___
;//  | ' \/ -_) | '_ \ -_) '_(_-<
;//  |_||_\___|_| .__\___|_| /__/
;//             |_|
(defn- validate-flow
  "Internal helper for validating a flow. I.e. whether both tips form a valid connection.
  Only checks for semantic validity (ie. whether the the :from and :to links match to form a valid connection."
  [flow]
  (let [{:keys [from to]} flow]
    (log/debug "validate-flow" flow)
    ;(log/debug "AssembledFlow" AssembledFlow)
    ;(log/debug "eplain" (s/explain AssembledFlow))


    (try
      (do

        ;(log/debug "s/validate OutLink")
        ;(s/validate OutLink from)
        ;(log/debug "s/validate InLink")
        ;(s/validate InLink to)
        ;(log/debug "s/validate Links")
        ;(s/validate Link from)
        ;(s/validate Link to)

        (log/debug "s/validate Flow")
        (s/validate Flow flow)

        ;(s/validate ColorLink to)
        ;(s/validate Link to)
        ;return value:
        :valid)
      (catch js/Error e
        (log/debug "validation error!" e)
        :invalid))))


;//    __ _
;//   / _| |_____ __ __
;//  |  _| / _ \ V  V /
;//  |_| |_\___/\_/\_/
;//
(def-event
  :flow/mouse-down
  (fn [db [_ {:keys [scene-id node-id id position]}]]
    (let [scene (get-in db [:scene scene-id])
          node-link (link/->get db scene-id node-id id)
          flow (condp = (kw* (:direction node-link))
                 :in {:from :mouse :to {:node-id node-id :id id}}
                 :out {:from {:node-id node-id :id id} :to :mouse}
                 nil)]
      (-> db
          (assoc-in [:tmp :flow] flow)
          (assoc-in [:tmp :start-pos] (vec-map position))
          (assoc-in [:tmp :mouse-pos] (vec-map position))
          (assoc-in [:tmp :mode] :link)
          (assoc-in [:tmp :scene] scene)))))


;; Called when moving the mouse during link creation.
;; Updates the mouse mosition (for rendering the temporary flow.
;; Also Checks the current target and sets appropriae values in the db.
(def-event-fx
  :flow/mouse-move
  (fn [{:keys [db]} [_ {:keys [scene-id position type] :as data}]]
    (let [dispatch* (if (= :link (kw* type))
                      [:flow/check-connection data]
                      [:flow/reset-target data])]
      {:db       (assoc-in db [:tmp :mouse-pos] (vec-map position))
       :dispatch dispatch*})))

(def-event-fx
  :flow/mouse-up
  (fn [{:keys [db]} [_ {:keys [scene-id position type] :as data}]]
    (let []
      {:db (-> db
               (assoc-in [:tmp :mode] :none)
               (assoc-in [:tmp :scene] nil)
               (assoc-in [:tmp :start-pos] nil)
               (assoc-in [:tmp :mouse-pos] nil)
               (assoc-in [:tmp :flow] nil))})))

(def-event
  ;; called during flow/mouse-move, when the mouse is above a nide link.
  ;; checks whether the first link tip mathches the current link, so that a valid link would be created.
  :flow/check-connection
  (fn [db [_ {:keys [scene-id node-id id]}]]
    (let [{:keys [from to] :as mouse-flow} (get-in db [:tmp :flow])
          flow-tip (if (= :mouse from) to from)
          flow-direction (if (= :mouse to) :downstream :upstream)
          mouse-link (get-in db [:scene scene-id :nodes (:node-id flow-tip) :links (kw* (:id flow-tip))])
          link (get-in db [:scene scene-id :nodes (kw* node-id) :links (kw* id)])

          ;; make sure the link (potential flow tip) ends in a different node than the one it originates from
          different-nodes? (not= (:node-id flow-tip) (kw* node-id))

          ;; make sure the link (potential flow tip) matches the direction of the mouse flow.
          ;; ie. make sure that the link is :in in case of a :downstream flow and vice versa…
          direction-ok? (or
                          (and (= flow-direction :downstream) (= (:direction link) "in"))
                          (and (= flow-direction :upstream) (= (:direction link) "out")))

          ]

      ;(log/debug ":flow/check-connection" link)
      ;(log/debug "flow-tip" flow-tip)
      ;(log/debug ":flow/different-nodes" different-nodes?)
      ;(log/debug ":flow/direction-ok?" direction-ok?)
      ;(log/debug "mouse-flow" mouse-flow)
      ;(log/debug "tmp-flow" tmp-flow)
      ;(log/debug "ids" (:id flow-tip) id)
      ;(log/debug "valid?" valid?)

      (if (and different-nodes? direction-ok?)
        (let [tmp-flow (if (= :mouse from)
                         {:from link :to mouse-link}
                         {:from mouse-link :to link})
              valid? (validate-flow tmp-flow)]
          (log/debug "valid?" valid?)
          db)
        ;; invalid
        db)
      )))

(def-event
  :flow/reset-target
  (fn [db [_ data]]
    ;(flow/reset-target data db)
    ;(log/debug "resetting flow target")
    db))

