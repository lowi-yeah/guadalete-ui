(ns guadalete-ui.core
  (:require
    [clojure.string :as s]
    [taoensso.sente :as sente]
    [system.components.sente :refer [new-channel-socket-client]]
    [com.stuartsierra.component :as component]
    [cljs-utils.core :as utils :refer [by-id]]

    [goog.events :as events]
    [reagent.core :as reagent]
    [re-frame.core :as re-frame]

    [guadalete-ui.handlers]
    [guadalete-ui.socket :as socket]
    [guadalete-ui.subscriptions]
    [guadalete-ui.views :as views]
    [guadalete-ui.routes :as routes]
    [guadalete-ui.util :refer [contains-value?]]
    [guadalete-ui.console :as log])

  (:import [goog.history Html5History]))

(def history (Html5History.))
(doto history
      (.setUseFragment false)
      (.setPathPrefix "")
      (.setEnabled true))
;
;(defn dev-mode? []
;      (let [domain (.. js/window -location -hostname)]
;           (= domain "localhost")))
;
;(if (dev-mode?)
;  (enable-console-print!)
;  (set! *print-fn*
;        (fn [& args]
;            (do))))
;
;(def app-state (atom {:view  :root
;                      :flash {}}))
;
;(def flash #(om/ref-cursor (:flash (om/root-cursor app-state))))
;
;
;(defn event-handler [event data owner]
;      (.log js/console "Event: %s" (pr-str event))
;      (match [event]
;             [[:chsk/state state]] (do (.log js/console "state change: %s" (pr-str state))
;                                       (if (= (:uid state) :taoensso.sente/nil-uid)
;                                         (om/set-state! owner :session :unauthenticated)
;                                         (do (om/set-state! owner :session :authenticated)
;                                             (om/transact! data :uid (fn [_] (:uid state))))))
;             [[:chsk/handshake _]] (f/info flash "Sente handshake")
;             [[:chsk/recv [:demo/flash payload]]] (do (.log js/console "Flash:" payload)
;                                                      (f/info flash (:message payload)))
;             [[:chsk/recv payload]] (.log js/console "Push event from server: %s" (pr-str payload))
;             :else (.log js/console "Unmatched event: %s" event)))
;
;(defn event-loop
;      "Handle inbound events."
;      [data owner]
;      (go-loop []
;               (let [{:as ev-msg :keys [event]} (<! (:ch-chsk sente-client))]
;                    (event-handler event data owner)
;                    (recur))))
;;; sente
;
;(defn login
;      "Om component for new login"
;      [data owner]
;      (reify
;        om/IDisplayName
;        (display-name [this]
;                      "login")
;        om/IRender
;        (render [_]
;                (dom/a #js {:href    "signin"
;                            :onClick (fn [e]
;                                         (let [url (.-href (.-target e))
;                                               csrf-token (:csrf-token @chsk-state)]
;                                              (.preventDefault e)
;                                              (POST url
;                                                    {:headers       {"X-CSRF-Token" csrf-token}
;                                                     :handler       (fn [response] (sente/chsk-reconnect! chsk))
;                                                     :error-handler (fn [{:keys [status status-text]}]
;                                                                        (f/alert flash (str status ": " status-text)))})))}
;                       "Sign in"))))
;
;(defn header
;      "Om component for new header"
;      [data owner]
;      (reify
;        om/IDisplayName
;        (display-name [this]
;                      "header")
;        om/IRenderState
;        (render-state [_ state]
;                      (let [left [["FAQ" #(.setToken history "/faq")]
;                                  ["Contact" #(set! (.. js/window -location -href) "mailto:daniel.szmulewicz@gmail.com?&subject=Demo")]]]
;                           (h/header data owner {:brand           ["system helpers" #(.setToken history "/")]
;                                                 :left            left
;                                                 :authenticated   [(dom/a #js {:href "/logout"} "Sign out")]
;                                                 :unauthenticated [(om/build login data)]})))))
;
;(defn ui
;      "Om component for main ui"
;      [data owner]
;      (reify
;        om/IDisplayName
;        (display-name [this]
;                      "Main ui")
;        om/IRender
;        (render [_]
;                (dom/div nil
;                         (dom/div #js {:className "panel panel-default"})))))
;
;(declare not-found)
;(defn app [data owner]
;      (reify
;        om/IInitState
;        (init-state [this]
;                    {:session :unauthenticated})
;        om/IDisplayName
;        (display-name [this]
;                      "app")
;        om/IWillMount
;        (will-mount [this]
;                    (event-loop data owner))
;        om/IRenderState
;        (render-state [this state]
;                      (dom/div nil
;                               (om/build header data {:react-key "header" :state state})
;                               (om/build f/widget {:flash flash :timeout 2})
;                               (when (dev-mode?) (om/build ankha/inspector data))
;                               (condp = (:view data)
;                                      :not-found (om/build not-found data)
;                                      :root (case (:session state)
;                                                  :authenticated (om/build ui data {:state state})
;                                                  :unauthenticated (om/build ui data)))))))

;(om/root app app-state
;         {:target    (by-id "main")
;          :tx-listen (fn [{:keys [path old-value new-value old-state new-state tag] :as tx-data}
;                          root-cursor]
;                         (match [path]
;                                [[:uid]] (js/setTimeout #(f/bless flash (str "Welcome " new-value)) 1000)
;                                :else (.log js/console (str "no match with cursor path " path))))})




;//         _
;//   _ __ (_)_ _  ___
;//  | '  \| | ' \/ -_)
;//  |_|_|_|_|_||_\___|
;//


;//              _
;//   ______ _ _| |_ ___
;//  (_-< -_) ' \  _/ -_)
;//  /__\___|_||_\__\___|
;//
;;; Add this: --->
;(let [{:keys [chsk ch-recv send-fn state]}
;      (sente/make-channel-socket! "/chsk"                   ; Note the same path as before
;                                  {:type :auto              ; e/o #{:auto :ajax :ws}
;                                   })]
;     (def chsk chsk)
;     (def ch-chsk ch-recv)                                  ; ChannelSocket's receive channel
;     (def chsk-send! send-fn)                               ; ChannelSocket's send API fn
;     (def chsk-state state)                                 ; Watchable, read-only atom
;     )


(def INPUTTYPE_WHITELIST
  ["TEXT" "PASSWORD" "SEARCH" "EMAIL" "NUMBER" "DATE" "FILE"])

(defn- swallow-backspace []
       "Register a new 'keydown' handler which captures 'backspace' and 'DEL' key-events and dispatches them via reframe
       instead of handling the event as ususal. This is required for PD in order to delete nodes."
       (-> (js/$ js/document)
           (.unbind "keydown")
           (.bind "keydown" (fn [ev]
                                (if (or (= (.-keyCode ev) 8) (= (.-keyCode ev) 46)) ; backspace or DEL key
                                  (let [d (or (.-target ev) (.-srcElement ev))
                                        tag-name (s/upper-case (.-tagName d))
                                        input? (= tag-name "INPUT")
                                        editable? (and input? (not (or (.-readOnly d) (.-disabled d))))
                                        type (s/upper-case (or (.-type d) "none"))
                                        whitelisted? (contains-value? INPUTTYPE_WHITELIST type)
                                        do-prevent (not (and input? whitelisted? editable?))]
                                       (if do-prevent
                                         (do
                                           (.preventDefault ev)
                                           (re-frame/dispatch [:backspace])))))))))


(defn- mount-root []
       (reagent/render [views/main-panel] (by-id "main")))

(defn ^:export init []
      (re-frame/dispatch-sync [:initialize-db])
      (routes/app-routes)
      (mount-root)
      ;(swallow-backspace)
      (socket/event-loop)
      (js/setTimeout socket/test-session 1000)
      )

(init)