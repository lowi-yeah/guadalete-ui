(ns guadalete-ui.schema.pd
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
  "A two-dimensional vector"
  {:x s/Num
   :y s/Num})


;//   _ _      _
;//  | (_)_ _ | |_____
;//  | | | ' \| / (_-<
;//  |_|_|_||_|_\_\__/
;//
(def LinkReference
  "A reference for looking up a Link"
  {:scene-id s/Str
   :node-id  s/Str
   :id       s/Str})

;; OUT
;; ********************************
(def ColorOutLink
  "A link emitting a color"
  {:ilk                   (s/eq "color")
   :id                    s/Str
   :direction             (s/eq "out")
   (s/optional-key :name) s/Str})

(def ValueOutLink
  "A link emitting values"
  {:ilk                   (s/eq "value")
   :id                    s/Str
   :direction             (s/eq "out")
   (s/optional-key :type) s/Str
   (s/optional-key :name) s/Str})

;; IN
;; ********************************
(def ColorInLink
  "A link accepting colors as input"
  {:ilk                   (s/eq "color")
   :id                    s/Str
   :direction             (s/eq "in")
   (s/optional-key :name) s/Str})

(def ValueInLink
  "A link accepting values as input"
  {:ilk                   (s/eq "value")
   :id                    s/Str
   :direction             (s/eq "in")
   (s/optional-key :type) s/Str
   (s/optional-key :name) s/Str})

;; Conditionals
;; ********************************
(s/defschema InLink
  "A schema for node links"
  (s/conditional
    #(= (:ilk %) "color") ColorInLink
    #(= (:ilk %) "value") ValueInLink))

(s/defschema OutLink
  "A schema for node links"
  (s/conditional
    #(= (:ilk %) "color") ColorOutLink
    #(= (:ilk %) "value") ValueOutLink))

(s/defschema Link
  "A schema for node links"
  (s/conditional
    #(= (:direction %) "in") InLink
    #(= (:direction %) "out") OutLink))

;//    __ _
;//   / _| |_____ __ __
;//  |  _| / _ \ V  V /
;//  |_| |_\___/\_/\_/
;//
(def FlowReference
  "A flow between two pd nodes.
  Between links of two nodes, to be more precise."
  {:from LinkReference
   :to   LinkReference})

(s/defschema ValueFlow
  "A schema for flows between value links"
  {:from ValueOutLink :to ValueInLink})

(s/defschema ColorFlow
  "A schema for flows between value links"
  {:from ColorOutLink :to ColorInLink})


(s/defschema Flow
  "An assembled flow between two pd nodes.
  In this context, assembled means that the actual links have been loaded,
  instead of just their reference ids."
  ;; i'd like to use an enum here, but validation always fails when I do so…
  ;(s/enum ValueFlow ColorFlow)
  (s/conditional
    #(= (-> % (get :from) (get :ilk)) "value") ValueFlow
    #(= (-> % (get :from) (get :ilk)) "color") ColorFlow))
