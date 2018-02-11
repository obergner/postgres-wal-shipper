(ns postgres-wal-shipper.kafka-writer
  (:require [postgres-wal-shipper.conf :as conf]
            [clojure.tools.logging :as log]
            [mount.core :as mount])
  (:import (org.apache.kafka.clients.producer KafkaProducer
                                              ProducerRecord
                                              Callback)))

(defn- create-logging-cb
  []
  (reify Callback
    (onCompletion [this record-metadata exception]
      (if (nil? exception)
        (log/debugf "SENT: [%s]" record-metadata)
        (log/errorf exception "SENT FAILED: %s" (.getMessage exception))))))

(def logging-callback
  (create-logging-cb))

(defn- start-kafka-producer
  [{:keys [brokers]}]
  (let [p-config {"bootstrap.servers" brokers
                  "value.serializer" "org.apache.kafka.common.serialization.StringSerializer"
                  "key.serializer" "org.apache.kafka.common.serialization.StringSerializer"}]
    (log/infof "START:   Kafka producer [brokers:%s] ..." brokers)
    (KafkaProducer. p-config)))

(defn- stop-kafka-producer
  [kafka-producer]
  (log/infof "STOP:    Kafka producer [%s] ..." kafka-producer)
  (.close kafka-producer)
  (log/infof "STOPPED: Kafka producer [%s]" kafka-producer))

(mount/defstate kafka-producer
  :start (start-kafka-producer (:kafka conf/config))
  :stop (stop-kafka-producer kafka-producer))

(defn send-message
  ([kafka-producer topic key msg]
   (send-message kafka-producer topic key msg logging-callback))
  ([kafka-producer topic key msg callback]
   (let [record (ProducerRecord. topic key msg)]
     (log/debugf "SEND: [topic:%s] %s -> [%s]" topic key msg)
     (.send kafka-producer record callback))))
