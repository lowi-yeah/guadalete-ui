(ns guadalete-ui.helpers.config
  (:require
    [clojure.edn :as edn]
    [taoensso.timbre :as log]
    [environ.core :refer [env]]
    [guadalete-ui.helpers.util :refer [deep-merge]]))

;//                __ _                    _   _
;//   __ ___ _ _  / _(_)__ _ _  _ _ _ __ _| |_(_)___ _ _
;//  / _/ _ \ ' \|  _| / _` | || | '_/ _` |  _| / _ \ ' \
;//  \__\___/_||_|_| |_\__, |\_,_|_| \__,_|\__|_\___/_||_|
;//                    |___/
(defn load-config []
  (reduce deep-merge (map (comp edn/read-string slurp)
                          [(:config-file env)])))