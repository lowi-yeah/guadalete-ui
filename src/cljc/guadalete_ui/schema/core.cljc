(ns guadalete-ui.schema.core
    #?@(:cljs
        [(:require
           [schema.utils :as utils]
           [schema.core :as s]
           [thi.ng.geom.core.vector :refer [Vec2]]
           [guadalete-ui.schema.scene :refer [Scene]])
         (:require-macros
           [schema.macros :as macros])])
    #?(:clj
       (:require
         [schema.utils :as utils]
         [schema.core :as s]
         [schema.macros :as macros]
         [guadalete-ui.helpers.util :refer [Vec2]]
         [guadalete-ui.schema.scene :refer [Scene]]
         )))


(def LightState
  {:brightness              s/Num
   :saturation              s/Num
   :hue                     s/Num
   (s/optional-key :toggle) s/Bool})

(def Light
  {:id           s/Str
   :name         s/Str
   :type         (s/enum :dmx :mqtt)
   :num-channels (s/enum 1 2 3 4)
   :dmx          [s/Int]
   ;(s/optional-key :layout-position) Vec2
   :state        LightState})


(def Room
  {:id     s/Str
   :name   s/Str
   :light  [s/Str]
   :sensor [s/Str]
   :scene  [s/Str]
   })

(def Rooms
  {s/Str Room})                                             ; map id->Room

(def Lights
  {s/Str Light})

(def Scenes
  {s/Str Scene})


(def frontend-db
  {:ws/connected?          s/Bool                           ;flag indicating that the sente (websocket) connection has been established
   :loading?               s/Bool                           ; flag indicating that data is being loaded
   :user/role              (s/enum :anonymous :user :admin :none)
   :main-panel             (s/enum :blank-panel :root-panel)
   :name                   s/Str
   :message                s/Str
   :current/view           s/Keyword
   :current/segment        (s/enum :scene :light :switch)
   ;:current/room-id        s/Str
   (s/optional-key :room)  Rooms                            ;
   (s/optional-key :light) Lights
   (s/optional-key :scene) Scenes


   ;:active-panel                       s/Keyword            ; the currenlty active ui panel
   ;:user/role                          (s/enum :anonymous :user :admin :none)
   ;:search/term                        s/Str
   ;:sort/light                         (s/enum [:name] [:dmx]) ; sort property for lights
   ;:sort-order/light                   (s/enum :asc :desc)  ; sort order for lights
   ;:sort/sensor                        (s/enum [:room :name] [:name] [:hostname]) ; sort property for sensors
   ;:sort-order/sensor                  (s/enum :asc :desc)  ; sort order for sensors
   ;:mqtt-client                        s/Any
   ;(s/optional-key :pd)                PD
   ;(s/optional-key :room)              Rooms                ;
   ;(s/optional-key :light)             Lights
   ;(s/optional-key :sensor)            Sensors
   ;(s/optional-key :signal)            Signals
   ;(s/optional-key :scene)             Scenes
   ;(s/optional-key :scenelayout)       SceneLayouts
   ;(s/optional-key :current/room-id)   s/Str
   ;(s/optional-key :current/sensor-id) s/Str
   ;(s/optional-key :current/scene-id)  s/Str
   ;(s/optional-key :edit)              [s/Str]
   })
