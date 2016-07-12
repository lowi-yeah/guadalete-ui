;//   _         __ _                     _
;//  | |____ _ / _| |____ _   ____  _ ___ |_ ___ _ __
;//  | / / _` |  _| / / _` | (_-< || (_-<  _/ -_) '  \
;//  |_\_\__,_|_| |_\_\__,_| /__/\_, /__/\__\___|_|_|_|
;//                              |__/

(ns guadalete-ui.components.kafka
    (:require
      [clojure.core.async :as async :refer [chan >! >!! <! go go-loop thread close! alts!]]
      [com.stuartsierra.component :as component]
      [taoensso.timbre :as log]
      [clj-kafka.admin :as admin]
      [clj-kafka.consumer.zk :as zk]
      [clj-kafka.core :as kafka]
      [clj-kafka.zk :refer [broker-list brokers topics partitions]]
      [clj-kafka.offset :as offset :refer [fetch-consumer-offsets]]
      [guadalete-ui.helpers.util :refer [in? uuid]]))

;; hypothetical transformation
;(def xform (comp (map deserialize-message)
;                 (filter production-traffic)
;                 (map parse-user-agent-string)))

(def silly-xform (comp (map #(identity %))
                       (map #(identity %))))

(defn- log-it [x]
       (log/debug "logging it:" (str x))
       )

(defn- message-to-vec
       "returns a vector of all of the message fields"
       [^kafka.message.MessageAndMetadata message]
       [(.topic message) (.offset message) (.partition message) (.key message) (.message message)])


(defn- default-iterator
       "processing all streams in a thread and printing the message field for each message"
       [stream]
       (log/debug "default iterator." (str stream))
       (let [c (chan)
             uuid (uuid)]
            (go
              (doseq
                [^kafka.message.MessageAndMetadata message stream]
                (>! c (String. (nth (message-to-vec message) 4)))))
            c))

(defrecord Kafka [config topiks]
           component/Lifecycle
           (start [component]
                  (log/info "**************** Starting Kafka component ***************")
                  (log/debug "***** config" config)
                  (log/debug "***** topikz" topiks)
                  (let [c (zk/consumer config)
                        t (->>
                            (topics config)
                            (filter #(in? (vals topiks) %))
                            (map (fn [t] [t (partitions config t)]))
                            (into {}))
                        stream (zk/create-message-stream c "gdlt-sgnl-v")
                        stream-channel (default-iterator stream)
                        stop-channel (chan)

                        ;offets (fetch-consumer-offsets "127.0.0.1:9092" {"zookeeper.connect" "127.0.0.1:2181"} "gdlt-sgnl-v" "guadalete-ui.kafka-consumer")
                        ]
                       (log/debug "consumer" (str c))
                       (log/debug "topics" (str t))
                       (log/debug "stream-channel " (str stream-channel))

                       ;(log/debug "offets" (str offets))


                       (go-loop
                         []
                         (let [[msg ch] (alts! [stream-channel stop-channel])]
                              (when-not (= ch stop-channel)
                                        (log/debug msg)
                                        (recur))))

                       (assoc component :consumer c :stop-channel stop-channel)))

           (stop [component]
                 (log/info "Stopping Kafka component")
                 (zk/shutdown (:consumer component))
                 (>!! (:stop-channel component) :halt)
                 (dissoc component :consumer)))

(defn new-kafka [config]
      (map->Kafka config))