(ns wikj.core
  (:require [clojure.string :as str]
            [clojure.tools.logging :as log]
            [hiccup.page :refer [html5 include-css]]
            [ring.middleware.anti-forgery :refer [*anti-forgery-token*]]
            [ring.middleware.defaults :refer [site-defaults
                                              wrap-defaults]]
            [wikj.formatting :refer [htmlize url-decode wiki->html]])
  (:import [java.io FileNotFoundException]
           [java.util Date]))


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

(defn render-show [path {:keys [data tstamp]} versions]
  (layout
   (titlize path)
   [:div.content (wiki->html data)]
   [:div.meta
    [:div.tstamp (htmlize tstamp)]]
   [:div.actions
    [:a {:href "?edit=1"} "@"]
    [:ol.versions
     (map #(vec [:li [:a {:href (str "?version=" %)} %]])
          (reverse (take versions (iterate inc 0))))]]))

(defn render-edit [path {:keys [data tstamp]}]
  (layout
   (titlize path)
   [:div.content (wiki->html data)]
   [:form.edit-page {:method "post"}
    [:input {:type "hidden" :name "__anti-forgery-token" :value *anti-forgery-token*}]
    [:textarea {:name "data"} data]
    [:button {:type "submit"} "@"]]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Handlers

(defn ok-html [body]
  {:status 200
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body body})

(defn redirect-to [url]
  {:status 302
   :headers {"Location" url}})

(defn handle-edit [pages uri]
  (ok-html
   (render-edit uri (last (get pages uri)))))

(defn handle-show [pages uri version]
  (let [versions (get pages uri)
        page (if version
               (nth versions (Integer. version))
               (last versions))]
    (ok-html
     (if page
       (render-show uri page (count versions))
       (render-edit uri page)))))

(defn handle-create [pages uri data]
  (assoc (redirect-to uri)
    :pages (update-in pages [uri]
                      #(conj (or %1 []) {:tstamp %2, :data %3})
                      (Date.) data)))

(defn handler [req]
  (let [{:keys [request-method uri params pages]} req]
    (case request-method
      :get (cond
            (= "/" uri)    (redirect-to "/HomePage")
            (:edit params) (handle-edit pages uri)
            :else          (handle-show pages uri (:version params)))
      :post (handle-create pages uri (:data params)))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Wiring

(defn wrap-exception
  [app]
  (fn [req]
    (try (app req)
         (catch Throwable t
           (log/error t (.getMessage t))
           {:status 500
            :headers {"Content-Type" "text/plain"}
            :body "auch.."}))))

(defn wrap-pages
  [app & [file]]
  (letfn [(backup! [data]
            (when file (future (spit file (pr-str data)))))
          (restore []
            (when file (try (read-string (slurp file))
                            (catch FileNotFoundException _ nil))))]
    (let [pages (atom (restore))]
      (fn [req]
        (let [res (app (assoc req :pages @pages))]
          (when-let [updated-pages (:pages res)]
            (reset! pages updated-pages)
            (backup! @pages))
          res)))))

(def app
  (-> handler
      (wrap-pages "/tmp/wikj.sexp")
      (wrap-defaults site-defaults)
      wrap-exception))
