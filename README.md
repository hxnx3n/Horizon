# Horizon

A lightweight, real-time server monitoring system built with Go, Spring Boot, and React.

![Dashboard Preview](https://img.shields.io/badge/status-active-brightgreen)
![Go](https://img.shields.io/badge/Go-1.21+-00ADD8?logo=go)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-6DB33F?logo=springboot)
![React](https://img.shields.io/badge/React-19-61DAFB?logo=react)

## Overview

Horizon is a modern infrastructure monitoring solution that provides real-time visibility into your servers' health and performance. It uses a push-based architecture where lightweight agents collect and send metrics to a central backend.

## Features

- **Real-time Monitoring** - CPU, Memory, Disk, Network metrics with live updates via SSE
- **Push-based Architecture** - Agents push metrics to the backend (no inbound ports required on monitored nodes)
- **Multi-node Support** - Monitor multiple servers from a single dashboard
- **Client Key Authentication** - Secure agent registration with API keys
- **Beautiful Dashboard** - Modern, responsive UI with real-time charts
- **Docker Ready** - Full Docker Compose setup for easy deployment

## Architecture

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   Horizon Agent │────▶│  Backend (API)  │◀────│    Frontend     │
│   (Go binary)   │push │  Spring Boot    │ SSE │  React + Vite   │
└─────────────────┘     └────────┬────────┘     └─────────────────┘
                                 │
                        ┌────────┴────────┐
                        │   PostgreSQL    │
                        │     + Redis     │
                        └─────────────────┘
```

## Quick Start

### 1. Start the Backend

```bash
docker-compose up -d
```

Or run individually:

```bash
# Backend
cd backend
./gradlew bootRun

# Frontend
cd frontend
pnpm install
pnpm dev
```

### 2. Generate a Client Key

1. Open the dashboard at `http://localhost`
2. Register/Login to your account
3. Go to **Client Keys** tab
4. Click **Generate New Key**
5. Save the key securely

### 3. Install the Agent

Download the latest agent from [Releases](https://github.com/hxnx3n/Horizon/releases).

```bash
# Authenticate the agent
./horizon-agent auth <your-client-key> http://your-server:8080

# Start monitoring
./horizon-agent run
```

## Agent Commands

| Command | Description |
|---------|-------------|
| `auth <key> <url>` | Authenticate agent with backend |
| `run` | Start the agent and push metrics |
| `status` | Check agent status and configuration |
| `update` | Update agent to the latest version |
| `uninstall` | Remove agent and configuration |

## Tech Stack

| Component | Technology |
|-----------|------------|
| Agent | Go 1.21+, gopsutil |
| Backend | Spring Boot 3.2, PostgreSQL 16, Redis 7 |
| Frontend | React 19, TypeScript, Vite, Tailwind CSS, Recharts |

## Metrics Collected

- **CPU** - Usage percentage, per-core stats
- **Memory** - Total, used, available (htop-style calculation)
- **Disk** - Per-partition usage and capacity
- **Network** - Per-interface RX/TX bytes and rates
- **System** - Uptime, process count, temperature

## Configuration

### Backend (`application.yml`)

```yaml
agent:
  port: 9090
  polling:
    interval: 250

metrics:
  retention-days: 7
```

### Agent

Configuration is stored in `~/.horizon/config.json` after running `auth`.

## Development

```bash
# Agent
cd agent
go build -o horizon-agent .

# Backend
cd backend
./gradlew build

# Frontend
cd frontend
pnpm build
```

## License

MIT License

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.
