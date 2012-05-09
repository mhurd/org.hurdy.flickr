(ns org.hurdy.flickr.scratch)

(defn calculate-triplets [product]
  (set (for [x (range 1 product)
        y (range 1 product)
        z (range 1 product)
     :when (= product (* x y z))]
    (sort (list x y z))))
  )