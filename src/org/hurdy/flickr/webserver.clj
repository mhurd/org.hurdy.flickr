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

(def images (atom '()))
(def thumbs (atom '()))

(defn get-image [index]
  (nth @images index)
  )

(defn get-rgb [index]
  (flickr/get-avg-rgb-value-as-css (nth @thumbs index))
  )

(deftemplate index-page "index-template.html" [random-index]
  [:div#example-img :img] (set-attr :src (get-image random-index))
  [:div#example-img-container] (set-attr :style (str "background-color:" (get-rgb random-index) ";"))
  )

(deftemplate lost-page "404-template.html" [])

(defroutes myroutes
  (GET "/" [] (index-page (rand-int 100)))
  (route/not-found (lost-page))
 )

(def app (->
           #'myroutes
           (wrap-file "public")))

(defn start-flickr-server []
  (let [random-index (rand-int 100)
        thumb-uris (flickr/get-public-photo-source-urls 1 100 "t")
        image-uris (flickr/get-public-photo-source-urls 1 100 "z")]
    (swap! images into image-uris)
    (swap! thumbs into thumb-uris)
    (def test-server (run-jetty app {:port 8080 :join? false})))
  )

(defn stop-flickr-server []
  (.stop test-server))
