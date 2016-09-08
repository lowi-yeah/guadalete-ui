(ns guadalete-ui.pd.nodes.mixer
  (:require
    [clojure.set :refer [difference]]
    [re-frame.core :refer [dispatch def-event def-event-fx]]
    [thi.ng.geom.svg.core :as svg]
    [thi.ng.geom.core.vector :refer [vec2]]

    [guadalete-ui.util :refer [pretty vec->map validate!]]
    [guadalete-ui.pd.color :refer [make-color]]
    [guadalete-ui.console :as log]
    [schema.core :as s]
    [guadalete-ui.schema :as gs]

    [guadalete-ui.views.widgets :refer [sparky]]
    [guadalete-ui.pd.nodes.core :refer [node-title click-target]]
    [guadalete-ui.pd.nodes.link :refer [links]]
    [guadalete-ui.pd.layout :refer [node-width line-height node-height]]))



;//                   _
;//   _ _ ___ _ _  __| |___ _ _
;//  | '_/ -_) ' \/ _` / -_) '_|
;//  |_| \___|_||_\__,_\___|_|
;//
(s/defn render-node
  [scene-id node item selected?]
  (let [id (:id node)
        position (:position node)
        height (* line-height 4.5)
        link-offset 2
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
     [node-title (str "Mixer – " (name (:mixin-fn item)))]

     [svg/rect (vec2 0 0) node-width height
      {:rx    1
       :class "click-target"}]
     [links scene-id node link-offset]
     ]))

;//              _
;//   _ __  __ _| |_____
;//  | '  \/ _` | / / -_)
;//  |_|_|_\__,_|_\_\___|
;//

(s/defn ^:always-validate make :- gs/Mixer
  []
  {:id       (str (random-uuid))
   :mixin-fn :avg})

(defn- mixer-link [name index]
  {:id        (str "in-" index)
   :name      name
   :accepts   :value
   :direction :in
   :index     index})

(defn- make-mixer-links
  "Helper function for creating the in/out links for a mixer."
  []
  (let [out-link [{:id        "out"
                   :accepts   :value
                   :name      "out"
                   :direction :out
                   :index     2}]
        in-links [(mixer-link "first" 0)
                  (mixer-link "second" 1)]]
    (flatten [in-links out-link])))

(s/defn ^:always-validate make-node :- gs/Node
  [item :- gs/Mixer
   {:keys [position]} :- gs/NodeData]
  (let [node {:id       (str "mix-" (:id item))
              :ilk      :mixer
              :position (vec->map position)
              :item-id  (:id item)
              :links    (make-mixer-links)}]
    node))
