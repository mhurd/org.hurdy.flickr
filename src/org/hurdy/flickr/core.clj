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

(defn flickr-people-getPublicPhotos [page numberOfPhotosPerPage]
  (parse (create-url "flickr.people.getPublicPhotos" {"per_page" numberOfPhotosPerPage "page" page}))
  )

(defn match-tag [element expectedTag]
  (if (= expectedTag (:tag element)) (:attrs element))
  )

(defn constructPhotoSourceURL [photo-attrs size]
  (str "http://farm" (:farm photo-attrs) ".staticflickr.com/" (:server photo-attrs) "/" (:id photo-attrs) "_" (:secret photo-attrs) "_" size ".jpg")
  )

(defn getPublicPhotoSourceURLs
  "Accepted sizes for photos are of the set [mstzb], medium, small, thumbnail, larger and even larger"
  ([numberOfPhotosPerPage size]
    (getPublicPhotoSourceURLs numberOfPhotosPerPage 1 size))
  ([numberOfPhotosPerPage page size]
    (println (str "Getting page " page))
    (for [x
        (map #(constructPhotoSourceURL % size)
          (filter identity (map #(match-tag % :photo) (xml-seq (flickr-people-getPublicPhotos page numberOfPhotosPerPage)))))] x))
  )

(defn getPages [photosAttrs]
  (Math/ceil (/ (Integer/parseInt (:pages photosAttrs)) 500))
  )

(defn getAllPublicPhotoSourceURLs
  "Accepted sizes for photos are of the set [mstzb], medium, small, thumbnail, larger and even larger"
  [size]
    ; Get a sample page to determine the number of pages given a max size of 500 photos per-page
    (let [pages (map #(getPages %)
      (filter identity (map #(match-tag % :photos) (xml-seq (flickr-people-getPublicPhotos 1 1)))))]
      ; count through and make each of the calls
      (for [x (range 1 (+ 1 (first pages)) 1)]
        (getPublicPhotoSourceURLs 500 x size))
    )
  )

(defn printAllSquarePublicPhotoSourceURLs []
    (for [x (filter identity (flatten (getAllPublicPhotoSourceURLs "s")))]
      (println x))
  )