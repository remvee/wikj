(ns wikj.core
  (:require [clojure.string :as str]
            [clojure.tools.logging :as log]
            [environ.core :refer [env]]
            [hiccup.page :refer [html5 include-css]]
            [hiccup.util :refer [escape-html]]
            [ring.middleware.defaults :refer [wrap-defaults]]
            [wikj.formatting :as f])
  (:import [java.io FileNotFoundException]
           [java.util Date]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Views

(defn titlize [path]
  (str/capitalize (f/url-decode (subs path 1))))

(defn layout [title & body]
  (html5
   [:head
    [:title title]
    (include-css "/css/screen.css")]
   [:body
    [:h1 title]
    body]))

(defn render-show [path {:keys [data tstamp] :as page} versions]
  (layout
   (titlize path)
   [:div.content (f/wiki->html data)]
   [:footer
    [:div.meta
     [:div.tstamp (f/htmlize tstamp)]]
    [:div.actions
     [:a {:href "?edit=1#edit"} "@"]
     [:ol.versions
      (reverse (map (fn [p i]
                      [:li
                       [:a (conj
                            {:title (f/htmlize (:tstamp p))}
                            (if-not (= page p)
                              {:href (str "?version=" i)}))
                        i]])
                    versions (iterate inc 0)))]]]))

(defn render-edit [path {:keys [data tstamp]}]
  (layout
   (titlize path)
   [:div.content (f/wiki->html data)]
   [:div.edit {:id "edit"}
    [:form.edit-page {:method "post"}
     [:textarea {:name "data", :placeholder f/doc} data]
     [:button {:type "submit"} "@"]]]
   [:footer
    [:div.meta
     [:div.tstamp (f/htmlize tstamp)]]]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Handlers

(defn ok-html [body]
  {:status 200
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body body})

(defn found [url]
  {:status 302
   :headers {"Location" url}})

(defn see-other [url]
  {:status 303
   :headers {"Location" url}})

(defn handle-edit [pages uri]
  (ok-html
   (render-edit uri (last (get pages uri)))))

(defn handle-show [pages uri version]
  (let [versions (get pages uri)
        page (if version
               (nth versions (Integer. version))
               (last versions))]
    (if page
      (ok-html (render-show uri page versions))
      (see-other (str uri "?edit=1")))))

(defn handle-create [pages uri data]
  (assoc (see-other uri)
    :pages (update-in pages [uri]
                      #(conj (or %1 []) {:tstamp %2, :data %3})
                      (Date.) data)))

(defn handler [req]
  (let [{:keys [request-method uri params pages]} req]
    (case request-method
      :get (cond
            (= "/" uri)    (found (str "/" (f/url-encode (:site-title req))))
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

(defn wrap-site-title
  [app title]
  (fn [req] (app (assoc req :site-title title))))

(def site-defaults
  {:params    {:keywordize true
               :urlencoded true}
   :responses {:absolute-redirects     true
               :content-types          true
               :not-modified-responses true}
   :static    {:resources "public"}})

(def app
  (-> handler
      (wrap-site-title (env :wikj-title "wikj"))
      (wrap-pages (env :wikj-backup-file))
      (wrap-defaults site-defaults)
      wrap-exception))
