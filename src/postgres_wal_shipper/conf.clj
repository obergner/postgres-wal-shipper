(ns postgres-wal-shipper.conf
  (:require [environ.core :refer [env]]
            [mount.core :as mount]))

(defn config
  "Read application configuration from environment variables and return it as a hash."
  []
  {:kafka-hosts (env :kafka-hosts)
   :management-api-port (read-string (env :management-api-port))})

(mount/defstate conf :start (config))
