(ns guadalete-ui.schema.scene
    #?@(:cljs [(:require
                 [schema.utils :as utils]
                 [schema.core :as s])
               (:require-macros
                 [schema.macros :as macros])])
    #?(:clj
       (:require [schema.utils :as utils]
         [schema.core :as s]
         [schema.macros :as macros])))

(def Vec2
  {:x s/Num
   :y s/Num})

(def Node
  {:id                       s/Str
   (s/optional-key :item-id) s/Str
   :position                 Vec2
   (s/optional-key :pos-0)   Vec2
   :type                     (s/enum "color" "light")
   :selected                 s/Bool})


(def Link
  "A link between two pd nodes."
  {:from                   s/Any
   :to                     s/Any
   (s/optional-key :mouse) Vec2})

(def Layout
  {
   :nodes                  [Node]
   ;:nodes                  [s/Any]
   :mode                   (s/enum :none :move :pan :link)
   :translation            Vec2
   (s/optional-key :link)  Link
   (s/optional-key :mouse) Vec2
   (s/optional-key :pos-0) Vec2
   (s/optional-key :pos-1) Vec2})

(def Scene
  {:id     s/Str
   ;:layout Layout
   :layout Layout
   :name   s/Str
   :on?    s/Bool})