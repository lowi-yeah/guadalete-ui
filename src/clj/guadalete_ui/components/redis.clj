(ns guadalete-ui.components.redis
    (:require
      [clojure.core.async :as async :refer [chan >! >!! <! go go-loop thread close! alts!]]
      [com.stuartsierra.component :as component]
      [taoensso.timbre :as log]
      [taoensso.carmine :as car :refer [wcar]]
      ;[cheshire.core :refer :all]
      ;[guadalete-ui.helpers.util :refer [in? uuid]]
      )
    )


(defrecord Redis [host port]
           component/Lifecycle
           (start [component]
                  (log/info (str "**************** Starting Redis component: " host ":" port " ***************"))
                  (let [connection {:pool {} :spec {:host host :port port}}]
                       (assoc component :connection connection)))
           (stop [component]
                 (log/info "Stopping Redis component")
                 (dissoc component :connection)))

(defn new-redis [config]
      (map->Redis config))