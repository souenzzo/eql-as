(ns br.com.souenzzo.eql-as.specs
  (:require [clojure.spec.alpha :as s]
            [br.com.souenzzo.eql-as :as eql-as]))

(s/def ::eql-as/ident
  (s/or :key any?
        :ref (s/tuple any? ::eql-as/as-map)))

(s/def ::eql-as/as-map
  (s/coll-of (s/tuple any? ::eql-as/ident)))

(s/def ::eql-as/as-key any?)

(s/fdef eql-as/as-query
        :args (s/cat :opts (s/keys :req [::eql-as/as-map]
                                   :opt [::eql-as/as-key]))
        :ret :edn-query-language.ast/query)


(s/fdef eql-as/ident-query
        :args (s/cat :opts (s/keys :req [::eql-as/as-map]
                                   :opt [::eql-as/as-key]))
        :ret :edn-query-language.ast/query)

(s/fdef eql-as/reverse
        :args (s/cat :as-map ::as-map)
        :ret ::as-map)
