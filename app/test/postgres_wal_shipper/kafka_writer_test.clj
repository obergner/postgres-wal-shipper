(ns postgres-wal-shipper.kafka-writer-test
  (:require [postgres-wal-shipper.kafka-writer :as sut]
            [clojure.java.shell :as shell]
            [clojure.tools.logging :as log]
            [clojure.string :as str]
            [clojure.test :as t])
  (:import (java.net Socket InetSocketAddress)
           (java.util.concurrent CountDownLatch TimeUnit)
           (org.apache.kafka.clients.producer Callback)))

;; Starting and stopping Kafka Docker container

(defn- wait-for-kafka
  [kafkaconfig timeout-ms]
  (let [brokers (:brokers kafkaconfig)
        broker (-> (str/split brokers #",")
                   (first))
        host (-> broker
                 (str/split #":")
                 (first))
        port (-> broker
                 (str/split #":")
                 (second))
        start-time (System/currentTimeMillis)
        connected? (fn []
                     (try
                       (Socket. host (read-string port))
                       true
                       (catch Exception e
                         false)))]
    (loop []
      (if (> (- (System/currentTimeMillis) start-time) timeout-ms)
        (throw (Exception. "Waiting for Kafka to come up timed out"))
        (if (connected?)
          (log/infof "Kafka started to respond after waiting for [%d] ms" (- (System/currentTimeMillis) start-time))
          (do
            (Thread/sleep 100)
            (recur)))))))

(defn- start-kafka
  [kafkaconfig]
  (let [startup-timeout-ms 10000
        brokers (:brokers kafkaconfig)
        broker (-> (str/split brokers #",")
                   (first))
        port (-> broker
                 (str/split #":")
                 (second))
        advertised-listeners (format "PLAINTEXT://%s" broker)]
    (log/infof "START: Kafka Docker container using [broker:%s] ..." broker)
    (shell/sh "docker"
              "run"
              "--detach"
              "--rm"
              (format "--env=ADVERTISED_LISTENERS=%s" advertised-listeners)
              (format "--publish=%s:%s" port port)
              "--publish=2181:2181"
              "--name=kafka-single-node"
              "kafka-single-node:1.0.0")
    (wait-for-kafka kafkaconfig startup-timeout-ms)))

(defn- stop-kafka
  []
  (log/infof "STOP: Postgresql Docker container ...")
  (shell/sh "docker" "stop" "kafka-single-node"))

(def kafkaconf {:brokers "localhost:9092"})

(defn with-kafka-broker
  [test-fn]
  (try
    (start-kafka kafkaconf)
    (test-fn)
    (finally (stop-kafka))))

(t/use-fixtures :each with-kafka-broker)

(defn- create-test-cb
  [count-down-latch]
  (reify Callback
    (onCompletion [this record-metadata exception]
      (when (nil? exception)
        (.countDown count-down-latch)))))

(t/deftest start-kafka-producer
  (t/testing "replication client receives all changes"
    (let [message-sent (CountDownLatch. 1)
          test-cb (create-test-cb message-sent)
          kafka-producer (#'sut/start-kafka-producer kafkaconf)]
      (try
        (sut/send-message kafka-producer "test-topic" "key-1" "value-1" test-cb)
        (t/is (= true (.await message-sent 10000 TimeUnit/MILLISECONDS)))
        (finally
          (#'sut/stop-kafka-producer kafka-producer))))))
