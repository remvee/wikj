(ns wikj.core
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.tools.logging :as log]
            [hiccup.page :refer [html5 include-css]]
            [ring.middleware.anti-forgery :refer [*anti-forgery-token*]]
            [ring.middleware.defaults :refer [site-defaults
                                              wrap-defaults]]
            [wikj.formatting :refer [url-decode wiki->html]])
  (:import (java.io FileInputStream FileOutputStream PushbackReader)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Views

(defn titlize [path]
  (str/capitalize (url-decode (subs path 1))))

(defn layout [title & body]
  (html5
   [:head
    [:title title]
    (include-css "/css/screen.css")]
   [:body
    [:h1 title]
    body]))

(defn render-show [path data versions]
  (layout
   (titlize path)
   [:div.content (wiki->html data)]
   [:div.actions
    [:a {:href "?edit=1"} "@"]
    [:ol.versions
     (map #(vec [:li [:a {:href (str "?version=" %)} %]])
          (reverse (take versions (iterate inc 0))))]]))

(defn render-edit [path data]
  (layout
   (titlize path)
   [:div.content (wiki->html data)]
   [:form.edit-page {:method "post"}
    [:input {:type "hidden" :name "__anti-forgery-token" :value *anti-forgery-token*}]
    [:textarea {:name "data"} data]
    [:button {:type "submit"} "@"]]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; State

(def backup-file "/tmp/wikj.sexp")

(defn backup! [data file]
  (-> (Thread.
       #(with-open [out (io/writer (FileOutputStream. file))]
          (binding [*out* out] (pr data))))
      .start))

(defn restore [file]
  (try
    (with-open [in (PushbackReader. (io/reader (FileInputStream. file)))]
      (binding [*in* in] (read)))
    (catch Exception e nil)))

(defonce pages (atom {}))

(defn restore-pages []
  (reset! pages (or (restore backup-file) {})))

(defn push-page [path data]
  (swap! pages
         update-in [path] #(conj (or %1 []) %2) data)
  (backup! @pages backup-file))

(defn get-page-versions [path]
  (@pages path))

(defn get-page
  ([path] (last (get-page-versions path)))
  ([path version] (nth (get-page-versions path) version)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Handlers

(defn ok-html [body]
  {:status 200
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body body})

(defn redirect-to [url]
  {:status 302
   :headers {"Location" url}})

(defn handle-edit [path]
  (ok-html
   (render-edit path (get-page path))))

(defn handle-show [path version]
  (let [data (if version
               (get-page path (Integer. version))
               (get-page path))]
    (ok-html
     (if data
       (render-show path data (count (get-page-versions path)))
       (render-edit path data)))))

(defn handle-create [path data]
  (push-page path data)
  (redirect-to path))

(defn handler [req]
  (let [{:keys [request-method uri params]} req]
    (case request-method
      :get (cond
            (= "/" uri)    (redirect-to "/HomePage")
            (:edit params) (handle-edit uri)
            :else          (handle-show uri (:version params)))
      :post (handle-create uri (:data params)))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Wiring

(defn bootstrap! []
  (restore-pages))

(defn wrap-exception
  [app]
  (fn [req]
    (try (app req)
         (catch Throwable t
           (log/error t (.getMessage t))
           {:status 500
            :headers {"Content-Type" "text/plain"}
            :body "auch.."}))))

(def app
  (-> handler
      (wrap-defaults site-defaults)
      wrap-exception))
