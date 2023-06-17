(ns br.com.souenzzo.eql-as-test
  (:require [br.com.souenzzo.eql-as.alpha :as eql-as]
            [clojure.test :refer [deftest is]]))

(comment
  ;; query back to alias-map
  (into {}
        (for [{:keys [dispatch-key children params] :as a} (:children x)]
          [dispatch-key (if children
                          [(:as params) (f a)]
                          (:as params))])))

(deftest eql-as
  (is (= (eql-as/as-query {::eql-as/as-map {:name    :user/name
                                            :address [:user/address {:street :address/street}]}})
         [:name
          {:address [:street]}]))
  (is (= (eql-as/ident-query {::eql-as/as-map {:name    :user/name
                                               :address [:user/address {:street :address/street}]}})
         [:user/name
          {:user/address [:address/street]}]))
  (is (= (eql-as/as-query {::eql-as/as-key :as
                           ::eql-as/as-map {:name    :user/name
                                            :address [:user/address {:street :address/street}]}})
         `[(:name {:as :user/name})
           ({:address [(:street {:as :address/street})]}
            {:as :user/address})]))
  (is (= (eql-as/ident-query {::eql-as/as-key :as
                              ::eql-as/as-map {:name    :user/name
                                               :address [:user/address {:street :address/street}]}})
         `[(:user/name {:as :name})
           ({:user/address [(:address/street {:as :street})]}
            {:as :address})]))
  (is (= (eql-as/reverse {:name    :user/name
                          :address [:user/address {:street :address/street}]})
         {:user/address [:address {:address/street :street}]
          :user/name    :name}))
  (is (= (eql-as/reverse {:user/address [:address {:address/street :street}]
                          :user/name    :name})
         {:name    :user/name
          :address [:user/address {:street :address/street}]}))
  (is (= (eql-as/reverse (eql-as/reverse {:user/address [:address {:address/street :street}]
                                          :user/name    :name}))
         {:user/address [:address {:address/street :street}]
          :user/name    :name})))
