(ns minuteman.app
  (:require [minuteman.core :as core]))

;;ignore println statements in prod
(set! *print-fn* (fn [& _]))

(core/init!)
