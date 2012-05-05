(ns org.hurdy.flickr.core)

(import java.net.URL)
(import javax.imageio.ImageIO)
(import java.awt.image.BufferedImage)

(require '[clj-http.client :as client])

(use '[clojure.string :only (join)])
(use '[clojure.xml :only (parse)])
(use '[clojure.java.io :only [as-file]])

(def flickr-key "1c46840d769cbf7e2281680ea58a45ed")

(def flickr-consumer-secret "2cd273980ccde5db")

(def flickr-user-id "67155975@N00")

(def flickr-rest-api "http://api.flickr.com/services/rest/?")

(defn create-param-strings [& params]
  ; accept a variable number of parameters in assumed key/value/key/value order
  ; turn the list of parameters into a map, deconstruct each key/value pair one at a time
  ; in a for loop then turn the pair into an '=' separated String and collect into
  ; a list (via the for) and return the list
  (for [[k v] (apply hash-map params)]
    (str k "=" v))
  )

(defn complete-request-params [method-name & request-params]
  ; accept a method name and a variable number of parameters.
  ; create the basic set of parameters then create the extra
  ; parameters, turn the results into one list and return the
  ; list sorted (by key effectively)
  (sort
    (concat
      (create-param-strings "method" method-name,
        "format" "rest",
        "api_key" flickr-key,
        "user_id" flickr-user-id)
      (apply create-param-strings request-params)))
  )

(defn create-url [method & request-params]
  ; create the url to call by joining the base flickr rest
  ; url with the processed parameters
  (str
    flickr-rest-api
    (join "&" (apply complete-request-params method request-params)))
  )

(defn match-tag [element expected-tag]
  ; if the :tag of the specified element matches
  ; the expectedTag the return the :attrs of the element
  ; otherwise return nil
  (if (= expected-tag (:tag element)) (:attrs element))
  )

(defn flickr-people-get-public-photos [page number-of-photos-per-page]
  ; use the xml parse function to call the flickr.people.getPublicPhotos method
  ; getting the specified page given the number of photos per page required
  (parse
    (create-url "flickr.people.getPublicPhotos" "per_page" number-of-photos-per-page "page" page))
  )

(defn construct-photo-source-url [photo-attrs size]
  ; creates the actual photo URL given the photo element attributes as
  ; retrieved from the Flickr API.
  ; use let to extract the named keys from the supplied photo attributes map
  ; and use them as local variables
  (let [{:keys [farm server id secret]} photo-attrs]
    (str
      "http://farm"
      farm
      ".staticflickr.com/"
      server
      "/"
      id
      "_"
      secret
      "_"
      size
      ".jpg")
    )
  )

(defn get-page-count [total-pages photos-per-page]
  (Math/ceil (/ total-pages photos-per-page))
  )

(defn get-matching-tags [tag flickr-xml-seq]
  (filter identity (map #(match-tag % tag) flickr-xml-seq))
  )

(defn get-public-photo-source-urls
  ([number-of-photos-per-page size]
    (get-public-photo-source-urls 1 number-of-photos-per-page size))
  ([page number-of-photos-per-page size]
    (println (str "Getting " number-of-photos-per-page " photos from page " page))
    (for [x (map #(construct-photo-source-url % size)
              (filter identity
                (map #(match-tag % :photo )
                  (xml-seq (flickr-people-get-public-photos page number-of-photos-per-page)))))]
      x))
  )

(defn spawn-agents [agents range-of-pages]
  (if (empty? range-of-pages)
     agents
     (recur (conj agents (agent (first range-of-pages)))
        (rest range-of-pages))))

(defn getR [rgb]
  (Math/abs (bit-and (bit-shift-right rgb 16) 0xFF))
  )

(defn getG [rgb]
  (Math/abs (bit-and (bit-shift-right rgb 8) 0xFF))
  )

(defn getB [rgb]
  (Math/abs (bit-and rgb 0xFF))
  )

(defn calculate-avg-rgb-value [num-of-pixels totalR totalG totalB]
  {:r (int (/ totalR num-of-pixels)),
   :g (int (/ totalG num-of-pixels)),
   :b (int (/ totalB num-of-pixels))}
  )

(defn get-pixel-rgb-values [img width height imgtype]
  (let [simg (java.awt.image.BufferedImage. width height imgtype)
        xRange (range 0 width 1)
        yRange (range 0 height 1)
        g (.createGraphics simg)]
      (.drawImage g img 0 0 width height nil)
      (for [y yRange
            x xRange]
            (let [rgb (.getRGB simg x y)
                  red (getR rgb)
                  green (getG rgb)
                  blue (getB rgb)]
              {:r red, :g green, :b blue}
            )
      )
    )
)

(defn get-avg-rgb-value [url]
  (let [img (javax.imageio.ImageIO/read (URL. url))
        imgtype (java.awt.image.BufferedImage/TYPE_INT_ARGB)
        width (.getWidth img)
        height (.getHeight img)
        totals (get-avg3 img width height imgtype)
        {:keys [r g b]} (apply merge-with + (get-pixel-rgb-values url))
        pixels (* width height)]
      (calculate-avg-rgb-value pixels r g b)
    )
  )

(defn get-all-public-photo-source-urls [size]
  ; Get a sample page to determine the number of pages given a max size of 500 photos per-page
  (let
    [max-photos-per-page 500
    flickr-xml-seq (xml-seq (flickr-people-get-public-photos 1 1))
    total-pages (:pages (first (get-matching-tags :photos flickr-xml-seq)))
    pages (get-page-count (Integer/parseInt total-pages) max-photos-per-page)
    page-range (range 1 (+ 1 pages) 1)
    agents (spawn-agents [] page-range)
    urls (ref [])]
      ; send each agent a job to do
      (println (str "Agent count = " (count agents)))
      (doseq [agent agents]
        (send agent #(get-public-photo-source-urls % max-photos-per-page size)))
      ; ensure successful dispatch
      (apply await agents)
      ; view the results
      (doseq [agent agents]
        #_(println (str "Agent" agent " got " (count @agent) " photos"))
        (dosync
          (ref-set urls (flatten (conj @urls @agent))))
        )
      ; clean up although not right now as this is running in the repl
      #_(shutdown-agents)
      @urls
    )
  )

(defn count-all-public-photo-source-urls []
  (println (str "Got " (count (get-all-public-photo-source-urls "s")) " photos"))
  )