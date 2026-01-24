FROM golang:1.24-alpine AS builder

RUN apk add --no-cache git

WORKDIR /app

COPY go.mod go.sum ./
RUN go mod download

COPY . .

RUN CGO_ENABLED=0 GOOS=linux GOARCH=amd64 go build -ldflags="-s -w" -o horizon-agent .

FROM alpine:latest

RUN apk add --no-cache ca-certificates tzdata

WORKDIR /root/

COPY --from=builder /app/horizon-agent .
# NOTE: COPY doesn't support shell redirection/conditionals.
# If you need .env in the image, ensure it exists in the build context (or pass it via env/secret handling instead).

EXPOSE 9090

CMD ["./horizon-agent"]
