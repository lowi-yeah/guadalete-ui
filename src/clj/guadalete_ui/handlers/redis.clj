(ns guadalete-ui.handlers.redis
  (:require
    [clojure.core.async :refer [go >!]]
    [taoensso.carmine :as car]
    [taoensso.timbre :as log]
    [clj-time.core :as time]
    [clj-time.coerce :as c]
    )
  )

(defmacro wcar* [connection & body] `(car/wcar ~connection ~@body))

(defn- fetch-event [connection prefix id]
  (let [key (str prefix ":" id)
        [id data at] (wcar* connection
                            (car/hmget key "id" "data" "at"))]
    {:id   id
     :at   at
     :data data}))

(defn- filter-by-signal-id [signal-id all-events]
  (let [events (->> all-events
                    (filter #(= signal-id (:id %)))
                    (map (fn [ev] [(:at ev) (:data ev)]))
                    (into []))]
    [signal-id events]))

(defn signal-values [connection signal-ids result-channel]
  (go
    (let [now (time/now)
          start (c/to-long (time/minus now (time/seconds 30)))
          stop (c/to-long now)
          event-ids (wcar* connection (car/zrangebyscore "set:sgnl" start stop))
          all-events (map #(fetch-event connection "sgnl" %) event-ids)
          events-by-signal (into {} (map #(filter-by-signal-id % all-events) signal-ids))]
      (>! result-channel events-by-signal))))