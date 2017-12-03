(ns postgres-wal-shipper.replication-client-test
  (:require [postgres-wal-shipper.replication-client :as sut]
            [clojure.java.jdbc :as jdbc]
            [clojure.java.shell :as shell]
            [clojure.tools.logging :as log]
            [clojure.core.async :as async]
            [clojure.test :as t])
  (:import [java.net Socket InetSocketAddress]
           [java.util.concurrent CountDownLatch TimeUnit]))

;; Starting and stopping Postgresql Docker container

(defn- wait-for-db
  [dbconf timeout-ms]
  (let [address (InetSocketAddress. (:host dbconf) (:port dbconf))
        start-time (System/currentTimeMillis)
        connected? (fn []
                     (try
                       (jdbc/query dbconf ["SELECT 1"])
                       true
                       (catch Exception e
                         false)))]
    (loop []
      (if (> (- (System/currentTimeMillis) start-time) timeout-ms)
        (throw (Exception. "Waiting for DB to come up timed out"))
        (if (connected?)
          (log/infof "DB started to respond after waiting for [%d] ms" (- (System/currentTimeMillis) start-time))
          (do
            (Thread/sleep 100)
            (recur)))))))

(defn- start-db
  [dbconf]
  (let [startup-ms 10000]
    (log/infof "START: Postgresql Docker container using [%s] - waiting for up to [%d] ms for the container to start ..."
               dbconf startup-ms)
    (shell/sh "docker"
              "run"
              "--detach"
              "--rm"
              (format "--env=POSTGRES_DB=%s" (:dbname dbconf))
              (format "--env=POSTGRES_USER=%s" (:user dbconf))
              (format "--env=POSTGRES_PASSWORD=%s" (:password dbconf))
              "--publish=5432:5432"
              "--name=postgres-wal-shipper-db"
              "postgres-jsoncdc:10.1")
    (wait-for-db dbconf startup-ms)))

(defn- stop-db
  []
  (log/infof "STOP: Postgresql Docker container ...")
  (shell/sh "docker" "stop" "postgres-wal-shipper-db"))

;; Connecting to and population Postgres DB

(def state-table-dll
  "CREATE TABLE IF NOT EXISTS state
  (
  state_id serial PRIMARY KEY,
  state VARCHAR(32),
  abrv VARCHAR(32)
  )")

(def insert-state-dml
  "INSERT INTO state (state, abrv) VALUES (?, ?)")

(defn- create-state-table
  [dbconn]
  (jdbc/execute! dbconn [state-table-dll]))

(defn- populate-state-table
  [dbconn rows]
  (jdbc/insert-multi! dbconn :state [:state :abrv] rows))

(def dbconf {:dbtype "postgresql"
             :dbname "walshipper"
             :user "walshipper-test"
             :password "secret"
             :host "localhost"
             :port 5432
             :output-plugin "jsoncdc"})

(def test-data [["state-1" "abbrv-1"] ["state-2" "abbrv-2"]])

(defn with-populated-db
  [test-fn]
  (try
    (start-db dbconf)
    (create-state-table dbconf)
    (test-fn)
    (finally (stop-db))))

(t/use-fixtures :each with-populated-db)

(t/deftest do-start-replication-client
  (t/testing "replication client receives all changes"
    (let [expected-changes 5
          changes-received (CountDownLatch. expected-changes)
          test-record (fn [change] (.countDown changes-received))
          [should-run exec] (#'sut/start-replication-client dbconf test-record)]
      (try
        (populate-state-table dbconf test-data)
        (t/is (= true (.await changes-received 10000 TimeUnit/MILLISECONDS)))
        (finally
          (#'sut/stop-replication-client [should-run exec]))))))
