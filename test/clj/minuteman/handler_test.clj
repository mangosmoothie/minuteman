(ns minuteman.handler-test
  (:require
   [clojure.test :refer [is deftest testing use-fixtures]]
   [luminus-migrations.core :as migrations]
   [minuteman.config :refer [env]]
   [minuteman.db.core :as db]
   [minuteman.elasticsearch.core :refer [refresh-instances]]
   [minuteman.handler :refer [app]]
   [minuteman.middleware.formats :as formats]
   [minuteman.middleware]
   [minuteman.routes.api :refer [api-routes]]
   [mount.core :as mount]
   [muuntaja.core :as m]
   [reitit.ring :as ring]
   [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
   [ring.mock.request :refer [request]]
   [ring.util.http-predicates :refer [ok? created? not-found? no-content?]]))

(defn parse-json [body]
  (m/decode formats/instance "application/json" body))

(def route-handler
  "need this to get around CSRF middleware"
  (let [f (fn [handler]
            (fn ([r] (handler r))
              ([req res raise]
               (handler req res raise))))]
    (with-redefs [minuteman.middleware/wrap-csrf f]
      (-> (ring/ring-handler (ring/router [(api-routes)]))
          (wrap-defaults
           (-> site-defaults
               (dissoc :security)
               (dissoc :session)))))))

(def test-es-instance {:name "test_name" :url "test_url"})

(def test-es-index {:name "test_name"})

(def get-es-instance-id
  "use the same instance-id between tests"
  (memoize
   (fn []
     (-> (db/get-es-instances) first :id))))

(use-fixtures
  :once
  (fn [f]
    (mount/start #'minuteman.config/env
                 #'minuteman.handler/app-routes
                 #'minuteman.db.core/*db*)
    (migrations/migrate ["migrate"] (select-keys env [:database-url]))
    (f)))

(deftest test-app
  (testing "main route"
    (let [response ((app) (request :get "/"))]
      (is (ok? response))))

  (testing "not-found route"
    (let [response ((app) (request :get "/invalid"))]
      (is (not-found? response))))

  (testing "create es-instance route"
    (let [response (with-redefs [refresh-instances (constantly nil)]
                     (route-handler
                      (request :post "/api/es-instances" test-es-instance)))]
      (is (created? response))))

  (testing "get es-instances route"
    (let [response ((app) (request :get "/api/es-instances"))
          data (-> response :body parse-json :data)]
      (is (ok? response))
      (is (seq data))
      (is (seq (filter #(= (:name test-es-instance) (:name %)) data)))))

  (testing "create es-index route"
    (let [es-instance-id (get-es-instance-id)
          response (route-handler
                    (request :post "/api/es-indices"
                             (assoc test-es-index :es-instance-id es-instance-id)))]
      (is (created? response))))

  (testing "get es-indices route"
    (let [response ((app) (request :get "/api/es-indices"))
          data (-> response :body parse-json :data)]
      (is (ok? response))
      (is (seq data))))

  (testing "toggle es-index watch"
    (let [es-index-id (-> (db/get-es-indices) first :id)
          toggle-watch (fn [s]
                         (route-handler
                          (request :put (str "/api/es-indices/"
                                             es-index-id "/" s))))
          response-watch (toggle-watch "watch")
          es-index-watched (db/get-es-index {:id es-index-id})
          response-unwatch (toggle-watch "unwatch")
          es-index-unwatched (db/get-es-index {:id es-index-id})]
      (is (no-content? response-watch))
      (is (:watching es-index-watched))
      (is (no-content? response-unwatch))
      (is (not (:watching es-index-unwatched))))))
