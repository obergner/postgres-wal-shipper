(ns postgres-wal-shipper.core
  (:require [clojure.core.async :as async]
            [mount.core :as mount]
            [mount-up.core :as mu]
            [postgres-wal-shipper.app :as app])
  (:import [java.util.logging LogManager]
           [org.slf4j.bridge SLF4JBridgeHandler])
  (:gen-class))

(-> (LogManager/getLogManager)
    (.reset))
(SLF4JBridgeHandler/removeHandlersForRootLogger)
(SLF4JBridgeHandler/install)

(defn -main
  "Start application"
  [& args]
  (mu/on-upndown :info mu/log :before)
  (mount/start))
