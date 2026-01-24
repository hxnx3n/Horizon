.PHONY: all deps fmt build build-linux build-win clean help \
        docker docker-build docker-run docker-logs docker-stop docker-push docker-clean

# ========================
# General configuration
# ========================
BINARY_NAME := horizon-agent
GO          := go
GOFLAGS     := -ldflags="-s -w"

DOCKER_IMAGE   ?= horizon-agent
DOCKER_TAG     ?= local
DOCKERFILE     ?= Dockerfile
DOCKER_CONTEXT ?= .

# ========================
# OS detection
# ========================
ifeq ($(OS),Windows_NT)
    EXE_EXT        := .exe
    RM             := del /Q
    SET_LINUX_ENV  := $$env:GOOS='linux'; $$env:GOARCH='amd64';
    SET_WIN_ENV    := $$env:GOOS='windows'; $$env:GOARCH='amd64';
    DOCKER_RUN_ENV := cmd /C
else
    EXE_EXT        :=
    RM             := rm -f
    SET_LINUX_ENV  := GOOS=linux GOARCH=amd64
    SET_WIN_ENV    := GOOS=windows GOARCH=amd64
    DOCKER_RUN_ENV :=
endif

# ========================
# Default targets
# ========================
all: deps fmt build

help:
	@echo "Available targets:"
	@echo "  make build         - Build for the current OS"
	@echo "  make build-linux   - Build for Linux (amd64)"
	@echo "  make build-win     - Build for Windows (amd64)"
	@echo "  make clean         - Remove build artifacts"
	@echo ""
	@echo "  make docker-build  - Build Docker image"
	@echo "  make docker-run    - Run Docker container (port 9090)"
	@echo "  make docker-logs   - Follow Docker container logs"
	@echo "  make docker-stop   - Stop and remove Docker container"
	@echo "  make docker-push   - Push Docker image to registry"
	@echo "  make docker-clean  - Remove Docker image"

# ========================
# Go targets
# ========================
deps:
	$(GO) mod download
	$(GO) mod tidy

fmt:
	$(GO) fmt ./...

build:
	$(GO) build $(GOFLAGS) -o $(BINARY_NAME)$(EXE_EXT) .

build-linux:
	$(SET_LINUX_ENV) $(GO) build $(GOFLAGS) -o $(BINARY_NAME)-linux .

build-win:
	$(SET_WIN_ENV) $(GO) build $(GOFLAGS) -o $(BINARY_NAME).exe .

clean:
	-$(RM) $(BINARY_NAME)$(EXE_EXT)
	-$(RM) $(BINARY_NAME)-linux

# ========================
# Docker targets
# ========================
docker: docker-build

docker-build:
	docker build -f $(DOCKERFILE) -t $(DOCKER_IMAGE):$(DOCKER_TAG) $(DOCKER_CONTEXT)

docker-run:
	$(DOCKER_RUN_ENV) docker run -d \
		--restart unless-stopped \
		--name $(BINARY_NAME) \
		-p 9090:9090 \
		$(DOCKER_IMAGE):$(DOCKER_TAG)

docker-logs:
	docker logs -f $(BINARY_NAME)

docker-stop:
	-docker stop $(BINARY_NAME)
	-docker rm $(BINARY_NAME)

docker-push:
	docker push $(DOCKER_IMAGE):$(DOCKER_TAG)

docker-clean:
	-docker rmi -f $(DOCKER_IMAGE):$(DOCKER_TAG)
