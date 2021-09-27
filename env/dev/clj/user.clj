(ns user
  "Userspace functions you can run by default in your local REPL."
  (:require
   [minuteman.config :refer [env]]
   [clojure.pprint]
   [clojure.spec.alpha :as s]
   [expound.alpha :as expound]
   [mount.core :as mount]
   [minuteman.core :refer [start-app]]
   [minuteman.middleware.formats :as formats]
   [muuntaja.core :as m]
   [minuteman.db.core]
   [conman.core :as conman]
   [luminus-migrations.core :as migrations]
   [minuteman.handler :refer [app]]
   [ring.mock.request :refer [request]]))

(alter-var-root #'s/*explain-out* (constantly expound/printer))

(add-tap (bound-fn* clojure.pprint/pprint))

(defn start
  "Starts application.
  You'll usually want to run this on startup."
  []
  (mount/start-without #'minuteman.core/repl-server))

(defn stop
  "Stops application."
  []
  (mount/stop-except #'minuteman.core/repl-server))

(defn restart
  "Restarts application."
  []
  (stop)
  (start))

(defn restart-db
  "Restarts database."
  []
  (mount/stop #'minuteman.db.core/*db*)
  (mount/start #'minuteman.db.core/*db*)
  (binding [*ns* (the-ns 'minuteman.db.core)]
    (conman/bind-connection minuteman.db.core/*db* "sql/queries.sql")))

(defn reset-db
  "Resets database."
  []
  (migrations/migrate ["reset"] (select-keys env [:database-url])))

(defn migrate
  "Migrates database up for all outstanding migrations."
  []
  (migrations/migrate ["migrate"] (select-keys env [:database-url])))

(defn rollback
  "Rollback latest database migration."
  []
  (migrations/migrate ["rollback"] (select-keys env [:database-url])))

(defn create-migration
  "Create a new up and down migration file with a generated timestamp and `name`."
  [name]
  (migrations/create name (select-keys env [:database-url])))

(defn parse-json [body]
  (m/decode formats/instance "application/json" body))

(defn get-instances [] (-> ((app) (request :get "/es-instances")) :body parse-json))

(defn delete-test-instances []
  (minuteman.db.core/delete-test-instances!))
