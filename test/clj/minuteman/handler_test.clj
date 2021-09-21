(ns minuteman.handler-test
  (:require
   [clojure.test :refer :all]
   [ring.mock.request :refer :all]
   [minuteman.handler :refer :all]
   [luminus-migrations.core :as migrations]
   [minuteman.middleware.formats :as formats]
   [muuntaja.core :as m]
   [minuteman.db.core :refer [*db*] :as db]
   [minuteman.config :refer [env]]
   [mount.core :as mount]))

(defn parse-json [body]
  (m/decode formats/instance "application/json" body))

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

  (testing "create instance route"
    (let [test-es-instance {:name "test_name" :url "test_url"}
          response1 ((app) (request :post "/es-instances" test-es-instance))
          response2 ((app) (request :get "/es-instances"))]
      (is (= 200 (:status response1)))
      (is (= 200 (:status response2)))
      (is (= [test-es-instance] (:body response2))))))
