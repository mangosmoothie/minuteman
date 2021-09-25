(ns minuteman.routes.api
  (:require
   [minuteman.db.core :as db]
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
                               (response/created
                                (str "/api/es-instances/" id))))}]

   ["/es-indices" {:get (fn [_] (response/ok {:data (db/get-es-indices)}))
                   :post (fn [{params :params}]
                           (let [id (-> params
                                        db/create-es-index!
                                        first :id)]
                             (response/created (str "/api/es-instances/" id))))}]

   ["/es-indices/:id/:watch" {:put (fn [{{:keys [watch] :as params} :path-params}]
                                     (if (= "watch" watch)
                                       (db/watch-es-index! params)
                                       (db/unwatch-es-index! params))
                                     (response/no-content))}]])
