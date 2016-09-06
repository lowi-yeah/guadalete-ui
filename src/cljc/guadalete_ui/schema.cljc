(ns guadalete-ui.schema
  #?@(:cljs [(:require
               [schema.utils :as utils]
               [schema.core :as s])
             (:require-macros
               [schema.macros :as macros])])
  #?(:clj
     (:require [schema.utils :as utils]
               [schema.core :as s]
               [schema.macros :as macros])))

(s/defschema Vec2
  {:x s/Num
   :y s/Num})
;
;"cbee4f1c-aa14-4a2f-9a27-1c236f5287f2" : {
;                                          "ilk"   : "signal",
;                                                  "item-id" : "quicksine",
;                                          "id"    : "cbee4f1c-aa14-4a2f-9a27-1c236f5287f2",
;                                                  "position" : {
;                                                                "y" : 97,
;                                                                    "x" : 198
;                                                                },
;                                          "links" : [
;                                                     {
;                                                      "index" : 0,
;                                                              "ilk" : "value",
;                                                      "id"    : "out",
;                                                              "direction" : "out"
;                                                      }
;                                                     ]
;
;
;                                }


(s/defschema Link
  {:id      s/Str
   :accepts (s/enum :value :color)
   :index s/Num})

(s/defschema Node
  {:id    s/Str
   :ilk   (s/enum :signal :color :mixer :light)
   :links [Link]})

(s/defschema Nodes
  {s/Str Node})

(s/defschema Flow
  s/Any)

(s/defschema Flows
  {s/Str Flow})

(s/defschema Scene
  "Scheme definition for a Scene"
  {:id          s/Str
   :name        s/Str
   :room-id     s/Str
   :mode        (s/enum :none :pan :link)                   ; flag used for interacting with the gui, indicates wthere the scene is being panned or whether a link is being created
   :translation Vec2                                        ; offset vector (pan) for rendering
   :nodes       Nodes
   :flows       Flows})
