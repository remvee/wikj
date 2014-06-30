(ns user
  (:require [clojure.tools.namespace.repl :refer [refresh]]
            [ring.adapter.jetty :refer [run-jetty]]
            [wikj.core :as wikj]))

(defonce server-instance (atom nil))

(defn start-server [& {:keys [host port]}]
  (when @server-instance
    (throw (Exception. "already started")))

  (reset! server-instance
          (run-jetty #'wikj/app {:host (or host "localhost")
                                 :port (or port 8080)
                                 :join? false})))

(defn stop-server []
  (when @server-instance
    (.stop @server-instance)
    (reset! server-instance nil)))
