(ns guadalete-ui.pd.nodes.constant
  (:require
    [clojure.set :refer [difference]]
    [re-frame.core :refer [dispatch def-event def-event-fx]]
    [thi.ng.geom.svg.core :as svg]
    [thi.ng.geom.core.vector :refer [vec2]]

    [guadalete-ui.util :refer [pretty vec->map]]
    [guadalete-ui.console :as log]
    [schema.core :as s]
    [guadalete-ui.schema :as gs]

    [guadalete-ui.views.widgets :refer [sparky]]
    [guadalete-ui.pd.nodes.core :refer [node-title click-target]]
    [guadalete-ui.pd.nodes.link :refer [links]]
    [guadalete-ui.pd.color :refer [render-color]]
    [guadalete-ui.pd.layout :refer [node-width line-height node-height]]))


;//                   _
;//   _ _ ___ _ _  __| |___ _ _
;//  | '_/ -_) ' \/ _` / -_) '_|
;//  |_| \___|_||_\__,_\___|_|
;//
(s/defn render-node
        [scene-id node item selected?]
        (let [
              id (:id node)
              position (:position node)
              height (* line-height 3)
              ;hackedy hack:
              ;to make the rendering a bit nicer, take the brightness value and set it as the alpha channel
              ;that way not a black splot gets rendered when the birightness is low, but rather a transparent one
              link-offset 2.5]
             [svg/group
              {:id            id
               :class         (if selected? "constant node selected" "constant node")
               :transform     (str "translate(" (:x position) " " (:y position) ")")
               :data-type     "node"
               :data-scene-id scene-id
               :data-ilk      (:ilk node)}

              [svg/rect (vec2 0 0) node-width height
               {:class "bg"
                :rx    1}]

              [node-title (str "Constant")]
              [svg/text
               (vec2 8 32)
               (str (int (* 100 (:value item))))
               {:class       "konst-val"
                :text-anchor "left"}]

              [svg/rect (vec2 0 0) node-width height
               {:rx    1
                :class "click-target"}]

              [links scene-id node link-offset]]))

;//              _
;//   _ __  __ _| |_____
;//  | '  \/ _` | / / -_)
;//  |_|_|_\__,_|_\_\___|
;//
(s/defn ^:always-validate make :- gs/Constant
        []
        {:id    (str (random-uuid))
         :value (rand)})

(s/defn ^:always-validate make-node :- gs/Node
        [item :- gs/Constant
         {:keys [position] :as data} :- gs/NodeData]
        {:id       (str "kon-" (random-uuid))
         :ilk      :constant
         :position (vec->map position)
         :item-id  (:id item)
         :links    [{:id        "out"
                     :emits     :value
                     :direction :out
                     :index     0}]})
