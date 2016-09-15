(set-env!
  :source-paths #{"src/clj" "src/cljs" "src/cljc"}
  :resource-paths #{"resources"}
  :dependencies '[[adzerk/boot-cljs "1.7.228-1" :scope "test"]
                  [adzerk/boot-reload "0.4.11" :scope "test"]
                  [org.clojure/clojure "1.8.0"]
                  [org.clojure/clojurescript "1.9.89"]
                  [org.clojure/tools.nrepl "0.2.12"]
                  [org.clojure/tools.reader "1.0.0-beta3"]
                  [org.danielsz/system "0.3.0"]
                  [com.taoensso/sente "1.9.0"]
                  [org.clojure/core.match "0.3.0-alpha4"]
                  [org.immutant/web "2.1.3"]
                  [ring/ring-core "1.6.0-beta4"]
                  [ring/ring-defaults "0.2.1"]
                  [ring-middleware-format "0.7.0"]
                  [danlentz/clj-uuid "0.1.6"]
                  [clj-time "0.11.0"]
                  [compojure "1.5.1"]
                  [environ "1.0.3"]
                  [org.clojure/core.async "0.2.385"]
                  [adzerk/boot-cljs-repl "0.3.3" :scope "test"]
                  [com.cemerick/piggieback "0.2.1" :scope "test"]
                  [weasel "0.7.0" :scope "test"]
                  [boot-environ "1.0.3"]
                  [hiccup "1.0.5"]
                  [org.danielsz/cljs-utils "0.1.0-SNAPSHOT"]
                  [expiring-map "0.1.7"]                    ;maps with ttl used for session storage
                  [com.apa512/rethinkdb "0.15.26"]
                  [cheshire "5.6.3"]
                  [com.taoensso/carmine "2.13.1"]

                  ; evaluate, keep one of them
                  [clj-kafka "0.3.4"]

                  [com.cemerick/friend "0.2.3"]

                  [differ "0.3.1"]

                  ; ---- frontend ----------------
                  [binaryage/devtools "0.7.2"]
                  [cljsjs/react "15.2.1-1"]
                  [cljsjs/react-dom "0.14.0-0"]
                  [reagent "0.6.0-rc"]
                  [reagent-utils "0.1.9"]
                  [re-frame "0.8.0-alpha2"]
                  ;[day8.re-frame/async-flow-fx "0.0.1"]
                  [secretary "1.2.3"]
                  [prismatic/schema "1.1.3"]
                  [metosin/schema-tools "0.9.0"]
                  [com.andrewmcveigh/cljs-time "0.4.0"]
                  [com.taoensso/encore "2.65.0"]
                  [com.joshuadavey/boot-middleman "0.0.7" :scope "test"]
                  [com.cognitect/transit-cljs "0.8.239"]
                  [thi.ng/geom "0.0.908"]
                  [thi.ng/math "0.2.1"]
                  [thi.ng/tweeny "0.1.0-SNAPSHOT"]
                  [thi.ng/color "1.2.0"]
                  ])

(require
  '[adzerk.boot-cljs :refer [cljs]]
  '[adzerk.boot-reload :refer [reload]]
  '[guadalete-ui.systems :refer [dev-system]]
  '[environ.boot :refer [environ]]
  '[system.boot :refer [system]]
  '[adzerk.boot-cljs-repl :refer [cljs-repl start-repl]]
  '[com.joshuadavey.boot-middleman :refer [middleman]])

(deftask dev
         "Run a restartable system in the Repl"
         []
         (comp
           (environ :env {:http-port "3041"
                          :config-file "resources/config.edn"})
           (watch :verbose true)
           (speak)
           ;;; the js build toolchain
           (middleman)
           (system :sys #'dev-system :auto true :files ["handler.clj"])
           (reload)
           (cljs-repl)
           (cljs :source-map true :optimizations :none)
           (target "target")))


;//   _     _      _
;//  | |___(_)_ _ (_)_ _  __ _ ___ _ _
;//  | / -_) | ' \| | ' \/ _` / -_) ' \
;//  |_\___|_|_||_|_|_||_\__, \___|_||_|
;//                      |___/
(defn- generate-lein-project-file! [& {:keys [keep-project] :or {:keep-project true}}]
       (require 'clojure.java.io)
       (let [pfile ((resolve 'clojure.java.io/file) "project.clj")
             ; Only works when pom options are set using task-options!
             {:keys [project version]} (:task-options (meta #'boot.task.built-in/pom))
             prop #(when-let [x (get-env %2)] [%1 x])
             head (list* 'defproject (or project 'boot-project) (or version "0.0.0-SNAPSHOT")
                         (concat
                           (prop :url :url)
                           (prop :license :license)
                           (prop :description :description)
                           [:dependencies (get-env :dependencies)
                            :repositories (get-env :repositories)
                            :source-paths (vec (concat (get-env :source-paths)
                                                       (get-env :resource-paths)))]))
             proj (pp-str head)]
            (if-not keep-project (.deleteOnExit pfile))
            (spit pfile proj)))

(deftask lein-generate
         "Generate a leiningen `project.clj` file.
          This task generates a leiningen `project.clj` file based on the boot
          environment configuration, including project name and version (generated
          if not present), dependencies, and source paths. Additional keys may be added
          to the generated `project.clj` file by specifying a `:lein` key in the boot
          environment whose value is a map of keys-value pairs to add to `project.clj`."
         []
         (generate-lein-project-file! :keep-project true))
