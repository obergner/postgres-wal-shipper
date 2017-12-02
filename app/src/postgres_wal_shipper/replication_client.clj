(ns postgres-wal-shipper.replication-client
  (:require [postgres-wal-shipper.conf :as conf]
            [clojure.core.async :as async]
            [clojure.tools.logging :as log]
            [mount.core :as mount])
  (:import (java.sql DriverManager)
           (java.util Properties)
           (java.nio ByteBuffer)
           (org.postgresql Driver
                           PGConnection
                           PGProperty)
           (org.postgresql.replication PGReplicationStream)
           (io.aleph.dirigiste Executors
                               Executor)))

(def ^:private driver-class-name "org.postgresql.Driver")

(defn- load-dbdriver
  [class-name]
  (clojure.lang.RT/loadClassForName class-name))

(defn- ^PGConnection get-replication-connection
  "Get a `PGConnection` instance to later obtain a PGReplicationStream from."
  [{:keys [dbname user password host port output-plugin]}]
  (let [conn-string (format "jdbc:postgresql://%s:%d/%s" host port dbname)
        props (Properties.)]
    (log/infof "Establishing replication connection to [%s] using output plugin [%s] ..." conn-string output-plugin)
    (load-dbdriver driver-class-name)
    (.set PGProperty/USER props user)
    (.set PGProperty/PASSWORD props password)
    (.set PGProperty/ASSUME_MIN_SERVER_VERSION props "9.4")
    (.set PGProperty/REPLICATION props "database")
    (.set PGProperty/PREFER_QUERY_MODE props "simple")
    (-> (DriverManager/getConnection conn-string props)
        (.unwrap PGConnection))))

(defn- register-replication-slot
  "Register a logical `replication slot` in our database, later to be used by our `replication stream`. Supplied
  `replication-connection` will be used to register our replication slot, which will be called `slot-name` and will use
  `output-plugin` to decode (in the database) the WAL."
  [^PGConnection replication-connection slot-name output-plugin]
  (log/infof "Registering replication slot [%s] for output plugin [%s] ..." slot-name output-plugin)
  (-> replication-connection
      (.getReplicationAPI)
      (.createReplicationSlot)
      (.logical)
      (.withSlotName slot-name)
      (.withOutputPlugin output-plugin)
      (.make)))

(defn- ^PGReplicationStream start-replication-stream
  [^PGConnection replication-connection slot-name]
  (log/infof "Starting replication stream using slot [%s] ..." slot-name)
  (-> replication-connection
      (.getReplicationAPI)
      (.replicationStream)
      (.logical)
      (.withSlotName slot-name)
      (.start)))

(defn- ^String read-replication-message
  [^PGReplicationStream replication-stream]
  (log/infof "Reading next replication message from [%s] ..." replication-stream)
  (try
    (let [msg (.read replication-stream)
          offset (.arrayOffset msg)
          payload (.array msg)
          length (- (.length payload) offset)
          decoded (String. payload offset length)]
      (log/infof "Decoded replication message [%s]" decoded)
      decoded)
    (catch Exception e
      (log/errorf e "Caught exception trying to read next replication message: %s" (.getMessage e))
      nil)))

(defn- run-replication-client
  [^PGReplicationStream replication-stream on-message-cb]
  (let [should-run (atom true)
        ^Executor exec (Executors/fixedExecutor 1)]
    (.execute exec
              #(while @should-run
                 (let [replication-message (read-replication-message replication-stream)]
                   (on-message-cb replication-message))))
    [should-run exec]))

(defn- start-replication-client
  [{:keys [output-plugin], :as dbconfig} on-message-cb]
  (let [slot-name "test_slot"
        pgconn (get-replication-connection dbconfig)]
    (register-replication-slot pgconn slot-name output-plugin)
    (let [replication-stream (start-replication-stream pgconn slot-name)]
      (run-replication-client replication-stream on-message-cb))))

(defn- stop-replication-client
  [[should-run exec]]
  (reset! should-run false)
  (.shutdown exec))

(defn log-replication-message
  [msg]
  (log/infof "RCVD: %s" msg))

(mount/defstate replication-client
  :start (start-replication-client (:postgres conf/config) log-replication-message)
  :stop (stop-replication-client replication-client))
