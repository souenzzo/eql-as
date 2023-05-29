(ns br.com.souenzzo.eql-as.ast
  (:require [edn-query-language.core :as eql]))

(defn as-query
  [{::keys [as-map as-key] :as opts}]
  {:type     :root,
   :children (for [[k v] as-map
                   :let [ref? (vector? v)]]
               (cond-> (cond
                         ref? (let [ast (as-query (assoc opts
                                                    ::as-map (last v)))]
                                (assoc ast
                                  :type :join
                                  :dispatch-key k
                                  :key k
                                  :query (eql/ast->query ast)))
                         :else {:type         :prop
                                :dispatch-key k
                                :key          k})
                       as-key (assoc :params {as-key (if ref?
                                                       (first v)
                                                       v)})))})

(defn ident-query
  [{::keys [as-map as-key] :as opts}]
  {:type     :root,
   :children (for [[k v] as-map
                   :let [ref? (vector? v)]]
               (cond-> (cond
                         ref? (let [ast (ident-query (assoc opts
                                                       ::as-map (last v)))]
                                (assoc ast
                                  :type :join
                                  :dispatch-key (first v)
                                  :key (first v)
                                  :query (eql/ast->query ast)))
                         :else {:type         :prop
                                :dispatch-key v
                                :key          v})
                       as-key (assoc :params {as-key k})))})
