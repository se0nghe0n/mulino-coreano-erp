#!/usr/bin/env bash
set -euo pipefail
# Explicit S0 profile only. The caller supplies a disposable DB and ephemeral key.
: "${JWT_PUBLIC_KEY:?JWT_PUBLIC_KEY must point to an ephemeral public key}"
: "${JWT_ISSUER:?JWT_ISSUER is required}"
: "${JWT_AUDIENCE:?JWT_AUDIENCE is required}"
if [[ $# -lt 1 ]]; then
  printf '%s\n' 'Usage: run-spike-server.sh /absolute/path/ontology.jar [server options]' >&2
  exit 2
fi
spike_jar=$1
shift
exec java -jar "$spike_jar" --spring.profiles.active=local,platform-spike "$@"
