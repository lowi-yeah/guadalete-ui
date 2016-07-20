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


(defrecord Redis [host port topics]
           component/Lifecycle
           (start [component]
                  (log/info (str "**************** Starting Redis component: " host ":" port " ***************"))
                  (log/info (str "topics" (str topics)))
                  (let [connection {:pool {} :spec {:host host :port port}}]
                       (assoc component :connection connection :topics topics)))
           (stop [component]
                 (log/info "Stopping Redis component")
                 (dissoc component :connection :topics)))

(defn new-redis [config]
      (map->Redis config))