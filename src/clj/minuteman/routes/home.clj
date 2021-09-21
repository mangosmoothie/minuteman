(ns minuteman.routes.home
  (:require
   [minuteman.layout :as layout]
   [minuteman.db.core :as db]
   [clojure.java.io :as io]
   [minuteman.middleware :as middleware]
   [ring.util.response]
   [ring.util.http-response :as response]))

(defn home-page [request]
  (layout/render request "home.html"))

(defn home-routes []
  [""
   {:middleware [middleware/wrap-csrf
                 middleware/wrap-formats]}

   ["/" {:get home-page}]

   ["/es-instances" {:get (fn [_] (db/get-es-instances))
                     :post (fn [{params :params}]
                             (db/create-es-instance! params))}]

   ["/es-indices/:id/:watch" {:put (fn [{:keys [id watch]}]
                                     (if (= "watch" watch)
                                       (db/watch-es-index! id)
                                       (db/unwatch-es-index! id)))}]

   ["/es-indices" {:get (fn [_] (db/get-es-indices))}]

   ["/docs" {:get (fn [_]
                    (-> (response/ok (-> "docs/docs.md" io/resource slurp))
                        (response/header "Content-Type" "text/plain; charset=utf-8")))}]])
