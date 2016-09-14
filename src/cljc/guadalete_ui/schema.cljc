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
  {:id                         s/Str
   :scene-id                   s/Str
   :room-id                    s/Str
   :node-id                    s/Str
   :type                       (s/enum :node :link :pd :flow)
   :position                   Vec2
   (s/optional-key :buttons)   s/Num
   (s/optional-key :modifiers) s/Any})


;//   _ _      _
;//  | (_)_ _ | |_____
;//  | | | ' \| / (_-<
;//  |_|_|_||_|_\_\__/
;//

(defn in-link? [link]
  (or (= :in (:direction link))
      (= "in" (:direction link))))

(def LinkReference
  "A reference for looking up a Link"
  (s/conditional keyword? (s/eq :mouse)
                 :else {:scene-id s/Str
                        :node-id  s/Str
                        :id       s/Str}))

;; IN
;; ********************************
(def ColorInLink
  "A link accepting colors as input"
  {:id                        s/Str
   (s/optional-key :scene-id) s/Str
   (s/optional-key :node-id)  s/Str
   :accepts                   (s/eq :color)
   :direction                 (s/eq :in)
   :index                     s/Num
   (s/optional-key :name)     s/Str})

(def ValueInLink
  "A link accepting values as input"
  {:id                        s/Str
   (s/optional-key :scene-id) s/Str
   (s/optional-key :node-id)  s/Str
   :accepts                   (s/eq :value)
   :direction                 (s/eq :in)
   :index                     s/Num
   (s/optional-key :type)     s/Str
   (s/optional-key :channel)  s/Str
   (s/optional-key :name)     s/Str})


;; OUT
;; ********************************
(def ColorOutLink
  "A link emitting a color"
  {:id                        s/Str
   (s/optional-key :scene-id) s/Str
   (s/optional-key :node-id)  s/Str
   :emits                     (s/eq :color)
   :direction                 (s/eq :out)
   :index                     s/Num
   (s/optional-key :name)     s/Str})

(def ValueOutLink
  "A link emitting values"
  {:id                        s/Str
   (s/optional-key :scene-id) s/Str
   (s/optional-key :node-id)  s/Str
   :emits                     (s/eq :value)
   :direction                 (s/eq :out)
   :index                     s/Num
   (s/optional-key :name)     s/Str})

(s/defschema InLink
  (s/conditional
    #(or
      (= (:accepts %) "color")
      (= (:accepts %) :color)) ColorInLink
    :else ValueInLink))

(s/defschema OutLink
  (s/conditional
    #(or
      (= (:emits %) "color")
      (= (:emits %) :color)) ColorOutLink
    :else ValueOutLink))

(s/defschema Link
  (s/conditional in-link? InLink :else OutLink))

(s/defschema NodeData
  {:room-id  s/Str
   :scene-id s/Str
   :ilk      (s/enum :signal :color :mixer :light)
   :position Vec2})

(s/defschema NodeReference
  {:scene-id s/Str
   :id       s/Str
   :type     (s/enum :node)
   :position Vec2})

(s/defschema Node
  {:id       s/Str
   :ilk      (s/enum :signal :color :mixer :light)
   :item-id  s/Str
   :position Vec2
   :links    [Link]})

(s/defschema Nodes
  {s/Keyword Node})


;//    __ _
;//   / _| |_____ __ __
;//  |  _| / _ \ V  V /
;//  |_| |_\___/\_/\_/
;//
(s/defschema FlowReference
  "A flow between two pd nodes.
  (Between links of two nodes, to be more precise.)"
  {:from                    LinkReference
   :to                      LinkReference
   (s/optional-key :id)     s/Str
   (s/optional-key :valid?) (s/enum :valid :invalid)})

(s/defschema FlowReferences
  {s/Keyword s/Any})

(s/defschema ValueFlow
  "A schema for flows between value links"
  {:from                ValueOutLink
   :to                  ValueInLink
   (s/optional-key :id) s/Str})

(s/defschema ColorFlow
  "A schema for flows between value links"
  {:from                ColorOutLink
   :to                  ColorInLink
   (s/optional-key :id) s/Str})

(s/defschema Flow
  "An assembled flow between two pd nodes.
  In this context, assembled means that the actual links have been loaded,
  instead of just their reference ids."
  ;; i'd like to use an enum here, but validation always fails when I do so…
  ;(s/enum ValueFlow ColorFlow)
  (s/conditional
    #(or (= (-> % (get :from) (get :emits)) "value")
         (= (-> % (get :from) (get :emits)) :value)) ValueFlow
    :else ColorFlow))

(s/defschema Room
  {:id     s/Str
   :name   s/Str
   :light  [s/Str]
   :scene  [s/Str]
   :sensor [s/Str]})

(s/defschema Rooms
  {s/Str s/Any})

(s/defschema DMXLight
  {:room-id   s/Str
   :id        s/Str
   :name      s/Str
   :type      (s/enum :v :sv :hsv)
   :channels  {:brightness                  [s/Num]
               (s/optional-key :saturation) [s/Num]
               (s/optional-key :hue)        [s/Num]}
   :color     {:brightness                  s/Num
               (s/optional-key :saturation) s/Num
               (s/optional-key :hue)        s/Num}
   :transport (s/eq :dmx)})

(s/defschema MqttLight
  {(s/optional-key :room-id) s/Str
   :id                       s/Str
   :name                     s/Str
   :type                     (s/enum :v :sv :hsv)
   :transport                (s/eq :mqtt)
   :accepted?                s/Bool
   (s/optional-key :color)   {:brightness                  s/Num
                              (s/optional-key :saturation) s/Num
                              (s/optional-key :hue)        s/Num}})

(s/defschema Light
  (s/conditional
    #(or
      (= (:transport %) "mqtt")
      (= (:transport %) :mqtt))
    MqttLight
    :else DMXLight))


(s/defschema Lights
  {s/Str Light})

(s/defschema Scene
  "Scheme definition for a Scene"
  {:id          s/Str
   :name        s/Str
   :room-id     s/Str
   :mode        (s/enum :none :pan :link)                   ; flag used for interacting with the gui, indicates wthere the scene is being panned or whether a link is being created
   :translation Vec2                                        ; offset vector (pan) for rendering
   :nodes       Nodes
   :flows       FlowReferences
   :on?         s/Bool})


(s/defschema Scenes
  {s/Str Scene})

(s/defschema Signal
  {:name s/Str
   :type s/Str
   :id   s/Str})

(s/defschema Signals
  {s/Str Signal})

(s/defschema Color
  {:id         s/Str
   :color-type (s/enum :v :sv :hsv)
   :brightness s/Num})

(s/defschema Colors
  {s/Str Color})

(s/defschema Mixer
  {:id       s/Str
   :mixin-fn s/Keyword})

(s/defschema Mixers
  {s/Str Mixer})



;; configuration data for the ui
(s/defschema Configuration
  {:signal {:sparkline/timespan-seconds s/Num}})

;; configuration data for the ui
(s/defschema Modal
  {:item-id    s/Str
   :ilk        (s/enum :light :color :mixer :signal)
   :modal-type (s/enum :light :color :mixer :signal)})


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


(s/defschema TemporaryDB
  "Schema for data that is kept temporarily, eg. mose data during user-interaction"
  {
   (s/optional-key :nodes)     s/Any
   (s/optional-key :selected)  s/Any
   (s/optional-key :flow)      FlowReference
   (s/optional-key :pos)       Vec2
   (s/optional-key :start-pos) Vec2
   (s/optional-key :mouse-pos) Vec2
   (s/optional-key :mode)      (s/enum :none :link :pan :move)
   (s/optional-key :scene)     Scene
   }
  )

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

   (s/optional-key :config) Configuration                   ;; map containing configuration data received from the server
   (s/optional-key :modal)  Modal

   ;; map for temporary data used during user-interaction
   ;; eg: the current interaction-mode, currently selected nodes or temporary (mouse) flows are put here
   ;:tmp                    {:nodes    s/Any
   ;                         :selected s/Any
   ;                         :mode     (s/enum :none :pan :link)}
   :tmp                     TemporaryDB
   })

(s/defschema Effect
  {:db                        DB
   (s/optional-key :sente)    s/Any
   (s/optional-key :dispatch) s/Any
   (s/optional-key :modal)    s/Any
   })

;//                   _                 _         _   _
;//   ____  _ _ _  __| |_  _ _ ___ _ _ (_)_____ _| |_(_)___ _ _
;//  (_-< || | ' \/ _| ' \| '_/ _ \ ' \| |_ / _` |  _| / _ \ ' \
;//  /__/\_, |_||_\__|_||_|_| \___/_||_|_/__\__,_|\__|_\___/_||_|
;//      |__/
(s/defschema Diff*
  "One half of a diff"
  {s/Str s/Any})

(s/defschema Diff
  "Definition of a Diff as returned by differ"
  ;; obacht: this won't chack that there are exactly two elements to a diff
  [Diff*])

(s/defschema Patch
  "Definition for a patch supplied when synchronizing with the backend"
  {:ilk  s/Keyword
   :diff Diff})

(s/defschema UpdateResponse
  "Definition for a patch supplied when synchronizing with the backend"
  {(s/enum :ok :error) s/Any})

;//                     _
;//   __ ___ ___ _ _ __(_)___ _ _
;//  / _/ _ \ -_) '_/ _| / _ \ ' \
;//  \__\___\___|_| \__|_\___/_||_|
;//
(def parse-db
  (coerce/coercer DB coerce/json-coercion-matcher))

(def coerce-light
  (coerce/coercer Light coerce/json-coercion-matcher))