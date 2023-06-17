(ns eql-as.test-runner
  (:require [clojure.java.io :as io]
            [clojure.test :as test]
            [clojure.test.junit :as junit]))

(set! *warn-on-reflection* true)

(defn exit
  [status]
  (when-not (Boolean/getBoolean "eql-as.test-runner.skip-exit")
    (System/exit status))
  status)

(defn -main
  [& _]
  (require 'br.com.souenzzo.eql-as-test)
  (let [out-file (doto (io/file "target" "junit.xml")
                   (some-> .getParentFile .mkdirs))
        summary (with-open [out (io/writer out-file)]
                  (binding [test/*test-out* out]
                    (junit/with-junit-output
                      (test/run-tests 'br.com.souenzzo.eql-as-test))))]
    (-> (if (test/successful? summary)
          1 0)
      (exit))))

(comment
  (System/setProperty "eql-as.test-runner.skip-exit" "true")
  (-main))
