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
      )

(init)