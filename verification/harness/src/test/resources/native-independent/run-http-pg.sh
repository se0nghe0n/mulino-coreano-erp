#!/bin/sh
set -eu
umask 077
repo=$(pwd)
run=$(python3 -c 'import uuid;print(uuid.uuid4())')
resource="$repo/.local/independent-observations/resources-$run"
mkdir "$resource"
evidence="$repo/.local/independent-observations/http-pg-$run"
mkdir "$evidence"
container="mulino-independent-$run"
created=false
pid=
export DOCKER_HOST="unix:///Users/se0nghe0n/.orbstack/run/docker.sock"
cleanup(){
 code=$?
 trap - EXIT
 set +e
 if [ -n "$pid" ];then kill "$pid" 2>/dev/null; wait "$pid" 2>/dev/null;printf '%s\n' "$?" > "$evidence/server-cleanup-exit.txt";fi
 if [ "$created" = true ];then docker logs "$container" > "$evidence/postgres.log" 2>&1;docker rm -fv "$container" > "$evidence/container-cleanup.txt" 2>&1;fi
 python3 - "$resource" <<'PY'
import pathlib,shutil,sys
p=pathlib.Path(sys.argv[1]);assert p.name.startswith('resources-') and p.parent.name=='independent-observations';shutil.rmtree(p)
PY
 printf '%s\n' "$code" > "$evidence/exit-code.txt"
 printf 'Own evidence: %s exit %s\n' "$evidence" "$code"
 exit "$code"
}
trap cleanup EXIT
export JAVA_HOME=${JAVA_HOME:-/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home}
export PATH="$JAVA_HOME/bin:$repo/.local/independent-observations/toolchain/node_modules/.bin:$PATH"
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out "$resource/private.pem" 2> "$resource/keygen.log"
openssl pkey -in "$resource/private.pem" -pubout -out "$resource/public.pem" 2> "$resource/pubkey.log"
export JWT_PRIVATE_KEY="$resource/private.pem" JWT_PUBLIC_KEY="$resource/public.pem"
export JWT_ISSUER=https://mulino-native.invalid JWT_AUDIENCE=isolated-ontology
export DB_USERNAME=postgres
DB_PASSWORD=$(python3 -c 'import secrets;print(secrets.token_urlsafe(24))')
export DB_PASSWORD
image=$(python3 -c 'import json;print(json.load(open("verification/platform/versions.json"))["postgresImage"]["reference"])')
docker run -d --name "$container" -e "POSTGRES_PASSWORD=$DB_PASSWORD" -e POSTGRES_DB=ontology -p 127.0.0.1::5432 "$image" > "$evidence/container-id.txt"
created=true
port=$(docker port "$container" 5432/tcp | sed 's/.*://')
export DB_URL="jdbc:postgresql://127.0.0.1:$port/ontology"
i=0
until docker exec "$container" pg_isready -U postgres -d ontology >/dev/null 2>&1;do i=$((i+1));[ "$i" -lt 100 ];sleep 0.2;done
java -jar backend/target/ontology-0.1.0-SNAPSHOT.jar --spring.profiles.active=local,verification --mulino.verification.instant=2026-10-07T09:00:00Z --mulino.verification.mcp-capture-enabled=true --server.address=127.0.0.1 --server.port=0 "--mulino.evidence.blob-root=$resource/blobs" > "$evidence/backend.log" 2>&1 &
pid=$!
i=0
while :;do
 kill -0 "$pid"
 http_port=$(python3 - "$evidence/backend.log" <<'PY'
import re,sys
s=open(sys.argv[1]).read();m=re.findall(r'Tomcat started on port (\d+)',s);print(m[-1] if m else '')
PY
)
 if [ -n "$http_port" ];then break;fi
 i=$((i+1));[ "$i" -lt 240 ];sleep 0.25
done
export ACTUAL_BASE_URL="http://127.0.0.1:$http_port" ACTUAL_DISPOSABLE_DATABASE=true INDEPENDENT_OBSERVER_ACTUAL=true
ACTUAL_BUILD_COMMIT=$(git rev-parse HEAD)
export ACTUAL_BUILD_COMMIT
python3 - "$evidence" <<'MANIFEST'
import hashlib,json,pathlib,subprocess,sys
root=pathlib.Path.cwd()
paths=subprocess.check_output(['git','ls-files','-m','-o','--exclude-standard'],text=True).splitlines()
paths += ['backend/target/ontology-0.1.0-SNAPSHOT.jar','backend/package-lock.json','verification/platform/versions.json','verification/model-binding/semantic-paths.json','verification/actual/s2/fixture.json','verification/actual/s3/flow.json','verification/actual/s4/fixture.json','verification/actual/s4/e1-upstream.json','verification/actual/s4/e1-dispatch-authority.json','verification/actual/s4/e1-sales-return.json']
paths += [str(p) for p in pathlib.Path('verification/harness/target/classes/org/mulino/verification').rglob('*.class') if any(x in p.parts for x in ['nativeclient','modelbinding'])]
entries=[]
for name in sorted(set(paths)):
 p=root/name
 if p.is_file():entries.append({'path':name,'sha256':hashlib.sha256(p.read_bytes()).hexdigest(),'sizeBytes':p.stat().st_size})
manifest={'baseline':subprocess.check_output(['git','rev-parse','HEAD'],text=True).strip(),'branch':subprocess.check_output(['git','branch','--show-current'],text=True).strip(),'files':entries,'modelCalls':0,'scenarioRunnerCalls':0,'apiOrBtpCharges':0}
(pathlib.Path(sys.argv[1])/'source-class-fixture-manifest.json').write_text(json.dumps(manifest,indent=2)+'\n')
MANIFEST
java -version > "$evidence/java-version.txt" 2>&1
node --version > "$evidence/node-version.txt"
npm --version > "$evidence/npm-version.txt"
./mvnw -B -ntp -f verification/harness/pom.xml -Dtest=NativeIndependentObservationPostgresTest "-Dverification.actual.blobRoot=$resource/blobs" test > "$evidence/tests.log" 2>&1
