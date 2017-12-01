# Include configuration from config.env
include config.env

DIR := $(dir $(realpath $(firstword $(MAKEFILE_LIST))))

# Define application name
APP_NAME := "postgres-wal-shipper"

# Grep version from project.clj
VERSION := $(shell ./version.sh)

# Read our secret Postgres password from profiles.clj
POSTGRES_PASSWORD := $(shell cat profiles.clj | sed -n 's/.*:postgres-password "\(.*\)".*/\1/p')

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

start-pg:
	@docker run -i -t --rm \
          -v $(DIR)/data:/var/lib/postgresql/data \
					--env-file=./config.env \
					--env=POSTGRES_PASSWORD=$(POSTGRES_PASSWORD) \
          --name=postgres-wal-shipper-db \
          postgres:10.1-alpine

# HELPERS

version: ## Output the current version
	@echo $(VERSION)
