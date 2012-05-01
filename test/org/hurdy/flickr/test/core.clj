(ns org.hurdy.flickr.test.core
  (:use [org.hurdy.flickr.core])
  (:use [clojure.test]))

(require '[clj-http.client :as client])

(deftest test-create-param-string
  (is (= (create-param-string {"foo" "bar"}) '("foo=bar")))
  )

(deftest test-complete-request-params
  (is (= (complete-request-params "my-method" {"foo" "bar"}) '("api_key=1c46840d769cbf7e2281680ea58a45ed" "foo=bar" "format=rest" "method=my-method" "user_id=67155975@N00")))
  )

(deftest test-create-url
  (is (= (create-url "my-method" {"foo" "bar" "bar" "foo"})
        "http://api.flickr.com/services/rest/?api_key=1c46840d769cbf7e2281680ea58a45ed&bar=foo&foo=bar&format=rest&method=my-method&user_id=67155975@N00"))
  )

(deftest test-call
  (is (= (get-public-photos 10) "foobar"))
  )