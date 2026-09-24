FROM golang:1.25-alpine AS extractor
RUN CGO_ENABLED=0 go install github.com/chenhuifeng/androidbinary/v2/apk/cmd/extract-icons@v2.0.10

FROM oven/bun:1.4-alpine
COPY --from=extractor /go/bin/extract-icons /usr/local/bin/extract-icons
