(ns minuteman.middleware
  (:require
    [clojure.tools.logging :as log]
    [minuteman.env :refer [defaults]]
    [minuteman.layout :refer [error-page]]
    [minuteman.middleware.formats :as formats]
    [muuntaja.middleware :refer [wrap-format wrap-params]]
    [ring.adapter.undertow.middleware.session :refer [wrap-session]]
    [ring.middleware.anti-forgery :refer [wrap-anti-forgery]]
    [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
    [ring.middleware.flash :refer [wrap-flash]]
    [ring.util.http-response :as response]))

(defn wrap-internal-error [handler]
  (fn [req]
    (try
      (handler req)
      (catch Throwable t
        (log/error t (.getMessage t))
        (error-page {:status 500
                     :title "Server error"
                     :message "Please contact the administrator"})))))

(defn wrap-csrf [handler]
  (wrap-anti-forgery
    handler
    {:error-response
     (error-page
       {:status 403
        :title "Invalid anti-forgery token"})}))

(defn wrap-formats [handler]
  (let [wrapped (-> handler wrap-params (wrap-format formats/instance))]
    (fn [request]
      ;; disable wrap-formats for websockets
      ;; since they're not compatible with this middleware
      ((if (:websocket? request) handler wrapped) request))))

;; TODO keep this?
(defn wrap-in-response
  "if response is bare map, wrap it in a response"
  [handler]
  (fn
    ([request] (handler request))
    ([request respond raise]
     (handler request
              (fn [res]
                (respond
                 (cond-> res
                   (map? res) (response/ok res)
                   (vector? res) (response/ok {:data res}))))
              raise))))

(defn wrap-base [handler]
  (-> ((:middleware defaults) handler)
      wrap-flash
      (wrap-session {:cookie-attrs {:http-only true}})
      (wrap-defaults
        (-> site-defaults
            (assoc-in [:security :anti-forgery] false)
            (dissoc :session)))
      wrap-internal-error))
