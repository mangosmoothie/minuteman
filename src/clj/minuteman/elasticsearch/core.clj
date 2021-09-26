(ns minuteman.elasticsearch.core
  (:require
   [camel-snake-kebab.core :refer [->snake_case_keyword]]
   [camel-snake-kebab.extras :refer [transform-keys]]
   [clojure.set]
   [clojure.string :as str]
   [minuteman.db.core :as db]
   [minuteman.elasticsearch.client :refer [get-indices]]))

(defn- transform-es-keys
  "change . to - and :index to :name"
  [index-state]
  (into {}
        (reduce
         (fn [acc [k v]]
           (if (= :index k)
             (conj acc [:name v])
             (conj acc [(keyword (str/replace (name k) "." "-")) v])))
         []
         index-state)))

(defn- add-new-states
  "retreive and add new states to an instance"
  [instance]
  (assoc instance :new-states
         (get-indices instance)))

(defn- ->add-watched-indices
  "add watched indices to instance"
  [instance-id->indices]
  (fn [instance]
    (assoc instance :watched-indices
           (filter :watching (instance-id->indices (:id instance))))))

(defn- tag-new-states
  "tag new states with instance id"
  [instance]
  (update instance :new-states
          (fn [states]
            (map #(assoc % :es-instance-id (:id instance))
                 states))))

(defn- parse-or-zero [n]
  (try
    (cond (string? n) (Integer/parseInt n)
          (int? n) n
          :else 0)
    (catch Exception _ 0)))

(defn- parse-counts [index-state]
  (-> index-state
      (update :docs-count parse-or-zero)
      (update :docs-deleted parse-or-zero)))

(def empty-index
  {:docs-count 0 :docs-deleted 0 :health "DELETED" :store-size nil})

(defn- mark-n-tag-index-states
  "make deleted index states and tag all states with index id"
  [index-states-map indices]
  (reduce
   (fn [acc {:keys [es-instance-id name] :as index}]
     (let [k [es-instance-id name]]
       (if (contains? acc k)
         (update acc k #(assoc % :es-index-id (:id index)))
         (assoc acc k (assoc empty-index
                             :es-index-id (:id index)
                             :name (:name index))))))
   index-states-map
   indices))

(defn- get-index-states
  "retrieve the latest index states from elasticsearch instances
   as map of [es-instance-id name] -> index state"
  [instances indices]
  (let [instance-id->indices (group-by :es-instance-id indices)
        add-watched-indices (->add-watched-indices instance-id->indices)
        ->key-value (fn [{:keys [es-instance-id name] :as m}]
                      [[es-instance-id name] m])]
    (-> (into {}
              (comp
               (map add-watched-indices)
               (map add-new-states)
               (map tag-new-states)
               (mapcat :new-states)
               (map transform-es-keys)
               (map parse-counts)
               (map ->key-value))
              instances)
        (mark-n-tag-index-states indices))))

(defn- detect-new-indices [index-states-map indices]
  (map #(get index-states-map %)
       (clojure.set/difference
        (into #{} (keys index-states-map))
        (into #{} (map (fn [{:keys [es-instance-id name]}]
                         [es-instance-id name])
                       indices)))))

(defn- refresh? [current-state]
  (if-let [db-count (-> {:es_index_id (:es-index-id current-state)}
                        db/get-current-index-state
                        :docs-count)]
    (not (= (:docs-count current-state) db-count))
    false))

(defn- create-new-indices [indices]
  (map (fn [index]
         (let [index-id (->> index
                             (transform-keys ->snake_case_keyword)
                             db/create-es-index!
                             first :id)]
           (assoc index :es-index-id index-id)))
       indices))

(defn- create-new-index-states [states]
  (doseq [state states]
    (db/create-es-index-state! (transform-keys ->snake_case_keyword state))))

(defn refresh-instances
  "refresh indices for given instances in database with latest states
  adding new ones if found"
  [instances]
  (let [known-indices  (db/get-es-indices)
        index-states (get-index-states instances known-indices)
        new-indices (detect-new-indices index-states known-indices)
        states-to-refresh (filter refresh? (vals index-states))
        new-states (create-new-indices new-indices)]
    (create-new-index-states (concat new-states states-to-refresh))))

(defn refresh-all-instances
  "refresh all indices in database with latest states, adding new ones if found"
  []
  (let [instances (db/get-es-instances)]
    (refresh-instances instances)))
