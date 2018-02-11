# Define application name
IMAGE_NAME := "kafka-single-node"

# Define Kafka version to use
KAFKA_VERSION := "1.0.0"

# Define Scala version to use
SCALA_VERSION := "2.12"

# Kafka host advertised by Kafka
ADVERTISED_HOST := "localhost"
# Kafka host advertised by Kafka
ADVERTISED_PORT := 9092
# Where Kafka listens for client requests
ADVERTISED_LISTENERS := "PLAINTEXT://$(ADVERTISED_HOST):$(ADVERTISED_PORT)"

# Default goal 
.DEFAULT_GOAL := build

# DOCKER TASKS

# Build docker image
build: 
	@docker build \
         --build-arg KAFKA_VERSION=$(KAFKA_VERSION) \
         --build-arg SCALA_VERSION=$(SCALA_VERSION) \
         -t $(IMAGE_NAME):$(KAFKA_VERSION) .

run: ## Run container
	docker run -i -t \
         --rm \
         --publish 2181:2181 \
         --publish $(ADVERTISED_PORT):$(ADVERTISED_PORT) \
         --env ADVERTISED_LISTENERS=$(ADVERTISED_LISTENERS) \
         --name "$(IMAGE_NAME)" \
         $(IMAGE_NAME):$(KAFKA_VERSION)

up: build run 

stop: ## Stop and remove a running container
	@docker stop $(IMAGE_NAME)

clean: ## Run container
	@docker rmi $(IMAGE_NAME):$(KAFKA_VERSION)

login: 
	@docker exec -t -i $(IMAGE_NAME) /bin/sh

# HELPERS

version: ## Output the current version
	@echo $(KAFKA_VERSION)
