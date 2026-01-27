.PHONY: all deps fmt build build-linux build-win clean install uninstall help

# ========================
# General configuration
# ========================
BINARY_NAME := horizon-agent
GO          := go
VERSION     ?= dev
GOFLAGS     := -ldflags="-s -w -X github.com/hxnx3n/Horizon/agent/cmd.Version=$(VERSION)"

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
	@echo "  make install       - Build and install to /usr/local/bin"
	@echo "  make uninstall     - Remove from /usr/local/bin"
	@echo "  make clean         - Remove build artifacts"

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
# Install targets
# ========================
install: build
	@echo "Installing $(BINARY_NAME) to /usr/local/bin..."
	sudo cp $(BINARY_NAME) /usr/local/bin/$(BINARY_NAME)
	sudo chmod +x /usr/local/bin/$(BINARY_NAME)
	@echo "✓ Installed! Run 'horizon-agent --help' to get started."

uninstall:
	@echo "Removing $(BINARY_NAME) from /usr/local/bin..."
	sudo rm -f /usr/local/bin/$(BINARY_NAME)
	@echo "✓ Uninstalled."
