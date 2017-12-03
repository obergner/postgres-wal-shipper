(ns user
  (:require [postgres-wal-shipper.app :as app]
            [postgres-wal-shipper.conf :as conf]
            [postgres-wal-shipper.replication-client :as replication-client]
            [postgres-wal-shipper.management-api :as management-api]
            [clj-http.client :as http]
            [mount.core :as mount]
            [mount-up.core :as mu]
            [clojure.java.jdbc :as jdbc]
            [clojure.pprint :as p]
            [clojure.java.shell :as shell]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [clojure.core.async :as async])
  (:import [java.net Socket InetSocketAddress]
           [java.util.logging LogManager]
           [org.slf4j.bridge SLF4JBridgeHandler]))

(-> (LogManager/getLogManager)
    (.reset))
(SLF4JBridgeHandler/removeHandlersForRootLogger)
(SLF4JBridgeHandler/install)

(mu/on-upndown :info mu/log :before)

(println
 "
-----------------------------------------------------------------------------------------
Welcome to Postges WAL Shipper's REPL. Here's an overview of some custom commands you
might find useful:

 * (start):
     start Postges WAL Shipper, including all subsystems
 * (stop):
     stop Postges WAL Shipper, taking care to stop all subsystems in reverse startup order
 * (restart):
     stop, then start again
 * (check-health):
     call Postges WAL Shipper's /health endpoint

Enjoy
-----------------------------------------------------------------------------------------
")

;; Starting, stopping etc. the application itself

(defn start
  []
  (mount/start))

(defn stop
  []
  (mount/stop))

(defn restart
  []
  (mount/stop)
  (mount/start))

(defn check-health
  []
  (let [port (get-in conf/config [:management-api :port])
        uri (format "http://localhost:%d/health" port)]
    (http/get uri {:accept :json
                   :throw-exceptions false})))

;; Starting and stopping Postgresql Docker container

(defn wait-for-db
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
        (when-not (connected?)
          (Thread/sleep 100)
          (recur))))))

(defn do-start-db
  [dbconf]
  (let [startup-ms 6000]
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

(defn do-stop-db
  []
  (log/infof "STOP: Postgresql Docker container ...")
  (shell/sh "docker" "stop" "postgres-wal-shipper-db"))

(mount/defstate db
  :start (do-start-db (:postgres conf/config))
  :stop (do-stop-db))

(defn start-db
  []
  (mount/start #'conf/config #'db))

(defn stop-db
  []
  (mount/stop #'db))

;; Connecting to and population Postgres DB

(defn dbconn
  []
  (:postgres conf/config))

(def state-table-dll
  "CREATE TABLE IF NOT EXISTS state
  (
  state_id serial PRIMARY KEY,
  state VARCHAR(32),
  abrv VARCHAR(32)
  )")

(def insert-state-dml
  "INSERT INTO state (state, abrv) VALUES (?, ?)")

(defn create-state-table
  [dbconn]
  (jdbc/execute! dbconn [state-table-dll]))

(defn do-start-dbgen
  [dbconn]
  (let [run-dbgen (atom true)]
    (log/infof "START: db data generator using [%s] ..." dbconn)
    (create-state-table dbconn)
    (async/go-loop [count 0]
      (async/<! (async/timeout 10000))
      (jdbc/db-do-prepared dbconn [insert-state-dml [(str "State-" count) (str "ABR-" count)]] {:multi? true})
      (when @run-dbgen
        (recur (inc count))))
    run-dbgen))

(defn do-stop-dbgen
  [run-dbgen]
  (reset! run-dbgen false))

(mount/defstate dbgen
  :start (do-start-dbgen (:postgres conf/config))
  :stop (do-stop-dbgen dbgen))

(defn start-dbgen
  []
  (mount/start #'conf/config #'db #'dbgen))

(defn stop-dbgen
  []
  (mount/stop #'dbgen))

(defn show-state-table
  []
  (p/pprint (jdbc/query (:postgres conf/config) ["SELECT * FROM state"])))
