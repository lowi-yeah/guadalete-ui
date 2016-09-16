(ns guadalete-ui.handlers.defaults
    (:require
      [taoensso.timbre :as log]
      [schema.core :as s]
      [guadalete-ui.schema :as gs]))



(s/defn one
        [item type]
        (condp = type
               :user item
               :room item
               :light (merge {:name (:id item)} item)
               :scene item
               :signal (merge {:name (:id item)} item)
               :color item
               (log/error (str "Cannot make default values for item: " item ". Dunno item type: " type))))

(defn all
      [coll type]
      (->> coll (map #(one % type))))