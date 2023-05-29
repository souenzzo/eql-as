(ns eql-as.build
  (:require [clojure.tools.build.api :as b]
            [shadow.cljs.devtools.server :as shadow.server]
            [shadow.cljs.devtools.api :as shadow.api]))

(def lib 'br.com.souenzzo/eql-as)
(def class-dir "target/classes")

(defn -main
  [& _]
  (try
    (shadow.server/start!)
    (shadow.api/release :node-test)
    (finally
      (shadow.server/stop!)))
  (let [version (format "0.0.%s" (b/git-count-revs nil))
        basis (b/create-basis {:project "deps.edn"})
        jar-file (format "target/eql-as-%s.jar" version)]
    (b/delete {:path "target"})
    (b/write-pom {:class-dir class-dir
                  :lib       lib
                  :version   version
                  :basis     basis
                  :src-dirs  ["src"]})
    (b/copy-dir {:src-dirs   ["src" "resources"]
                 :target-dir class-dir})
    (b/jar {:class-dir class-dir
            :jar-file  jar-file})))

(comment
  (-main))