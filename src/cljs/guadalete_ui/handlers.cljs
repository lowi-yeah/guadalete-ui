(ns guadalete-ui.handlers
  (:require
    [re-frame.core :refer [dispatch def-event def-event-fx def-fx path trim-v after]]
    [secretary.core :as secretary]
    [taoensso.sente :as sente]
    [schema.core :as s]
    [guadalete-ui.schema :as gs]
    [re-frame.core :refer [subscribe]]
    [taoensso.encore :refer [ajax-lite]]
    [differ.core :as differ]
    [cljs-time.core :as time :refer [seconds ago]]
    [cljs-time.coerce :refer [to-long]]
    [cognitect.transit :as transit]
    [guadalete-ui.schema.core :refer [DB]]
    [guadalete-ui.console :as log]
    [guadalete-ui.socket :refer [chsk-send! chsk-state chsk-reconnect!]]
    [guadalete-ui.pd.util :refer [modal-room modal-scene modal-node]]
    [guadalete-ui.pd.scene :as scene]
    [guadalete-ui.util :refer [pretty kw* mappify]]
    [guadalete-ui.util.queue :as queue]
    [guadalete-ui.dmx :as dmx]
    [guadalete-ui.util.dickens :as dickens]
    [guadalete-ui.views.modal :as modal]
    [guadalete-ui.pd.nodes :as node]))

;//         _    _    _ _
;//   _ __ (_)__| |__| | |_____ __ ____ _ _ _ ___
;//  | '  \| / _` / _` | / -_) V  V / _` | '_/ -_)
;//  |_|_|_|_\__,_\__,_|_\___|\_/\_/\__,_|_| \___|
;//
(defn check-and-throw
  "throw an exception if db doesn't match the schema."
  [a-schema db]
  (if-let [problems (s/check a-schema db)]
    (throw (js/Error. (str "schema check failed: " problems)))))

;; after an event handler has run, this middleware can check that
;; it the value in app-db still correctly matches the schema.
(def check-schema-mw (after (partial check-and-throw DB)))
;
;//          _
;//   ______| |_ _  _ _ __
;//  (_-< -_)  _| || | '_ \
;//  /__\___|\__|\_,_| .__/
;//                  |_|
(def-event
  :set-panel
  (fn [db [_ role]]
    (let [panel (condp = role
                  :none :blank
                  :anonymous :login
                  :admin :root
                  :blank)]
      (assoc-in db [:view :panel] panel))))

;//   _          _
;//  | |___ __ _(_)_ _
;//  | / _ \ _` | | ' \
;//  |_\___\__, |_|_||_|
;//        |___/
(defn- ajax-login
  "POST the login information via ajax"
  [name pwd]
  (ajax-lite "/login"
             {:method            :post
              :params            {:username   (str name)
                                  :password   (str pwd)
                                  :csrf-token (:csrf-token @chsk-state)}
              :resp-type         :text
              :timeout-ms        8000
              :with-credentials? false}
             (fn async-callback [resp-map]
               (dispatch [:chsk/connect]))))

(def-event
  :login
  (fn [db [_ name pwd]]
    (ajax-login name pwd)
    db))

(def-event
  :chsk/reconnect
  (fn [db _]
    (log/debug ":chsk/reconnekkt")
    (chsk-reconnect!)
    (assoc db :ws/connected? false)))

(def-event
  :ws/handshake
  (fn [db [_ foo]]
    (log/debug ":ws/handshake" foo)
    db))



;//       _
;//  __ ___)_____ __ _____
;//  \ V / / -_) V  V (_-<
;//   \_/|_\___|\_/\_//__/
;//

(defn- first-scene-id
  "retrun the id of the 'first' scene in a room."
  [db room-id]
  (-> db (get-in [:room room-id :scene]) (first)))

(def-event-fx
  :view/root
  (fn [{:keys [db]} _]
    {:db (-> db
             (assoc-in [:view :panel] :root)
             (assoc-in [:view :section] :dash))}))

(s/defn ^:always-validate view-room* :- gs/DB
  [db [_ [room-id segment]]]
  (let [loading? (subscribe [:db/loading?])]
    (condp = segment
      ; :current is passed, if the room changes but the segment to be shown remains the same
      ; (e.g switching form the secenes segment in roomA to the scenes segment in roomB)
      :current (let [scene-id (-> db
                                  (get-in [:room room-id :scene])
                                  (first))]
                 (log/debug "room id" room-id)
                 (log/debug "scene-id" scene-id)
                 (-> db
                     (assoc-in [:view :section] :room)
                     (assoc-in [:view :room-id] (keyword room-id))
                     (assoc-in [:view :scene-id] (keyword scene-id))
                     (assoc-in [:view :ready?] (not @loading?))))
      (-> db
          (assoc-in [:view :section] :room)
          (assoc-in [:view :segment] segment)
          (assoc-in [:view :ready?] (not @loading?))
          (assoc-in [:view :room-id] (keyword room-id))))))


(s/defn ^:always-validate view-scene* :- gs/DB
  [db [_ [room-id scene-id]]]
  (let [scene-id* (or scene-id (first-scene-id db room-id))]
    (-> db
        (assoc-in [:view :section] :room)
        (assoc-in [:view :segment] :scene)
        (assoc-in [:view :room-id] (keyword room-id))
        (assoc-in [:view :scene-id] (keyword scene-id*)))))

(s/defn ^:always-validate view-dash* :- gs/DB
  [db [_ room-id]]
  (-> db
      (assoc-in [:view :section] :room)
      (assoc-in [:view :segment] :dash)
      (assoc-in [:view :room-id] (keyword room-id))))

(def-event :view/room view-room*)
(def-event :view/scene view-scene*)
(def-event :view/dash view-dash*)

;//                _      _
;//   _ __  ___ __| |__ _| |___
;//  | '  \/ _ \ _` / _` | (_-<
;//  |_|_|_\___\__,_\__,_|_/__/
;//

(def-event
  :modal/new-room
  (fn [db _]
    (log/debug ":modal/new-room")
    db))


;//   _ _
;//  (_) |_ ___ _ __  ___
;//  | |  _/ -_) '  \(_-<
;//  |_|\__\___|_|_|_/__/
;//
(def-event
  :item/create
  (fn [db [_ item-key item]]
    (let [msg-key (-> item-key
                      (name)
                      (str "/create")
                      keyword)]
      (chsk-send! [msg-key item])
      (assoc-in db [item-key (:id item)] item))))

(def-event
  :item/update
  (fn [db [_ item-key update]]
    (let [id (:id update)
          original (get-in db [item-key id])
          patch (differ/diff original update)
          msg-key (-> item-key
                      (name)
                      (str "/update")
                      keyword)]
      ;;if the patch is empty, send the whole item together with a replace-flag
      (if (and (empty? (first patch)) (empty? (second patch)))
        (chsk-send! [msg-key [id original :replace]])
        (chsk-send! [msg-key [id patch]]))
      (assoc-in db [item-key id] update))))

(def-event
  :item/trash
  (fn [db [_ item-key item-id]]
    (let [msg-key (-> item-key
                      (name)
                      (str "/trash")
                      keyword)]
      (log/debug ":item/trash" (str msg-key) item-id)
      (chsk-send! [msg-key item-id])
      db)))



;//      _                _
;//   ____)__ _ _ _  __ _| |___
;//  (_-< / _` | ' \/ _` | (_-<
;//  /__/_\__, |_||_\__,_|_/__/
;//       |___/
(def json-reader (transit/reader :json))

(def-event
  :signal/value
  (fn [db [_ message]]
    (log/debug ":signal/value" (str message))
    (let [id (get message "id")
          [timestamp value] (get message "data")
          signal (get-in db [:signal id])
          values (or (:values signal) (queue/make))
          values* (queue/push values (str timestamp) (int value))
          timespan (get-in db [:config :signal :sparkline/timespan-seconds])]
      (if timespan
        (let [threshold (-> timespan seconds ago to-long)
              values** (queue/truncate values* threshold)]
          (assoc-in db [:signal id :values] values**))
        ;else
        (assoc-in db [:signal id :values] values*)))))

(defn- batch-signal-values [[id entries]]
  (doall
    (for [entry entries]
      (dispatch [:signal/value {"id" id "data" entry}]))))

(def-event
  :signals/values
  (fn [db [_topic message]]
    ;;sideffect
    (doall (map batch-signal-values message))
    db))

;//          _
;//   __ ___| |___ _ _
;//  / _/ _ \ / _ \ '_|
;//  \__\___/_\___/_|
;//
(def-event
  :color/make
  (fn [db [_ color]]
    (log/debug ":color/make" (pretty color))
    (chsk-send! [:color/make color])
    (assoc-in db [:color (:id color)] color)))


;(defn- unused-lights? []

;//           _
;//   _ __ __| |
;//  | '_ \ _` |
;//  | .__\__,_|
;//  |_|

;// => see pd.handlers
