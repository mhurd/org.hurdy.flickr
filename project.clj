(defproject org.hurdy.flickr "1.0.0-SNAPSHOT"
  :description "Clojure Flickr"
  :dependencies [
                  [org.clojure/clojure "1.4.0"]
                  [clj-http "0.4.1-SNAPSHOT"]
                  [criterium "0.2.1-SNAPSHOT"]
                  [ring "1.1.0-beta3"]
                  [enlive "1.0.0-SNAPSHOT"]
                  [compojure "1.1.0-SNAPSHOT"]
                ]
  :plugins  [
              [lein-marginalia "0.7.0"]
              [lein-ring "0.7.0-SNAPSHOT"]
            ]
  :main org.hurdy.flickr.webserver
)