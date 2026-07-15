#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
ZENWAVE_SDK_DIR="$(cd -- "$SCRIPT_DIR/../../.." && pwd)"
WORKSPACE_DIR="$(cd -- "$ZENWAVE_SDK_DIR/.." && pwd)"

EVENTCATALOG_REPO="$WORKSPACE_DIR/archcatalog"
PROJECT_DIR="$SCRIPT_DIR"
CONTENT_DIR="$ZENWAVE_SDK_DIR/plugins/event-catalog-generator/target/event-catalog-output-test"
CATALOG_DIR="$EVENTCATALOG_REPO/packages/core/eventcatalog"

cd "$EVENTCATALOG_REPO/packages/core"

PROJECT_DIR="$PROJECT_DIR" \
CONTENT_DIR="$CONTENT_DIR" \
CATALOG_DIR="$CATALOG_DIR" \
node ./bin/eventcatalog.js dev --debug
