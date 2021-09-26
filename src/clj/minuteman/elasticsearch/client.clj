(ns minuteman.elasticsearch.client
  (:require
   [clj-http.client :as http]
   [clojure.string :refer [join]]))

(defn get-index-count [url index]
  (http/get (join "/" [url index "_count"])))

(defn refresh-watched-indices
  "force index updates for watched indices"
  [url indices]
  (doseq [index indices]
    (get-index-count url index)))

(defn get-indices
  "get current metrics for all indices in an es-instance"
  [{:keys [url watched-indices]}]
  (refresh-watched-indices url (map :name watched-indices))
  (:body (http/get (str url "/_cat/indices")
                   {:accept :json :as :json})))
