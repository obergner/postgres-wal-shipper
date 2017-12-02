# Include configuration from config.env
include config.env

DIR := $(dir $(realpath $(firstword $(MAKEFILE_LIST))))

# Define application name
APP_NAME := "postgres-wal-shipper"

# Grep version from project.clj
VERSION := $(shell ./version.sh)

# Read our secret Postgres password from project.clj
POSTGRES_PASSWORD := $(shell cat project.clj | sed -n 's/.*:dbpassword "\(.*\)".*/\1/p')

# Where Postgresql stores its data
POSTGRES_DBDIR := $(DIR)/target/data

# Our custom Postgresql configuration, activating logical replication
POSTGRES_CONF := $(DIR)/conf/postgres/postgresql.conf

# Default goal 
.DEFAULT_GOAL := build

# LEININGEN TASKS

# Clean output
clean:
	@lein clean

# Run tests
.PHONY: test
test:
	@lein test

# Create executable uberjar
package:
	@lein uberjar

# Create documentation
.PHONY: doc
doc:
	@lein doc

# DOCKER TASKS

# Build the container
image: package
	@docker build \
         --build-arg version=$(VERSION) \
         --build-arg managementApiPort=$(MANAGEMENT_API_PORT) \
         -t $(APP_NAME):$(VERSION) .

# Build the container
build: test container

run: ## Run container
	@docker run -i -t --rm \
         --env-file=./config.env \
         --publish=$(MANAGEMENT_API_PORT):$(MANAGEMENT_API_PORT) \
         --name="$(APP_NAME)" \
         $(APP_NAME):$(VERSION)

up: build run ## Run container on port configured in `config.env` (Alias to run)

stop: ## Stop and remove a running container
	@docker stop $(APP_NAME); docker rm $(APP_NAME)

# Start/stop Postgresql DB

$(POSTGRES_DBDIR):
	@mkdir -p $(POSTGRES_DBDIR)

start-pg: $(POSTGRES_DBDIR)
	@docker run --rm \
          --detach \
          --volume=$(POSTGRES_CONF):/etc/postgresql.conf \
          --volume=$(POSTGRES_DBDIR):/var/lib/postgresql/data \
					--env-file=./config.env \
					--env=POSTGRES_PASSWORD=$(POSTGRES_PASSWORD) \
          --publish=5432:5432 \
          --name=postgres-wal-shipper-db \
          postgres:10.1-alpine \
          -c config_file=/etc/postgresql.conf

stop-pg: 
	@docker stop postgres-wal-shipper-db
	@rm -rf $(POSTGRES_DBDIR)

login-pg: 
	@docker exec -t -i postgres-wal-shipper-db /bin/sh

# HELPERS

version: ## Output the current version
	@echo $(VERSION)
