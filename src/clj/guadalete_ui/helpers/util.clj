(ns guadalete-ui.helpers.util)

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