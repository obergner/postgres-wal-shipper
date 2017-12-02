(ns postgres-wal-shipper.replication-client
  (:require [postgres-wal-shipper.conf :as conf])
  (:import (java.sql DriverManager)
           (java.util Properties)
           (org.postgresql Driver
                           PGConnection
                           PGProperty)
           (org.postgresql.replication LogSequenceNumber
                                       PGReplicationStream)))

(def ^:private driver-class-name "org.postgresql.Driver")

(defn- load-dbdriver
  [class-name]
  (clojure.lang.RT/loadClassForName class-name))

(defn- ^PGConnection get-replication-connection
  "Get a `PGConnection` instance to later obtain a PGReplicationStream from."
  [dbconfig]
  (let [dbname (:dbname dbconfig)
        user (:user dbconfig)
        password (:password dbconfig)
        conn-string (format "jdbc:postgresql://localhost:5432/%s" dbname)
        props (Properties.)]
    (load-dbdriver driver-class-name)
    (.set PGProperty/USER props user)
    (.set PGProperty/PASSWORD props password)
    (.set PGProperty/ASSUME_MIN_SERVER_VERSION props "9.4")
    (.set PGProperty/REPLICATION props "database")
    (.set PGProperty/PREFER_QUERY_MODE props "simple")
    (-> (DriverManager/getConnection conn-string props)
        (.unwrap PGConnection))))

(defn- create-replication-slot
  [replication-connection slot-name decoding-plugin]
  (-> replication-connection
      (.getReplicationAPI)
      (.createReplicationSlot)
      (.logical)
      (.withSlotName slot-name)
      (.withOutputPlugin decoding-plugin)
      (.make)))
