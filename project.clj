(defproject postgres-wal-shipper "1.0.0-SNAPSHOT"
  :description "Replicating Postgresql's WAL into a Kafka stream"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :plugins [[lein-environ "1.1.0"]
            [lein-cljfmt "0.5.7"]
            [lein-marginalia "0.9.1"]]
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/core.async "0.3.465"]
                 [mount "0.1.11"]
                 [tolitius/mount-up "0.1.1"]
                 [environ "1.1.0"]
                 [org.clojure/java.jdbc "0.7.3"]
                 [org.postgresql/postgresql "42.1.4"]
                 [compojure "1.6.0"]
                 [ring/ring-core "1.6.3"]
                 [ring/ring-jetty-adapter "1.6.3"]
                 [ring/ring-json "0.4.0"]
                 [ring-logger "0.7.7"]
                 [org.clojure/tools.logging "0.4.0"]
                 [org.slf4j/slf4j-api "1.7.25"]
                 [ch.qos.logback/logback-classic "1.2.3"]]
  :main ^:skip-aot postgres-wal-shipper.core
  :target-path "target/%s"
  :manifest {"Application-Name" ~#(:name % "UNKNOWN")
             "Application-Version" ~#(:version % "UNKNOWN")
             "Application-Description" ~#(:description % "UNKNOWN")
             "Git-Branch" ~#(clojure.string/trim (:out (clojure.java.shell/sh "git" "rev-parse" "--abbrev-ref" "HEAD") %))
             "Git-Commit" ~#(clojure.string/trim (:out (clojure.java.shell/sh "git" "rev-parse" "HEAD") %))
             "Git-Dirty" ~#(str (not (empty? (clojure.string/trim (:out (clojure.java.shell/sh "git" "status" "--porcelain") %)))))}
  :dbpassword "secret"
  :env {:postgres-dbname "walshipper"
        :postgres-user "walshipper"
        :postgres-password :project/dbpassword
        :kafka-hosts "localhost"
        :management-api-port "3100"}
  :profiles {:dev {:source-paths ["profiles/dev/src"]
                   :resource-paths ["profiles/dev/resources"]
                   :dependencies [[clj-http "3.7.0"]
                                  [pjstadig/humane-test-output "0.8.3"]]
                   :plugins      [[com.jakemccrary/lein-test-refresh "0.21.1"]]
                   :repl-options {:init-ns user}
                   :injections [(require 'pjstadig.humane-test-output)
                                (pjstadig.humane-test-output/activate!)]
                   :env {:postgres-dbname "walshipper"
                         :postgres-password :project/dbpassword
                         :postgres-user "walshipper"
                         :kafka-hosts "localhost"
                         :management-api-port "2200"}}
             :test {:resource-paths ["test-resources"]}
             :uberjar {:aot :all}}
  :aliases {"doc" ["marg" "--dir" "./target/doc"]})
