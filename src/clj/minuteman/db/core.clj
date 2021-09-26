(ns minuteman.db.core
  (:require
    [camel-snake-kebab.core :refer [->kebab-case-keyword ->snake_case_keyword]]
    [camel-snake-kebab.extras :refer [transform-keys]]
    [conman.core :as conman]
    [hugsql.adapter]
    [hugsql.core]
    [minuteman.config :refer [env]]
    [mount.core :refer [defstate]]
    [next.jdbc.result-set]))

(defstate ^:dynamic *db*
          :start (conman/connect! {:jdbc-url (env :database-url)})
          :stop (conman/disconnect! *db*))

(conman/bind-connection *db* "sql/queries.sql")

(extend-protocol next.jdbc.result-set/ReadableColumn
  java.sql.Timestamp
  (read-column-by-label [^java.sql.Timestamp v _]
    (.toLocalDateTime v))
  (read-column-by-index [^java.sql.Timestamp v _2 _3]
    (.toLocalDateTime v))
  java.sql.Date
  (read-column-by-label [^java.sql.Date v _]
    (.toLocalDate v))
  (read-column-by-index [^java.sql.Date v _2 _3]
    (.toLocalDate v))
  java.sql.Time
  (read-column-by-label [^java.sql.Time v _]
    (.toLocalTime v))
  (read-column-by-index [^java.sql.Time v _2 _3]
    (.toLocalTime v)))

(defn result-one-snake->kebab
  [this result options]
  (->> (hugsql.adapter/result-one this result options)
       (transform-keys ->kebab-case-keyword)))

(defn result-many-snake->kebab
  [this result options]
  (->> (hugsql.adapter/result-many this result options)
       (map #(transform-keys ->kebab-case-keyword %))))

(defmethod hugsql.core/hugsql-result-fn :1 [sym]
  'minuteman.db.core/result-one-snake->kebab)

(defmethod hugsql.core/hugsql-result-fn :one [sym]
  'minuteman.db.core/result-one-snake->kebab)

(defmethod hugsql.core/hugsql-result-fn :* [sym]
  'minuteman.db.core/result-many-snake->kebab)

(defmethod hugsql.core/hugsql-result-fn :many [sym]
  'minuteman.db.core/result-many-snake->kebab)
