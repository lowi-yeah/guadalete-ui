(ns guadalete-ui.helpers.db-import

  (:require
    [rethinkdb.query :as r]
    [clojure.data.json :as json]
    [cheshire.core :refer [parse-string]]
    [taoensso.timbre :as log]
    [guadalete-ui.helpers.util :refer [mappify]]
    [guadalete-ui.helpers.config :refer [load-config]]
    ))

(defn- json-path
  "A little helper to get the absolute path to the json files"
  [file]
  (str "/Users/lowi/code/github/guadaleteui/resources/json/" file))

(defn- import!
  "generic function for importing data"
  [conn table data]
  (try
    (-> (r/table-drop table)
        (r/run conn))
    (catch Exception e (str "caught exception: " (.getMessage e))))

  (-> (r/table-create table)
      (r/run conn))

  (-> (r/table table)
      (r/insert data)
      (r/run conn))
  )

(defn items [conn type]
  (let [data (parse-string (slurp (json-path (str type "s.json"))) true)]
    (import! conn type data))
  )

(defn truncate [conn table]
  (try
    (-> (r/table-drop table)
        (r/run conn))
    (catch Exception e (str "caught exception: " (.getMessage e))))

  (-> (r/table-create table)
      (r/run conn)))

;;//
;;//   _   _ ___  ___ _ __ ___
;;//  | | | / __|/ _ \ '__/ __|
;;//  | |_| \__ \  __/ |  \__ \
;;//   \__,_|___/\___|_|  |___/
;;//
(defn users [conn]
  (let [predefined-users [{:username "lowi"                 ;OBACHT: this is dangerous and for debugging only.
                           :password "$2a$10$yLL3yck2cA60sTHoF6.ZQusMgOYNjGFHUuj0hmc/dnhtfYMdQPbPm" ;(hash-bcrypt "sfx123")
                           :roles    [:admin]}
                          {:username "w00t"
                           :password "$2a$10$QVXfCO5OJnLv80O0sl82Pe/aHtBx78DaTnHWm67fHOqVjWKsDWxxu" ; (hash-bcrypt "f00")
                           :roles    [:user]}]]
    (import! conn "user" predefined-users)))

;//        _ _
;//   __ _| | |
;//  / _` | | |
;//  \__,_|_|_|
(defn all! []
  (let [{:keys [host port auth-key db]} (:rethinkdb (load-config))]
    (with-open [conn (r/connect :host host :port port :auth-key auth-key :db db)]
      (truncate conn "light")
      (truncate conn "color")
      (truncate conn "mixer")
      (items conn "dmx")
      (items conn "room")
      (items conn "scene")
      (items conn "signal")

      (users conn)
      )))
