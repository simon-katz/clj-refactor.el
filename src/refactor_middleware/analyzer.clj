(ns refactor-middleware.analyzer
  (:refer-clojure :exclude [macroexpand-1 read read-string])
  (:require [clojure.tools.analyzer :as ana]
            [clojure.tools.analyzer.ast :refer :all]
            [clojure.tools.analyzer.utils :refer [resolve-var]]
            [clojure.tools.nrepl.misc :refer [response-for]]
            [clojure.tools.nrepl.transport :as transport]
            [clojure.tools.nrepl.middleware :refer [set-descriptor!]]
            [clojure.tools.reader :as r])
  (:import java.io.PushbackReader))

(defn desugar-host-expr [[op & expr :as form]]
  (if (symbol? op)
    (let [opname (name op)]
      (cond
       (= (first opname) \.) ; (.foo bar ..)
       (let [[target & args] expr
             args (list* (symbol (subs opname 1)) args)]
         (with-meta (list '. target (if (= 1 (count args)) ;; we don't know if (.foo bar) ia
                                      (first args) args)) ;; a method call or a field access
           (meta form)))
       (= (last opname) \.) ;; (class. ..)
       (with-meta (list* 'new (symbol (subs opname 0 (dec (count opname)))) expr)
         (meta form))
       :else form))
    form))

(defn macroexpand-1 [form env]
  (if (seq? form)
    (let [op (first form)]
      (if (ana/specials op)
        form
        (let [v (resolve-var op env)]
          (if (and (not (-> env :locals (get op))) ;; locals cannot be macros
                   (:macro (meta v)))
            (apply v form env (rest form)) ; (m &form &env & args)
            (desugar-host-expr form)))))
        form))

(def e (ana/empty-env))

(defmacro ast [form]
  `(binding [ana/macroexpand-1 macroexpand-1
             ana/create-var    ~(fn [sym env]
                                  (doto (intern (:ns env) sym)
                                    (reset-meta! (meta sym))))
             ana/parse         ana/-parse
             ana/var?          ~var?]
     (ana/analyze '~form e)))

(defmacro string-ast [string]
  `(binding [ana/macroexpand-1 macroexpand-1
             ana/create-var    ~(fn [sym env]
                                  (doto (intern (:ns env) sym)
                                    (reset-meta! (meta sym))))
             ana/parse         ana/-parse
             ana/var?          ~var?]
     (ana/analyze (r/read-string ~string) e)))

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
