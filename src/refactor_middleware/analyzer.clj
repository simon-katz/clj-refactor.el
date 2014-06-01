(ns refactor-middleware.analyzer
  (:refer-clojure :exclude [macroexpand-1 read read-string])
  (:require [clojure.tools.analyzer :as ana]
            [clojure.tools.analyzer.passes.source-info :as ana.si]
            [clojure.tools.analyzer.jvm :as ana.jvm]
            [clojure.tools.analyzer.ast :refer :all]
            [clojure.tools.analyzer.utils :refer [get-line get-col source-info -source-info]]
            [clojure.tools.nrepl.misc :refer [response-for]]
            [clojure.tools.nrepl.transport :as transport]
            [clojure.tools.nrepl.middleware :refer [set-descriptor!]]
            [clojure.tools.reader :as r]
            [clojure.tools.reader.reader-types :as rts])
  (:import java.io.PushbackReader))

(def e (ana.jvm/empty-env))

(defmacro ast [form]
  `(binding [ana/macroexpand-1 ana.jvm/macroexpand-1
             ana/create-var    ana.jvm/create-var
             ana/parse         ana.jvm/parse
             ana/var?          ~var?]
     (ana/analyze '~form e)))

(defn -full-source-info
  "Returns the source-info of x"
  [x env]
  (merge
   (source-info env)
   (when-let [file (or (-> x meta :file)
                       (and (not= *file* "NO_SOURCE_FILE")
                            *file*))]
     {:file file})
   (when-let [line (get-line x env)]
     {:line line})
   (when-let [column (get-col x env)]
     {:column column})
   (when-let [end-line (-> x meta :end-line)]
     {:end-line end-line})
   (when-let [end-column (-> x meta :end-column)]
     {:end-column end-column})))

;; monkey patching source info to add end-line and end-column info
;; will worth a pull a request to tools.analyzer
(intern 'clojure.tools.analyzer.utils '-source-info -full-source-info)

(defmacro string-ast [string]
  `(binding [ana/macroexpand-1 ana.jvm/macroexpand-1
             ana/create-var    ana.jvm/create-var
             ana/parse         ana.jvm/parse
             ana/var?          ~var?]
     (-> ~string
         rts/indexing-push-back-reader
         (r/read nil (reify))
         (ana/analyze e)
         (prewalk ana.si/source-info))))

(defn analyze [{:keys [transport ns-string] :as msg}]
  (transport/send transport (response-for msg :ast (string-ast ns-string)))
  (transport/send transport (response-for msg :status :done)))

(defn wrap-analyze
  "Middleware that builds AST for given ns string"
  [handler]
  (fn [{:keys [op orig-op ns-string] :as msg}]
    (cond (and (= "analyze" op) (not orig-op))
          (analyze msg)
          (and (= "analyze" op) orig-op)
          (handler (assoc msg
                     :op orig-op
                     :ast (string-ast ns-string)))
          :else (handler msg))))

(set-descriptor!
 #'wrap-analyze
 {:handles
  {"analyze"
   {:doc "Analyzes the ns-string using clojure.tools.analyzer"
    :requires {"ns-string" "the body of the namespace to build the AST with"
               "orig-op" "original operation delegating to analyzer"}
    :returns {"status" "done"
              "ast" "the AST built for the ns-string"}}}})
