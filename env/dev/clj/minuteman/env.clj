(ns minuteman.env
  (:require
    [selmer.parser :as parser]
    [clojure.tools.logging :as log]
    [minuteman.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "\n-=[minuteman started successfully using the development profile]=-"))
   :stop
   (fn []
     (log/info "\n-=[minuteman has shut down successfully]=-"))
   :middleware wrap-dev})
