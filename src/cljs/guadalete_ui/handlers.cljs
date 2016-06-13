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
           (log/debug "update item " item-key msg-key (pretty update))
           ;;if the patch is empty, send the whole item together with a replace-flag
           (if (and (empty? (first patch)) (empty? (second patch)))
             (chsk-send! [msg-key [id original :replace]])
             (chsk-send! [msg-key [id patch]]))
           (assoc-in db [item-key id] update))))


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
  (fn [db [_ modal-id]]
      (modal/open modal-id)
      db))

(register-handler
  :modal/approve
  (fn [db [_ modal-id]]
      (log/debug ":modal/approve" modal-id)
      (condp = modal-id
             :new-light (do (dispatch [:light/make]))
             :default (comment "nothing yet"))
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
                       :channels     [[(first channels)]]}]
           (dispatch [:modal/open :new-light])
           (assoc db :new/light new-light))))

(register-handler
  :light/make
  (fn [db _]
      (let [new-light (:new/light db)
            new-light* (if (= "" (:name new-light))
                         (assoc new-light :name "anonymous")
                         new-light)
            room (get-in db [:room (:room-id new-light*)])
            room-lights* (conj (:light room) (:id new-light*))
            room* (assoc room :light room-lights*)]

           (dispatch [:item/create :light new-light*])
           (dispatch [:room/update room*])
           (-> db
               (dissoc :new/light)))))


(register-handler
  :light/update
  (fn [db [_ update]]
      (dispatch [:item/update :light update])
      db))


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
            channels (:channels light)
            num-channels (:num-channels light)
            range (range num-channels (+ num-channels additional))
            channels* (into channels (map (fn [i] [(nth assignables i)]) range))]
           (into [] channels*)))

(register-handler
  :light/update-new
  (fn [db [_ light*]]
      (let [light (:new/light db)
            num-channels* (:num-channels light*)
            num-channels (:num-channels light)
            channels* (cond
                        (< num-channels* num-channels) (remove-channels light* num-channels num-channels*)
                        (> num-channels* num-channels) (add-channels light (- num-channels* num-channels) db)
                        :default (:channels light*))
            light* (assoc light* :channels channels*)]
           (log/debug "num-channels*" num-channels*)
           (assoc db :new/light light*))))

;//           _
;//   _ __ __| |
;//  | '_ \ _` |
;//  | .__\__,_|
;//  |_|

;// => see pd.handlers
