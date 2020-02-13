# eql-as

Utility functions to create [EQL](http://edn-query-language.org) queries with "renaming" capabilities

## How? 

This library do not implement renaming. It helps you to create queries that used with EQL parsers like 
[pathom](https://github.com/wilkerlucio/pathom) will result in a renaming

### Quick example:

Let's say that you have a unqualified map:

```clojure
{:name    "Alex"
 :address [{:street "Atlantic"}]}
```

You can create a EQL Query describing the qualify process, like this
```clojure
[(:name {:pathom/as :user/name})
 {(:address {:pathom/as :user/address}) [(:street {:pathom/as :address/street})]}]
```

In [pathom](https://github.com/wilkerlucio/pathom) case, it use `:pathom/as` as `alias` keyword.

Once you run this query in this data, it will be qualified

```clojure
;; (require '[com.wsscode.pathom.core :as p])
(p/map-select
  {:name    "Alex"
   :address [{:street "Atlantic"}]}
 `[(:name {:pathom/as :user/name})
   {(:address {:pathom/as :user/address}) [(:street {:pathom/as :address/street})]}])
;; => {:user/name "eql-as"
;;     :user/address [{:address/street "Atlantic"}]
```

We now have a "free" map-qualifier. `eql-as` will help you to create this kind of query.

## Usage

Add to your `deps.edn`
```clojure
br.com.souenzzo/eql-as {:git/url "https://github.com/souenzzo/eql-as.git"
                        :sha     "e59e457c77603384276d67ed446c2d1cbc8cab85"}
```

Let's start with a sample data, like

```json
{"name": "Alex",
 "address": {"street": "Atlantic"}}
```

Then we create a `as-map`, that specify how you want to "qualify" your data.

Here we say `{:the-final-name-that-i-want :the-name-on-original-data}`

```clojure
(def user-as-map
  {:user/name    :name
   :user/address [:address {:address/street :street}]}) 
```

### Qualify a map

Now we can create a query that [pathtom](https://github.com/wilkerlucio/pathom) will know who to qualify your data

```clojure
;; (require '[br.com.souenzzo.eql-as.alpha :as eql-as]
;;          '[com.wsscode.pathom.connect :as pc]
;;          '[com.wsscode.pathom.core :as p])

(->> {::eql-as/as-map user-as-map
      ::eql-as/as-key :pathom/as}
     eql-as/ident-query
     (p/map-select {:name "Alex"
                    :address {:street "Atlantic"}}))
;; => {:user/name "Alex", :user/address {:address/street "Atlantic"}}
```

### Request a query, with unqualified keys.

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
       eql-as/as-query
       (parser {})))
;; => {:name "Alex", :address {:street "Atlantic"}}
```

### Datomic

You can use it with datomic, using [eql-datomic](https://github.com/souenzzo/eql-datomic)

```clojure
;; (require '[br.com.souenzzo.eql-as.alpha :as eql-as]
;;          '[br.com.souenzzo.eql-datomic :as eqld]
;;          '[edn-query-language.core :as eql]
;;          '[datomic.api :as d])
(let [pattern (->> {::eql-as/as-map user-as-map
                    ::eql-as/as-key :as}
                   eql-as/as-query
                   eql/query->ast
                   eqld/ast->query)]
  (d/pull db pattern user-id))
;; => => {:name "Alex", :address {:street "Atlantic"}}
```


## Tips and tricks

### coercion

You can use this libs with [spec-coerce](https://github.com/wilkerlucio/spec-coerce) to get coercion

```clojure
;; (require '[br.com.souenzzo.eql-as.alpha :as eql-as]
;;          '[clojure.spec.alpha :as s]
;;          '[spec-coerce.core :as sc])

(s/def ::born inst?)

(let [data {:born-date "1993"}
      pattern (->> {::eql-as/as-map {::born :born-date}
                    ::eql-as/as-key :pathom/as}
                   eql-as/ident-query)]
  (sc/coerce-structure (p/map-select data pattern)))
;; => {::born #inst"1993"}
```

### validation

You can use this libs with [spec](https://github.com/clojure/spec.alpha) to get validation (usually after coercion)

```clojure
;; (require '[br.com.souenzzo.eql-as.alpha :as eql-as]
;;          '[clojure.spec.alpha :as s]
;;          '[com.wsscode.pathom.core :as p]
;;          '[spec-coerce.core :as sc])

(s/def ::born inst?)

(let [data {:born-date "1993"}
      pattern (->> {::eql-as/as-map {::born :born-date}
                    ::eql-as/as-key :pathom/as}
                   eql-as/ident-query)]
  (s/valid? (s/keys :req [::born]) (sc/coerce-structure (p/map-select data pattern))))
;; => true
```
