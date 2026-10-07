#!/bin/sh
# Runs only a copied, already-built backend and owns exactly its created resources.
set -eu
umask 077
repo=$(CDPATH= cd -- "$(dirname -- "$0")/../../.." && pwd)
cd "$repo"
run_id=$(python3 -c 'import uuid; print(uuid.uuid4())')
evidence=${1:-"$repo/verification/harness/target/evidence/actual-s1-run-$run_id"}
# A fresh directory prevents overwriting receipts from an earlier attempt.
mkdir -p "$(dirname -- "$evidence")"
mkdir "$evidence"
evidence=$(CDPATH= cd -- "$evidence" && pwd)
fixture=$(mktemp -d /tmp/mulino-s1-native.XXXXXX)
container="mulino-s1-native-$run_id"
container_created=false
server_pid=
phase=source-custody
receipt_started=false
cleanup() {
  code=$?
  trap - EXIT HUP INT TERM
  set +e
  backend_stopped=true
  container_removed=true
  keys_removed=true
  if [ -n "$server_pid" ]; then
    kill "$server_pid" 2>/dev/null
    i=0
    while kill -0 "$server_pid" 2>/dev/null && [ "$i" -lt 40 ]; do sleep 0.25; i=$((i+1)); done
    kill -KILL "$server_pid" 2>/dev/null
    wait "$server_pid" 2>/dev/null
    if kill -0 "$server_pid" 2>/dev/null; then backend_stopped=false; fi
  fi
  if [ "$container_created" = true ]; then
    docker logs "$container" > "$evidence/postgres-runtime.log" 2>&1
    docker rm -f -v "$container" > "$evidence/container-cleanup.txt" 2>&1 || container_removed=false
  fi
  python3 - "$fixture" <<'PY_CLEANUP' || keys_removed=false
import pathlib,shutil,sys
p=pathlib.Path(sys.argv[1]);parent=pathlib.Path('/tmp').resolve()
if p.resolve().parent!=parent or not p.name.startswith('mulino-s1-native.'):
    raise SystemExit('Refusing cleanup outside the created fixture directory')
shutil.rmtree(p)
PY_CLEANUP
  if [ "$receipt_started" = true ]; then
    python3 verification/actual/s1/receipt.py finish "$repo" "$evidence" "$code" "$phase" "$backend_stopped" "$container_removed" "$keys_removed"
    code=$?
  fi
  printf 'S1 disposable receipt: %s\n' "$evidence/run-receipt.json"
  exit "$code"
}
trap cleanup EXIT
trap 'exit 3' HUP INT TERM
[ -f backend/target/ontology-0.1.0-SNAPSHOT.jar ]
[ -f verification/harness/target/classpath.txt ]
[ -f verification/harness/target/classes/org/mulino/verification/actual/NativeS1ReadMain.class ]
receipt_started=true
python3 verification/actual/s1/receipt.py start "$repo" "$evidence"
cp backend/target/ontology-0.1.0-SNAPSHOT.jar "$fixture/ontology.jar"
python3 - "$fixture/ontology.jar" "$evidence/run-receipt.json" <<'PY_JAR'
import hashlib,json,pathlib,sys
p=pathlib.Path(sys.argv[2]);r=json.loads(p.read_text());actual=hashlib.sha256(pathlib.Path(sys.argv[1]).read_bytes()).hexdigest()
if actual!=r['executedJarSha256']:raise SystemExit('JAR drifted while copied')
r['executedJarSha256']=actual;r['executedCopiedJar']=True;p.write_text(json.dumps(r,indent=2)+'\n')
PY_JAR
image=$(python3 -c 'import json; print(json.load(open("verification/platform/versions.json"))["postgresImage"]["reference"])')
case "$image" in postgres@sha256:*) ;; *) exit 3 ;; esac
printf '%s\n' "$image" > "$evidence/postgres-image-reference.txt"
phase=ephemeral-identity
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out "$fixture/private.pem" 2> "$fixture/keygen.log"
openssl pkey -in "$fixture/private.pem" -pubout -out "$fixture/public.pem" 2> "$fixture/pubkey.log"
export JWT_PRIVATE_KEY="$fixture/private.pem" JWT_PUBLIC_KEY="$fixture/public.pem"
export JWT_ISSUER=synthetic-fixture-issuer JWT_AUDIENCE=isolated-ontology
export DB_USERNAME=postgres DB_PASSWORD
DB_PASSWORD=$(python3 -c 'import secrets; print(secrets.token_urlsafe(24))')
phase=disposable-postgres
# Credentials stay out of receipts; no existing container/volume is inspected or altered.
docker run -d --name "$container" -e "POSTGRES_PASSWORD=$DB_PASSWORD" -e POSTGRES_DB=ontology -p 127.0.0.1::5432 "$image" > "$evidence/container-id.txt"
container_created=true
port=$(docker port "$container" 5432/tcp | sed 's/.*://')
export DB_URL="jdbc:postgresql://127.0.0.1:$port/ontology"
i=0
until docker exec "$container" pg_isready -U postgres -d ontology > /dev/null 2>&1; do i=$((i+1)); [ "$i" -lt 120 ]; sleep 0.25; done
docker inspect --format '{{.Image}}' "$container" > "$evidence/postgres-image-id.txt"
docker exec "$container" psql -U postgres -d ontology -Atc 'SELECT version()' > "$evidence/postgres-version.txt"
phase=ephemeral-backend
java -jar "$fixture/ontology.jar" --spring.profiles.active=local --server.address=127.0.0.1 --server.port=0 > "$evidence/backend-runtime.log" 2>&1 &
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
printf '%s\n' "$ACTUAL_BASE_URL" > "$evidence/backend-loopback-url.txt"
phase=actual-http-jdbc
classpath="$repo/verification/harness/target/classes:$(cat verification/harness/target/classpath.txt)"
set +e
java "-Drepo.root=$repo" '-Dverification.command=./verify actual-s1' "-Dverification.actual.output=$evidence" -cp "$classpath" org.mulino.verification.actual.NativeS1ReadMain > "$evidence/native-stdout.json" 2> "$evidence/native-stderr.txt"
code=$?
set -e
phase=completed-native-attempt
exit "$code"
