(ns guadalete-ui.pd.mouse
  (:require-macros [reagent.ratom :refer [reaction]]
                   [schema.macros])
  (:require [clojure.set :refer [difference]]
            [schema.core :as s]
            [re-frame.core :refer [dispatch]]
            [thi.ng.geom.core :as g]
            [thi.ng.geom.core.vector :refer [vec2]]
            [guadalete-ui.schema.core :refer [DB MouseEventData]]
            [guadalete-ui.util :refer [pretty vec-map]]
            [guadalete-ui.console :as log]
            [guadalete-ui.pd.util :refer [pd-screen-offset]]))

;//   _        _
;//  | |_  ___| |_ __ ___ _ _ ___
;//  | ' \/ -_) | '_ \ -_) '_(_-<
;//  |_||_\___|_| .__\___|_| /__/
;//             |_|
(defn- in?
       "true if seq contains elm"
       [seq elm]
       (some #(= elm %) seq))

(defn- unselect-all
       "Sets :seleted to false for all given nodes"
       [nodes]
       ;(into {} (map (fn [[k v]] [k (-> v (assoc :selected false) (dissoc :pos-0))])) nodes))
       (into [] (map (fn [n] (-> n (assoc :selected false) (dissoc :pos-0)))) nodes))

(defn- selected-nodes
       "Gets all selected nodes form the editor"
       [editor]
       (into [] (filter (fn [[_id n]] (true? (:selected n)))) (:node editor)))

(defn- move-node [[id n] δ]
       "Move a node by the specified centre."
       (let [offset (g/+ (:pos-0 n) δ)]
            [id (assoc n :pos (vec-map offset))]))

(defn- move-node-start [scene-id node-id position db]
       (let [scene (get-in db [:scene scene-id])
             layout (:layout scene)
             nodes (:nodes layout)
             node (->> nodes
                       (filter #(= (:id %) node-id))
                       (first))
             node* (assoc node
                          :selected true
                          :pos-0 (vec-map (:position node)))

             nodes* (->> nodes
                         (unselect-all)
                         (remove #(= (:id %) node-id)))
             nodes* (conj nodes* node*)

             layout* (assoc layout
                            :mode :move
                            :pos-0 (vec-map position)
                            :nodes nodes*)
             scene* (assoc scene :layout layout*)]

            (assoc-in db [:scene scene-id] scene*)))



(defmulti check-outlet* (fn [type outlet] type))

(defmethod check-outlet* :signal [_ outlet] outlet)

(defmethod check-outlet* :color [_ outlet]
           (assoc outlet :state "active"))

(defn- check-outlet
       "Checks whether is is ok to use the given outlet bsed on its type."
       [outlet]
       (if (= :mouse outlet)
         :mouse
         (let [type (keyword (:type outlet))]
              (check-outlet* type outlet))))


(defmulti check-inlet* (fn [type inlet] type))


(defmethod check-inlet* :signal [_ inlet] inlet)

(defmethod check-inlet* :color [_ inlet]
           (assoc inlet :state "active"))

(defn- check-inlet
       "Checks whether is is ok to use the given outlet based on its type."
       [inlet]
       (if (= :mouse inlet)
         :mouse
         (let [type (keyword (:type inlet))]
              (log/debug "check-inlet" inlet type)
              (check-inlet* type inlet))))

(defn- get-outlet [from layout]
       (if (= :mouse from)
         :mouse
         (let [node (->> (:nodes layout)
                         (filter #(= (:id %) (:node-id from)))
                         (first))
               outlet (->> (:outlets node)
                           (filter #(= (:id %) (:id from)))
                           (first))]
              outlet)))

(defn- set-outlet [outlet layout node-id]
       (if (= :mouse outlet)
         layout
         (let [
               nodes (:nodes layout)
               node (->> nodes
                         (filter #(= (:id %) node-id))
                         (first))
               outlets (:outlets node)
               outlets* (remove #(= (:id %) (:id outlet)) outlets)
               outlets* (conj outlets* outlet)
               node* (assoc node :outlets outlets*)
               nodes* (remove #(= (:id %) node-id) nodes)
               nodes* (conj nodes* node*)]
              (assoc layout :nodes nodes*))))

(defn- set-inlet [inlet layout node-id]
       (if (= :mouse inlet)
         layout
         (let [
               nodes (:nodes layout)
               node (->> nodes
                         (filter #(= (:id %) node-id))
                         (first))
               inlets (:inlets node)
               inlets* (remove #(= (:id %) (:id inlet)) inlets)
               inlets* (conj inlets* inlet)
               node* (assoc node :inlets inlets*)
               nodes* (remove #(= (:id %) node-id) nodes)
               nodes* (conj nodes* node*)]
              (assoc layout :nodes nodes*))))

(defn- get-inlet [to layout]
       (if (= :mouse to)
         :mouse
         (let [node (->> (:nodes layout)
                         (filter #(= (:id %) (:node-id to)))
                         (first))
               inlet (->> (:inlets node)
                          (filter #(= (:id %) (:id to)))
                          (first))]
              inlet)))


(defn- reset-outlets* [node]
       (assoc node :outlets (->> (:outlets node)
                                 (map #(assoc % :state "normal"))
                                 (into []))))
(defn- reset-outlets [nodes]
       (->> nodes
            (map reset-outlets*)
            (into [])))


(defn- reset-inlets* [node]
       (assoc node :inlets (->> (:inlets node)
                                (map #(assoc % :state "normal"))
                                (into []))))
(defn- reset-inlets [nodes]
       (->> nodes
            (map reset-inlets*)
            (into [])))


(defn- begin-link*
       "Internal function for staring the creation of a link.
       Checks whether it is ok to create a link from the given outlet-inlet"

       [scene-id node-id link position db]
       (let [scene (get-in db [:scene scene-id])
             layout (:layout scene)

             from (get-outlet (:from link) layout)
             to (get-inlet (:to link) layout)

             from* (check-outlet from)
             to* (check-inlet to)

             layout* (set-outlet from* layout node-id)
             layout* (set-inlet to* layout* node-id)

             position* (g/- position (vec2 (:translation layout)))
             layout* (assoc layout*
                            :mode :link
                            :link link
                            :mouse (vec-map position*))
             scene* (assoc scene :layout layout*)]

            (assoc-in db [:scene scene-id] scene*)))

(defn- begin-link [scene-id node-id id position db]
       (let [link {:from {:node-id node-id :id id}
                   :to   :mouse}]
            (begin-link* scene-id node-id link position db)))

(defn- begin-reverse-link [scene-id node-id id position db]
       (let [link {:from :mouse
                   :to   {:node-id node-id :id id}}]
            (begin-link* scene-id node-id link position db)))

(defn- update-link [layout node-id type id]

       layout

       )


(defn link-mouse [db scene-id position node-id type id]
      (let [scene (get-in db [:scene scene-id])
            layout (:layout scene)
            position* (g/- position (vec2 (:translation layout)))
            layout* (assoc layout :mouse (vec-map position*))
            layout* (update-link layout* node-id type id)
            scene* (assoc scene :layout layout*)]
           (log/debug "link-mouse" (str type) id node-id)
           (assoc-in db [:scene scene-id] scene*)))

;//                              _
;//   _ __  ___ _  _ ______   __| |_____ __ ___ _
;//  | '  \/ _ \ || (_-< -_) / _` / _ \ V  V / ' \
;//  |_|_|_\___/\_,_/__\___| \__,_\___/\_/\_/|_||_|
;//
(defmulti mouse-down*
          (fn [type data] type))

(defmethod mouse-down* :pd [_ {:keys [scene-id node-id position db] :as data}]
           (let [scene (get-in db [:scene scene-id])
                 layout (:layout scene)
                 nodes* (unselect-all (:nodes layout))
                 layout* (assoc layout
                                :mode :pan
                                :pos-0 (vec-map position)
                                :pos-1 (vec-map (:translation layout))
                                :nodes nodes*)
                 scene* (assoc scene :layout layout*)]

                (assoc-in db [:scene scene-id] scene*)))

(defmethod mouse-down* :node/light [_ {:keys [scene-id id position db]}]
           (move-node-start scene-id id position db))

(defmethod mouse-down* :node/color [_ {:keys [scene-id id position db]}]
           (move-node-start scene-id id position db))

(defmethod mouse-down* :outlet/color [_ {:keys [scene-id id node-id position db] :as data}]
           (begin-link scene-id node-id id position db))

(defmethod mouse-down* :inlet/color [_ {:keys [scene-id id node-id position db]}]
           (begin-reverse-link scene-id node-id id position db))

(defmethod mouse-down* :default [type {:keys [db]}]
           (log/error (str "mouse-down: I don't know the type: " type))
           db)

(s/defn down :- DB
        [{:keys [type] :as data} :- MouseEventData
         db :- DB]
        (mouse-down* type (assoc data :db db)))

;//
;//   _ __  ___ _  _ ______   _  _ _ __
;//  | '  \/ _ \ || (_-< -_) | || | '_ \
;//  |_|_|_\___/\_,_/__\___|  \_,_| .__/
;//                               |_|
(defn- default-up*
       "This is the standard behaviour upon mouse up.
       Canceles everything that might have been going on during move.
       Called by modes [:none :pd :move]"
       [type scene-id node-id position db]
       (let [scene (get-in db [:scene scene-id])
             layout (:layout scene)
             nodes* (unselect-all (:nodes layout))
             nodes* (reset-outlets nodes*)
             nodes* (reset-inlets nodes*)
             layout* (assoc layout :mode :none)
             layout* (assoc layout* :nodes nodes*)
             layout* (dissoc layout* :pos-0 :pos-1 :link)
             scene* (assoc scene :layout layout*)]
            (dispatch [:scene/update scene*])
            (assoc-in db [:scene scene-id] scene*)))

(defn- inlet-up*
       [type scene-id node-id position db]
       (log/debug "inlet-up*:" type)
       (let [scene (get-in db [:scene scene-id])
             layout (:layout scene)
             layout-0 (assoc layout :mode :none)
             nodes-0 (unselect-all (:nodes layout-0))
             layout-1 (assoc layout-0 :nodes nodes-0)
             layout-2 (dissoc layout-1 :pos-0 :pos-1 :link)
             scene-0 (assoc scene :layout layout-2)]
            (dispatch [:scene/update scene-0])
            (assoc-in db [:scene scene-id] scene-0)))

(defn- outlet-up*
       [type scene-id node-id position db]
       (log/debug "outlet-up*:" type)
       (default-up* type scene-id node-id position db))


(defmulti up* (fn [type data] type))

(defmethod up* :pd [_ {:keys [scene-id node-id position db]}]
           (default-up* :pd scene-id node-id position db))

(defmethod up* :node/light [_ {:keys [scene-id node-id position db]}]
           (default-up* :light scene-id node-id position db))

(defmethod up* :node/color [_ {:keys [scene-id node-id position db]}]
           (default-up* :color scene-id node-id position db))

(defmethod up* :outlet/color [_ {:keys [scene-id node-id position db]}]
           (outlet-up* :outlet scene-id node-id position db))

(defmethod up* :inlet/color [_ {:keys [scene-id node-id position db]}]
           (inlet-up* :inlet/color scene-id node-id position db))

(defmethod up* :default [type {:keys [db]}]
           (log/error (str "mouse UP: I don't know the type: " type))
           db)

;(s/defn up :- DB
;        [{:keys [type] :as data} :- MouseEventData
;         db :- DB]
;        (up* type (assoc data :db db)))

(s/defn up :- s/Any
        [{:keys [type] :as data} :- s/Any
         db :- s/Any]
        (up* type (assoc data :db db)))


;//
;//   _ __  ___ _  _ ______   _ __  _____ _____
;//  | '  \/ _ \ || (_-< -_) | '  \/ _ \ V / -_)
;//  |_|_|_\___/\_,_/__\___| |_|_|_\___/\_/\___|
;//

(defmulti move*
          (fn [mode data] mode))

(defmethod move* :move [_ {:keys [scene-id node-id position db]}]
           (let [scene (get-in db [:scene scene-id])
                 layout (:layout scene)
                 δ (g/- (vec2 position) (vec2 (:pos-0 layout)))
                 nodes (:nodes layout)
                 node (->> nodes
                           (filter #(:selected %))
                           (first))
                 node-position (vec2 (:pos-0 node))
                 node-position* (g/+ node-position δ)
                 node* (assoc node :position (vec-map node-position*))
                 nodes* (remove #(= (:id %) (:id node*)) nodes)
                 nodes* (conj nodes* node*)
                 layout* (assoc layout :nodes nodes*)
                 scene* (assoc scene :layout layout*)]
                (assoc-in db [:scene scene-id] scene*)))

(defmethod move* :pan [_ {:keys [scene-id position db]}]
           (let [scene (get-in db [:scene scene-id])
                 layout (:layout scene)
                 δ (g/- (vec2 position) (vec2 (:pos-0 layout)))
                 translation* (g/+ (vec2 (:pos-1 layout)) δ)
                 layout* (assoc layout :translation (vec-map translation*))
                 scene* (assoc scene :layout layout*)]
                (assoc-in db [:scene scene-id] scene*)))

(defmethod move* :link [_ {:keys [scene-id node-id type id position db] :as data}]
           (link-mouse db scene-id position node-id type id))

(defmethod move* :none [_ {:keys [db]}] db)

(defmethod move* :default [_ {:keys [db]}] db)

(s/defn move :- DB
        [data :- MouseEventData
         db :- DB]
        (let [scene (get-in db [:scene (:scene-id data)])
              layout (:layout scene)
              mode (:mode layout)]
             (move* mode (assoc data :db db))))


;//                   _
;//   _____ _____ _ _| |_
;//  / -_) V / -_) ' \  _|
;//  \___|\_/\___|_||_\__|
;//
(defn- target-type [target]
       "Return the targets data-type, or – in case it has none - recursively walk up the dom to find the first ancestor with a data-type."
       (let [type (.attr (js/$ target) "data-type")]
            (if (nil? type)
              (target-type (.parent (js/$ target)))
              (keyword type))))

(defn- target-id [target]
       "Return the targets id, or – in case it has none - recursively walk up the dom to find the first ancestor with an id."
       (let [id (.attr (js/$ target) "id")]
            (if (nil? id)
              (target-id (.parent (js/$ target)))
              id)))


(defn- load-node
       "loads the node if for the given inlet/outlet"
       [{:keys [id type] :as inlet-or-outlet}]
       (let [node-id* (-> (str "#" id)
                          (js/$)
                          (.parent)
                          (.attr "id"))]
            (assoc inlet-or-outlet :node-id node-id*)))

(defn- ->page [ev]
       (vec2 (.-pageX ev) (.-pageY ev)))

(defn event-target [ev]
      (let [target* (.-target ev)
            type (target-type target*)
            id (target-id target*)]
           (condp = type
                  :pd {:type type}

                  :node/light {:type type :id id}
                  :node/color {:type type :id id}
                  :node/signal {:type type :id id}

                  :inlet/color (load-node {:type type :id id})
                  :outlet/color (load-node {:type type :id id})

                  {:type type}
                  )))

(defn event-buttons [ev]
      {:buttons (.-buttons ev)})

(defn event-position [ev]
      (let [ev* (.-nativeEvent ev)
            pos (vec2 (.-x ev*) (.-y ev*))
            offset (pd-screen-offset)]
           {:position (g/- pos offset)}))
