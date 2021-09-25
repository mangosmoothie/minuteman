(ns minuteman.core
  (:require
    [day8.re-frame.http-fx]
    [reagent.dom :as rdom]
    [reagent.core :as r]
    [re-frame.core :as rf]
    [goog.events :as events]
    [goog.history.EventType :as HistoryEventType]
    [markdown.core :refer [md->html]]
    [minuteman.ajax :as ajax]
    [minuteman.events]
    [reitit.core :as reitit]
    [reitit.frontend.easy :as rfe]
    [clojure.string :as string]
    [re-com.core :refer [single-dropdown v-box h-box p label button input-text modal-panel title]]
    [re-com.util :refer [item-for-id]])
  (:import goog.History))

(defn nav-link [uri title page]
  [:a.navbar-item
   {:href   uri
    :class (when (= page @(rf/subscribe [:common/page-id])) :is-active)}
   title])

(defn navbar []
  (r/with-let [expanded? (r/atom false)]
              [:nav.navbar.is-info>div.container
               [:div.navbar-brand
                [:a.navbar-item {:href "/" :style {:font-weight :bold}} "minuteman"]
                [:span.navbar-burger.burger
                 {:data-target :nav-menu
                  :on-click #(swap! expanded? not)
                  :class (when @expanded? :is-active)}
                 [:span][:span][:span]]]
               [:div#nav-menu.navbar-menu
                {:class (when @expanded? :is-active)}
                [:div.navbar-start
                 [nav-link "#/" "Home" :home]
                 [nav-link "#/about" "About" :about]]]]))

(defn about-page []
  [:section.section>div.container>div.content
   [:img {:src "/img/warning_clojure.png"}]])

(defn create-es-instance-form [form-data on-submit on-cancel]
  [v-box
   :padding "10px"
   :gap "10px"
   :children [[title :label "Connect to Elasticsearch Instance"]
              [v-box
               :class "form-group"
               :children [[:label {:for "new-esi-name"} "Display name"]
                          [input-text
                           :model (:name @form-data)
                           :on-change #(swap! form-data assoc :name %)
                           :placeholder "Choose a name"
                           :class "form-control"
                           :attr {:id "new-esi-name"}]]]
              [v-box
               :class "form-group"
               :children [[:label {:for "new-esi-url"} "URL"]
                          [input-text
                           :model (:url @form-data)
                           :on-change #(swap! form-data assoc :url %)
                           :placeholder "https://example.com:9200"
                           :class "form-control"
                           :attr {:id "new-esi-url"}]]]
              [h-box
               :gap "13px"
               :children [[button
                           :label "Submit"
                           :class "btn-primary"
                           :on-click on-submit]
                          [button
                           :label "Cancel"
                           :on-click on-cancel]]]]])

(defn create-es-instance-modal [show?]
  (let [form-data (r/atom nil)
        submit-form #(rf/dispatch [:create-es-instance @form-data])
        on-submit (fn [_] (reset! show? false) (submit-form) (reset! form-data nil) false)
        on-cancel (fn [_] (reset! show? false) (reset! form-data nil))]
    (when @show? [modal-panel
                  :backdrop-color "grey"
                  :backdrop-opacity 0.4
                  :child [create-es-instance-form form-data on-submit on-cancel]])))

(defn select-environment []
  (let [selected-env-id (r/atom nil)
        es-instances (rf/subscribe [:es-instances])
        show-create-dialog? (r/atom false)]
    (fn []
      [h-box
       :gap "10px"
       :children [[single-dropdown
                   :style {:max-width 300}
                   :model selected-env-id
                   :placeholder "Select an Elasticsearch Environment"
                   :on-change #(reset! selected-env-id %)
                   :choices (or @es-instances [])]
                  [button
                   :label "Add New"
                   :class "btn-primary"
                   :on-click #(reset! show-create-dialog? true)]
                  [create-es-instance-modal show-create-dialog?]]])))

(defn home-page []
  [:section.section>div.container>div.content
   [select-environment]])

(defn page []
  (if-let [page @(rf/subscribe [:common/page])]
    [:div
     [navbar]
     [page]]))

(defn navigate! [match _]
  (rf/dispatch [:common/navigate match]))

(def router
  (reitit/router
    [["/" {:name        :home
           :view        #'home-page
           :controllers [{:start (fn [_] (rf/dispatch [:page/init-home]))}]}]
     ["/about" {:name :about
                :view #'about-page}]]))

(defn start-router! []
  (rfe/start!
    router
    navigate!
    {}))

;; -------------------------
;; Initialize app
(defn ^:dev/after-load mount-components []
  (rf/clear-subscription-cache!)
  (rdom/render [#'page] (.getElementById js/document "app")))

(defn init! []
  (start-router!)
  (ajax/load-interceptors!)
  (mount-components))
