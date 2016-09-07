(ns guadalete-ui.pd.nodes.signal
  (:require
    [clojure.set :refer [difference]]
    [re-frame.core :refer [dispatch def-event def-event-fx]]
    [thi.ng.geom.svg.core :as svg]
    [thi.ng.geom.core.vector :refer [vec2]]

    [guadalete-ui.util :refer [pretty vec->map]]
    [guadalete-ui.pd.color :refer [make-color]]
    [guadalete-ui.console :as log]
    [schema.core :as s]
    [guadalete-ui.schema :as gs]

    [guadalete-ui.views.widgets :refer [sparky]]
    [guadalete-ui.pd.nodes.core :refer [node-title click-target]]
    [guadalete-ui.pd.nodes.link :refer [links]]
    [guadalete-ui.pd.layout :refer [node-width line-height node-height]]))

;//   _        _
;//  | |_  ___| |_ __ ___ _ _ ___
;//  | ' \/ -_) | '_ \ -_) '_(_-<
;//  |_||_\___|_| .__\___|_| /__/
;//             |_|
(defn- find-unused-signal
  "Finds a signal which is not yet in use by the given scene.
  Used for assigning a signal during make-node"
  [{:keys [scene-id]} db]
  (let [all-signal-ids (->>
                         (get-in db [:signal])
                         (into [])
                         (map #(first %))
                         (into #{}))
        used-signal-ids (->>
                          (get-in db [:scene scene-id :nodes])
                          (filter (fn [[_ l]] (= :signal (keyword (:ilk l)))))
                          (map (fn [[_ l]] (:item-id l)))
                          (filter (fn [id] id))
                          (into #{}))
        unused (difference all-signal-ids used-signal-ids)]
    (first unused)))

(s/defn render-node
  [scene-id node item selected?]
  (let [id (:id node)
        position (:position node)
        height (* line-height 4)
        link-offset 3.5
        ]
    [svg/group
     {:id            id
      :class         (if selected? "signal node selected" "signal node")
      :transform     (str "translate(" (:x position) " " (:y position) ")")
      :data-type     "node"
      :data-scene-id scene-id
      :data-ilk      (:ilk node)}

     [svg/rect (vec2 0 0) node-width height
      {:class "bg"
       :rx    1}]

     [node-title "Signal"]

     [sparky item
      {:position  (vec2 0 (* line-height 2))
       :dimension (vec2 node-width line-height)}]

     [svg/text
      (vec2 4 (+ 12 line-height))
      (str (:name item))
      {:class       "node-text"
       :text-anchor "left"}]
     [click-target height]
     [links scene-id node link-offset]
     ]))

(s/defn ^:always-validate make-node :- gs/Node
  [db :- gs/DB
   {:keys [position] :as data} :- gs/NodeData]
  (let [node-id (str "sgnl-" (random-uuid))
        item-id (find-unused-signal data db)]
    {:id       node-id
     :ilk      :signal
     :position (vec->map position)
     :item-id  item-id
     :links    [{:id        "out"
                 :accepts   :value
                 :direction :out
                 :index     0}]}))
