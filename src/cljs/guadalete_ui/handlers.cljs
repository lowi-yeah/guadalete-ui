(ns guadalete-ui.handlers
  ;(:require-macros [reagent.ratom :refer [reaction]])
  (:require
    [re-frame.core :refer [dispatch def-event def-event-fx def-fx path trim-v after]]
    [secretary.core :as secretary]
    [taoensso.sente :as sente]
    [schema.core :as s]
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
;(log/debug "day8.re-frame.async-flow-fx" (:make-flow-event-handler day8.re-frame.async-flow-fx))
;
;(fx/register :async-flow (:make-flow-event-handler day8.re-frame.async-flow-fx))

;(fx/register
;  :aync-flow
;  (fn [{:as flow :keys [id]}]
;    (def-event-fx
;      (or id :async/flow)
;      (day8.re-frame.async-flow-fx/make-flow-event-handler flow))
;    (dispatch [id :setup])))

;//          _
;//   ______| |_ _  _ _ __
;//  (_-< -_)  _| || | '_ \
;//  /__\___|\__|\_,_| .__/
;//                  |_|


(def-event
  :set-root-panel
  (fn [db _]
    (condp = (:user/role db)
      :none (assoc db :main-panel :blank-panel)
      :anonymous (assoc db :main-panel :login-panel)
      :admin (assoc db :main-panel :root-panel))))

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
              :with-credentials? false                      ; Enable if using CORS (requires xhr v2+)
              }
             (fn async-callback [resp-map]
               (dispatch [:chsk/reconnect]))))

(def-event
  :login
  (fn [db [_ name pwd]]
    (ajax-login name pwd)
    db))

(def-event
  :chsk/reconnect
  (fn [db]
    (log/debug ":chsk/reconnekkt")
    (chsk-reconnect!)
    (assoc db :ws/connected? false)))


;//       _
;//  __ ___)_____ __ _____
;//  \ V / / -_) V  V (_-<
;//   \_/|_\___|\_/\_//__/
;//

(defn- first-scene-id
  "retrun the id of the 'first' scene in a room."
  [db room-id]
  (-> db (get-in [:room room-id :scene]) (first)))

(def-event
  :view/room
  (fn [db [_ [room-id segment]]]
    (condp = segment
      ; :current is passed, if the room changes but the segment to be shown remains the same
      ; (e.g switching form the secenes segment in roomA to the scenes segment in roomB)
      :current (do
                 ; workaround for current/scene-id
                 ; if current is scene, dispatch ':view/scene' so that the hack below can kick in
                 (when (= :scene (:current/segment db))
                   (dispatch [:view/scene [room-id]]))
                 (-> db
                     (assoc :current/view :room)
                     (assoc :current/room-id room-id)
                     (dissoc :current/scene-id)))
      (-> db
          (assoc :current/view :room)
          (assoc :current/segment segment)
          (assoc :current/room-id room-id)))))

(def-event
  :view/scene
  (fn [db [_ [room-id scene-id]]]
    (let [scene-id* (or scene-id (first-scene-id db room-id))]

      ; obacht: hack
      ; recursicely dispatch as long as the database has not been loaded
      (when (nil? scene-id*) (dispatch [:view/scene [room-id scene-id]]))

      (-> db
          (assoc :current/view :room)
          (assoc :current/segment :scene)
          (assoc :current/room-id room-id)
          (assoc :current/scene-id scene-id*)))))


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


;//
;//   _ _ ___ ___ _ __
;//  | '_/ _ \ _ \ '  \
;//  |_| \___\___/_|_|_|
;//
(def-event
  :room/update
  (fn [db [_ update]]
    (dispatch [:item/update :room update])
    db))


;//
;//   _____ ___ _ _  ___
;//  (_-< _/ -_) ' \/ -_)
;//  /__\__\___|_||_\___|
;//
(def-event
  :scene/update
  (fn [db [_ update]]

    (if (nil? update)
      (do
        (log/error "Error during update scene. Scene is nil.")
        db)
      (let [id (:id update)
            original (get-in db [:scene id])
            patch (differ/diff original update)]

        ;; if the id is nil, log an error
        (if (nil? id)
          (do
            (log/error "Error during update scene. Scene is nil.")
            db)
          (do
            ;;if the patch is empty, send the whole scene and a replace-flag
            (if (and (empty? (first patch)) (empty? (second patch)))
              (chsk-send! [:scene/update [id original :replace]])
              (chsk-send! [:scene/update [id patch]]))
            (assoc-in db [:scene id] update)))))))

;//                _      _
;//   _ __  ___ __| |__ _| |
;//  | '  \/ _ \ _` / _` | |
;//  |_|_|_\___\__,_\__,_|_|
;//
(def-event
  :modal/open
  (fn [db [_ data]]
    (let [modal-id (:id data)
          options (or (:options data) {})]
      (log/debug "open moddal" modal-id options)
      (modal/open modal-id options)
      db)))

(def-event
  :modal/approve
  (fn [db [_ modal-id]]
    (log/debug ":modal/approve" modal-id)
    (condp = modal-id
      ;:new-light (do (dispatch [:light/make]))
      (comment "nothing yet"))
    db))

(def-event
  :modal/deny
  (fn [db _]
    (dissoc db :pd/modal-node-data :new/light)))

(def-event
  :modal/register-node
  (fn [db [_ {:keys [item-id]}]]
    (if (= item-id "nil")
      db
      (let [scene (modal-scene db)
            nodes (get scene :nodes)
            node (modal-node db)
            type (keyword (:type node))
            item (get-in db [type item-id])
            scene-items (get scene type)
            node* (assoc node :item-id item-id)
            nodes* (assoc nodes (kw* (:id node*)) node*)
            scene* (assoc scene :nodes nodes*)
            db* (assoc-in db [:scene (:id scene)] scene*)]
        (dispatch [:scene/update scene*])
        db*))))

;//   _ _      _   _
;//  | (_)__ _| |_| |_
;//  | | / _` | ' \  _|
;//  |_|_\__, |_||_\__|
;//      |___/
;
(def-event
  :light/prepare-new
  (fn [db [_ room-id]]
    (let [channels (sort (into [] (dmx/assignable db)))
          new-light {:id           (str (random-uuid))
                     :name         (dickens/generate-name)
                     :room-id      room-id
                     :num-channels 1
                     :transport    :dmx
                     ; obacht:
                     ; nested arrays, since each color can be assigned multiple channels
                     :channels     [[(first channels)]]
                     :color        {:h 0 :s 0 :v 0}}

          room (get-in db [:room room-id])
          room-lights* (conj (:light room) (:id new-light))
          room* (assoc room :light room-lights*)]

      (dispatch [:item/create :light new-light])
      (dispatch [:room/update room*])

      ; obacht: hackish
      ; delay the modal-dispatch so the room update won't kill it instantly
      (js/setTimeout #(dispatch [:modal/open {:id :edit-light}]) 300)
      (assoc db :current/light-id (:id new-light)))))

(def-event
  :light/edit
  (fn [db [_ light-id]]
    (dispatch [:modal/open {:id :edit-light}])
    (assoc db :current/light-id light-id)))

(defn remove-channels
  "Internal helper to remove channel-assignments during :light/update-new"
  [light old-count new-count]
  (into []
        (map
          (fn [index] (get-in light [:channels index]))
          (range new-count))))

(defn add-channels
  "Internal helper to remove channel-assignments during :light/update-new"
  [light additional db]
  (let [assignables (sort (into [] (dmx/assignable db)))
        num-channels (int (:num-channels light))
        channels (:channels light)
        range (range num-channels (+ num-channels additional))
        channels* (into channels (map (fn [i] [(nth assignables (- i 1))]) range))]
    (into [] channels*)))

(def-event
  :light/update
  (fn [db [_ update]]
    (let [original (get-in db [:light (:id update)])
          num-update-channels (int (:num-channels update))
          num-original-channels (int (:num-channels original))]
      (if (not (= num-update-channels num-original-channels))
        (let [channels* (cond
                          (< num-update-channels num-original-channels) (remove-channels update num-original-channels num-update-channels)
                          (> num-update-channels num-original-channels) (add-channels original (- num-update-channels num-original-channels) db)
                          :default (:channels update))]
          (dispatch [:item/update :light (assoc update :channels channels*)]))
        (dispatch [:item/update :light update]))
      db)))

(def-event
  :light/prepare-trash
  (fn [db [_ light-id]]
    (dispatch [:modal/open {:id      :trash-item
                            :item    {:type :light
                                      :id   light-id}
                            :options {:closable true}}])
    db))

;//      _                _
;//   ____)__ _ _ _  __ _| |___
;//  (_-< / _` | ' \/ _` | (_-<
;//  /__/_\__, |_||_\__,_|_/__/
;//       |___/
(def json-reader (transit/reader :json))

(def-event
  :signal/value
  (fn [db [_ message]]
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
