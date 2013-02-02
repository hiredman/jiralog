(ns com.thelastcitadel.querystring
  (:require [clojure.core.logic :as logic]
            [clojure.string :as str])
  (:import (java.util UUID)))

(defonce rfeaturec
  (delay
   (require 'com.thelastcitadel.jiralog)
   @(resolve 'com.thelastcitadel.jiralog/rfeaturec)))

(def searchers (atom {}))

(defn register [fun]
  (let [id (UUID/randomUUID)]
    (swap! searchers assoc id fun)
    id))

(defn unregister [id]
  (swap! searchers dissoc id)
  true)


;; key
(register (fn [a m]
            (let [key (get m :key)]
              (when (and key (not (logic/lvar? (logic/walk a key))))
                [(str "key = " (pr-str (logic/walk a key)))]))))

(register (fn [a m]
            (for [[field-name v] (:fields m)
                  v (if (map? v)
                      (vals v)
                      (if (sequential? v)
                        v
                        [v]))
                  :when (and v (not (logic/lvar? (logic/walk a v))))]
              (str (name field-name) " = " (pr-str (logic/walk a v))))))

(register (fn [a m]
            (for [[k v] (:fields m)
                  v (if (logic/lvar? v)
                      [v]
                      (when (map? v)
                        (filter logic/lvar? (vals v))))
                  cid (get (.km (.cs a)) v)
                  :let [m (meta (get (.cm (.cs a)) cid))]]
              (str (name k)
                   " "
                   (:com.thelastcitadel.constraints/operation m)
                   " "
                   (pr-str (second (:com.thelastcitadel.constraints/args m)))))))

(defn build-query-string [a m]
  (str/join " AND " (mapcat (fn [fun] (fun a m)) (vals @searchers))))
