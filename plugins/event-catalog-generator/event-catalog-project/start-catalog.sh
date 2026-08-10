#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
ZENWAVE_SDK_DIR="$(cd -- "$SCRIPT_DIR/../../.." && pwd)"
WORKSPACE_DIR="$(cd -- "$ZENWAVE_SDK_DIR/.." && pwd)"
PLUGIN_DIR="$(cd -- "$SCRIPT_DIR/.." && pwd)"
TARGET_DIR="$PLUGIN_DIR/target"

EVENTCATALOG_REPO="$WORKSPACE_DIR/archcatalog"
PROJECT_DIR="$SCRIPT_DIR"
CONTENT_DIR="$ZENWAVE_SDK_DIR/plugins/event-catalog-generator/target/arcadia-event-catalog-output-test"
CATALOG_DIR="$EVENTCATALOG_REPO/packages/core/eventcatalog"
MODE="dev"
BUILD_OUTPUT_DIR="$PLUGIN_DIR/target/arcadia-event-catalog-site"
REMOTE_SCHEMA_FAIL_ON_ERROR="${EVENTCATALOG_REMOTE_SCHEMA_FAIL_ON_ERROR:-true}"

usage() {
  cat <<EOF
Usage:
  $(basename "$0") [--allow-missing-remote-schemas]
  $(basename "$0") --build [target-folder] [--allow-missing-remote-schemas]

Remote schemas fail the catalog build by default. Use
--allow-missing-remote-schemas to render a warning for an unavailable or invalid
remote schema and continue. Flags may be provided in any order.

The default build output is:
  $BUILD_OUTPUT_DIR

Custom output folders must be children of:
  $TARGET_DIR
EOF
}

POSITIONAL_ARGS=()
while [[ $# -gt 0 ]]; do
  case "$1" in
    --build)
      MODE="build"
      shift
      ;;
    --allow-missing-remote-schemas)
      REMOTE_SCHEMA_FAIL_ON_ERROR="false"
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    -*)
      usage >&2
      exit 2
      ;;
    *)
      POSITIONAL_ARGS+=("$1")
      shift
      ;;
  esac
done

if [[ "$MODE" == "build" ]]; then
  if [[ ${#POSITIONAL_ARGS[@]} -gt 1 ]]; then
    usage >&2
    exit 2
  fi
  if [[ ${#POSITIONAL_ARGS[@]} -eq 1 ]]; then
    BUILD_OUTPUT_DIR="${POSITIONAL_ARGS[0]}"
  fi
elif [[ ${#POSITIONAL_ARGS[@]} -gt 0 ]]; then
  usage >&2
  exit 2
fi

if [[ "$MODE" == "build" ]]; then
  if [[ "$BUILD_OUTPUT_DIR" != /* && ! "$BUILD_OUTPUT_DIR" =~ ^[[:alpha:]]:[/\\] ]]; then
    BUILD_OUTPUT_DIR="$PLUGIN_DIR/$BUILD_OUTPUT_DIR"
  fi

  BUILD_OUTPUT_DIR="$(realpath -m "$BUILD_OUTPUT_DIR")"
  TARGET_DIR="$(realpath -m "$TARGET_DIR")"

  case "$BUILD_OUTPUT_DIR" in
    "$TARGET_DIR"/*)
      ;;
    *)
      echo "Build output must be a child of: $TARGET_DIR" >&2
      echo "Resolved output was: $BUILD_OUTPUT_DIR" >&2
      exit 2
      ;;
  esac

  BUILD_OUTPUT_RELATIVE="$(realpath -m --relative-to="$PROJECT_DIR" "$BUILD_OUTPUT_DIR")"
fi

if [[ ! -d "$CONTENT_DIR" ]]; then
  echo "EventCatalog content directory does not exist: $CONTENT_DIR" >&2
  echo "Generate the Arcadia catalog before starting EventCatalog." >&2
  exit 1
fi

clean_cache_dir() {
  local cache_dir="$1"

  case "$cache_dir" in
    "$CATALOG_DIR/.astro"|"$CATALOG_DIR/node_modules/.astro"|"$CATALOG_DIR/node_modules/.vite")
      if [[ -d "$cache_dir" ]]; then
        echo "Removing stale EventCatalog cache: $cache_dir"
        rm -rf -- "$cache_dir"
      fi
      ;;
    *)
      echo "Refusing to remove unexpected cache path: $cache_dir" >&2
      exit 1
      ;;
  esac
}

clean_cache_dir "$CATALOG_DIR/.astro"
clean_cache_dir "$CATALOG_DIR/node_modules/.astro"
clean_cache_dir "$CATALOG_DIR/node_modules/.vite"

cd "$EVENTCATALOG_REPO/packages/core"

if [[ "$MODE" == "build" ]]; then
  echo "Building EventCatalog from content: $CONTENT_DIR"
  echo "Static output directory: $BUILD_OUTPUT_DIR"

  PROJECT_DIR="$PROJECT_DIR" \
  CONTENT_DIR="$CONTENT_DIR" \
  CATALOG_DIR="$CATALOG_DIR" \
  EVENTCATALOG_OUTPUT_DIR="$BUILD_OUTPUT_RELATIVE" \
  EVENTCATALOG_REMOTE_SCHEMA_FAIL_ON_ERROR="$REMOTE_SCHEMA_FAIL_ON_ERROR" \
  node ./bin/eventcatalog.js build

  echo
  echo "Build complete: $BUILD_OUTPUT_DIR"
else
  echo "Starting EventCatalog with content: $CONTENT_DIR"

  PROJECT_DIR="$PROJECT_DIR" \
  CONTENT_DIR="$CONTENT_DIR" \
  CATALOG_DIR="$CATALOG_DIR" \
  EVENTCATALOG_REMOTE_SCHEMA_FAIL_ON_ERROR="$REMOTE_SCHEMA_FAIL_ON_ERROR" \
  node ./bin/eventcatalog.js dev --debug
fi
