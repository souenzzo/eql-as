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

<details>
  <summary>Click here if you can't understand this query</summary>

Without params, the query will look like this
```clojure
[:name
 {:address [:street]}]
```
this query says:
- From the map, select the key `:name`
- From the map, select the key `:address`
- From the map inside `:address`, select the key `:street`

Now we can add params to this query

```clojure
[(:name {})
 {:address [:street]}]
```
this query says:
- From the map, select the key `:name` with params `{}`
....

[pathom](https://github.com/wilkerlucio/pathom) know how to use some special params, like `:pathom/as`


With params, the query will look like this
```clojure
[(:name {:pathom/as :user/name})
 {(:address {:pathom/as :user/address}) [(:street {:pathom/as :address/street})]}]
```
this query says:
- From the map, select the key `:name` with params `{:pathom/as :user/name}`. Pathom will assoc `:name` as `:user/name` in the result
- From the map, select the key `:address` with params `{:pathom/as :user/address}`.  Pathom will assoc `:address` as `:user/address` in the result
- From the map inside `:address`, select the key `:street` with params  `{:pathom/as :address/street}`.  Pathom will assoc `:street` as `:address/street` in the result



</details>


In [pathom](https://github.com/wilkerlucio/pathom) case, it use `:pathom/as` as `alias` keyword.

Once you run this query in this data, it will be qualified

```clojure
;; (require '[com.wsscode.pathom.core :as p])
(p/map-select
  {:name    "Alex"
   :address [{:street "Atlantic"}]}
 `[(:name {:pathom/as :user/name})
   {(:address {:pathom/as :user/address}) [(:street {:pathom/as :address/street})]}])
;; => {:user/name "Alex"
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

Now we can create a query that [pathom](https://github.com/wilkerlucio/pathom) will know who to qualify your data

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


### placeholders 

Pathom has a [placeholder](https://wilkerlucio.github.io/pathom/v2/pathom/2.2.0/core/placeholders.html) concept and you can use it.
```clojure
;; (require '[br.com.souenzzo.eql-as.alpha :as eql-as]
;;          '[com.wsscode.pathom.connect :as pc]
;;          '[com.wsscode.pathom.core :as p])

(let [parser (p/parser {::p/plugins [(pc/connect-plugin {::pc/register [(pc/constantly-resolver :user/name "Alex")
                                                                        (pc/constantly-resolver :user/address {})
                                                                        (pc/constantly-resolver :address/street "Atlantic")]})]
                        ::p/env     {::p/reader [p/map-reader
                                                 pc/reader2
                                                 p/env-placeholder-reader]
                                     ::p/placeholder-prefixes #{">"}}})]
  (->> {::eql-as/as-map {:user/name    :name
                         :user/address [:address {:>/street [:street {:address/street :name}]}]}
        ::eql-as/as-key :pathom/as}
       eql-as/as-query
       (parser {})))
;; => {:name "Alex", :address {:street {:name "Atlantic"}}}
```

### Advanced coercion

Sometimes we need a "real" function to mae the "coercion". We can do it again with parsers and queries.

```clojure
;; (require '[br.com.souenzzo.eql-as.alpha :as eql-as]
;;          '[com.wsscode.pathom.connect :as pc]
;;          '[com.wsscode.pathom.core :as p])

(let [register [(pc/single-attr-resolver :user/roles-str :user/roles (partial mapv (partial keyword "user.roles")))]
      parser (p/parser {::p/plugins [(pc/connect-plugin {::pc/register register})]
                        ::p/env     {::p/reader [p/map-reader
                                                 pc/reader2]}})
      data {:id    "123"
            :roles ["admin"]}
      qualified (->> {::eql-as/as-map {:user/id        :id
                                       :user/roles-str :roles}
                      ::eql-as/as-key :pathom/as}
                     eql-as/ident-query
                     (p/map-select data))]
  (parser {::p/entity qualified}
          [:user/id
           :user/roles]))
;; => {:user/id "123", :user/roles [:user.roles/admin]}
```


## Real World exmaple

Let's implement a REST API, like [CreateUser](https://github.com/gothinkster/realworld/blob/master/api/swagger.json#L81)
from [RealWorld](https://github.com/gothinkster/realworld) spec

```clojure
;; (require '[br.com.souenzzo.eql-as.alpha :as eql-as]
;;          '[com.wsscode.pathom.core :as p])
(let [json-params {:user {:username "souenzzo"
                          :email    "souenzzo@souenzzo.com.br"
                          :password "*****"}}
      params (->> {::eql-as/as-map {:>/user [:user {:user/email    :email
                                                    :user/password :password
                                                    :user/slug     :username
                                                    :image         :user/image}]}
                   ::eql-as/as-key :pathom/as}
                  eql-as/ident-query
                  (p/map-select json-params))
      returning (-> {::eql-as/as-map {:user [:>/user {:email    :user/email
                                                      :token    :user/token
                                                      :username :user/slug
                                                      :bio      :user/bio
                                                      :image    :user/image}]}
                     ::eql-as/as-key :pathom/as}
                    eql-as/ident-query)
      query `[{(create-user ~(:>/user params))
               ~returning}]]
  query)
;; => [{(user/create-user {:user/email "souenzzo@souenzzo.com.br",
;;                         :user/password "*****",
;;                         :user/slug "souenzzo"})
;;       [({:>/user [(:user/email {:pathom/as :email})
;;                   (:user/token {:pathom/as :token})
;;                   (:user/slug  {:pathom/as :username})
;;                   (:user/bio   {:pathom/as :bio})
;;                   (:user/image {:pathom/as :image})]}
;;         {:pathom/as :user})]}]
```
This query you can pipe into your parser and the return can be directly back on `:body`
