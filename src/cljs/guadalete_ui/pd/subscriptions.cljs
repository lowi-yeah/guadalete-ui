(ns guadalete-ui.pd.subscriptions
  (:require-macros
    [reagent.ratom :refer [reaction]])
  (:require
    [clojure.set :refer [difference]]
    [clojure.string :as str]
    [re-frame.core :refer [register-sub]]
    [thi.ng.color.core :as col]
    [guadalete-ui.console :as log]
    [guadalete-ui.util :refer [pretty]]
    [guadalete-ui.pd.util :refer [nil-node modal-room modal-scene modal-node nil-item]]
    [guadalete-ui.pd.color :refer [from-id]]))

(defn- get-item-reaction [db type id]
       (let [item (get-in @db [type id])]
            (reaction item)))

(defn- get-color-reaction [id]
       (reaction (from-id id)))

(register-sub
  :pd/node-item
  (fn [db [_ {:keys [id type]}]]
      (if (nil? id)
        (reaction (nil-item type))
        (condp = type
               :light (get-item-reaction db type id)
               :sgnl (get-item-reaction db type id)
               :color (get-color-reaction id)))))

(register-sub
  :pd/modal-node-data
  (fn [db _]
      (reaction (:pd/modal-node-data @db))))

(register-sub
  :pd/modal-node
  (fn [db _]
      (reaction (modal-node @db))))


(register-sub
  :pd/modal-item
  (fn [db _]
      (let [node (modal-node @db)
            type (keyword (:type node))
            id (:item-id node)]
           (condp = type
                  :color (get-color-reaction id)
                  (get-item-reaction db type id)))))

(defn- get-scene-item-ids [scene type]
       (let [all-nodes (get-in scene [:layout :nodes])
             nodes (filter #(= (:type %) type) all-nodes)
             ids (map #(:item-id %) nodes)]
            (into #{} ids)
            ))

(register-sub
  :pd/modal-select-options
  (fn [db _]
      (let [room (modal-room @db)
            scene (modal-scene @db)
            node (modal-node @db)
            type (keyword (:type node))

            ; the ids of all items (light, sensor ect…), which are linked to the room and thus can be used by the scene
            room-item-ids (set (get-in @db [:room (:id room) type]))

            ; the ids of the items that are already used by the scene
            scene-item-ids (get-scene-item-ids scene type)

            ; filter away those items that are already in the scene
            item-ids (difference room-item-ids scene-item-ids)
            ; re-add the item-id of the node (if it exists)
            item-ids (if (:item-id node) (cons (:item-id node) item-ids) item-ids)

            ; load the items
            items (map (fn [id] (get-in @db [type id])) item-ids)]
           (reaction items))))