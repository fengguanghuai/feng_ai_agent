#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="${FENG_ENV_FILE:-${HOME}/.config/feng-ai-agent/runtime.env}"
if [[ ! -f "$ENV_FILE" ]]; then
  printf 'Missing private environment file: %s\n' "$ENV_FILE" >&2
  exit 1
fi
# This file is trusted local shell configuration, never supplied by the browser.
source "$ENV_FILE"
# Optional private Bailian credentials; never loaded by the frontend.
RAG_ENV_FILE="${FENG_RAG_ENV_FILE:-${HOME}/.config/feng-ai-agent/bailian.env}"
if [[ -f "$RAG_ENV_FILE" ]]; then
  source "$RAG_ENV_FILE"
fi
if [[ -z "${JAVA_HOME:-}" && -x "${HOME}/.local/share/feng-ai-agent/zulu21/Contents/Home/bin/java" ]]; then
  export JAVA_HOME="${HOME}/.local/share/feng-ai-agent/zulu21/Contents/Home"
fi
JAVA="${JAVA_HOME:+${JAVA_HOME}/bin/}java"
JAR="$ROOT/target/feng-ai-agent-0.0.1-SNAPSHOT.jar"
if [[ ! -f "$JAR" ]]; then
  printf 'Build first: mvn -Dmaven.test.skip=true package\n' >&2
  exit 1
fi
exec "$JAVA" -jar "$JAR" --spring.profiles.active=supabase "$@"
