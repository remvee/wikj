(defproject wikj "0.1.0-SNAPSHOT"
  :description "A tiny wiki"
  :url "http://example.com/FIXME"

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/tools.logging "0.3.0"]
                 [hiccup "1.0.5"]
                 [ring/ring-defaults "0.1.0"]
                 [ring-server "0.3.1"]]

  :plugins [[lein-ring "0.8.10"]]

  :ring {:handler wikj.core/app}

  :profiles {:dev {:source-paths ["dev"]
                   :dependencies [[org.clojure/tools.namespace "0.2.3"]]}
             :uberjar {:aot [wikj.core]
                       :main wikj.core}})
