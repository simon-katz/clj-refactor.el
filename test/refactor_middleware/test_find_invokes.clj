(ns refactor-middleware.test-find-invokes
  (:require [refactor-middleware.test-util :as u]
            [refactor-middleware.refactor :refer :all]
            [clojure.test :refer :all]))

(def test-ns-string
"(ns secret-santa.core
  (:require [clojure.set :refer [difference]]
            [clojure.string :refer [trim]]))

(defn- rnd-pair-up [participants]
  (pr participants)
  (let [receivers (shuffle participants)]
    (partition 2 (interleave participants receivers))))

(defn pair-up [participants]
  (println
   \"pa\"
   \"rtic\"
   \"ipants\"
   participants)
  (trim \" fooobar \")
  (println
   \"just trimmed ' fooobar '\")
  (let [pairs (rnd-pair-up participants)]
    (println
     \"pa\"
     \"irs\"
     pairs)
    (if (some (fn [p] (= (first p) (second p))) pairs)
      (pair-up participants)
      pairs)))")

(def find-invokes #'refactor-middleware.refactor/find-invokes)

(def test-ast (u/test-ast test-ns-string))

(deftest finds-core-debug-fns
  (let [result (find-invokes test-ast "#'clojure.core/println,#'clojure.core/pr,#'clojure.core/prn")]
    (println result)

    (is (= 4 (count result)) (format "4 core debug fn was expected but %d found" (count result)))

    (is (= [6 11 17 20] (map first result))  "line numbers don't match")

    (is (= ["#'clojure.core/pr" "#'clojure.core/println" "#'clojure.core/println" "#'clojure.core/println"] (map last result)) "found fn names don't match")

    (is (= [3 3 3 5] (map #(nth % 2) result)) "column numbers don't match")

    (is (= [6 15 18 23] (map second result)) "end line numbers don't match")))
