(ns wikj.core
  (:require [hiccup.page :refer [html5 include-css]]
            [ring.middleware.anti-forgery :refer [*anti-forgery-token*]]
            [ring.middleware.defaults :refer [site-defaults
                                              wrap-defaults]]
            [wikj.formatting :refer [wiki->html]]))

(def pages (atom {}))

(defn page-add [path data]
  (swap! pages
         update-in [path] #(conj (or %1 []) %2) data))

(defn page-get [path]
  (last (@pages path)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Views

(defn titlize [path]
  (subs path 1))

(defn layout [title & body]
  (html5
   [:head
    [:title title]
    (include-css "/css/screen.css")]
   [:body
    [:h1 title]
    body]))

(defn existing-page [path data]
  (layout (titlize path)
          [:div.content (wiki->html data)]
          [:a {:href "?edit=1"} "@"]))

(defn edit-page [path data]
  (layout (titlize path)
          [:div.content (wiki->html data)]
          [:form.edit-page {:method "post"}
           [:input {:type "hidden" :name "__anti-forgery-token" :value *anti-forgery-token*}]
           [:textarea {:name "data"} data]
           [:button {:type "submit"} "@"]]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Actions

(defn edit [path]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (edit-page path (page-get path))})

(defn show [path]
  (let [data (page-get path)
        html (if data
               (existing-page path data)
               (edit-page path data))]
    {:status 200
     :headers {"Content-Type" "text/html"}
     :body html}))

(defn create [path data]
  (page-add path data)
  {:status 302
   :headers {"Location" path}})


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Wirering

(defn handler [req]
  (case (:request-method req)
    :get (cond
          (= "/" (:uri req))
          {:status 302
           :headers {"Location" "/HomePage"}}
          
          (:edit (:params req))
          (edit (:uri req))

          :else
          (show (:uri req)))
    
    :post (create (:uri req) (:data (:params req)))))

(def app
  (wrap-defaults handler site-defaults))
