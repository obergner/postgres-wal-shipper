(ns postgres-wal-shipper.app
  (:require [environ.core :as env]
            [postgres-wal-shipper.conf :as conf]
            [postgres-wal-shipper.management-api :as mapi]
            [mount.core :as mount]
            [clojure.tools.logging :as log]))

(defn- manifest-map
  "Return the `mainAttributes` of file `META-INF/MANIFEST.MF` located in the jar the class named `class-name` is part
  of, as a map.

  Note that looking up `class-name`'s `MANIFEST.MF` will fail if the class named `class-name` is not stored in a jar
  file. Or if it simply does not exist. In this case, return an empty map."
  [class-name]
  (try
    (->> (str "jar:"
              (-> (Class/forName class-name)
                  .getProtectionDomain
                  .getCodeSource
                  .getLocation)
              "!/META-INF/MANIFEST.MF")
         clojure.java.io/input-stream
         java.util.jar.Manifest.
         .getMainAttributes
         (map (fn [[k v]] [(str k) v]))
         (into {}))
    (catch Exception e
      (log/warnf e "Failed to load MANIFEST.MF (maybe not running from a jar?): %s" (.getMessage e))
      {})))

(defn- log-startup-banner
  "Log a startup banner on behalf of class named `class-name`."
  [class-name]
  (let [manifest (manifest-map class-name)]
    (log/infof "============================================================================")
    (log/infof " Starting: %s v. %s" (get manifest "Application-Name") (get manifest "Application-Version"))
    (log/infof "           %s" (get manifest "Application-Description"))
    (log/infof "")
    (log/infof " Git-Commit: %s" (get manifest "Git-Commit"))
    (log/infof " Git-Branch: %s" (get manifest "Git-Branch"))
    (log/infof " Git-Dirty : %s" (get manifest "Git-Dirty"))
    (log/infof "============================================================================")
    (log/infof "")
    (doseq [[k v] env/env]
      (log/infof "%40.40s: %s" k v))
    (log/infof "")
    (log/infof "============================================================================")))

(defn- start-app
  "Start this application by starting all supplied subsystems in order."
  [config management-api]
  (log-startup-banner "postgres_wal_shipper.core"))

(defn- stop-app
  "Stop this application by stopping all subsystems in reverse startup order."
  [app])

(mount/defstate app
  :start (start-app conf/conf mapi/management-api)
  :stop (stop-app app))
