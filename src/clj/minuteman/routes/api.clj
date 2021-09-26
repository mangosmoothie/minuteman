(ns minuteman.routes.api
  (:require
   [camel-snake-kebab.core :refer [->snake_case_keyword]]
   [camel-snake-kebab.extras :refer [transform-keys]]
   [minuteman.db.core :as db]
   [minuteman.elasticsearch.core :refer [refresh-instances]]
   [minuteman.middleware :as middleware]
   [ring.util.http-response :as response]))

(defn api-routes []
  ["/api"
   {:middleware [middleware/wrap-csrf
                 middleware/wrap-formats]}

   ["/es-instances" {:get (fn [_]
                            (response/ok {:data (db/get-es-instances)}))
                     :post (fn [{params :params}]
                             (let [id (-> (merge {:headers nil} params)
                                          db/create-es-instance!
                                          first :id)]
                               (refresh-instances [(assoc params :id id)])
                               (response/created
                                (str "/api/es-instances/" id))))}]

   ["/es-indices" {:get (fn [_] (response/ok {:data (db/get-es-indices)}))
                   :post (fn [{params :params}]
                           (let [id (->> params
                                         (transform-keys ->snake_case_keyword)
                                         db/create-es-index!
                                         first :id)]
                             (response/created (str "/api/es-instances/" id))))}]

   ["/es-indices/:id/:watch" {:put (fn [{{:keys [watch] :as params} :path-params}]
                                     (if (= "watch" watch)
                                       (db/watch-es-index! params)
                                       (db/unwatch-es-index! params))
                                     (response/no-content))}]])
