(ns wikj.formatting
  (:refer-clojure :exclude [replace])
  (:require [clojure.string :as str]
            [hiccup.util :refer [escape-html]]))

(defn- replace [data pattern f]
  (let [pattern (re-pattern (str "(?s)(?:(" pattern ")|.+?)"))]
    (apply str
           (map (fn [[s m]] (if m (f m) s))
                (re-seq pattern data)))))

(defn wiki->html-newlines [data]
  (-> data
      (replace #"\n\r?\n\r?" (constantly "<p>"))
      (replace #"\n\r?" (constantly "<br>"))))

(defn decamelize [s]
  (str/join " " (map str/lower-case (re-seq #"[A-Z][a-z]+" s))))

(defn wiki->html-links [data]
  (replace data
           #"(?:[A-Z][a-z]+){2,}"
           #(str "<a href='" % "'>" (decamelize %) "</a>")))

(defn wiki->html [data]
  (when data
    (-> (escape-html data)
        wiki->html-newlines
        wiki->html-links)))
