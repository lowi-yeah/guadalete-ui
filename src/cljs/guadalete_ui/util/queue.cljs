(ns guadalete-ui.util.queue
  (:require
    [guadalete-ui.console :as log]))

(defn make
      "Create a new timestamp-keyed queue"
      []
      ;return an empty sorted set
      (sorted-map))

(defn pop
      "Return a new queue without the oldest entry"
      [q]
      (rest q))

(defn peek
      "Return the first (i.e. oldest) entry"
      [q]
      (first q))

(defn empty?
      "Returns true if the queue is empty"
      [q]
      (not (boolean (seq q))))

(defn push
      "Push an entry into the end of the queue"
      [q timestamp value]
      (into (sorted-map) (conj q {timestamp value})))

(defn truncate
      "Return a new queue with all entries older than timespamp removed"
      [q threshold]
      (remove (fn [[timestamp value]] (< timestamp threshold)) q))

