(ns guadalete-ui.handlers.redis
    (:require
      [taoensso.carmine :as car :refer [wcar]]
      [taoensso.timbre :as log]
      )
    )

(defmacro wcar* [connection & body] `(car/wcar ~connection ~@body))

(defn- signal-values [connection signal-id]
       (log/debug "ping" (wcar* connection (car/ping))) ; => "PONG" (1 command -> 1 reply)
       )
