(ns org.hurdy.flickr.core)

(require '[clj-http.client :as client])

(use '[clojure.string :only (join)])
(use '[clojure.xml :only (parse)])

(def flickr-key "1c46840d769cbf7e2281680ea58a45ed")

(def flickr-consumer-secret "2cd273980ccde5db")

(def flickr-user-id "67155975@N00")

(def flickr-rest-api "http://api.flickr.com/services/rest/?")

(defn create-param-string [keyvalues]
  (for [[k v] keyvalues] (str k "=" v))
  )

(defn complete-request-params [method request-params-map]
  (sort (concat (create-param-string {"method" method "format" "rest" "api_key" flickr-key "user_id" flickr-user-id}) (create-param-string request-params-map)))
  )

(defn create-url [method request-params]
  (str flickr-rest-api (clojure.string/join "&" (complete-request-params method request-params)))
  )

(defn get-public-photos [numberOfPhotos]
  (let [uri (create-url "flickr.people.getPublicPhotos" {"per_page" numberOfPhotos})]
    (println uri)
    (parse uri)
    )
  )