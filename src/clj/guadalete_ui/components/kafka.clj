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
      [cheshire.core :refer :all]
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
       (log/debug "logging it:" (str x)))

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
                (>! c (String. (nth (message-to-vec message) 4)))
                ))
            c))

(defn- send [topic message sente]
       (let [uids (:any @(:connected-uids sente))
             zend! (:chsk-send! sente)
             message* (parse-string message true)
             message** (->
                         {:id   (:id message*)
                          :data [(:at message*) (:data message*)]}
                         generate-string)]
            (doseq [uid uids]
                   (try
                     ;(log/debug "zend!" message**)
                     (zend! uid [topic message**])
                     (catch Exception e
                       (log/debug "EXCEPTION" e))))))

(defrecord Kafka [config topiks sente]
           component/Lifecycle
           (start [component]
                  (log/info "**************** Starting Kafka component ***************")
                  (log/debug "***** config" config)
                  (log/debug "***** topikz" topiks)
                  (log/debug "***** sente" sente)
                  (let [
                        t (->>
                            (topics config)
                            (filter #(in? (vals topiks) %))
                            (map (fn [t] [t (partitions config t)]))
                            (into {}))

                        ;config-consumer (zk/consumer config)
                        ;config-stream (zk/create-message-stream config-consumer "gdlt-sgnl-c")
                        ;config-channel (default-iterator config-stream)

                        value-consumer (zk/consumer config)
                        value-stream (zk/create-message-stream value-consumer "gdlt-sgnl-v")
                        value-channel (default-iterator value-stream)

                        stop-channel (chan)
                        ;offets (fetch-consumer-offsets "127.0.0.1:9092" {"zookeeper.connect" "127.0.0.1:2181"} "gdlt-sgnl-v" "guadalete-ui.kafka-consumer")
                        ]
                       (go-loop
                         []
                         ;(let [[msg ch] (alts! [value-channel config-channel stop-channel])]
                         (let [[msg ch] (alts! [value-channel stop-channel])]
                              (condp = ch
                                     ;config-channel (do
                                     ;                 (send :signal/config msg sente)
                                     ;                 (recur))
                                     value-channel (do
                                                     (send :signal/value msg sente)
                                                     (recur))
                                     (log/info "kafka consumer received stop signal"))))

                       (assoc component
                              ;:config-consumer config-consumer
                              :value-consumer value-consumer
                              :stop-channel stop-channel)))

           (stop [component]
                 (log/info "Stopping Kafka component")

                 ;(zk/shutdown (:config-consumer component))
                 (zk/shutdown (:value-consumer component))
                 (>!! (:stop-channel component) :halt)
                 (dissoc component :consumer)))

(defn new-kafka [config]
      (map->Kafka config))