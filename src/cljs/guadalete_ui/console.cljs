(ns guadalete-ui.console
  "Functions wrapping the JavaScript `console` API."
  (:refer-clojure :exclude [time]))

;;;; Logging
(defn js-apply [f target args]
      (.apply f target (to-array args)))

(defn log
      "Display messages to the console."
      [& args]
      (js-apply (.-log js/console) js/console args))

(defn debug
      "Like `log` but marks the output as debugging information."
      [& args]
      (js-apply (.-debug js/console) js/console args))

(defn info
      "Like `log` but marks the output as an informative message."
      [& args]
      (js-apply (.-info js/console) js/console args))

(defn warn
      "Like `log` but marks the output as a warning."
      [& args]
      (js-apply (.-warn js/console) js/console args))

(defn error
      "Like `log` but marks the output as an error."
      [& args]
      (js-apply (.-error js/console) js/console args))