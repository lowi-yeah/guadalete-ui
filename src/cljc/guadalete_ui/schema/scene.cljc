(ns guadalete-ui.schema.scene
    #?@(:cljs [(:require
                 [schema.utils :as utils]
                 [schema.core :as s]
                 [thi.ng.geom.core.vector :refer [Vec2]])
               (:require-macros
                 [schema.macros :as macros])])
    #?(:clj
       (:require [schema.utils :as utils]
         [schema.core :as s]
         [schema.macros :as macros]
         [guadalete-ui.helpers.util :refer [Vec2]])))

(def Node
  {:id       s/Str
   :item-id  s/Str
   :position Vec2
   :type     (s/enum :color :light)
   :selected s/Bool})

(def Layout
  {:nodes                  [Node]
   :mode                   (s/enum :none :move :pan :link)
   :translation            Vec2
   (s/optional-key :mouse) Vec2})

(def Scene
  {:id     s/Str
   :layout Layout
   :name   s/Str
   :on?    s/Bool})
