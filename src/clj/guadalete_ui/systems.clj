(ns guadalete-ui.systems
    (:require
      [guadalete-ui.handlers.http :refer [ring-handler site]]
      [guadalete-ui.handlers.socket :as socket :refer [sente-handler]]
      [guadalete-ui.helpers.config :refer [load-config]]
      [guadalete-ui.middleware
       [not-found :refer [wrap-not-found]]]
      [com.stuartsierra.component :as component]
      [ring.middleware.defaults :refer [wrap-defaults]]
      [taoensso.sente.server-adapters.immutant :refer (sente-web-server-adapter)]
      [taoensso.timbre :as log]
      [taoensso.timbre.appenders.core :as appenders]
      (system.components
        [immutant-web :refer [new-web-server]]
        [sente :refer [new-channel-socket-server sente-routes]]
        [repl-server :refer [new-repl-server]]
        [endpoint :refer [new-endpoint]]
        [handler :refer [new-handler]]
        [middleware :refer [new-middleware]])
      [environ.core :refer [env]]
      (guadalete-ui.components
        [rethinkdb :refer [new-rethink-db]]
        [kafka :refer [new-kafka]]
        [redis :refer [new-redis]])))

;//                __ _                     _               _
;//   __ ___ _ _  / _(_)__ _ _  _ _ _ ___  | |___ __ _ __ _(_)_ _  __ _
;//  / _/ _ \ ' \|  _| / _` | || | '_/ -_) | / _ \ _` / _` | | ' \/ _` |
;//  \__\___/_||_|_| |_\__, |\_,_|_| \___| |_\___\__, \__, |_|_||_\__, |
;//                    |___/                     |___/|___/       |___/
(log/set-level! :debug)
(log/merge-config!
  {:appenders {:spit (appenders/spit-appender {:fname "log/guadalete-ui.log"})}})

;//      _             _                        _
;//   __| |_____ _____| |___ _ __ _ __  ___ _ _| |_
;//  / _` / -_) V / -_) / _ \ '_ \ '  \/ -_) ' \  _|
;//  \__,_\___|\_/\___|_\___/ .__/_|_|_\___|_||_\__|
;//                         |_|

;(defsystem dev-system [])

(defn dev-system
      "Assembles and returns components for a base application"
      []
      (let [config (load-config)]
           (component/system-map
             :db (new-rethink-db (:rethinkdb config))
             :redis (new-redis (:redis config))
             :sente (component/using
                      (new-channel-socket-server
                        sente-handler
                        sente-web-server-adapter
                        {:wrap-component?   true
                         :handshake-data-fn socket/handshake-data-fn
                         :user-id-fn        socket/user-id-fn})
                      [:db :redis])
             :kafka (component/using (new-kafka (:kafka config)) [:sente])
             :sente-endpoint (component/using
                               (new-endpoint sente-routes)
                               [:sente])
             :routes (component/using
                       (new-endpoint ring-handler)
                       [:db])
             :middleware (new-middleware {:middleware [[wrap-defaults :defaults]
                                                       [wrap-not-found :not-found]]
                                          :defaults   site
                                          :not-found  "<h2>The requested page does not exist.</h2>"})
             :handler (component/using
                        (new-handler)
                        [:sente-endpoint :routes :middleware])
             :http (component/using
                     (new-web-server (Integer. (env :http-port)))
                     [:handler]))))

;//                   _         _   _
;//   _ __ _ _ ___ __| |_  _ __| |_(_)___ _ _
;//  | '_ \ '_/ _ \ _` | || / _|  _| / _ \ ' \ _ _ _
;//  | .__/_| \___\__,_|\_,_\__|\__|_\___/_||_(_)_)_)
;//  |_|
(comment (defsystem prod-system …))