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


(defn- find-unused-light
  "Finds a light inside a room which is not yet in use by the given scene.
  Used for assigning a light during pd/light-node-creation"
  [{:keys [room-id scene-id]} db]

  (let [all-light-ids (into #{} (get-in db [:room room-id :light]))
        used-light-ids (->>
                         (get-in db [:scene scene-id :nodes])
                         (filter (fn [[id l]] (= :light (kw* (:ilk l)))))
                         (map (fn [[id l]] (:item-id l)))
                         (filter (fn [id] id))
                         (into #{}))
        unused (difference all-light-ids used-light-ids)]
    (first unused)))

(defn- find-unused-signal
  "Finds a signal which is not yet in use by the given scene.
  Used for assigning a signal during pd/signal-node-creation"
  [{:keys [room-id scene-id]} db]

  (let [all-signal-ids (->> (get-in db [:signal])
                            (into [])
                            (map #(first %))
                            (into #{}))
        used-signal-ids (->>
                          (get-in db [:scene scene-id :nodes])
                          (filter (fn [[id l]] (= :signal (kw* (:ilk l)))))
                          (map (fn [[id l]] (:item-id l)))
                          (filter (fn [id] id))
                          (into #{}))
        unused (difference all-signal-ids used-signal-ids)]
    (log/debug "find-unused-signal" (str used-signal-ids))
    (log/debug "all-signal-ids" (str all-signal-ids))
    (first unused)))


;//                  _
;//   _ _ ___ ______| |_
;//  | '_/ -_)_-< -_)  _|
;//  |_| \___/__\___|\__|
;//
(defn- reset-link [[id link]] [id (assoc link :state :normal)])
(defn- reset-links [links] (into {} (map reset-link links)))
(defn- reset-node
  "Resets a node (selection, links, tmp-positions…)"
  [[id node]]
  (let [links* (reset-links (:links node))
        node* (-> node
                  (dissoc :pos-0)
                  (assoc :selected false :links links*))]
    [id node*]))

(defn- reset-nodes [nodes]
  (into {} (map reset-node) nodes))

(defn reset-scene
  "'Resets' a scene, ie. unselects all nodes and removes other flags that are only needed for frontend rendering."
  [scene]
  (let [nodes (-> scene
                  (get :nodes)
                  (reset-nodes))]
    (-> scene
        (assoc :nodes nodes)
        (dissoc :mode))))
