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

(defn existing-page [path data versions]
  (layout (titlize path)
          [:div.content (wiki->html data)]
          [:div.actions
           [:a {:href "?edit=1"} "@"]
           [:ol.versions
            (map #(vec [:li [:a {:href (str "?version=" %)} %]])
                 (reverse (take versions (iterate inc 0))))]]))

(defn edit-page [path data]
  (layout (titlize path)
          [:div.content (wiki->html data)]
          [:form.edit-page {:method "post"}
           [:input {:type "hidden" :name "__anti-forgery-token" :value *anti-forgery-token*}]
           [:textarea {:name "data"} data]
           [:button {:type "submit"} "@"]]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Data

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

(defn page-add [path data]
  (swap! pages
         update-in [path] #(conj (or %1 []) %2) data)
  (backup! @pages backup-file))

(defn page-get-versions [path]
  (@pages path))

(defn page-get [path & version]
  (let [version (and (first version) (Integer. (first version)))]
    (if version
      (nth (page-get-versions path) version)
      (last (page-get-versions path)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Actions

(defn ok-html [body]
  {:status 200
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body body})

(defn redirect-to [url]
  {:status 302
   :headers {"Location" url}})

(defn edit [path]
  (ok-html (edit-page path (page-get path))))

(defn show [path version]
  (let [data (page-get path version)
        body (if data
               (existing-page path data (count (page-get-versions path)))
               (edit-page path data))]
    (ok-html body)))

(defn create [path data]
  (page-add path data)
  (redirect-to path))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Wirering

(defn handler [req]
  (case (:request-method req)
    :get (cond
          (= "/" (:uri req))
          (redirect-to "/HomePage")

          (:edit (:params req))
          (edit (:uri req))

          :else
          (show (:uri req) (:version (:params req))))

    :post (create (:uri req) (:data (:params req)))))

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
