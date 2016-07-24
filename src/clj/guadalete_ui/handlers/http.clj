(ns guadalete-ui.handlers.http
    (:require
      [compojure.core :refer [routes GET POST ANY]]
      [compojure.route :as route]
      [ring.util.response :as response]
      [ring.middleware.defaults :refer [site-defaults]]
      [ring.middleware.format :refer [wrap-restful-format]]
      [clojure.core.match :as match :refer (match)]
      [environ.core :refer [env]]
      [cemerick.friend :as friend]
      (cemerick.friend [workflows :as workflows]
                       [credentials :as creds])

      [guadalete-ui.helpers.session :as session]
      [guadalete-ui.handlers.database :as db]

      ))

(defn index []
      (-> (response/resource-response "index.html")
          (response/content-type "text/html")))

(defn- friend-config [db]
       {:allow-anon?         true
        :default-landing-uri "/"
        :credential-fn       #(creds/bcrypt-credential-fn (db/all-users-as-map (:conn db)) %)
        :workflows           [(workflows/interactive-form)]})

(defn ring-handler [{db :db}]
      (-> (routes
            (GET "/" [] (index))

            ; ---- login/logout --------------------
            ; login but redirects to the index page. sorting out whether the user is authenticated
            ; or not is done during page load.
            ; doing so is required, since friend automatically redirects to '/login' if authorization fails.
            (GET "/login" _ (response/redirect "/"))

            ;(GET "/status" req (session/status req))
            ;(GET "/logout" req (session/logout req))
            )
          (friend/authenticate (friend-config db))
          wrap-restful-format))

(def site
  (-> site-defaults
      (assoc-in [:static :resources] "/")
      (assoc-in [:security :anti-forgery] {:read-token (fn [req] (-> req :params :csrf-token))})
      (assoc-in [:session :store] (session/ttl-memory-store))))
