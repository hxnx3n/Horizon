#!/bin/bash

set -e

BINARY_NAME="horizon-agent"
INSTALL_DIR="/usr/local/bin"
GITHUB_REPO="hxnx3n/Horizon"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${GREEN}"
echo "  _    _            _                  "
echo " | |  | |          (_)                 "
echo " | |__| | ___  _ __ _ _______  _ __    "
echo " |  __  |/ _ \| '__| |_  / _ \| '_ \   "
echo " | |  | | (_) | |  | |/ / (_) | | | |  "
echo " |_|  |_|\___/|_|  |_/___\___/|_| |_|  "
echo "                                        "
echo -e "${NC}"
echo "Horizon Agent Installer"
echo ""

for cmd in curl; do
    if ! command -v $cmd &> /dev/null; then
        echo -e "${RED}Error: $cmd is required but not installed.${NC}"
        exit 1
    fi
done

if [ "$EUID" -ne 0 ]; then
    SUDO="sudo"
else
    SUDO=""
fi

OS=$(uname -s | tr '[:upper:]' '[:lower:]')
case $OS in
    linux)
        OS="linux"
        ;;
    darwin)
        OS="darwin"
        ;;
    mingw*|msys*|cygwin*)
        OS="windows"
        ;;
    *)
        echo -e "${RED}Unsupported operating system: $OS${NC}"
        exit 1
        ;;
esac

ARCH=$(uname -m)
case $ARCH in
    x86_64|amd64)
        ARCH="amd64"
        ;;
    aarch64|arm64)
        ARCH="arm64"
        ;;
    *)
        echo -e "${RED}Unsupported architecture: $ARCH${NC}"
        exit 1
        ;;
esac

EXT=""
if [ "$OS" = "windows" ]; then
    EXT=".exe"
fi

echo -e "Detected: ${BLUE}${OS}/${ARCH}${NC}"
echo ""

echo "Fetching latest release..."
LATEST_RELEASE=$(curl -s "https://api.github.com/repos/${GITHUB_REPO}/releases" | grep '"tag_name": "agent-v' | head -1 | sed -E 's/.*"([^"]+)".*/\1/')

if [ -z "$LATEST_RELEASE" ]; then
    echo -e "${RED}Error: Could not fetch latest release.${NC}"
    echo "Please check your internet connection or try again later."
    exit 1
fi

echo -e "Latest version: ${GREEN}${LATEST_RELEASE}${NC}"
echo ""

BINARY_FILE="${BINARY_NAME}-${OS}-${ARCH}${EXT}"
DOWNLOAD_URL="https://github.com/${GITHUB_REPO}/releases/download/${LATEST_RELEASE}/${BINARY_FILE}"

echo "Downloading ${BINARY_FILE}..."
TEMP_DIR=$(mktemp -d)
cd "$TEMP_DIR"

HTTP_CODE=$(curl -sL -w "%{http_code}" -o "${BINARY_NAME}${EXT}" "$DOWNLOAD_URL")

if [ "$HTTP_CODE" != "200" ]; then
    echo -e "${RED}Error: Failed to download binary (HTTP $HTTP_CODE)${NC}"
    echo "URL: $DOWNLOAD_URL"
    rm -rf "$TEMP_DIR"
    exit 1
fi

echo "Installing ${BINARY_NAME} to ${INSTALL_DIR}..."
chmod +x "${BINARY_NAME}${EXT}"
$SUDO mv "${BINARY_NAME}${EXT}" "${INSTALL_DIR}/${BINARY_NAME}${EXT}"

cd /
rm -rf "$TEMP_DIR"

if command -v ${BINARY_NAME} &> /dev/null; then
    echo ""
    echo -e "${GREEN}✓ Installation complete!${NC}"
    echo ""
    echo "Usage:"
    echo "  ${BINARY_NAME} auth <api-key> <server-url>  - Authenticate with server"
    echo "  ${BINARY_NAME} run                          - Start pushing metrics"
    echo "  ${BINARY_NAME} status                       - Show authentication status"
    echo "  ${BINARY_NAME} deauth                       - Remove authentication"
    echo ""
    echo "Quick start:"
    echo "  1. Get an API key from your Horizon dashboard"
    echo "  2. Run: ${BINARY_NAME} auth <your-key> http://your-server:8080"
    echo "  3. Run: ${BINARY_NAME} run"
    echo ""
else
    echo -e "${YELLOW}Warning: Installation may have succeeded but ${BINARY_NAME} is not in PATH.${NC}"
    echo "Try running: ${INSTALL_DIR}/${BINARY_NAME}${EXT}"
fi
