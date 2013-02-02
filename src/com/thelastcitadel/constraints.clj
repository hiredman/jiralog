(ns com.thelastcitadel.constraints
  (:require [clojure.core.logic :as logic]))

;; from core.logic with the addition of the ability to hang metadata
;; on the constraint

(defn make-constraint [rator pred & args]
  (reify
    clojure.lang.IFn
    (invoke [this a]
      (when (apply pred (map #(logic/walk* a %) args))
        ((logic/remcg this) a)))
    logic/IConstraintOp
    (rator [_]
      rator)
    (rands [_]
      (filter logic/lvar? (flatten args)))
    logic/IReifiableConstraint
    (reifyc [_ _ r a]
      (list* rator (map #(logic/-reify r %) args)))
    logic/IRunnable
    (runnable? [_ s]
      (logic/ground-term? args s))
    logic/IConstraintWatchedStores
    (watched-stores [_]
      #{:clojure.core.logic/subst})))

(defmacro defc [name args & body]
  (let [-name (symbol (str "-" name))
        [md body] (if (map? (first body))
                    [(first body) (rest body)]
                    [{} body])]
    `(defn ~name ~args
       (logic/cgoal
        (with-meta
          (make-constraint '~name (fn ~args ~@body) ~@args)
          (assoc ~md ::args ~args))))))

(defc string-containsc [x y]
  {::operation "~"}
  (and (string? x)
       (.contains x y)))
