(ns guadalete-ui.schema
  #?@(:cljs [(:require
               [schema.utils :as utils]
               [schema.core :as s]
               [schema.coerce :as coerce])
             (:require-macros
               [schema.macros :as macros])])
  #?(:clj
     (:require [schema.utils :as utils]
               [schema.core :as s]
               [schema.coerce :as coerce]
               [schema.macros :as macros])))

(s/defschema Vec2
  (s/conditional
    map? {:x s/Num
          :y s/Num}
    :else [s/Num]))

(s/defschema MouseEventData
  {:id       s/Str
   :scene-id s/Str
   :type     (s/enum :node :link :pd :flow)
   :position Vec2})

(s/defschema Link
  {:id        s/Str
   :accepts   (s/enum :value :color)
   :index     s/Num                                         ; used for rendering
   :direction (s/enum :in :out)})


(s/defschema NodeData
  {:room-id  s/Str
   :scene-id s/Str
   :ilk      (s/enum :signal :color :mixer :light)
   :position Vec2
   })

(s/defschema Node
  {:id       s/Str
   :ilk      (s/enum :signal :color :mixer :light)
   :item-id  s/Str
   :position Vec2
   :links    [Link]
   })

(s/defschema Nodes
  {s/Keyword Node})

(s/defschema Flow
  s/Any)

(s/defschema Flows
  {s/Keyword Flow})



(s/defschema Rooms
  {s/Str s/Any})                                            ; map id->Room

(s/defschema Lights
  {s/Str s/Any})

(s/defschema Scene
  "Scheme definition for a Scene"
  {:id          s/Str
   :name        s/Str
   :room-id     s/Str
   :mode        (s/enum :none :pan :link)                   ; flag used for interacting with the gui, indicates wthere the scene is being panned or whether a link is being created
   :translation Vec2                                        ; offset vector (pan) for rendering
   :nodes       Nodes
   :flows       Flows
   :on?         s/Bool})


(s/defschema Scenes
  {s/Str Scene})

(s/defschema Signal
  {:name s/Str
   :type s/Str
   :id   s/Str})

(s/defschema Signals
  {s/Str Signal})

(s/defschema Colors
  {s/Str s/Any})

(s/defschema Mixers
  {s/Str s/Any})



;; configuration data for the ui
(s/defschema Configuration
  {:signal {:sparkline/timespan-seconds s/Num}})

;; description of the current ui-view
(s/defschema View
  {:panel      (s/enum :blank :login :root)
   :section    (s/enum :blank :dash :room)
   :segment    (s/enum :scene :dash :light :switch :signal :debug)
   :dimensions {:root   Vec2
                :view   Vec2
                :header Vec2}
   :ready?     s/Bool
   :room-id    (s/maybe s/Keyword)
   :scene-id   (s/maybe s/Keyword)})


;//    __             _               _   ___  ___
;//   / _|_ _ ___ _ _| |_ ___ _ _  __| | |   \| _ )
;//  |  _| '_/ _ \ ' \  _/ -_) ' \/ _` | | |) | _ \
;//  |_| |_| \___/_||_\__\___|_||_\__,_| |___/|___/
;//
(s/defschema DB
  {:name                    s/Str
   :message                 s/Str
   :view                    View
   (s/optional-key :room)   Rooms                           ;
   (s/optional-key :light)  Lights
   (s/optional-key :scene)  Scenes
   (s/optional-key :color)  Colors
   (s/optional-key :mixer)  Mixers
   (s/optional-key :signal) Signals

   ;; flag indicating whether or not the (sente/websocket) connection with the server has been established
   :ws/connected?           s/Bool
   ;; flag indicating that the frontend is still loading (used during bootstrap)
   :loading?                s/Bool

   :user/role               (s/enum :anonymous :user :admin :none)

   ;; map containing configuration data received from the server
   (s/optional-key :config)   Configuration

   ;; map for temporary data used during user-interaction
   ;; eg: the current interaction-mode, currently selected nodes or temporary (mouse) flows are put here
   ;:tmp                    {:nodes    s/Any
   ;                         :selected s/Any
   ;                         :mode     (s/enum :none :pan :link)}
   :tmp                     s/Any
   })



;//                     _
;//   __ ___ ___ _ _ __(_)___ _ _
;//  / _/ _ \ -_) '_/ _| / _ \ ' \
;//  \__\___\___|_| \__|_\___/_||_|
;//
(def parse-db
  (coerce/coercer DB coerce/json-coercion-matcher))
