#!/bin/sh
# This runner owns only disposable resources created below.
set -eu
repo=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
cd "$repo"
evidence=${1:?Provide an evidence directory outside source, e.g. /tmp/mulino-s0-evidence}
mkdir -p "$evidence"
evidence=$(CDPATH= cd -- "$evidence" && pwd)
: "${NODE24_BIN:?Set NODE24_BIN to the verified Node 24.19.0 executable}"
[ "$("$NODE24_BIN" --version)" = v24.19.0 ]
PATH="$(dirname "$NODE24_BIN"):$PATH"
export PATH NODE24_BIN
fixture=$(mktemp -d /tmp/mulino-platform-fixture.XXXXXX)
container="mulino-platform-verify-$$"
server_pid=
container_created=false
cleanup() {
  if [ -n "$server_pid" ]; then kill "$server_pid" 2>/dev/null || true; wait "$server_pid" 2>/dev/null || true; fi
  if [ "$container_created" = true ]; then docker rm -f "$container" >/dev/null 2>&1 || true; fi
  rm -rf "$fixture"
}
trap cleanup EXIT HUP INT TERM
"$NODE24_BIN" verification/platform/security/security-smoke.mjs prepare "$fixture"
export JWT_PUBLIC_KEY="$fixture/public.pem"
export JWT_ISSUER=https://mulino.local.invalid JWT_AUDIENCE=mulino-platform
npm --version > "$evidence/npm-version.txt"
[ "$(cat "$evidence/npm-version.txt")" = 11.19.1 ]
(cd backend && npm ci --ignore-scripts --no-audit --no-fund) > "$evidence/npm-ci.log" 2>&1
(cd backend && ../mvnw -B -ntp test package) > "$evidence/integration.log" 2>&1
shasum -a 256 backend/target/ontology-0.1.0-SNAPSHOT.jar > "$evidence/executed-jar.sha256"
python3 - "$evidence" <<'PY'
from pathlib import Path
import hashlib,json,sys
files=sorted(p for p in Path('backend').rglob('*') if p.is_file() and not any(part in ('target','node_modules','edmx') for part in p.parts))
rows={str(p):hashlib.sha256(p.read_bytes()).hexdigest() for p in files}
Path(sys.argv[1],'backend-input-hashes.json').write_text(json.dumps(rows,indent=2)+'\n')
PY
docker run -d --name "$container" -e POSTGRES_PASSWORD=local-fixture-only -e POSTGRES_DB=ontology -p 127.0.0.1::5432 postgres@sha256:4ef4dbc939d61acea57712655ddb4b4ab27419c913f94cca0cd57cb3ea3c2280 > "$evidence/container-id.txt"
container_created=true
port=$(docker port "$container" 5432/tcp | sed 's/.*://')
export DB_URL="jdbc:postgresql://127.0.0.1:$port/ontology" DB_USERNAME=postgres DB_PASSWORD=local-fixture-only
java -jar backend/target/ontology-0.1.0-SNAPSHOT.jar --spring.profiles.active=local > "$evidence/runtime.log" 2>&1 &
server_pid=$!
i=0
while [ "$(curl -s -o /dev/null -w '%{http_code}' http://localhost:8080/api/platform/scopes/00000000-0000-0000-0000-000000000001 || true)" != 401 ]; do
  kill -0 "$server_pid"
  i=$((i+1)); [ "$i" -lt 100 ]
  sleep 0.1
done
docker exec -i "$container" psql -U postgres -d ontology < deploy/local/platform-fixture.sql > "$evidence/seed.txt"
"$NODE24_BIN" verification/platform/security/create-local-config.mjs "$fixture/tokens.json" "$fixture/config.json"
"$NODE24_BIN" verification/platform/security/security-smoke.mjs run "$fixture/config.json" "$evidence/security.json"
docker exec "$container" psql -U postgres -d ontology -c 'SELECT reserved,revision,(SELECT count(*) FROM mulino_platform_audit) audit,(SELECT count(*) FROM mulino_platform_outbox) outbox,(SELECT count(*) FROM mulino_platform_idempotency) idem FROM mulino_platform_scopes' > "$evidence/security-db.txt"
docker exec "$container" psql -U postgres -d ontology -c 'TRUNCATE mulino_platform_idempotency,mulino_platform_audit,mulino_platform_outbox,mulino_platform_restrictions; UPDATE mulino_platform_scopes SET reserved=0,revision=0;' > "$evidence/reset.txt"
MCP_TOKEN=$("$NODE24_BIN" -e 'const fs=require("fs"); process.stdout.write(JSON.parse(fs.readFileSync(process.argv[1],"utf8")).writer)' "$fixture/tokens.json")
export MCP_TOKEN
python3 verification/platform/protocol/wire_probe.py --scope-id 00000000-0000-0000-0000-000000000001 --output "$evidence/protocol.json"
unset MCP_TOKEN
docker exec "$container" psql -U postgres -d ontology -c 'SELECT reserved,revision,(SELECT count(*) FROM mulino_platform_audit) audit,(SELECT count(*) FROM mulino_platform_outbox) outbox,(SELECT count(*) FROM mulino_platform_idempotency) idem FROM mulino_platform_scopes' > "$evidence/protocol-db.txt"
printf 'S0 deterministic platform checks finished. Evidence: %s\n' "$evidence"
