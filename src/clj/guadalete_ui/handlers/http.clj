(ns guadalete-ui.handlers.http
    (:require
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
            ;(GET "/status" req (helper/status req))
            ;(GET "/logout" req (helper/logout req))
            )
          wrap-restful-format))

(def site
  (-> site-defaults
      (assoc-in [:static :resources] "/")))
