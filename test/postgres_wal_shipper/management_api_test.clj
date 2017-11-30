(ns postgres-wal-shipper.management-api-test
  (:require [postgres-wal-shipper.management-api :as sut]
            [clojure.test :as t]
            [clj-http.client :as http]
            [postgres-wal-shipper.conf :as conf]
            [clojure.core.async :as async]
            [cheshire.core :as json]))

(t/deftest start-stop-management-api
  (t/testing "that /health endpoint returns 200/OK"
    (let [config {:management-api-port 34567}
          server (#'sut/start-management-api config)]
      (try
        (t/is (= 200 (:status (http/get (format "http://localhost:%d/health" (:management-api-port config))))))
        (finally
          (#'sut/stop-management-api server))))))
