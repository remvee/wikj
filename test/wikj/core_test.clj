(ns wikj.core-test
  (:use clojure.test
        wikj.core)
  (:import [java.util Date]))

(defn request
  ([method uri] (request method uri nil))
  ([method uri params] {:request-method method, :uri uri, :params params}))

(defn str-contains? [val part]
  (not (= -1 (.indexOf val part))))

(deftest test-handler
  (testing "main route"
    (let [{status :status {location "Location"} :headers}
          (handler (request :get "/"))]
      (is (= 302 status))
      (is (= "/wikj" location))))

  (testing "new page route"
    (let [{status :status {location "Location"} :headers}
          (handler (request :get "/new+page"))]
      (is (= status 303))
      (is (= "/new+page?edit=1" location))))

  (let [path "/existing+page"
        page {:tstamp (Date.), :data "test content"}]

    (testing "existing page"
      (let [{:keys [status body]}
            (handler (assoc (request :get path) :pages {path [page]}))]
        (is (= 200 status))
        (is (str-contains? body (:data page)))))

    (testing "edit page"
      (let [{:keys [status body]}
            (handler (assoc (request :get path {:edit "1"}) :pages {path [page]}))]
        (is (= 200 status))
        (is (re-seq #"<form [^>]*edit-page" body))
        (is (str-contains? body (:data page))))))

  (testing "create a page"
    (let [path "/new+page", data "test content"
          {status :status {location "Location"} :headers pages :pages}
          (handler (request :post path {:data data}))]
      (is (= 303 status))
      (is (= path location))
      (is (not (nil? pages)))
      (is (= data (:data (last (get pages path))))))))
