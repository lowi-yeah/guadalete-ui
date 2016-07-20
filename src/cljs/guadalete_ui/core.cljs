(ns guadalete-ui.core
  (:require-macros [secretary.core :refer [defroute]])
  (:require
    [clojure.string :as s]
    [taoensso.sente :as sente]
    [schema.core :as schema]
    [secretary.core :as secretary]
    [system.components.sente :refer [new-channel-socket-client]]
    [com.stuartsierra.component :as component]
    [cljs-utils.core :as utils :refer [by-id]]

    [goog.events :as events]
    [reagent.core :as reagent]
    [re-frame.core :as re-frame]
    [devtools.core :as devtools]

    [guadalete-ui.handlers]
    [guadalete-ui.subscriptions]
    [guadalete-ui.pd.handlers]
    [guadalete-ui.pd.subscriptions]
    [guadalete-ui.socket :as socket]
    [guadalete-ui.views :as views]
    [guadalete-ui.routes :as routes]
    [guadalete-ui.util :refer [contains-value?]]
    [guadalete-ui.console :as log])

  (:import [goog History]
           [goog.history EventType]))

;; -- Debugging aids ----------------------------------------------------------
(devtools/install!)       ;; we love https://github.com/binaryage/cljs-devtools


(def history
  (doto (History.)
    (events/listen EventType.NAVIGATE
                   (fn [event] (secretary/dispatch! (.-token event))))
    (.setEnabled true)))

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

      ;(schema/set-fn-validation! true)
      (schema/set-fn-validation! false)

      (re-frame/dispatch-sync [:initialize-db])
      (routes/app-routes)
      (mount-root)
      ;(swallow-backspace)
      (socket/event-loop)
      )

(init)