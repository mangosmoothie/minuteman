(ns datagen
  "for filling an elasticsearch index with sample data"
  (:require
   [clj-http.client :as http]
   [clojure.spec.alpha :as s]
   [clojure.spec.gen.alpha :as gen]
   [clojure.string]))

(defn gen-uuid-str []
  (str (java.util.UUID/randomUUID)))

(def non-blank-str? (fn [s] (not (clojure.string/blank? s))))

(def str-with-gen
  (s/with-gen string?
    #(gen/such-that non-blank-str?
                    (gen/string-alphanumeric))))

(def email-regex #"^\S+@\S+\.\S+$")
(s/def ::email-formatter (s/and string? #(re-matches email-regex %)))

(s/def ::id (s/with-gen string?
              #(gen/fmap str
                         (s/gen uuid?))))
(s/def ::first-name str-with-gen)
(s/def ::last-name  str-with-gen)
(s/def ::email (s/with-gen ::email-formatter
                 #(gen/fmap (fn [[user host tld]] (str user "@" host "." tld))
                            (gen/tuple (gen/such-that non-blank-str? (gen/string-alphanumeric))
                                       (gen/such-that non-blank-str? (gen/string-alphanumeric))
                                       (gen/such-that non-blank-str? (gen/string-alphanumeric))))))
(s/def ::phone int?)

(s/def ::person (s/keys :req [::id ::first-name ::last-name ::email]
                        :opt [::phone]))

(def local "http://localhost:9200")
(def customers "customers")
(def employees "employees")

(defn generate-data [size]
  (gen/sample (s/gen ::person) size))

(defn fill-sample-data [url index size throttle?]
  (future
    (doseq [data (generate-data size)]
      (when throttle? (Thread/sleep 500))
      (http/post (clojure.string/join "/" [url index "_doc"])
                 {:form-params data :content-type :json}))))

(defn drop-index [url index]
  (http/delete (str url "/" index)))
