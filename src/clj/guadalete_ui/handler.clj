(ns guadalete-ui.handler
    (:require
      [guadalete-ui.html :as html]
      [guadalete-ui.helpers.session :as helper]
      [compojure.core :refer [routes GET POST ANY]]
      [compojure.route :as route]
      [ring.util.response :as util]
      [ring.middleware.defaults :refer [site-defaults]]
      [ring.middleware.format :refer [wrap-restful-format]]
      [clojure.core.match :as match :refer (match)]
      [environ.core :refer [env]]
      [taoensso.timbre :as log]))

(defn index []
      (-> (util/resource-response "index.html")
          (util/content-type "text/html")))

(defn ring-handler [{db :db}]
      (-> (routes
            ;(GET "/" [] (html/index))
            (GET "/" [] (index))
            (POST "/signin" req (let [session (assoc (:session req) :uid "John Doe")]
                                     (-> (util/response "John Doe")
                                         (assoc :session session))))
            (GET "/status" req (helper/status req))
            (GET "/logout" req (helper/logout req)))
          wrap-restful-format))

(def site
  (-> site-defaults
      (assoc-in [:static :resources] "/")))

(defn sente-handler [{db :db}]
      (fn [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
          (let [session (:session ring-req)
                headers (:headers ring-req)
                uid (:uid session)
                [id data :as ev] event]

               (log/debug "Session:" session)
               (log/debug "ev-msg:" ev-msg)
               (match [id data]
                      :else nil))))
