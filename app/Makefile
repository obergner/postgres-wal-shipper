# Include configuration from config.env
include config.env

DIR := $(dir $(realpath $(firstword $(MAKEFILE_LIST))))

# Define application name
APP_NAME := "postgres-wal-shipper"

# Grep version from project.clj
APP_VERSION := $(shell ./version.sh)

# Read our secret Postgres password from project.clj
PG_PASSWORD := $(shell cat project.clj | sed -n 's/.*:dbpassword "\(.*\)".*/\1/p')

PG_CONTAINER_NAME := "postgres-wal-shipper-db"

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
         --build-arg version=$(APP_VERSION) \
         --build-arg managementApiPort=$(MANAGEMENT_API_PORT) \
         -t $(APP_NAME):$(APP_VERSION) .

# Build the container
build: test image

run: ## Run container
	@docker run -i -t --rm \
         --env-file=./config.env \
         --publish=$(MANAGEMENT_API_PORT):$(MANAGEMENT_API_PORT) \
         --name="$(APP_NAME)" \
         $(APP_NAME):$(APP_VERSION)

up: build run ## Run container on port configured in `config.env` (Alias to run)

stop: ## Stop and remove a running container
	@docker stop $(APP_NAME); docker rm $(APP_NAME)

# Start/stop Postgresql DB

start-db: 
	@docker run --rm \
          --detach \
					--env-file=./config.env \
					--env=POSTGRES_PASSWORD=$(PG_PASSWORD) \
          --publish=5432:5432 \
          --name=$(PG_CONTAINER_NAME) \
          postgresql-jsoncdc:10.1

start-db-foreground: 
	@docker run -i -t --rm \
					--env-file=./config.env \
					--env=POSTGRES_PASSWORD=$(PG_PASSWORD) \
          --publish=5432:5432 \
          --name=$(PG_CONTAINER_NAME) \
          postgresql-jsoncdc:10.1

stop-db: 
	@docker stop $(PG_CONTAINER_NAME)

login-db: 
	@docker exec -t -i $(PG_CONTAINER_NAME) /bin/sh

# HELPERS

version: ## Output the current version
	@echo $(APP_VERSION)
