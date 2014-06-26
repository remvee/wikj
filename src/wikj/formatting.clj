(ns wikj.formatting
  (:require [hiccup.util :refer [escape-html]]))

(defn replace [data pattern f]
  (let [pattern (re-pattern (str "(?s)(?:(" pattern ")|.+?)"))]
    (apply str
           (map (fn [[s m]] (if m (f m) s))
                (re-seq pattern data)))))

(defn wiki->html-newlines [data]
  (-> data
      (replace #"\n\r?\n\r?" (constantly "<p>"))
      (replace #"\n\r?" (constantly "<br>"))))

(defn wiki->html-links [data]
  (replace data #"(?:[A-Z][a-z]+){2,}" #(str "<a href='" % "'>" % "</a>")))

(defn wiki->html [data]
  (when data
    (-> (escape-html data)
        wiki->html-newlines
        wiki->html-links)))
