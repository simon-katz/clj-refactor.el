(defproject clj-refactor "0.12.0"
  :description "To initate a helper cider session with analyzer support for ASTs"
  :profiles {:dev {:source-paths ["dev"]
                   :dependencies [[org.clojure/tools.analyzer "0.1.0-beta10"]
                                  [org.clojure/tools.analyzer.jvm "0.1.0-beta10"]
                                  [org.clojure/tools.reader "0.8.4"]]}})
