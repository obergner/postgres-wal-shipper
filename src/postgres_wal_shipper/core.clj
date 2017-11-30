(ns postgres-wal-shipper.core
  (:require [clojure.core.async :as async]
            [mount.core :as mount]
            [mount-up.core :as mu]
            [postgres-wal-shipper.app :as app])
  (:gen-class))

(defn -main
  "Start application"
  [& args]
  (mu/on-upndown :info mu/log :before)
  (mount/start))
