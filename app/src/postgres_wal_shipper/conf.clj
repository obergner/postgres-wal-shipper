(ns postgres-wal-shipper.conf
  (:require [environ.core :refer [env]]
            [postgres-wal-shipper.conf :as conf]
            [mount.core :as mount]))

(defn load-config
  "Read application configuration from environment variables and return it as a hash."
  []
  {:kafka {:hosts (env :kafka-hosts)}
   :postgres {:dbtype "postgresql"
              :dbname (env :postgres-dbname)
              :user (env :postgres-user)
              :password (env :postgres-password)
              :host (env :postgres-host)
              :port (read-string (env :postgres-port))
              :output-plugin (env :postgres-output-plugin)}
   :management-api {:port (read-string (env :management-api-port))}})

(mount/defstate config :start (load-config))
