(ns guadalete-ui.items
  (:require
    [clojure.set :refer [difference]]
    [guadalete-ui.console :as log]
    [guadalete-ui.util :refer [pretty kw*]]))

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
    "w00t"))




;//                  _
;//   _ _ ___ ______| |_
;//  | '_/ -_)_-< -_)  _|
;//  |_| \___/__\___|\__|
;//
;(defn- reset-link [[id link]] [id (assoc link :state :normal)])
;(defn- reset-links [links] (into {} (map reset-link links)))
;(defn- reset-node
;  "Resets a node (selection, links, tmp-positions…)"
;  [[id node]]
;  (let [links* (reset-links (:links node))
;        node* (-> node
;                  (dissoc :pos-0)
;                  (assoc :selected false :links links*))]
;    [id node*]))
;
;(defn- reset-nodes [nodes]
;  (into {} (map reset-node) nodes))
;
;(defn reset-scene
;  "'Resets' a scene, ie. unselects all nodes and removes other flags that are only needed for frontend rendering."
;  [scene]
;  (let [nodes (-> scene
;                  (get :nodes)
;                  (reset-nodes))]
;    (-> scene
;        (assoc :nodes nodes))))
