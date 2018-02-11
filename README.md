# Postgresql WAL Shipper

Experiment with replicating Postgres' WAL - Write-Ahead-Log - to a Kafka topic.

## Customized REPL

This project uses the rather excellent [mount](https://github.com/tolitius/mount) library for clean and powerful state
management. It is therefore possible to start and stop this application - while disposing cleanly of any resources if need be - in the REPL. Commands for this have been added in namespace [user](./profiles/dev/src/user.clj), along with some other convenient helper functions.

## License

Copyright © 2018 Olaf Bergner <olaf.bergner AT gmx.de>

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
