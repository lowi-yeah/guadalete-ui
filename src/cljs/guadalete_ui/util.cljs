(ns guadalete-ui.util
  (:require-macros [cljs.core.async.macros :refer [go-loop]]
                   [cljs.core :refer [or]])

  (:require [cljs.core.async :as async :refer [<! >! chan close! put! to-chan timeout]]
            [guadalete-ui.console :as log]))

(defn debounce [in ms]
      (let [out (chan)]
           (go-loop [last-val nil]
                    (let [val (if (nil? last-val) (<! in) last-val)
                          timer (timeout ms)
                          [new-val ch] (alts! [in timer])]
                         (condp = ch
                                timer (do (>! out val) (recur nil))
                                in (recur new-val))))
           out))

(defn mappify
      "Generic convenience function for converting a collection into a map.
       As the key for the map, the given mak-key is being used.
       Returns a transducer when no collection is provided."
      ;([map-key]
      ; (fn [rf]
      ;   (fn
      ;     ([] (rf))
      ;     ([result] (rf result))
      ;     ([result input]
      ;      (rf result (f input)))
      ;     ([result input & inputs]
      ;      (rf result (apply f input inputs))))))
      [map-key coll]
      (into {} (map (fn [x] {(get x map-key) x}) coll))
      )

(defn recordify
      [data map->type]
      (let [maping-fn (fn [x] {(key x) (map->type (val x))})
            record-map (into {} (map maping-fn data))]
           record-map))

(defn contains-value? [coll element]
      (boolean (some #(= element %) coll)))

(defn contains-any-values? [coll elements]
      "Returns true if the collection contains any one of the elements"
      ;(log/debug "contains-any-values?" coll elements (or (map #(contains-value? coll %) elements)))
      (reduce #(or %2 %1) (map #(contains-value? coll %) elements)))

(defn- get-by-key [coll key val]
       "Helper for getting an element inside an array based on a given key"
       (first (filter #(= val (get % key)) coll))
       )

(defn- get-by-id [coll val]
       "Helper for getting an element inside an array based on the :id key"
       (get-by-key coll :id val)
       )