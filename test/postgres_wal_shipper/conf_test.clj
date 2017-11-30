(ns postgres-wal-shipper.conf-test
  (:require [postgres-wal-shipper.conf :as sut]
            [clojure.test :as t]
            [environ.core :as env]))

(t/deftest config
  (t/testing "that config correctly reads configuration from environment variables"
    (let [exp-kafka-hosts (env/env :kafka-hosts)
          exp-management-api-port (read-string (env/env :management-api-port))
          actual-config (sut/config)]
      (t/is (= exp-kafka-hosts (:kafka-hosts actual-config)))
      (t/is (= exp-management-api-port (:management-api-port actual-config))))))
