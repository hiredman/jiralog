(ns com.thelastcitadel.query
  (:require [clj-http.client :as http]
            [cheshire.core :as json]
            [clojure.java.io :as io]
            [carica.core :refer [config]]))

(defonce
  ^{:doc "last 10 query strings"}
  queries (agent ()))

(defn sections [start end size]
  (lazy-seq
   (let [se (+ start size)]
     (cond
      (> se end)
      [[start end]]
      (= se end)
      []
      :else
      (cons [start se] (sections (inc se) end size))))))

(defn query* [query-string & {:keys [fields start-at max-results]}]
  (let [result (http/get (config :jiralog/url)
                         (merge {:query-params (merge {:jql query-string
                                                       :maxResults max-results}
                                                      (when fields
                                                        {:fields fields}))
                                 :insecure? true
                                 :as :stream}
                                (when (config :jiralog/username)
                                  {:basic-auth [(config :jiralog/username)
                                                (config :jiralog/password)]})))]
    (with-open [s (:body result)
                r (io/reader s)]
      (json/decode-stream r true))))

(defn query [query-string & {:keys [fields start-at max-results]
                             :or {start-at 0
                                  max-results 500}}]
  (send-off queries (fn [qs] (cons query-string (take 9 qs))))
  (let [result (query* query-string
                       :start-at start-at
                       :max-results max-results
                       :fields fields)]
    (if (or (> start-at 0)
            (> (:maxResults result) (:total result)))
      (:issues result)
      (cons (:issues result)
            (for [[start end] (sections (inc (count (:issues result)))
                                        (:total result)
                                        max-results)
                  result (:issues (query* query-string
                                          :fields fields
                                          :start-at start
                                          :max-results max-results))]
              result)))))

(alter-var-root #'query* (fn [orig]
                           (fn [& args]
                             (alter-meta! #'query* update-in [:call-count] (fnil inc 0))
                             (apply orig args))))
