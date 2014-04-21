(ns user
  (:refer-clojure :exclude [macroexpand-1 read read-string])
  (:require [clojure.java.io :as io]
            [clojure.tools.analyzer :as ana]
            [clojure.tools.analyzer.utils :refer [resolve-var]]
            [clojure.tools.analyzer.ast :refer :all]
            [clojure.tools.reader :as r])
  (:import java.io.PushbackReader))

;; will I need this?
;;  perhaps just get the ns as string from the emacs buffer
(defn read-all
  [input]
  (let [eof (Object.)]
    (take-while #(not= % eof) (repeatedly #(read input false eof)))))

(defn read-all-file [file] (-> file io/reader (PushbackReader.) read-all))

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

(defmacro file-ast [file]
  `(binding [ana/macroexpand-1 macroexpand-1
             ana/create-var    ~(fn [sym env]
                                  (doto (intern (:ns env) sym)
                                    (reset-meta! (meta sym))))
             ana/parse         ana/-parse
             ana/var?          ~var?]
     (ana/analyze (read-all-file ~file) e)))

(defn find-referred [ns-body referred]
  (some #(= (symbol referred) (:class %)) (nodes (string-ast ns-body))))
