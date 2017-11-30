(ns user
  (:require [postgres-wal-shipper.app :as app]
            [postgres-wal-shipper.conf :as conf]
            [clj-http.client :as http]
            [postgres-wal-shipper.management-api :as management-api]
            [mount.core :as mount]
            [mount-up.core :as mu]))

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
 * (schedule interval-ms org repo last):
     schedule importing last `last` pull requests from GitHub repository `org`/`repo`
     every `interval-ms` milliseconds
 * (check-health):
     call Postges WAL Shipper's /health endpoint

Enjoy
-----------------------------------------------------------------------------------------
")

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
  (let [port (:management-api-port conf/conf)
        uri (format "http://localhost:%d/health" port)]
    (http/get uri {:accept :json
                   :throw-exceptions false})))
