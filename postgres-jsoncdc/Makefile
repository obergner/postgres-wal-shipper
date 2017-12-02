# Define application name
IMAGE_NAME := "postgres-jsoncdc"

# Define Postgresql version to use
PG_VERSION := "10.1"

# Default goal 
.DEFAULT_GOAL := build

# DOCKER TASKS

# Build docker image
build: 
	@docker build \
         --build-arg PG_VERSION=$(PG_VERSION) \
         -t $(IMAGE_NAME):$(PG_VERSION) .

run: ## Run container
	@docker run -i -t --rm \
         --name="$(IMAGE_NAME)" \
         $(IMAGE_NAME):$(PG_VERSION)

up: build run 

stop: ## Stop and remove a running container
	@docker stop $(IMAGE_NAME)

clean: ## Run container
	@docker rmi $(IMAGE_NAME):$(PG_VERSION)

login: 
	@docker exec -t -i $(IMAGE_NAME) /bin/sh

# HELPERS

version: ## Output the current version
	@echo $(PG_VERSION)
