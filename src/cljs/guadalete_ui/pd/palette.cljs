(ns guadalete-ui.pd.palette
  (:require-macros
    [thi.ng.math.macros :as mm]
    [reagent.ratom :refer [reaction]])
  (:require
    [reagent.core :as r]
    [re-frame.core :refer [dispatch]]
    [cljs.reader :refer [read-string]]
    [thi.ng.geom.core :as g]
    [thi.ng.geom.core.vector :as v :refer [vec2 vec3]]
    [guadalete-ui.console :as log]
    ))


(defn palette
      "the toolbar for the editor."
      []
      (fn []
          [:div#palette
           [:div.ui.list
            [:div#palette-light.item
             [:button.ui.circular.floating.icon.button
              [:i.icon.facebook]]]
            [:div#palette-light.item
             [:button.ui.circular.floating.icon.button
              [:i.icon.google]]]]]))
