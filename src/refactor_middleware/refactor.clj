(ns refactor-middleware.refactor
  (:require [refactor-middleware.analyzer :refer [wrap-analyze]]
            [clojure.tools.analyzer.ast :refer :all]
            [clojure.tools.nrepl.middleware :refer [set-descriptor!]]
            [clojure.tools.nrepl.misc :refer [response-for]]
            [clojure.tools.nrepl.transport :as transport]))

(defn find-referred [ast referred]
  (some #(= (symbol referred) (:class %)) (nodes ast)))

(defn find-referred-reply [{:keys [transport ast referred] :as msg}]
  (let [result (find-referred ast referred)]
    (transport/send transport (response-for msg :value (when result (str result))))
    (transport/send transport (response-for msg :status :done))))

(defn wrap-find-referred
  "Checks if given referred symbol is used or not."
  [handler]
  (fn [{:keys [op ast referred] :as msg}]
    (if (= "ast-refactor-find-referred" op)
          (find-referred-reply msg)
          (handler msg))))

(defn wrap-prepare-analyze
  ""
  [handler]
  (fn [{:keys [op ns-string] :as msg}]
    (if (= "refactor-find-referred" op)
      (handler (assoc msg
                 :op "analyze"
                 :orig-op "ast-refactor-find-referred"))
      (handler msg))))

(set-descriptor!
 #'wrap-find-referred
 {:requires #{"analyze" "refactor-find-referred"}
  :handles
  {"ast-refactor-find-referred"
   {:doc "Returns a boolean depending on if the referred found in the AST"
    :requires {"ast" "AST representing clojure code to search the referred in"
               "referred" "The referred symbol to look for"}
    :returns {"status" "done"
              "value" "true if referred found nil otherwise"}}}})

(set-descriptor!
 #'wrap-prepare-analyze
 {:expects #{"analyze"}
  :handles
  {"refactor-find-referred"
   {:returns {"status" "done"}}}})
