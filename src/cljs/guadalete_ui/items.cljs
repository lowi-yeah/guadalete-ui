(ns guadalete-ui.items
  (:require [guadalete-ui.console :as log]
            [guadalete-ui.util :refer [pretty]]))

(defn- get-lights [light-ids db]
       (let [lights (map #(get-in db [:light %]) light-ids)]
            (into [] lights)))

(defn- get-scenes [scene-ids db]
       (let [scenes (map #(get-in db [:scene %]) scene-ids)]
            (into [] scenes)))

(defmulti assemble-item
          (fn [type db item]
              type))

(defmethod assemble-item :room
           [_ db room]
           (let [lights (get-lights (:light room) db)
                 scenes (get-scenes (:scene room) db)]
                (assoc room
                       :light lights
                       :scene scenes)))

(defn light-type
      "Returns the type of light (white, warm-cool-white, rgb, rgbw),
      depending on the number of channels"
      [light]
      (condp = (:num-channels light)
             1 "white"
             2 "warm-cool"
             3 "rgb"
             4 "rgbw"
             "w00t"
             )
      )