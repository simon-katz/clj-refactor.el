(ns refactor-middleware.refactor
  (:require [refactor-middleware.analyzer :refer [wrap-analyze]]
            [clojure.tools.analyzer.ast :refer :all]
            [clojure.tools.nrepl.middleware :refer [set-descriptor!]]
            [clojure.tools.nrepl.misc :refer [response-for]]
            [clojure.tools.nrepl.transport :as transport]))

(defn- find-referred [ast referred]
  (some #(= (symbol referred) (:class %)) (nodes ast)))

(defn- find-referred-reply [{:keys [transport ast referred] :as msg}]
  (let [result (find-referred ast referred)]
    (transport/send transport (response-for msg :value (when result (str result))))
    (transport/send transport (response-for msg :status :done))))

(defn wrap-refactor
  "Ensures that refactor only triggered with the right operation and forks to the appropriate refactor function"
  [handler]
  (fn [{:keys [op ast refactor-fn] :as msg}]
    (if (= "ast-refactor" op)
      (cond (= "find-referred" refactor-fn) (find-referred-reply msg)
            :else
            (handler msg))
      (handler msg))))

(defn wrap-prepare-analyze
  "Reroutes the msg to the analyze middleware to have the AST created for the refactor function"
  [handler]
  (fn [{:keys [op ns-string] :as msg}]
    (if (= "refactor" op)
      (handler (assoc msg
                 :op "analyze"
                 :orig-op "ast-refactor"))
      (handler msg))))

(set-descriptor!
 #'wrap-refactor
 {:requires #{"analyze" "refactor-find-referred"}
  :handles
  {"ast-refactor"
   {:doc "Returns a boolean depending on if the referred found in the AST"
    :requires {"ast" "AST representing clojure code to search the referred in"
               "refactor-fn" "The refactor function to invoke"}
    :returns {"status" "done"
              "value" "result of the refactor"}}}})

(set-descriptor!
 #'wrap-prepare-analyze
 {:expects #{"analyze"}
  :handles
  {"refactor"
   {:returns {"status" "done"}}}})
