(ns org.hurdy.flickr.core)

(import java.net.URL)
(import javax.imageio.ImageIO)
(import java.awt.image.BufferedImage)

(require '[clj-http.client :as client])

(use '[clojure.string :only (join)])
(use '[clojure.xml :only (parse)])
(use '[clojure.java.io :only [as-file]])

(def
  ^{:doc "Your flickr API key passed with calls - replace this with yours"}
  flickr-key "1c46840d769cbf7e2281680ea58a45ed")

(def
  ^{:doc "The Flickr ID to use - replace this with yours"}
  flickr-user-id "67155975@N00")

(def
  ^{:doc "Base Flickr REST API URL"}
  flickr-rest-api "http://api.flickr.com/services/rest/?")

(defn create-param-strings
  "Accept a variable number of parameters in assumed key/value/key/value order
  turn the list of parameters into a map, deconstruct each key/value pair one at a time
  in a for loop then turn the pair into an '=' separated String and collect into
  a list (via the for) and return the list"
  [& params]
    (for [[k v] (apply hash-map params)]
      (str k "=" v))
  )

(defn complete-request-params
  "Accept a method name and a variable number of parameters.
  create the basic set of parameters then create the extra
  parameters, turn the results into one list and return the
  list sorted (by key effectively)"
  [method-name & request-params]
    (sort
      (concat
        (create-param-strings "method" method-name,
          "format" "rest",
          "api_key" flickr-key,
          "user_id" flickr-user-id)
        (apply create-param-strings request-params)))
  )

(defn create-url
  "Create the url to call by joining the base flickr rest
  url with the processed parameters"
  [method & request-params]
    (str
      flickr-rest-api
      (join "&" (apply complete-request-params method request-params)))
  )

(defn flickr-people-get-public-photos
  "Use the xml parse function to call the flickr.people.getPublicPhotos method
  getting the specified page given the number of photos per page required"
  [page number-of-photos-per-page]
    (parse
      (create-url "flickr.people.getPublicPhotos" "per_page" number-of-photos-per-page "page" page))
  )

(defn construct-photo-source-base-url
  "Creates the actual base photo URL (excluding the size and .jpg) given
  the photo element attributes as retrieved from the Flickr API.
  use let to extract the named keys from the supplied photo attributes map
  and use them as local variables"
  [photo-attrs]
    (let [{:keys [farm server id secret]} photo-attrs]
      (str
        "http://farm"
        farm
        ".staticflickr.com/"
        server
        "/"
        id
        "_"
        secret)
      )
  )

(defn get-page-count
  "Determine how many pages to load given the total number of
  photos and the desired number per page."
  [total-number-of-photos photos-per-page]
  (Math/ceil (/ total-number-of-photos photos-per-page))
  )

(defn get-attributes-of-elements
  "Get the attributes with the specified name from the element of the
  specified name"
  [attribute element xml-sequence]
    (for [x xml-sequence
            :when (= (:tag x) element)]
              (attribute x))
  )

(defn get-public-photo-source-base-urls
  "Gets the specified page of public photo source URLs with no size specified (the tail of the URL
  including the .jpg)"
  ([number-of-photos-per-page]
    (get-public-photo-source-base-urls 1 number-of-photos-per-page))
  ([page number-of-photos-per-page]
    (println (str "Getting " number-of-photos-per-page " photos from page " page))
    (map #(construct-photo-source-base-url %)
      (for [x (xml-seq (flickr-people-get-public-photos page number-of-photos-per-page))
              :when (= :photo (:tag x))]
              (:attrs x)))
  )
)

(defn spawn-agents
  "Spawns an agent for each of the pages"
  [agents range-of-pages]
  (if (empty? range-of-pages)
     agents
     (recur (conj agents (agent (first range-of-pages)))
        (rest range-of-pages))))

(defn getR
  "Extract the Red component from the RGB value."
  [rgb]
  (Math/abs (bit-and (bit-shift-right rgb 16) 0xFF))
  )

(defn getG
  "Extract the Green component from the RGB value."
  [rgb]
  (Math/abs (bit-and (bit-shift-right rgb 8) 0xFF))
  )

(defn getB
  "Extract the Blue component from the RGB value."
  [rgb]
  (Math/abs (bit-and rgb 0xFF))
  )

(defn calculate-avg-rgb-value
  "Calculates the average RGB value given the total
  Red, Blue and Green values and the size of the
  image in pixels."
  [number-of-pixels totalR totalG totalB]
  {:r (int (/ totalR number-of-pixels)),
   :g (int (/ totalG number-of-pixels)),
   :b (int (/ totalB number-of-pixels))}
  )

(defn convert-to-complimentary-rgb-value
  "Calculates the average RGB value given the total
  Red, Blue and Green values and the size of the
  image in pixels."
  [rgb]
  {:r (- 255 (:r rgb)),
   :g (- 255 (:g rgb)),
   :b (- 255 (:b rgb))}
  )

(defn get-rgb-data
  "Collects all of the RGB values for the pixels of the image in the
  form {:r X :g Y :b Z}"
  [img width height imgtype]
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

(defn strip-non-colors
  "Remove rgb values where R & G & B < 30 or r & G & B > 225
  mmh, how to do this on the frequency map that looks
  like {:r 187, :g 194, :b 213} 1, {:r 213, :g 175, :b 6} 1
  ... till' I work this out just return the map back again."
  [rgb-frequencies]
    rgb-frequencies
  )

(defn get-dominant-rgb-value
  "Downloads the image at the specified URL and then returns the
    dominant RGB value of the image in the form {:r X :g Y :b Z}"
  [url]
  (let [img (javax.imageio.ImageIO/read (URL. url))
        imgtype (java.awt.image.BufferedImage/TYPE_INT_ARGB)
        width (.getWidth img)
        height (.getHeight img)
        rgb-data (get-rgb-data img width height imgtype)]
      (first (strip-non-colors (sort-by val > (frequencies rgb-data))))
    )
  )

(defn get-avg-rgb-value
  "Downloads the image at the specified URL and then returns the
  average RGB value of the image in the form {:r X :g Y :b Z}"
  [url]
  (let [img (javax.imageio.ImageIO/read (URL. url))
        imgtype (java.awt.image.BufferedImage/TYPE_INT_ARGB)
        width (.getWidth img)
        height (.getHeight img)
        rgb-data (get-rgb-data img width height imgtype)
        {:keys [r g b]} (apply merge-with + rgb-data)
        number-of-pixels (* width height)]
      (calculate-avg-rgb-value number-of-pixels r g b)
    )
  )

(defn as-css-rgb-value
  "Converts {:r R :g G :b B} to a string = rgb(R,G,B)"
  [rgb]
    (str "rgb(" (:r rgb) "," (:g rgb) "," (:b rgb) ")")
  )

(defn get-small-square-image-url
  "small square 75x75"
  [base-url]
  (str base-url "_s.jpg")
  )

(defn get-large-square-image-url
  "large square 150x150"
  [base-url]
  (str base-url "_q.jpg")
  )

(defn get-thumbnail-image-url
    "thumbnail, 100 on longest side"
    [base-url]
    (str base-url "_t.jpg")
    )

(defn get-small240-image-url
  "small, 240 on longest side"
  [base-url]
  (str base-url "_m.jpg")
  )

(defn get-small320-image-url
  "small, 320 on longest side"
  [base-url]
  (str base-url "_n.jpg")
  )

(defn get-medium500-image-url
  "medium, 500 on longest side"
  [base-url]
  (str base-url ".jpg")
  )

(defn get-medium640-image-url
  "medium 640, 640 on longest side"
  [base-url]
  (str base-url "_z.jpg")
  )

(defn get-large-image-url
  "large, 1024 on longest side"
  [base-url]
  (str base-url "_b.jpg")
  )

(defn get-all-public-photo-source-base-urls
  "Get a sample page to determine the number of pages given a max size of 500 photos per-page."
  []
  (let
    [max-photos-per-page 500
    flickr-xml-seq (xml-seq (flickr-people-get-public-photos 1 1))
    total-number-of-photos (:pages (first (get-attributes-of-elements :attrs :photos flickr-xml-seq)))
    pages (get-page-count (Integer/parseInt total-number-of-photos) max-photos-per-page)
    page-range (range 1 (inc pages) 1)
    agents (spawn-agents [] page-range)
    urls (ref [])]
      ; send each agent a job to do
      (println (str "Agent count = " (count agents)))
      (doseq [agent agents]
        (send agent #(get-public-photo-source-base-urls % max-photos-per-page)))
      ; ensure successful dispatch
      (apply await agents)
      ; view the results
      (doseq [agent agents]
        #_(println (str "Agent" agent " got " (count @agent) " photos"))
        (dosync
          ; use ref-set as no other threads are modifying this var
          (ref-set urls (flatten (conj @urls @agent))))
        )
      ; clean up although marked as ignore as this is running in the repl for now
      #_(shutdown-agents)
      @urls
    )
  )

(defn count-all-public-photo-source-urls []
  (println (str "Got " (count (get-all-public-photo-source-base-urls)) " photos"))
  )