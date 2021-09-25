(ns minuteman.events
  (:require
   [re-frame.core :as rf]
   [ajax.core :as ajax]
   [reitit.frontend.easy :as rfe]
   [reitit.frontend.controllers :as rfc]))

;;dispatchers

(rf/reg-event-db
 :common/navigate
 (fn [db [_ match]]
   (let [old-match (:common/route db)
         new-match (assoc match :controllers
                          (rfc/apply-controllers (:controllers old-match) match))]
     (assoc db :common/route new-match))))

(rf/reg-fx
 :common/navigate-fx!
 (fn [[k & [params query]]]
   (rfe/push-state k params query)))

(rf/reg-event-fx
 :common/navigate!
 (fn [_ [_ url-key params query]]
   {:common/navigate-fx! [url-key params query]}))

(rf/reg-event-db
 :set-docs
 (fn [db [_ docs]]
   (assoc db :docs docs)))

(rf/reg-event-fx
 :fetch-docs
 (fn [_ _]
   {:http-xhrio {:method          :get
                 :uri             "/docs"
                 :response-format (ajax/raw-response-format)
                 :on-success       [:set-docs]}}))

(rf/reg-event-db
 :common/set-error
 (fn [db [_ error]]
   (assoc db :common/error error)))

(rf/reg-event-fx
 :page/init-home
 (fn [_ _]
   {:dispatch [:fetch-es-instances]}))

(rf/reg-event-fx
 :fetch-es-instances
 (fn [_ _]
   {:http-xhrio {:method          :get
                 :uri             "/api/es-instances"
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success      [:set-es-instances]
                 :on-fail         [:common/set-error
                                   "unable to fetch es instances"]}}))

(rf/reg-event-db
 :set-es-instances
 (fn [db [_ {es-instances :data}]]
   (assoc db :es-instances (map #(assoc % :label (:name %)) es-instances))))

(rf/reg-event-fx
 :fetch-es-indices
 (fn [_ _]
   {:http-xhrio {:method          :get
                 :uri             "/api/es-indices"
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success      [:set-es-indices]
                 :on-fail         [:common/set-error
                                   "unable to fetch es indices"]}}))

(rf/reg-event-db
 :set-es-indices
 (fn [db [_ es-indices]]
   (assoc db :es-indices es-indices)))

(rf/reg-event-fx
 :fetch-es-index-metrics
 (fn [{:keys [db]} [_ es-index-id]]
   {:db         (assoc db :es-index-metrics-loading true)
    :http-xhrio {:method          :get
                 :uri             (str "/api/es-indices/" es-index-id)
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success      [:set-es-index-metrics]
                 :on-fail         [:common/set-error
                                   "unable to fetch metrics"]}}))

(rf/reg-event-db
 :set-es-index-metrics
 (fn [db [_ es-index-metrics]]
   (assoc db
          :es-index-metrics es-index-metrics
          :es-index-metrics-loading false)))

(rf/reg-event-fx
 :set-es-index-watch
 (fn [{:keys [db]} [_ es-index-id es-index-watch]]
   {:db         (assoc db :es-index-metrics-loading true)
    :http-xhrio {:method          :put
                 :uri             (str "/api/es-indices/" es-index-id
                                       (if es-index-watch "/watch" "/unwatch"))
                 :response-format (ajax/raw-response-format)
                 :on-success      [:set-es-index-metrics]
                 :on-fail         [:common/set-error
                                   "failed to update watch on index"]}}))

(rf/reg-event-fx
 :create-es-instance
 (fn [_ [_ es-instance]]
   {:http-xhrio {:method          :post
                 :uri             "/api/es-instances"
                 :format          (ajax/json-request-format)
                 :response-format (ajax/raw-response-format)
                 :params          es-instance
                 :on-success      [:fetch-es-instances]
                 :on-fail         [:common/set-error
                                   "unable to create es instance"]}}))
;;subscriptions

(rf/reg-sub
 :common/route
 (fn [db _]
   (-> db :common/route)))

(rf/reg-sub
 :common/page-id
 :<- [:common/route]
 (fn [route _]
   (-> route :data :name)))

(rf/reg-sub
 :common/page
 :<- [:common/route]
 (fn [route _]
   (-> route :data :view)))

(rf/reg-sub
 :docs
 (fn [db _]
   (:docs db)))

(rf/reg-sub
 :common/error
 (fn [db _]
   (:common/error db)))

(rf/reg-sub
 :es-instances
 (fn [db _]
   (:es-instances db)))

(rf/reg-sub
 :es-indices
 (fn [db _]
   (:es-indices db)))

(rf/reg-sub
 :es-index-states
 (fn [db _]
   (:es-index-states db)))

(rf/reg-sub
 :es-load-monitors
 (fn [db _]
   (:es-load-monitors db)))
