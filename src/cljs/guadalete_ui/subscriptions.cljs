(ns guadalete-ui.subscriptions
  (:require-macros
    [reagent.ratom :refer [reaction]])
  (:require
    [re-frame.core :refer [def-sub subscribe]]
    [thi.ng.geom.core.vector :refer [vec2]]
    [clojure.set :refer [difference]]
    [guadalete-ui.items :refer [assemble-item]]
    [guadalete-ui.util :refer [pretty in? kw*]]
    [guadalete-ui.dmx :as dmx]
    [guadalete-ui.console :as log]))


(def-sub
  :user/role
  (fn [db _]
    (:user/role db)))

;//       _
;//  __ ___)_____ __ __
;//  \ V / / -_) V  V /
;//   \_/|_\___|\_/\_/
;// Definitions of the containers

;; The hierarchy goes like this:
;; panel    ::= blank | login | admin (user to be made)
;;    blank ::= 'An empty panel'
;;    login ::= 'A login form'
;;    admin ::= sidebar section
;;
;; sidebar  ::= 'section navigation'
;; section  ::= dash | room
;;    dash  ::= 'a dashboard for the most important stats'
;;    room  ::= segment
;; segment  ::= scene | lights | sensors

(def-sub
  :view/panel
  (fn [db _]
    (get-in db [:view :panel])))

(def-sub
  :view/section
  (fn [db _]
    (get-in db [:view :section])))

(def-sub
  :view/segment
  (fn [db _]
    (get-in db [:view :segment])))


;Dynamic subscriptions need to pass a fn which takes app-db, the static vector, and the dereffed dynamic values.
(def-sub
  :view/room-id
  (fn [db _]
    (get-in db [:view :room-id])))

(def-sub
  :view/room
  (fn [db [_ {:keys [assemble?]}]]
    (let [room-id-rctn (subscribe [:view/room-id])
          room (get-in db [:room @room-id-rctn])]
      (if assemble?
        (assemble-item :room db room)
        room))))

(def-sub
  :view/scene-id
  (fn [db _]
    (get-in db [:view :scene-id])))

(def-sub
  :view/scene
  (fn [db [_ {:keys [assemble?]}]]
    (let [scene-id-rctn (subscribe [:view/scene-id])
          scene (get-in db [:scene @scene-id-rctn])]
      (if assemble?
        (assemble-item :scene db scene)
        scene))))

(def-sub
  :view/pd-dimensions
  (fn [db _]
    (let [dimensions (get-in db [:view :dimensions])
          view (vec2 (:view dimensions))
          header (vec2 (:header dimensions))
          offsets (vec2 144 24)
          width (max 0 (- (:x view) (:x offsets)))
          height (max 0 (- (:y view) (:y header) (:y offsets)))]
      (vec2 width height))))



;//
;//   _ _ ___ ___ _ __  ___
;//  | '_/ _ \ _ \ '  \(_-<
;//  |_| \___\___/_|_|_/__/
;//
(def-sub
  :rooms
  (fn [db _]
    (vals (:room db))))

(def-sub
  :room
  (fn [db [_ room-id]]
    (get-in db [:room room-id])))

;//
;//   _____ ___ _ _  ___
;//  (_-< _/ -_) ' \/ -_)
;//  /__\__\___|_||_\___|
;//
(def-sub
  :scene
  (fn [db [_ scene-id]]
    (get-in db [:scene scene-id])))

;//      _                _
;//   ____)__ _ _ _  __ _| |
;//  (_-< / _` | ' \/ _` | |
;//  /__/_\__, |_||_\__,_|_|
;//       |___/
(def-sub
  :signal/all
  (fn [db _]
    (->> (:signal db)
         (remove (fn [s] (nil? s))))))

;//      _
;//   __| |_ __ __ __
;//  / _` | '  \\ \ /
;//  \__,_|_|_|_/_\_\
;//
(def-sub
  :dmx/available
  (fn [db _]
    (dmx/assignable db)))

(def-sub
  :dmx/all
  (fn [db _]
    (dmx/all db)))

(def-sub
  :selected
  (fn [db _]
    (let [nodes (get-in db [:scene "scene2" :nodes])
          selected-nodes (filter (fn [[k v]] (:selected v)) nodes)]
      selected-nodes)))

;//                _      _
;//   _ __  ___ __| |__ _| |
;//  | '  \/ _ \ _` / _` | |
;//  |_|_|_\___\__,_\__,_|_|
;//


;Dynamic subscriptions allow you to create subscriptions that depend on Ratoms or Reactions (lets call them Signals).
; These subscriptions will be rerun when the Ratom or Reaction changes.
; You subscribe as usual with a vector like [:todos-list], and pass an additional vector of Signals.
; The Signals are dereferenced and passed to the handler-fn.
; Dynamic subscriptions need to pass a fn which takes app-db, the static vector, and the dereffed dynamic values.
(def-sub
  :modal/item-dynamic
  (fn modal-item-dynamic [db _ [data]]
    (let [id (:item-id data)
          ilk (:ilk data)]
      (get-in db [ilk id]))))

(def-sub
  :modal/items-dynamic
  (fn modal-item-dynamic [db _ [data]]
    (let [ilk (:ilk data)]
      (get db ilk))))

(def-sub
  :modal/data
  (fn [db _]
    (get db :modal)))

(def-sub
  :modal/item
  (fn [_ _]
    (let [data-rctn (subscribe [:modal/data])
          item-rctn (subscribe [:modal/item-dynamic] [data-rctn])]
      @item-rctn)))

(def-sub
  :modal/same-ilk-items
  (fn [_ _]
    (let [data-rctn (subscribe [:modal/data])
          items-rctn (subscribe [:modal/items-dynamic] [data-rctn])]
      @items-rctn)))




;//   _ _      _   _
;//  | (_)__ _| |_| |_
;//  | | / _` | ' \  _|
;//  |_|_\__, |_||_\__|
;//      |___/
(def-sub
  :light/unused-by-scene
  (fn [db [_ room-id scene-id]]
    (let [all-light-ids (into #{} (get-in db [:room room-id :light]))
          used-light-ids (->>
                           (get-in db [:scene scene-id :nodes])
                           (filter (fn [[id l]] (= :light (kw* (:ilk l)))))
                           (map (fn [[id l]] (:item-id l)))
                           (filter (fn [id] id))
                           (into #{}))
          unused (difference all-light-ids used-light-ids)
          lights (map #(get-in db [:light %]) unused)]
      (into [] lights))))


;//           _
;//   _ __ __| |
;//  | '_ \ _` |
;//  | .__\__,_|
;//  |_|
;// => see pd.subscriptions

;//   _   _                 _        _          _      _        _
;//  | |_| |_  ___  __ __ __ |_  ___| |___   __| |__ _| |_ __ _| |__ __ _ ______
;//  |  _| ' \/ -_) \ V  V / ' \/ _ \ / -_) / _` / _` |  _/ _` | '_ \ _` (_-< -_)
;//   \__|_||_\___|  \_/\_/|_||_\___/_\___| \__,_\__,_|\__\__,_|_.__\__,_/__\___|
;//
(def-sub :db (fn [db _] db))
