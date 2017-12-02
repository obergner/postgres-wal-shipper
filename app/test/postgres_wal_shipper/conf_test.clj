(ns postgres-wal-shipper.conf-test
  (:require [postgres-wal-shipper.conf :as sut]
            [clojure.test :as t]
            [environ.core :as env]))

(t/deftest load-config
  (t/testing "that config correctly reads configuration from environment variables"
    (let [exp-kafka-hosts (env/env :kafka-hosts)
          exp-postgres-dbname (env/env :postgres-dbname)
          exp-postgres-user (env/env :postgres-user)
          exp-postgres-password (env/env :postgres-password)
          exp-management-api-port (read-string (env/env :management-api-port))
          actual-config (sut/load-config)]
      (t/is (= exp-kafka-hosts (get-in actual-config [:kafka :hosts])))
      (t/is (= exp-postgres-dbname (get-in actual-config [:postgres :dbname])))
      (t/is (= exp-postgres-user (get-in actual-config [:postgres :user])))
      (t/is (= exp-postgres-password (get-in actual-config [:postgres :password])))
      (t/is (= exp-management-api-port (get-in actual-config [:management-api :port]))))))
