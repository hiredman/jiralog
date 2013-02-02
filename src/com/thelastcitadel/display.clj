(ns com.thelastcitadel.display
  (:require [clojure.pprint :refer [print-table]]))

(defn g []
  (->> (for [c (conj (supers clojure.lang.ISeq)
                     clojure.lang.ISeq)
             m (.getMethods c)
             :let [argc (count (.getParameterTypes m))
                   args (repeatedly argc gensym)]]
         {c [`(~(symbol (.getName m)) [~'this ~@args] (. ~'item (~(symbol (.getName m)) ~@args)))]})
       (apply merge-with into)
       (mapcat (fn [[c ms]] (cons c (seq ms))))
       (cons '[item])
       (cons 'deftype)))

(declare table)

(deftype TableSeq [item]
  clojure.lang.IPersistentCollection
  clojure.lang.Seqable
  clojure.lang.ISeq
  (seq [this]
    (when-let [s (. item (seq))]
      (table s)))
  (next [this]
    (when-let [s (. item (next))]
      (table s)))
  (more [this]
    (when-let [s (. item (more))]
      (table s)))
  (first [this] (. item (first)))
  (count [this] (. item (count)))
  (cons [this G__80716]
    (table (. item (cons G__80716))))
  (empty [this]
    (table (. item (empty))))
  (equiv [this G__80717] (. item (equiv G__80717))))

(defmethod print-method TableSeq [o ^java.io.Writer w]
  (binding [*out* w]
    (print-table o)))

(defn table [s]
  (TableSeq. s))
