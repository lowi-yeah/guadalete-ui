(ns guadalete-ui.pd.util
  (:require-macros
    [reagent.ratom :refer [reaction]])
  (:require
    [goog.dom]
    [goog.dom.dataset]
    [goog.style]

    [thi.ng.geom.core :as g]
    [thi.ng.geom.core.matrix :refer [matrix32]]
    [thi.ng.geom.core.vector :refer [vec2]]
    [guadalete-ui.console :as log]))


(defn target-id [target]
      "Return the targets id, or – in case it has none - recursively walk up the dom to find the first ancestor with an id."
      (let [id (.attr (js/$ target) "id")]
           (if (nil? id)
             (target-id (.parent (js/$ target)))
             id)))

(defn target-type [target]
      "Return the targets data-type, or – in case it has none - recursively walk up the dom to find the first ancestor with an data-type."
      (let [type (.attr (js/$ target) "data-type")]
           ;(log/debug "target-typppe" target type)
           (if (nil? type)
             (target-type (.parent (js/$ target)))
             type)))

(defn css-matrix-string
      "Converts a thin.ng/Matrix32 to its css-transform representation"
      [layout]
      (let [translation (if (nil? (:translation layout)) (vec2) (:translation layout))
            matrix (-> (matrix32)
                       (g/translate translation))]
           (str "matrix(" (clojure.string/join ", " (g/transpose matrix)) ")")))


(defn pd-dimensions []
      (let [jq-view (js/$ "#view")
            jq-header (js/$ "#header")
            view (vec2 (.outerWidth jq-view true) (.outerHeight jq-view true))
            header (vec2 (.outerWidth jq-header true) (.outerHeight jq-header true))
            offsets (vec2 88 24)
            width (max 0 (- (:x view) (:x offsets)))
            height (max 0 (- (:y view) (:y header) (:y offsets)))]
           (reaction (vec2 width height))))

(defn is-line?
      "checks whether a form represents a grid line.
      used in grid @see below."
      [x]
      (if (vector? x) (= :line (first x)) false))

(defn nil-node []
      {:type     :none
       :id       "nil-node"
       :position {:x 0 :y 0}})


(defn- get-layout-node [node-id scene]
       (first (filter #(= (:id %) node-id) (get-in scene [:layout :nodes]))))

(defn modal-node [db]
      (let [mn (:pd/modal-node-data db)]
           (if mn
             (let [scene (get-in db [:scene (:scene mn)])
                   node (get-layout-node (:node mn) scene)]
                  node)
             (nil-node))))

(defn modal-scene [db]
      (let [mn (:pd/modal-node-data db)]
           (if mn
             (get-in db [:scene (:scene mn)])
             nil)))

(defn modal-room [db]
      (let [mn (:pd/modal-node-data db)]
           (if mn
             (get-in db [:room (:room mn)])
             nil)))

(defmulti nil-item
          (fn [type] type))

(defmethod nil-item :light
           [_]
           {:type :light
            :id   "nil-light"
            :name "light"})

(defmethod nil-item :color
           [_]
           {:type :color
            :id   "nil-color"})

(defmethod nil-item :sgnl
           [_]
           {:type :sgnl
            :id   "nil-signal"
            :name "signal"})

(defn pd-screen-offset []
      (let [jq-svg (js/$ "#pd-svg")
            offset (.offset jq-svg)]
           (vec2 (.-left offset) (.-top offset))))