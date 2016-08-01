(ns guadalete-ui.views.editor
  (:require
    [reagent.core :refer [create-class]]
    [guadalete-ui.util :refer [pretty]]
    [guadalete-ui.console :as log]))

(defn editor [data]
  (create-class
    {:component-did-mount
     (fn [_]
       ;; init the sidebar upon mount
       (let [editor (.edit js/ace "editor")]
         (.setTheme editor "ace/theme/kr_theme")            ;
         (-> editor
             (.getSession)
             (.setMode "ace/mode/json"))
         (-> editor
             (.getSession)
             (.setValue (pretty data)))))

     :reagent-render
     (fn []
       [:div#editor])}))