(ns guadalete-ui.handlers
  ;(:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :refer [dispatch register-handler path trim-v after]]
            [taoensso.sente :as sente]
            [schema.core :as s]
            [taoensso.encore :refer [ajax-lite]]
            [differ.core :as differ]
            [guadalete-ui.schema.core :refer [DB]]
            [guadalete-ui.console :as log]
            [guadalete-ui.socket :refer [chsk-send! chsk-state chsk-reconnect!]]
            [guadalete-ui.pd.util :refer [modal-room modal-scene modal-node]]
            [guadalete-ui.util :refer [pretty kw* mappify]]
            [guadalete-ui.dmx :as dmx]
            [guadalete-ui.util.dickens :as dickens]

    ;[secretary.core :as secretary]
    ;        [redonaira.schema :as schema]
    ;[schema.core :as s]
    ;[differ.core :as differ]
    ;[taoensso.encore :refer [ajax-lite]]
    ;[clojure.string :as string]
    ;[thi.ng.geom.core.vector :as v]
    ;[guadalete-ui.ws :as ws]
    ;[guadalete-ui.helpers :as helpers]
    ;[guadalete-ui.toaster :refer [toast]]
            [guadalete-ui.views.modal :as modal]))



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


;//          _
;//   ______| |_ _  _ _ __
;//  (_-< -_)  _| || | '_ \
;//  /__\___|\__|\_,_| .__/
;//                  |_|
(register-handler
  :initialize-db
  check-schema-mw
  (fn [_db _]
      (dispatch [:sync/role])
      ;return default db
      {:ws/connected?   false
       :loading?        false
       :user/role       :none
       :main-panel      :blank-panel
       :name            "guadalete-ui"
       :message         ""
       :current/view    :blank
       :current/segment :scene
       }))

(register-handler
  :ws/handshake
  ;check-schema-mw
  (fn [db [_ role]]
      (dispatch [:set-root-panel role])
      (assoc db :ws/connected? true :user/role role)))

; more of a convenience handler for development
; when auto-reload updates the page, no new handshake is being exchanged,
; and thus the userr-role is not set. Here this is done explicitly
; irrelevant for normal operation, as not auto-reload happens there
(register-handler
  :sync/role
  (fn [db _]
      (chsk-send! [:sync/role]
                  8000
                  (fn [reply]
                      (dispatch [:ws/handshake (keyword (:role reply))])))
      db))


(register-handler
  :set-root-panel
  (fn [db _]
      (condp = (:user/role db)
             :none (assoc db :main-panel :blank-panel)
             :anonymous (assoc db :main-panel :login-panel)
             :admin (do
                      (dispatch [:sync/state])
                      (assoc db :main-panel :root-panel)))))

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
                   :with-credentials? false                 ; Enable if using CORS (requires xhr v2+)
                   }
                  (fn async-callback [resp-map]
                      (dispatch [:chsk/reconnect]))))

(register-handler
  :login
  (fn [db [_ name pwd]]
      (ajax-login name pwd)
      db))

(register-handler
  :chsk/reconnect
  (fn [db]
      (chsk-reconnect!)
      (assoc db :ws/connected? false)))


;//       _
;//  __ ___)_____ __ _____
;//  \ V / / -_) V  V (_-<
;//   \_/|_\___|\_/\_//__/
;//
(register-handler
  :view/room
  (fn [db [_ [room-id segment]]]
      (if (= segment :current)
        (-> db
            (assoc :current/view :room)
            (assoc :current/room-id room-id))
        (-> db
            (assoc :current/view :room)
            (assoc :current/segment segment)
            (assoc :current/room-id room-id)))))


;//                _      _
;//   _ __  ___ __| |__ _| |___
;//  | '  \/ _ \ _` / _` | (_-<
;//  |_|_|_\___\__,_\__,_|_/__/
;//

(register-handler
  :modal/new-room
  (fn [db _]
      (log/debug ":modal/new-room")
      db))



;//      _        _
;//   ___ |_ __ _| |_ ___
;//  (_-<  _/ _` |  _/ -_)
;//  /__/\__\__,_|\__\___|
;//
(register-handler
  :sync/state
  (fn [db _]
      (chsk-send! [:sync/state]
                  8000                                      ; Timeout
                  (fn [reply]                               ; Reply is arbitrary Clojure data
                      (when (sente/cb-success? reply)       ; Checks for :chsk/closed, :chsk/timeout, :chsk/error
                            (dispatch [:state reply]))))
      db))

(register-handler
  :state
  (fn [db [_ state]]
      (let [rooms-map (mappify :id (:room state))
            lights-map (mappify :id (:light state))
            scenes-map (mappify :id (:scene state))]
           (assoc db
                  :room rooms-map
                  :light lights-map
                  :scene scenes-map))))

;//   _ _
;//  (_) |_ ___ _ __  ___
;//  | |  _/ -_) '  \(_-<
;//  |_|\__\___|_|_|_/__/
;//
(register-handler
  :item/create
  (fn [db [_ item-key item]]
      (let [msg-key (-> item-key
                        (name)
                        (str "/create")
                        keyword)]
           (chsk-send! [msg-key item])
           (assoc-in db [item-key (:id item)] item))))

(register-handler
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

(register-handler
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
(register-handler
  :room/update
  (fn [db [_ update]]
      (dispatch [:item/update :room update])
      db))


;//
;//   _____ ___ _ _  ___
;//  (_-< _/ -_) ' \/ -_)
;//  /__\__\___|_||_\___|
;//
(register-handler
  :scene/update
  (fn [db [_ update]]
      (let [id (:id update)
            original (get-in db [:scene id])
            patch (differ/diff original update)]

           ;;if the patch is empty, send the whole scene and a replace-flag

           (if (and (empty? (first patch)) (empty? (second patch)))
             (chsk-send! [:scene/update [id original :replace]])
             (chsk-send! [:scene/update [id patch]]))

           ;(log/debug "update scene")

           (assoc-in db [:scene id] update))))

;//                _      _
;//   _ __  ___ __| |__ _| |
;//  | '  \/ _ \ _` / _` | |
;//  |_|_|_\___\__,_\__,_|_|
;//
(register-handler
  :modal/open
  (fn [db [_ data]]
      (let [modal-id (:id data)
            options (or (:options data) {})
            item (:item data)
            ]
           (modal/open modal-id item options)
           db)))

(register-handler
  :modal/approve
  (fn [db [_ modal-id]]
      (log/debug ":modal/approve" modal-id)
      (condp = modal-id
             ;:new-light (do (dispatch [:light/make]))
             (comment "nothing yet"))
      db))

(register-handler
  :modal/deny
  (fn [db _]
      (dissoc db :pd/modal-node-data :new/light)))

(register-handler
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
(register-handler
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

(register-handler
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

(register-handler
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

(register-handler
  :light/prepare-trash
  (fn [db [_ light-id]]
      (dispatch [:modal/open {:id      :trash-item
                              :item    {:type :light
                                        :id   light-id}
                              :options {:closable true}}])
      db))
;//           _
;//   _ __ __| |
;//  | '_ \ _` |
;//  | .__\__,_|
;//  |_|

;// => see pd.handlers
