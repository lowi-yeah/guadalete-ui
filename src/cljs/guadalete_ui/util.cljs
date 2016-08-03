(ns guadalete-ui.util
  (:require-macros [cljs.core.async.macros :refer [go-loop]]
                   [cljs.core :refer [or]])
  (:require [cljs.core.async :as async :refer [<! >! chan close! put! to-chan timeout]]
            [thi.ng.geom.core :as g]
            [thi.ng.geom.svg.core :refer [svg-attribs]]
            [thi.ng.geom.core.vector :refer [vec2]]
            [guadalete-ui.console :as log]))

(declare clj->js*)

(defn key->js* [k]
  (if (satisfies? IEncodeJS k)
    (-clj->js k)
    (if (or (string? k)
            (number? k)
            (keyword? k)
            (symbol? k))
      (clj->js* k)
      (pr-str k))))

(defn clj->js*
  "Recursively transforms ClojureScript values to JavaScript.
sets/vectors/lists become Arrays, Keywords and Symbol become Strings,
Maps become Objects. Arbitrary keys are encoded to by key->js."
  [x]
  (when-not (nil? x)
    (if (satisfies? IEncodeJS x)
      (do (-clj->js x))

      (do (cond
            (keyword? x) (let [ns-x (if (.-ns x) (str (.-ns x) "/") "")]
                           (str ns-x (name x)))
            (symbol? x) (str x)
            (map? x) (let [m (js-obj)]
                       (doseq [[k v] x]
                         (aset m (key->js* k) (clj->js* v)))
                       m)
            (coll? x) (let [arr (array)]
                        (doseq [x (map clj->js* x)]
                          (.push arr x))
                        arr)
            :else x)))))

(defn debounce [in ms]
  (let [out (chan)]
    (go-loop [last-val nil]
             (let [val (if (nil? last-val) (<! in) last-val)
                   timer (timeout ms)
                   [new-val ch] (alts! [in timer])]
               (condp = ch
                 timer (do (>! out val) (recur nil))
                 in (recur new-val))))
    out))

(defn mappify
  "Generic convenience function for converting a collection into a map.
   As the key for the map, the given mak-key is being used.
   Returns a transducer when no collection is provided."
  [map-key coll]
  (into {} (map (fn [x] {(get x map-key) x}) coll))
  )

(defn recordify
  [data map->type]
  (let [maping-fn (fn [x] {(key x) (map->type (val x))})
        record-map (into {} (map maping-fn data))]
    record-map))

(defn contains-value? [coll element]
  (boolean (some #(= element %) coll)))

(defn contains-any-values? [coll elements]
  "Returns true if the collection contains any one of the elements"
  ;(log/debug "contains-any-values?" coll elements (or (map #(contains-value? coll %) elements)))
  (reduce #(or %2 %1) (map #(contains-value? coll %) elements)))

(defn- get-by-key [coll key val]
  "Helper for getting an element inside an array based on a given key"
  (first (filter #(= val (get % key)) coll))
  )

(defn- get-by-id [coll val]
  "Helper for getting an element inside an array based on the :id key"
  (get-by-key coll :id val)
  )

(defn pretty
  "returns a prettyrinted string representation of whatever you throw at it. ok, just json for now…"
  [something]
  (.stringify js/JSON (clj->js* something) nil 2))

(defn vec-map
  "returns a {:x :y } map for the given Vec2"
  [vec]
  {:x (:x vec) :y (:y vec)})

(defn map-value
  "Map a value v from domain(vec2) to range(vec2)"
  [value domain range]

  (-> value
      (- (:x domain))
      (/ (- (:y domain) (:x domain)))
      (* (- (:y range) (:x range)))
      (+ (:x range))
      ))

(defn abs "(abs n) is the absolute value of n" [n]
  (cond
    (not (number? n)) (log/error "abs requires a number")
    (neg? n) (- n)
    :else n))

(defn offset-position [pos scene]
  (log/debug "offset-position" pos scene)
  (let [pos* (vec2 pos)
        translation (vec2 (:translation scene))]
    (g/- pos* translation)))

(defn kw* [something]
  (keyword something))

(defn- in?
  "true if seq contains elm"
  [seq elm]
  (some #(= elm %) seq))


(defn mod-svg
  [attribs & body]
  [:svg
   (svg-attribs
     attribs
     {})
   body])

(defn dimensions [jq]
  (vec2 (.outerWidth jq true) (.outerHeight jq true)))