(ns guadalete-ui.pd.subscriptions
  (:require-macros
    [reagent.ratom :refer [reaction]])
  (:require
    [clojure.set :refer [difference]]
    [clojure.string :as str]
    [re-frame.core :refer [def-sub]]
    [thi.ng.color.core :as col]
    [guadalete-ui.console :as log]
    [guadalete-ui.util :refer [pretty kw*]]
    [guadalete-ui.pd.util :refer [nil-node modal-room modal-scene modal-node nil-item]]
    [guadalete-ui.pd.color :refer [from-id]]))

(defn- get-item [db ilk id]
  (get-in db [ilk id]))

(defn- get-color-reaction [db ilk id]
  (get-in db [ilk id]))

(def-sub
  :pd/node-item
  (fn [db [_ {:keys [id ilk]}]]
    (if (nil? id)
      (nil-item ilk)
      (get-item db ilk id))))

(def-sub
  :pd/modal-node-data
  (fn [db _]
    (:pd/modal-node-data db)))

(def-sub
  :pd/modal-node
  (fn [db _]
    (modal-node db)))


(def-sub
  :pd/modal-item
  (fn [db _]
    (let [node (modal-node db)
          ilk (kw* (:ilk node))
          id (:item-id node)]
      (get-item db ilk id))))

(defn- get-scene-item-ids [scene ilk]
  (let [all-nodes (:nodes scene)
        nodes (filter #(= (kw* (:ilk %)) ilk) (vals all-nodes))
        ids (map #(:item-id %) nodes)]
    (into #{} ids)))

(def-sub
  :pd/modal-select-options
  (fn [db _]
    (let [room (modal-room db)
          scene (modal-scene db)
          node (modal-node db)
          ilk (keyword (:ilk node))
          ; the ids of all items (light, sensor ect…), which are linked to the room and thus can be used by the scene
          room-item-ids (set (get-in db [:room (:id room) ilk]))
          ; the ids of the items that are already used by the scene
          scene-item-ids (get-scene-item-ids scene ilk)
          ; filter away those items that are already in the scene
          item-ids (difference room-item-ids scene-item-ids)
          ; re-add the item-id of the node (if it exists)
          item-ids* (if (:item-id node) (cons (:item-id node) item-ids) item-ids)
          ; load the items
          items (map (fn [id] (get-in @db [ilk id])) item-ids*)]
      items)))