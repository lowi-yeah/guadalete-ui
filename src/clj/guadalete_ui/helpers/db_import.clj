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

;(defn- uuid []
;       (str (clj-uuid/v4)))
;

;(defn- import-no-drop!
;       "generic function for importing data"
;       [table data]
;       (let [conn (:conn (:rethink-db system))
;             db (r/db (:db (:rethink-db system)))]
;            (-> (r/table table)
;                (r/insert data)
;                (r/run conn))
;            ))
;
;(defn- drop!
;       "generic function for importing data"
;       [table]
;       (let [conn (:conn (:rethink-db system))
;             db (r/db (:db (:rethink-db system)))]
;            (try
;              (-> (r/table-drop table)
;                  (r/run conn))
;              (catch Exception e (str "caught exception: " (.getMessage e))))
;            (-> (r/table-create db table)
;                (r/run conn))))
;

;
;;//       _
;;//    __| |_ __ ___ __  __
;;//   / _` | '_ ` _ \\ \/ /
;;//  | (_| | | | | | |>  <
;;//   \__,_|_| |_| |_/_/\_\
;;//
;(defn dmx
;      "imports all DMX channels into the database"
;      []
;      (let [data (json/read-str (slurp (json-path "dmx.json")) :key-fn keyword)]
;           (import! "dmx" data)))
;
;;//   _ _       _     _
;;//  | (_) __ _| |__ | |_ ___
;;//  | | |/ _` | '_ \| __/ __|
;;//  | | | (_| | | | | |_\__ \
;;//  |_|_|\__, |_| |_|\__|___/
;;//       |___/
;(defn light-essentials
;      "returns a light with only the essential inofmration (name, id, dmx-channels)"
;      [light]
;      (debugf "light essentials %s" light)
;      (dissoc light :numChannels :toggleOnly :state)
;      )
;
;(defn lights
;      "imports all lights into the database"
;      []
;      (let [data (json/read-str (slurp (json-path "lights.json")) :key-fn keyword)]
;           (import! "light" (map light-essentials data))))
;
;
;;//
;;//   ___  ___ _ __  ___  ___  _ __ ___
;;//  / __|/ _ \ '_ \/ __|/ _ \| '__/ __|
;;//  \__ \  __/ | | \__ \ (_) | |  \__ \
;;//  |___/\___|_| |_|___/\___/|_|  |___/
;;//
;(defn sensor-map
;      "returns a sensor with only the essential information"
;      [sensor]
;      (-> sensor
;          (assoc :online? (:isOnline sensor) :armed? false)
;          (dissoc :isOnline :roomId)))
;
;(defn sensors
;      "imports all sensors into the database"
;      []
;      (let [data (json/read-str (slurp (json-path "sensors.json")) :key-fn keyword)]
;           (import! "sensor" data)))




;;//      _                _
;;//   __(_)__ _ _ _  __ _| |___
;;//  (_-< / _` | ' \/ _` | (_-<
;;//  /__/_\__, |_||_\__,_|_/__/
;;//       |___/
;(defn signals []
;      (let [data (json/read-str (slurp (json-path "signals.json")) :key-fn keyword)]
;           (import! "signal" data)))
;
;;//
;;//   _____ ___ _ _  ___ ___
;;//  (_-< _/ -_) ' \/ -_)_-<
;;//  /__\__\___|_||_\___/__/
;
;(defn- make-scene-node [type item]
;       {:id       (uuid)
;        :item-id  item
;        :type     type
;        :position {:x (rand-int 400) :y (rand-int 400)}
;        })
;
;(defn- make-scene-nodes [type items]
;       (into [] (map #(make-scene-node type %) items)))
;
;(defn- make-scene-layout [scene]
;       (let [scene-lights (make-scene-nodes :light (:light scene))]
;            {:id       (uuid)
;             :scene-id (:id scene)
;             :node     scene-lights}))
;
;
;(defn- make-scene-layouts [scenes]
;       (let [layouts (into [] (map #(make-scene-layout %) scenes))]
;            (log/debug "made scene layouts" (str layouts))
;            (import! "scenelayout" layouts)
;            layouts))
;
;
;(defn- augment-layout [scene layouts]
;       (let [layout (first (filter #(= (:scene-id %) (:id scene)) layouts))]
;            (assoc scene :layout-id (:id layout))))
;
;(defn- augment-layouts [scenes layouts]
;       (map #(augment-layout % layouts) scenes))
;
;(defn scenes []
;      ;(drop! "scenelayoutnode")
;      (let [data (json/read-str (slurp (json-path "scenes.json")) :key-fn keyword)
;            layouts (make-scene-layouts data)
;            scenes (augment-layouts data layouts)]
;           (import! "scene" scenes)))
