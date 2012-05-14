(ns org.hurdy.flickr.webserver
  (:use compojure.core
        ring.adapter.jetty
        ring.middleware.resource
        ring.middleware.reload
        ring.util.response
        ring.middleware.file
        ring.middleware.params
        net.cgrand.enlive-html)
  (:require [compojure.route :as route]
            [org.hurdy.flickr.core :as flickr]))

(def photo-range 500)

(def base-uris (atom '()))

(defn get-image [index]
  (flickr/get-medium500-image-url (nth @base-uris index))
  )

(defn get-complimentary-rgb [index]
  (flickr/as-css-rgb-value (flickr/convert-to-complimentary-rgb-value (flickr/get-avg-rgb-value (flickr/get-thumbnail-image-url (nth @base-uris index)))))
  )

(defn get-average-rgb [index]
  (flickr/as-css-rgb-value (flickr/get-avg-rgb-value (flickr/get-thumbnail-image-url (nth @base-uris index))))
  )

(deftemplate index-page "index-template.html" [random-index]
  [:div#example-img :img] (set-attr :src (get-image random-index))
  [:div#example-img-container] (set-attr :style (str "background-color:" (get-average-rgb random-index) ";"))
  [:div#example-img-text :h2] (content (str "random image " random-index " bordered with the image's average RGB colour"))
  [:div#example-img-text :h2] (set-attr :style (str "color:" (get-complimentary-rgb random-index) ";"))
  )

(deftemplate index-page-complimentary "index-template.html" [random-index]
  [:div#example-img :img] (set-attr :src (get-image random-index))
  [:div#example-img-container] (set-attr :style (str "background-color:" (get-complimentary-rgb random-index) ";"))
  [:div#example-img-text :h2] (content (str "random image " random-index " bordered with the image's compilmentary average RGB colour"))
  [:div#example-img-text :h2] (set-attr :style (str "color:" (get-average-rgb random-index) ";"))
  )

(deftemplate lost-page "404-template.html" [])

(defroutes myroutes
  (GET "/" [] (index-page (rand-int photo-range)))
  (GET "/complimentary" [] (index-page-complimentary (rand-int photo-range)))
  (route/not-found (lost-page))
 )

(def app (->
           #'myroutes
           (wrap-file "public")))

(defn start-flickr-server []
  (let [uris (flickr/get-public-photo-source-base-urls 1 photo-range)]
    (swap! base-uris into uris)
    (def test-server (run-jetty app {:port 8080 :join? false})))
  )

(defn stop-flickr-server []
  (.stop test-server))
