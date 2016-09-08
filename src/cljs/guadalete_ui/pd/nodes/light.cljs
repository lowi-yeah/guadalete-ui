(ns guadalete-ui.pd.nodes.light
  (:require
    [clojure.set :refer [difference]]
    [clojure.string :as string]
    [reagent.core :refer [create-class]]
    [re-frame.core :refer [dispatch def-event def-event-fx]]
    [thi.ng.geom.svg.core :as svg]
    [thi.ng.geom.core.vector :refer [vec2]]

    [guadalete-ui.util :refer [pretty vec->map]]
    [guadalete-ui.pd.color :refer [make-color]]
    [guadalete-ui.console :as log]
    [schema.core :as s]
    [guadalete-ui.schema :as gs]

    [guadalete-ui.pd.nodes.core :refer [node-title click-target]]
    [guadalete-ui.pd.nodes.link :refer [links]]
    [guadalete-ui.pd.layout :refer [node-width line-height node-height]]
    ))



;//                   _
;//   _ _ ___ _ _  __| |___ _ _
;//  | '_/ -_) ' \/ _` / -_) '_|
;//  |_| \___|_||_\__,_\___|_|
;//
(defn- split-name*
  "recursice helper for split name"
  [words lines]
  (if (= 0 (count words))
    (do
      (reverse lines))
    (let [max-length 14
          [line & lines*] lines
          [word & words*] words
          joined (string/trim (str line " " word))]
      (if (<= max-length (count joined))
        ;; too long. keep seperate
        (split-name* words* (conj lines word))
        ;; short enough. join
        (split-name* words* (conj lines* joined))))))

(defn- split-name [name]
  (split-name* (string/split name #" ") [""]))

(defn- light-name []
  (fn [name]
    (let [lines (split-name name)]
      [svg/group
       {:transform (str "translate(4 " line-height ")")}
       [:text
        {:x           0
         :y           0
         :class       "node-text"
         :text-anchor "left"}
        (doall
          (for [index (range (count lines))
                :let [line (nth lines index)
                      offset "1rem"]]
            ^{:key (str (random-uuid))}
            [:tspan {:x "0" :dy offset} line]))]])))

(s/defn render-node
  [scene-id node item selected?]
  (let [id (:id node)]
    (create-class
      {:component-did-mount
       (fn [_this]
         (let [offset-top (* 2 line-height)
               text-height (-> (str "#" id " .node-text") (js/$) (.height))
               height (+ offset-top text-height)]
           (-> (str "#" id " > .bg")
               (js/$)
               (.height height))
           (-> (str "#" id " .click-target")
               (js/$)
               (.height height))))

       :reagent-render
       (fn [scene-id node item selected?]
         (let [position (:position node)
               link-offset 2]
           [svg/group
            {:id            id
             :class         (if selected? "light node selected" "light node")
             :transform     (str "translate(" (:x position) " " (:y position) ")")
             :data-type     "node"
             :data-ilk      (:ilk node)
             :data-scene-id scene-id}

            [svg/rect (vec2) node-width node-height
             {:rx    2
              :class "bg"}]

            [node-title "Light"]

            [svg/group
             {:class "node-content"}
             [light-name (:name item)]]

            [svg/rect (vec2 0 0) node-width node-height
             {:rx    1
              :class "click-target"}]

            [links scene-id node link-offset]]))})))

;//              _
;//   _ __  __ _| |_____
;//  | '  \/ _` | / / -_)
;//  |_|_|_\__,_|_\_\___|
;//


(s/defn ^:always-validate get-avilable
  "Finds a light inside a room which is not yet in use by the given scene.
  Used for assigning a light during pd/light-node-creation"
  [db :- gs/DB
   {:keys [room-id scene-id]} :- gs/NodeData]
  (let [all-light-ids (into #{} (get-in db [:room room-id :light]))
        used-light-ids (->>
                         (get-in db [:scene scene-id :nodes])
                         (filter (fn [[id l]] (= :light (keyword (:ilk l)))))
                         (map (fn [[id l]] (:item-id l)))
                         (filter (fn [id] id))
                         (into #{}))
        unused (difference all-light-ids used-light-ids)
        light-id (first unused)]
    (get-in db [:light light-id])))

(s/defn ^:always-validate make-node :- gs/Node
  [item :- gs/Light
   {:keys [position]} :- gs/NodeData]
  (let [node-id (str "lght-" (:id item))]
    {:id       node-id
     :ilk      :light
     :item-id  (:id item)
     :position (vec->map position)
     :links    [{:id        "in"
                 :index     0
                 :accepts   :color
                 :direction :in}]}))
