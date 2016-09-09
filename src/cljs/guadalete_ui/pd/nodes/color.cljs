(ns guadalete-ui.pd.nodes.color
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

;//   _        _
;//  | |_  ___| |_ __ ___ _ _ ___
;//  | ' \/ -_) | '_ \ -_) '_(_-<
;//  |_||_\___|_| .__\___|_| /__/
;//             |_|
(defn- color-channel-link [channel index]
  {:id        (name channel)
   :name      (name channel)
   :accepts   :value
   :direction :in
   :index     index})


(defn- make-color-links
  "Helper function for creating the in/out links for a given color.
  The number of input-links corresponds to the number of color channels (h,s,v)"
  [color]
  (let [out-link [{:id        "out"
                   :emits   :color
                   :name      "out"
                   :direction :out
                   :index     (-> color (:color-type) (name) (count))}]
        in-links (condp = (:color-type color)
                   :v [(color-channel-link :brightness 0)]
                   :sv [(color-channel-link :brightness 0)
                        (color-channel-link :saturation 1)]
                   :hsv [(color-channel-link :brightness 0)
                         (color-channel-link :saturation 1)
                         (color-channel-link :hue 2)]
                   :default (log/error (str "Unknown color type " (:color-type color) ". Must be either :v :sv or :hsv")))]
    (concat in-links out-link)))

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
        height (* line-height (+ 3 (count (name (:color-type item)))))
        ;hackedy hack:
        ;to make the rendering a bit nicer, take the brightness value and set it as the alpha channel
        ;that way not a black splot gets rendered when the birightness is low, but rather a transparent one
        hacked-color (render-color item)
        link-offset 2.5]
    [svg/group
     {:id            id
      :class         (if selected? "color node selected" "color node")
      :transform     (str "translate(" (:x position) " " (:y position) ")")
      :data-type     "node"
      :data-scene-id scene-id
      :data-ilk      (:ilk node)}

     [svg/rect (vec2 0 0) node-width height
      {:class "bg"
       :rx    1}]

     [node-title (str "Color: " (:color-type item))]

     [svg/rect (vec2 0 line-height) node-width (/ line-height 2)
      {:fill  hacked-color
       :class "indicator"
       :rx    1}]

     [svg/rect (vec2 0 0) node-width height
      {:rx    1
       :class "click-target"}]

     [links scene-id node link-offset]]))

;//              _
;//   _ __  __ _| |_____
;//  | '  \/ _` | / / -_)
;//  |_|_|_\__,_|_\_\___|
;//
(s/defn ^:always-validate make :- gs/Color
  []
  {:id         (str (random-uuid))
   :color-type :v
   :brightness 0})

(s/defn ^:always-validate make-node :- gs/Node
  [item :- gs/Color
   {:keys [position] :as data} :- gs/NodeData]

  (let [links (make-color-links item)]
    {:id       (str "clr-" (:id item))
     :ilk      :color
     :position (vec->map position)
     :item-id  (:id item)
     :links    links}))
