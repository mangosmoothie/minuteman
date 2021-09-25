(ns minuteman.handler-test
  (:require
   [clojure.test :refer [is deftest testing use-fixtures]]
   [luminus-migrations.core :as migrations]
   [minuteman.config :refer [env]]
   [minuteman.db.core :refer [*db*] :as db]
   [minuteman.handler :refer [app]]
   [minuteman.middleware :refer [wrap-base wrap-formats]]
   [minuteman.middleware.formats :as formats]
   [minuteman.routes.api :refer [api-routes]]
   [mount.core :as mount]
   [muuntaja.core :as m]
   [reitit.ring :as ring]
   [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
   [ring.middleware.params :refer [wrap-params]]
   [ring.mock.request :refer [request]]))

(defn parse-json [body]
  (m/decode formats/instance "application/json" body))

(def post-route-handler
  "need this to get around CSRF middleware"
  (-> (ring/ring-handler (ring/router [(api-routes)]))
      (wrap-defaults
       (-> site-defaults
           (dissoc :security)
           (dissoc :session)))))

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
      (is (= 200 (:status response)))))

  (testing "not-found route"
    (let [response ((app) (request :get "/invalid"))]
      (is (= 404 (:status response)))))

  (testing "create es-instance route"
    (let [response (post-route-handler
                    (request :post "/api/es-instances" test-es-instance))]
      (is (= 201 (:status response)))))

  (testing "get es-instances route"
    (let [response ((app) (request :get "/api/es-instances"))
          data (-> response :body parse-json :data)]
      (is (= 200 (:status response)))
      (is (seq data))
      (is (seq (filter #(= (:name test-es-instance) (:name %)) data)))))

  (testing "create es-index route"
    (let [es-instance-id (get-es-instance-id)
          response (post-route-handler
                    (request :post "/api/es-indices"
                             (assoc test-es-index :es_instance_id es-instance-id)))]
      (is (= 201 (:status response)))))

  (testing "get es-indices route"
    (let [response ((app) (request :get "/api/es-indices"))
          data (-> response :body parse-json :data)]
      (is (= 200 (:status response)))
      (is (seq data))))

  (testing "toggle es-index watch"
    (let [es-index-id (-> (db/get-es-indices) first :id)
          toggle-watch (fn [s]
                         (post-route-handler
                          (request :put (str "/api/es-indices/"
                                             es-index-id "/" s))))
          response-watch (toggle-watch "watch")
          es-index-watched (db/get-es-index {:id es-index-id})
          response-unwatch (toggle-watch "unwatch")
          es-index-unwatched (db/get-es-index {:id es-index-id})]
      (is (= 204 (:status response-watch)))
      (is (:watching es-index-watched))
      (is (= 204 (:status response-unwatch)))
      (is (not (:watching es-index-unwatched))))))
