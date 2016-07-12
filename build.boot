(set-env!
  :source-paths #{"src/clj" "src/cljs" "src/cljc"}
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
                  [danlentz/clj-uuid "0.1.6"]
                  [clj-time "0.11.0"]
                  [compojure "1.5.0"]
                  [environ "1.0.2"]
                  [org.clojure/core.async "0.2.374"]
                  [adzerk/boot-cljs-repl "0.3.0" :scope "test"]
                  [com.cemerick/piggieback "0.2.1" :scope "test"]
                  [weasel "0.7.0" :scope "test"]
                  [boot-environ "1.0.2"]
                  [hiccup "1.0.5"]
                  [org.danielsz/cljs-utils "0.1.0-SNAPSHOT"]
                  [expiring-map "0.1.7"]                    ;maps with ttl used for session storage
                  [com.apa512/rethinkdb "0.15.23"]
                  [cheshire "5.6.1"]

                  ; evaluate, keep one of them
                  [clj-kafka "0.3.4"]
                  ;[kafka-clj "3.6.5"]

                  ; ---- workaround for 'No namespace: clojure.core.memoize' error
                  ; ---- @see https://github.com/cemerick/friend/issues/116
                  [org.clojure/core.cache "0.6.4"]
                  [org.clojure/core.memoize "0.5.6" :exclusions [org.clojure/core.cache]]
                  [com.cemerick/friend "0.2.1"]

                  [differ "0.3.1"]

                  ; ---- frontend ----------------
                  [cljsjs/react "0.14.0-0"]
                  [cljsjs/react-dom "0.14.0-0"]
                  [reagent "0.6.0-alpha2"]
                  [reagent-utils "0.1.8"]
                  [re-frame "0.7.0"]
                  [secretary "1.2.3"]
                  [prismatic/schema "1.1.1"]
                  [com.taoensso/encore "2.53.0"]
                  [com.joshuadavey/boot-middleman "0.0.7" :scope "test"]
                  [com.cognitect/transit-cljs "0.8.237"]
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
           ;(middleman)
           (system :sys #'dev-system :auto true :files ["handler.clj" "html.clj"])
           (reload)
           (cljs-repl)
           ;(cljs :source-map true :optimizations :none)
           (target "target")))

