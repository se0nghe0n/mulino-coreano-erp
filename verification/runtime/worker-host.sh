#!/bin/sh
set -eu
: "${MULINO_RUNTIME_CLASSPATH_FILE:?Explicit compiled dependency classpath required}"
: "${MULINO_FIXTURE_EXTERNAL_URL:?Explicit loopback external fixture required}"
: "${MULINO_JAVA_HOME:?Explicit Java21 runtime required}"
root=$(CDPATH='' cd -- "$(dirname -- "$0")/../.." && pwd)
classpath=$(cat "$MULINO_RUNTIME_CLASSPATH_FILE")
exec "$MULINO_JAVA_HOME/bin/java" -cp "$root/backend/target/classes:$classpath" com.mulino.application.runtime.RuntimeVerificationHost "$@"
