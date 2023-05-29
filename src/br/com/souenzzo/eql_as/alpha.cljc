(ns br.com.souenzzo.eql-as.alpha
  (:refer-clojure :exclude [reverse])
  (:require [edn-query-language.core :as eql]
            [br.com.souenzzo.eql-as.ast :as ast]
            [clojure.spec.alpha :as s]))

(defn as-query
  [{::keys [as-map as-key]}]
  (-> (ast/as-query {::ast/as-map as-map
                     ::ast/as-key as-key})
      (eql/ast->query)))

(defn ident-query
  [{::keys [as-map as-key]}]
  (-> (ast/ident-query {::ast/as-map as-map
                        ::ast/as-key as-key})
      (eql/ast->query)))

(defn reverse
  [as-map]
  (into (empty as-map)
        (map (fn [[k v]]
               (if (vector? v)
                 [(first v) [k (reverse (last v))]]
                 [v k])))
        as-map))
