(ns com.thelastcitadel.jiralog
  (:require [clojure.core.logic :as logic]
            [clojure.string :as str]
            [com.thelastcitadel.display :refer [table]]
            [com.thelastcitadel.constraints :as c]
            [com.thelastcitadel.query :refer [query]]
            [com.thelastcitadel.querystring :refer [build-query-string]])
  (:import (java.util UUID)))

(defn rfeaturec [m f]
  (let [new-f (reduce (fn [m [k v]] (assoc m k (logic/lvar (name k)))) {} (seq f))]
    (logic/fresh []
      (logic/featurec m new-f)
      (logic/everyg
       (fn [[k lvar]]
         (let [v (get f k)]
           (if (map? v)
             (logic/fresh []
               (logic/featurec m {k lvar})
               (rfeaturec lvar v))
             (logic/== lvar v))))
       new-f))))

(defn jira
  "tabling saves my bacon"
  ([m] (fn [a] ((jira (build-query-string a m) m) a)))
  ([query-string m]
     ((logic/tabled [query-string m]
        (fn [a]
          (logic/to-stream
            (map #(trampoline (rfeaturec % m) a)
                 (query query-string
                        :fields (str/join \, (map name (keys (:fields m)))))))))
      query-string
      m)))
