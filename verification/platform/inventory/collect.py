#!/usr/bin/env python3
"""S0 보존 metadata만 수집한다. Legacy blob 내용은 열지 않는다."""
import hashlib
import json
from pathlib import Path
import subprocess
from datetime import datetime, timezone

ROOT = Path(__file__).resolve().parents[3]
OUT = ROOT / "verification/platform/inventory"
ORIGINAL = Path("/Volumes/VideoStore/Developer/mulino-coreano-erp")
BASELINE = "4280a750dca0aef35abaf10b6797881181dd88d7"
INITIAL = "847758afa6cb58e2a4c661e578773f1531098ee8"
commands = []


def command(args, cwd=ROOT, stdin=None, required=True):
    result = subprocess.run(args, cwd=cwd, input=stdin, text=True,
                            capture_output=True, check=False)
    commands.append({"argv": args, "cwd": str(cwd),
                     "exitCode": result.returncode,
                     "stdout": result.stdout, "stderr": result.stderr})
    if required and result.returncode:
        raise RuntimeError(f"metadata command failed: {args}")
    return result


def git(*args, cwd=ROOT):
    return command(["git", *args], cwd=cwd).stdout.strip()


def tree(commit, cwd=ROOT):
    raw = git("ls-tree", "-rz", commit, cwd=cwd)
    rows = []
    for item in raw.split("\0"):
        if not item:
            continue
        metadata, path = item.split("\t", 1)
        mode, kind, oid = metadata.split()
        rows.append({"path": path, "mode": mode, "type": kind, "hash": oid})
    return rows


def category(path):
    if path in ("AGENTS.md", "CLAUDE.md"):
        return "workspace-guidance"
    if path == "README.md" or "startup" in path.lower():
        return "readme-startup"
    if path.startswith(".github/"):
        return "templates-ci"
    if path.startswith((".agents/", ".claude/", "agents/skills/")):
        return "skills-links"
    if path.startswith("agents/cli/"):
        return "agents-cli"
    if path.startswith("agents/"):
        return "agents-runtime"
    if path.startswith("docs/"):
        return "documentation"
    if "test" in path.lower() or path.startswith("verification/"):
        return "tests"
    return path.split("/", 1)[0] if "/" in path else "root-other"


def replacement(path):
    if path in ("AGENTS.md", "CLAUDE.md", "README.md"):
        return path
    if path.startswith(".github/ISSUE_TEMPLATE/"):
        return path
    if path == ".github/pull_request_template.md":
        return path
    c = category(path)
    return {"templates-ci": ".github/workflows/",
            "skills-links": ".agents/skills/",
            "agents-cli": "backend/adapters/mcp/",
            "agents-runtime": "agents/",
            "tests": "verification/",
            "backend": "backend/",
            "database": "backend/platform/migrations/",
            "mcp-server": "backend/adapters/mcp/",
            "governance": "backend/domain/governance/",
            "dashboard": "dashboard/"}.get(c)


def write(name, data):
    (OUT / name).write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n")


def main():
    timestamp = datetime.now(timezone.utc).isoformat()
    assert git("rev-parse", "step2-complete") == BASELINE
    baseline_rows = tree(BASELINE)
    baseline_by_path = {r["path"]: r for r in baseline_rows}
    initial_docs = [r for r in tree(INITIAL) if r["path"].startswith("docs/")]
    assert len(initial_docs) == 7
    preserved = []
    for row in initial_docs:
        path = row["path"]
        assert baseline_by_path[path]["hash"] == row["hash"]
        # These are the seven permitted ontology documents, never legacy blobs.
        sha256 = hashlib.sha256((ROOT / path).read_bytes()).hexdigest()
        assert git("hash-object", "--", path) == row["hash"]
        preserved.append({"path": path, "gitBlobHash": row["hash"],
                          "sha256": sha256, "status": "PASS"})
    plan = next(r for r in preserved if r["path"] == "docs/ontology-implementation-plan.md")
    assert plan["sha256"] == "7f010093b1fba3673f674c78dddc8c407512e037831f093babc87d7421a22a00"

    fork_refs = git("for-each-ref", "--format=%(refname) %(objectname)",
                    "refs/heads/main", "refs/remotes/origin", "refs/tags")
    fork_live = git("ls-remote", "--symref", "origin", "HEAD",
                    "refs/heads/main", "refs/heads/integration/merged-prs",
                    "refs/heads/integration/assumed-merged-completion-20261006")
    original_refs = git("for-each-ref", "--format=%(refname) %(objectname)",
                        "refs/heads/main", "refs/remotes/origin/main", cwd=ORIGINAL)
    original_live = git("ls-remote", "--symref", "origin", "HEAD",
                        "refs/heads/main", cwd=ORIGINAL)
    anchors = [
        {"ref": "refs/remotes/origin/integration/merged-prs", "cwd": ROOT},
        {"ref": "refs/remotes/origin/main", "cwd": ROOT},
        {"ref": "refs/remotes/origin/integration/assumed-merged-completion-20261006", "cwd": ROOT},
        {"ref": "HEAD", "cwd": ORIGINAL},
    ]
    legacy, recovery = [], []
    for anchor in anchors:
        cwd, ref = anchor["cwd"], anchor["ref"]
        commit = git("rev-parse", ref, cwd=cwd)
        assert git("cat-file", "-t", commit, cwd=cwd) == "commit"
        rows = tree(commit, cwd=cwd)
        oids = sorted({r["hash"] for r in rows})
        check = command(["git", "cat-file", "--batch-check=%(objectname) %(objecttype) %(objectsize)"],
                        cwd=cwd, stdin="\n".join(oids) + "\n").stdout
        assert "missing" not in check and len(check.splitlines()) == len(oids)
        assert all(line.split()[1] in ("blob", "commit") for line in check.splitlines())
        recovery.append({"repository": str(cwd), "archiveRef": ref,
                         "archiveCommit": commit,
                         "treeHash": git("rev-parse", f"{commit}^{{tree}}", cwd=cwd),
                         "entryCount": len(rows), "objectsPresent": len(oids),
                         "status": "PASS", "scope": "reachable-tree-and-objects-only",
                         "checkoutOrContentRestorePerformed": False})
        for row in rows:
            target = replacement(row["path"])
            legacy.append({"path": row["path"], "currentHash": row["hash"],
                           "hashScope": "archive-tree-git-object", "mode": row["mode"],
                           "category": category(row["path"]),
                           "classification": "REPLACE" if target else "HISTORY_ARCHIVE",
                           "replacementPath": target, "replacementStatus": "INTENDED_MAPPING_NOT_REUSE",
                           "archiveRef": ref, "archiveCommit": commit,
                           "archiveRepository": str(cwd), "owner": "coordinator/data owner",
                           "newBaselineHashAtSamePath": baseline_by_path.get(row["path"], {}).get("hash"),
                           "validation": "PASS_TREE_OBJECT_PRESENT_CONTENT_NOT_READ"})
    new = [{"path": r["path"], "currentHash": r["hash"],
            "hashScope": "step2-baseline-git-object", "category": category(r["path"]),
            "classification": "KEEP", "replacementPath": r["path"],
            "archiveRef": "refs/tags/step2-complete", "archiveCommit": BASELINE,
            "owner": "coordinator/current module owner",
            "validation": "TRACKED_AT_BASELINE_NOT_RUNTIME_ACCEPTANCE"}
           for r in baseline_rows]
    docker = command(["docker", "ps", "-a", "--format", "{{.ID}}\t{{.Names}}\t{{.Image}}\t{{.Status}}"], required=False)
    # Inspect only a known project container; never dump Config/Env or private files.
    project_containers = []
    if docker.returncode == 0:
        for line in docker.stdout.splitlines():
            parts = line.split("\t", 3)
            if len(parts) == 4 and parts[1] == "mulino-scenario-pg":
                mount = command(["docker", "inspect", parts[1], "--format", "{{json .Mounts}}"], required=False)
                mounts = json.loads(mount.stdout) if mount.returncode == 0 else []
                project_containers.append({"id": parts[0], "name": parts[1],
                                           "image": parts[2], "state": parts[3], "mounts": mounts,
                                           "authoritative": "UNKNOWN", "contentsRead": False})
                for item in mounts:
                    if item.get("Type") == "volume":
                        volume = command(["docker", "volume", "inspect", item["Name"],
                                          "--format", "{{json .}}"], required=False)
                        project_containers[-1].setdefault("volumeMetadata", []).append(
                            json.loads(volume.stdout) if volume.returncode == 0 else
                            {"name": item["Name"], "metadataStatus": "UNKNOWN"})
    # Keep unrelated container names out of the committed evidence.
    for entry in commands:
        if entry["argv"][:3] == ["docker", "ps", "-a"]:
            entry["stdout"] = "\n".join(line for line in entry["stdout"].splitlines()
                                         if "\tmulino-scenario-pg\t" in line) + "\n"
            entry["scopeNote"] = "unrelated container metadata omitted"
    resources = [{"resource": resource, "presence": "NONE", "owner": "data owner/user",
                  "basis": "USER_DECLARATION", "declaredDate": "2026-10-08",
                  "declaredTimezone": "Asia/Seoul",
                  "declaration": "실제 운영 자료 없음 — 새 DB와 개발 fixture로 진행",
                  "evidenceRefs": ["verification/platform/inventory/resources.json", "verification/platform/inventory/commands.json"],
                  "missingInput": None,
                  "allowedScope": "fresh isolated development with synthetic ontology fixtures",
                  "blockedStep": None,
                  "validation": "PASS_USER_CONFIRMED_FRESH_BRANCH_NOT_LIVE_RESOURCE_QUERY"}
                 for resource, missing in [
                     ("authoritative-db", "authoritative resource identifier and authorized read-only existence/snapshot inventory"),
                     ("original-evidence-blob", "authoritative original evidence store identifier and owner inventory"),
                     ("pending-external-effects", "authoritative pending/unknown external effect register and owner"),
                     ("unresolved-work-obligations", "authoritative open obligation/intake responsibility register and owner")]]
    write("files.json", {"schemaVersion": 1, "capturedAt": timestamp,
                        "baseline": BASELINE, "legacy": legacy, "newBaseline": new,
                        "hashAlgorithm": "git-sha1", "selfReferentialArtifactHashesExcluded": True})
    write("preserved-documents.json", {"capturedAt": timestamp, "baseline": INITIAL, "documents": preserved})
    write("archive-recovery.json", {"capturedAt": timestamp, "archives": recovery,
                                   "limitation": "No legacy checkout/content inspection or DB/blob restore. Remote tracking refs are mutable; archiveCommit pins this observation."})
    write("git-metadata.json", {"capturedAt": timestamp, "stepBaseline": BASELINE,
                              "fork": {"origin": git("remote", "get-url", "origin"),
                                       "localMain": git("rev-parse", "refs/heads/main"),
                                       "localRefs": fork_refs, "remoteReadOnlyObservation": fork_live},
                              "original": {"origin": git("remote", "get-url", "origin", cwd=ORIGINAL),
                                           "localMain": git("rev-parse", "refs/heads/main", cwd=ORIGINAL),
                                           "checkoutHead": git("rev-parse", "HEAD", cwd=ORIGINAL),
                                           "localRefs": original_refs, "remoteReadOnlyObservation": original_live},
                              "worktrees": git("worktree", "list", "--porcelain")})
    write("resources.json", {"capturedAt": timestamp, "dockerMetadataAvailable": docker.returncode == 0,
                           "knownProjectContainers": project_containers,
                           "resources": resources, "status": "USER_CONFIRMED_NONE",
                           "R3": {"status": "CONFIRMED", "owner": "data owner/user",
                                  "inputRefs": ["2026-10-08 Asia/Seoul user declaration via coordinator"],
                                  "proposedValue": "fresh isolated DB and synthetic ontology fixtures",
                                  "confirmedValue": "no actual operating data; fresh isolated DB and development fixtures",
                                  "decidedAt": "2026-10-08 Asia/Seoul (exact time not supplied)",
                                  "blockedStep": None, "evidencePath": "verification/platform/inventory/resources.json"},
                           "declarationScope": "actual operating data, blobs, pending external effects and open operating obligations",
                           "legacyLiveMigration": "NOT_APPLICABLE_USER_DECLARATION",
                           "newOntologyV1V2BackupRestore": "MANDATORY_NOT_RUN",
                           "existingVolumeDisposition": "PRESERVE_NO_DELETION_AUTHORIZED",
                           "noDatabaseQueryNoConfigReadNoMutation": True})
    owned = []
    for folder in (ROOT / "docs/execution/s0-inventory", OUT):
        for path in sorted(folder.rglob("*")):
            if path.is_file() and path.suffix not in (".json", ".jsonl"):
                owned.append({"path": str(path.relative_to(ROOT)),
                              "currentHash": hashlib.sha256(path.read_bytes()).hexdigest(),
                              "hashScope": "owned-file-sha256", "classification": "KEEP",
                              "replacementPath": str(path.relative_to(ROOT)),
                              "archiveRef": "worker commit supplied in handoff", "owner": "S0 inventory worker",
                              "validation": "PASS_CONTENT_DIGEST"})
    write("owned-files.json", {"files": owned,
                              "generatedJsonIntegrity": "check.py parses and verifies relational evidence; JSON self-digests excluded"})
    write("commands.json", {"capturedAt": timestamp, "commands": commands})
    print(json.dumps({"status": "PASS", "legacyEntries": len(legacy), "baselineEntries": len(new),
                      "preservedDocuments": len(preserved), "archives": len(recovery),
                      "authoritativeResources": "NONE_USER_DECLARATION"}))


if __name__ == "__main__":
    main()
