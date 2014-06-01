(ns refactor-middleware.test-util
  (:require [refactor-middleware.analyzer :refer :all]))

(defn test-ast [ns-body]
  (string-ast (format "(%s)" ns-body)))
