(ns guadalete-ui.dmx
  (:require [clojure.set :refer [difference]]
            [guadalete-ui.console :as log]
            [guadalete-ui.util :refer [pretty]]))

(defn dmx []
      (fn [dmx-rctn]
          [:div.side-margins
           [:h1 "DMX"]
           [:pre.code] (pretty @dmx-rctn)]

          ))

(defn all [db]
      (let [assigned-dmx (flatten (into [] (map #(:channels %)) (vals (:light db))))
            all-dmx (set (range 1 513))
            result (-> (difference all-dmx assigned-dmx)
                       (into [])
                       (sort))]
           result)
      )

(defn assignable [db]
      (let [lights (vals (:light db))
            assigned-dmx (set (flatten (into [] (map #(:channels %)) lights)))
            new-light-dmx (set (flatten (into [] (:channels (:new/light db)))))
            all-dmx (set (range 1 513))
            result (difference all-dmx assigned-dmx new-light-dmx)]
           result))
