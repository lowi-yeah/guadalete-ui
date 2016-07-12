(ns guadalete-ui.helpers.util
    (:require
      [schema.core :as s]))

(defn deep-merge
      "Deep merge two maps"
      [& values]
      (if (every? map? values)
        (apply merge-with deep-merge values)
        (last values)))

(defn mappify
      "Generic convenience function for converting a collection into a map.
       As the key for the map, the given mak-key is being used.
       Returns a transducer when no collection is provided."
      [map-key coll]
      (into {} (map (fn [x] {(get x map-key) x}) coll))
      )

(defn in?
      "true if coll contains elm"
      [coll elm]
      (some #(= elm %) coll))

(defn uuid [] (str (java.util.UUID/randomUUID)))


; this is here due to a weird behaviour when loading the schema definition (@see cljc/redonaira/schema)
; since there seems to be no Vec2 in the thin/ng clojure (ie. not the clojure) implementation,
; one needs to be defined so that the compiler won't complain.
(def Vec2
  {:x s/Num
   :y s/Num})
