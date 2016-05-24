(set-env!
  :source-paths #{"src/clj" "src/cljs"}
  :resource-paths #{"resources"}
  :dependencies '[[adzerk/boot-cljs "1.7.48-5" :scope "test"]
                  [adzerk/boot-reload "0.4.7" :scope "test"]
                  [org.clojure/clojure "1.8.0"]
                  [org.clojure/clojurescript "1.8.51"]
                  [org.clojure/tools.nrepl "0.2.12"]
                  [org.clojure/tools.reader "1.0.0-alpha3"]
                  [org.danielsz/system "0.3.0-SNAPSHOT"]
                  [com.taoensso/sente "1.8.1"]
                  [org.clojure/core.match "0.3.0-alpha4"]
                  [org.immutant/web "2.1.3"]
                  [ring/ring-core "1.4.0"]
                  [ring/ring-defaults "0.2.0"]
                  [ring-middleware-format "0.7.0"]

                  [clj-time "0.11.0"]
                  [compojure "1.5.0"]
                  [environ "1.0.2"]
                  [org.clojure/core.async "0.2.374"]
                  [cljs-ajax "0.5.4"]
                  [adzerk/boot-cljs-repl "0.3.0" :scope "test"]
                  [com.cemerick/piggieback "0.2.1" :scope "test"]
                  [weasel "0.7.0" :scope "test"]
                  [boot-environ "1.0.2"]
                  [hiccup "1.0.5"]
                  [org.danielsz/cljs-utils "0.1.0-SNAPSHOT"]

                  [yesql "0.5.2"]
                  [com.novemberain/monger "3.0.2"]
                  [org.clojure/java.jdbc "0.4.2"]
                  [com.h2database/h2 "1.4.191"]

                  ; om
                  [org.omcljs/om "0.9.0"]
                  [org.danielsz/om-flash-bootstrap "0.1.0-SNAPSHOT"]
                  [org.danielsz/om-header-bootstrap "0.1.0-SNAPSHOT"]
                  [ankha "0.1.5.1-64423e"]

                  ; ---- frontend ----------------
                  [cljsjs/react "0.14.0-0"]
                  [cljsjs/react-dom "0.14.0-0"]
                  [reagent "0.6.0-alpha2"]
                  [reagent-utils "0.1.8"]
                  [re-frame "0.7.0"]
                  [secretary "1.2.3"]
                  [prismatic/schema "1.1.1"]
                  [cljs-ajax "0.5.4"]


                  [com.joshuadavey/boot-middleman "0.0.7" :scope "test"]
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
           (environ :env {:http-port "3041"})
           (watch :verbose true)
           (speak)
           (middleman)
           (system :sys #'dev-system :auto true :files ["handler.clj" "html.clj"])
           (reload)
           (cljs-repl)
           (cljs :source-map true :optimizations :none)
           (target "target")))

