#!/bin/sh
# ./verify scenarios --actual: runs every scenarios-profile subcase against a copied, freshly built backend
# and a disposable PostgreSQL container, then removes exactly the resources it created.
# Usage: run.sh [evidence-dir] [-- case.json ...]
set -eu
umask 077
repo=$(CDPATH= cd -- "$(dirname -- "$0")/../../.." && pwd)
cd "$repo"
run_id=$(python3 -c 'import uuid; print(uuid.uuid4())')
evidence=
if [ "$#" -gt 0 ] && [ "$1" != "--" ]; then evidence=$1; shift; fi
[ "$#" -eq 0 ] || { [ "$1" = "--" ] && shift; }
evidence=${evidence:-"$repo/verification/harness/target/evidence/actual-scenarios-run-$run_id"}
mkdir -p "$(dirname -- "$evidence")"
mkdir "$evidence"
evidence=$(CDPATH= cd -- "$evidence" && pwd)
fixture=$(mktemp -d /tmp/mulino-scenarios.XXXXXX)
container="mulino-scenarios-$run_id"
container_created=false
server_pid=
cleanup() {
  code=$?
  trap - EXIT HUP INT TERM
  set +e
  if [ -n "$server_pid" ]; then
    kill "$server_pid" 2>/dev/null
    i=0
    while kill -0 "$server_pid" 2>/dev/null && [ "$i" -lt 40 ]; do sleep 0.25; i=$((i+1)); done
    kill -KILL "$server_pid" 2>/dev/null
    wait "$server_pid" 2>/dev/null
  fi
  if [ "$container_created" = true ]; then
    docker logs "$container" > "$evidence/postgres-runtime.log" 2>&1
    docker rm -f -v "$container" > "$evidence/container-cleanup.txt" 2>&1
  fi
  python3 - "$fixture" <<'PY_CLEANUP'
import pathlib,shutil,sys
p=pathlib.Path(sys.argv[1]);parent=pathlib.Path('/tmp').resolve()
if p.resolve().parent!=parent or not p.name.startswith('mulino-scenarios.'):
    raise SystemExit('Refusing cleanup outside the created fixture directory')
shutil.rmtree(p)
PY_CLEANUP
  printf 'Scenario evidence: %s (exit %s)\n' "$evidence" "$code"
  exit "$code"
}
trap cleanup EXIT
trap 'exit 3' HUP INT TERM
[ -f backend/target/ontology-0.1.0-SNAPSHOT.jar ]
[ -f verification/harness/target/classpath.txt ]
cp backend/target/ontology-0.1.0-SNAPSHOT.jar "$fixture/ontology.jar"
python3 -c 'import hashlib,sys; print(hashlib.sha256(open(sys.argv[1],"rb").read()).hexdigest())' "$fixture/ontology.jar" > "$evidence/executed-jar-sha256.txt"
git rev-parse HEAD > "$evidence/code-commit.txt"
git status --porcelain > "$evidence/working-tree-status.txt"
printf 'partialFixtures=%s\nrelaxWire=%s\n' "${ACTUAL_PARTIAL_FIXTURES:-true}" "${ACTUAL_RELAX_WIRE:-false}" > "$evidence/run-mode.txt"
image=$(python3 -c 'import json; print(json.load(open("verification/platform/versions.json"))["postgresImage"]["reference"])')
case "$image" in postgres@sha256:*) ;; *) exit 3 ;; esac
printf '%s\n' "$image" > "$evidence/postgres-image-reference.txt"
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out "$fixture/private.pem" 2> "$fixture/keygen.log"
openssl pkey -in "$fixture/private.pem" -pubout -out "$fixture/public.pem" 2> "$fixture/pubkey.log"
export JWT_PRIVATE_KEY="$fixture/private.pem" JWT_PUBLIC_KEY="$fixture/public.pem"
export JWT_ISSUER=https://mulino-native.invalid JWT_AUDIENCE=isolated-ontology
export DB_USERNAME=postgres DB_PASSWORD
DB_PASSWORD=$(python3 -c 'import secrets; print(secrets.token_urlsafe(24))')
docker run -d --name "$container" -e "POSTGRES_PASSWORD=$DB_PASSWORD" -e POSTGRES_DB=ontology -p 127.0.0.1::5432 "$image" > "$evidence/container-id.txt"
container_created=true
port=$(docker port "$container" 5432/tcp | sed 's/.*://')
export DB_URL="jdbc:postgresql://127.0.0.1:$port/ontology"
i=0
until docker exec "$container" pg_isready -h 127.0.0.1 -U postgres -d ontology > /dev/null 2>&1; do i=$((i+1)); [ "$i" -lt 120 ]; sleep 0.25; done
docker exec -e "PGPASSWORD=$DB_PASSWORD" "$container" psql -h 127.0.0.1 -U postgres -d ontology -Atc 'SELECT version()' > "$evidence/postgres-version.txt"
# The starting instant is a placeholder: every installed fixture resets the clock to its own asOf (fixture-start).
java -jar "$fixture/ontology.jar" --spring.profiles.active=local,verification --mulino.verification.instant=2026-10-01T00:00:00Z --server.address=127.0.0.1 --server.port=0 "--mulino.evidence.blob-root=$fixture/blobs" > "$evidence/backend-runtime.log" 2>&1 &
server_pid=$!
i=0
while :; do
  kill -0 "$server_pid"
  http_port=$(python3 - "$evidence/backend-runtime.log" <<'PY'
import re,sys
s=open(sys.argv[1]).read();m=re.findall(r'Tomcat started on port (\d+)',s);print(m[-1] if m else '')
PY
)
  if [ -n "$http_port" ]; then
    status=$(curl --max-time 2 -s -o /dev/null -w '%{http_code}' "http://127.0.0.1:$http_port/api/ontology/queries/getInventory" || true)
    [ "$status" != 401 ] || break
  fi
  i=$((i+1)); [ "$i" -lt 480 ]; sleep 0.25
done
export ACTUAL_BASE_URL="http://127.0.0.1:$http_port" ACTUAL_DISPOSABLE_DATABASE=true
ACTUAL_BUILD_COMMIT=$(git rev-parse HEAD)
export ACTUAL_BUILD_COMMIT
classpath="$repo/verification/harness/target/classes:$(cat verification/harness/target/classpath.txt)"
argv_json=$(python3 -c 'import json,sys; print(json.dumps(["./verify","scenarios","--actual"]+sys.argv[1:]))' "$@")
rm -f verification/harness/target/evidence/scenarios.json
set +e
java -Dverification.driver=actual -Dverification.agentRunner=scripted -Dverification.actual.suiteIsolation=true "-Dverification.actual.partialFixtures=${ACTUAL_PARTIAL_FIXTURES:-true}" "-Dverification.actual.relaxWire=${ACTUAL_RELAX_WIRE:-false}" \
  -Dverification.actual.identityBinding=verification/actual/scenarios/identity-binding.json "-Dverification.actual.blobRoot=$fixture/blobs" \
  "-Drepo.root=$repo" '-Dverification.command=./verify scenarios --actual' "-Dverification.argv=$argv_json" \
  -cp "$classpath" org.mulino.verification.Main profile scenarios "$@" > "$evidence/harness-stdout.json" 2> "$evidence/harness-stderr.txt"
code=$?
set -e
cp verification/harness/target/evidence/scenarios.json "$evidence/scenarios.json" 2>/dev/null || true
exit "$code"
