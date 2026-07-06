#!/usr/bin/env bash
set -euo pipefail

EVENTCATALOG_REPO="/c/Users/ivangsa/workspace/zenwave/archcatalog"
PROJECT_DIR="/c/Users/ivangsa/workspace/zenwave/zenwave-sdk/plugins/event-catalog-generator/event-catalog-project"
CONTENT_DIR="/c/Users/ivangsa/workspace/zenwave/zenwave-sdk/plugins/event-catalog-generator/src/test/resources/event-catalog-content"
CATALOG_DIR="$EVENTCATALOG_REPO/packages/core/eventcatalog"

cd "$EVENTCATALOG_REPO/packages/core"

PROJECT_DIR="$PROJECT_DIR" \
CONTENT_DIR="$CONTENT_DIR" \
CATALOG_DIR="$CATALOG_DIR" \
node ./bin/eventcatalog.js dev --debug
