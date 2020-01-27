# eql-as

Utility functions to create [EQL](http://edn-query-language.org) queries with "renaming" capabilities

## How? 

This library do not implement renaming. It helps you to create queries that used with EQL parsers like 
[pathtom](https://github.com/wilkerlucio/pathom) will result in a renaming

## Usage

Let's start with a sample data, like

```json
{"name": "Alex",
 "address": {"street": "Atlantic"}}
```

Then we create a `as-map`, that specify how you want to "qualify" your data

```clojure
(def user-as-map
  {:name    :user/name
   :address [:user/address {:street :address/street}]}) 
```

Now we can create a query that [pathtom](https://github.com/wilkerlucio/pathom) will know who to qualify your data

```clojure
;; (require '[br.com.souenzzo.eql-as.alpha :as eql-as]
;;          '[com.wsscode.pathom.connect :as pc]
;;          '[com.wsscode.pathom.core :as p])

(->> {::eql-as/as-map user-as-map
      ::eql-as/as-key :pathom/as}
     eql-as/as-query
     (p/map-select {:name "Alex"
                    :address {:street "Atlantic"}}))
;; => {:user/name "Alex", :user/address {:address/street "Atlantic"}}
```

We can also do the oposite operation, run our parser asking for the data

```clojure
;; (require '[br.com.souenzzo.eql-as.alpha :as eql-as]
;;          '[com.wsscode.pathom.connect :as pc]
;;          '[com.wsscode.pathom.core :as p])

(let [parser (p/parser {::p/plugins [(pc/connect-plugin {::pc/register [(pc/constantly-resolver :user/name "Alex")
                                                                        (pc/constantly-resolver :user/address {})
                                                                        (pc/constantly-resolver :address/street "Atlantic")]})]
                        ::p/env     {::p/reader [p/map-reader
                                                 pc/reader2]}})]
  (->> {::eql-as/as-map user-as-map
        ::eql-as/as-key :pathom/as}
       eql-as/ident-query
       (parser {})))
;; => {:name "Alex", :address {:street "Atlantic"}}
```

## Datomic

You can use it with datomic, using [eql-datomic]("https://github.com/souenzzo/eql-datomic")

```clojure
;; (require '[br.com.souenzzo.eql-as.alpha :as eql-as]
;;          '[br.com.souenzzo.eql-datomic :as eqld]
;;          '[datomic.api :as d])
(let [pattern (->> {::eql-as/as-map user-as-map
                    ::eql-as/as-key :pathom/as}
                   eql-as/ident-query
                   eql/query->ast
                   eqld/ast->query)]
  (d/pull db pattern user-id))
;; => => {:name "Alex", :address {:street "Atlantic"}}
```
