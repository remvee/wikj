(ns wikj.formatting
  (:refer-clojure :exclude [replace])
  (:require [clojure.string :as str]
            [hiccup.util :refer [escape-html]])
  (:import [java.text SimpleDateFormat]
           [java.util Date]))

(def ^:dynamic *encoding* "UTF-8")

(defn url-encode [str]
  (java.net.URLEncoder/encode ^String str *encoding*))

(defn url-decode [str]
  (java.net.URLDecoder/decode ^String str *encoding*))

(defn- replace [data pattern f]
  (let [pattern (re-pattern (str "(?s)(?:(" pattern ")|.+?)"))]
    (apply str
           (map (fn [[s m]] (if m (f m) s))
                (re-seq pattern data)))))

(defn wiki->html-newlines
  "Single new becomes a <br> and a double <p>"
  [data]
  (-> data
      (replace #"\n\r?(?:\n\r?)+" (constantly "<p>"))
      (replace #"\n\r?" (constantly "<br>"))))

(defn wiki->html-links
  "Links like orgmode; [[link][description]] or alternatively [[link]]"
  [data]
  (replace data
           #"\[\[.*?\]\]"
           #(let [expr (str/split (subs % 2 (- (count %) 2)) #"\]\[")
                  link (first expr)
                  text (last expr)]
              (str "<a href='" (url-encode link) "'>" text  "</a>"))))

(defn wiki->html
  "Format a wiki syntax into HTML"
  [data]
  (when data
    (-> (escape-html data)
        wiki->html-newlines
        wiki->html-links)))

(defmulti htmlize "HTMLize value" class)

(defmethod htmlize Date
  [val]
  (escape-html
   (. (SimpleDateFormat. "YYYY/MM/dd @ HH:mm:ss")
      (format val))))

(defmethod htmlize :default
  [val]
  (escape-html (str val)))
