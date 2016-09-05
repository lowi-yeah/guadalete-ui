(ns guadalete-ui.pd.layout
  (:require [guadalete-ui.console :as log]))


(def node-size 36)
(def node-width 92)
(def node-height 28)
(def line-height 14)
(def handle-width 6)
(def handle-height 10)
(def handle-text-padding 4)

(defn link-offset [node link]
  (let [node-offset (condp = (:ilk node)
                      "signal" 3.5
                      "color" 2.5
                      "light" 2
                      "mixer" 2
                      1)]
    (+ (:index link) (- node-offset 0.64))))