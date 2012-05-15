(ns org.hurdy.flickr.test.core
  (:use [org.hurdy.flickr.core])
  (:use [clojure.test]))

(require '[clj-http.client :as client])

(deftest test-create-param-strings
  (is
    (=
      (create-param-strings "name" "mike" "age" 10)
      '("name=mike", "age=10")))
  )

(deftest test-complete-request-params
  (is
    (=
      (complete-request-params "my-method" "foo" "bar")
      '("api_key=1c46840d769cbf7e2281680ea58a45ed" "foo=bar" "format=rest" "method=my-method" "user_id=67155975@N00")))
  )

(deftest test-create-url
  (is
    (=
      (create-url "my-method" "foo" "bar" "bar" "foo")
      "http://api.flickr.com/services/rest/?api_key=1c46840d769cbf7e2281680ea58a45ed&bar=foo&foo=bar&format=rest&method=my-method&user_id=67155975@N00"))
  )

(deftest test-get-attributes-of-elements
  (is
    (=
      '()
      (get-attributes-of-elements :attrs :bar '({:tag :foo, :attrs "test"}))))
  (is
    (=
      '("test1" "test2")
      (get-attributes-of-elements :attrs :foo '({:tag :foo, :attrs "test1"},{:tag :foo, :attrs "test2"}))))
  )

(deftest test-construct-photo-source-url
  (is
    (=
      (construct-photo-source-base-url {:farm "f1" :server "s1" :id "i1" :secret "s2"})
      "http://farmf1.staticflickr.com/s1/i1_s2"))
  )

(deftest test-get-page-count
  (is
    (=
      (get-page-count 500 50)
      10.0))
  )