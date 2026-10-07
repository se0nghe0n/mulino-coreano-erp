#!/usr/bin/env python3
"""기록한 보존 hash와 자료 분기 근거를 검증한다. 운영 자료를 읽지 않는다."""
import hashlib
import json
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parents[3]
OUT = ROOT / "verification/platform/inventory"


def read(name):
    return json.loads((OUT / name).read_text())


def main():
    files = read("files.json")
    required = {"currentHash", "classification", "replacementPath", "archiveRef", "owner", "validation"}
    assert files["legacy"] and files["newBaseline"]
    assert all(required <= row.keys() for row in files["legacy"] + files["newBaseline"])
    archives = read("archive-recovery.json")["archives"]
    assert len(archives) == 4
    archive_pairs = {(a["repository"], a["archiveCommit"]) for a in archives}
    for row in files["legacy"]:
        assert (row["archiveRepository"], row["archiveCommit"]) in archive_pairs
    for archive in archives:
        assert archive["status"] == "PASS" and archive["entryCount"] > 0
        probe = subprocess.run(["git", "cat-file", "-t", archive["archiveCommit"]],
                               cwd=archive["repository"], capture_output=True, text=True, check=True)
        assert probe.stdout.strip() == "commit"
    docs = read("preserved-documents.json")["documents"]
    assert len(docs) == 7
    for doc in docs:
        assert hashlib.sha256((ROOT / doc["path"]).read_bytes()).hexdigest() == doc["sha256"]
    for row in read("owned-files.json")["files"]:
        assert hashlib.sha256((ROOT / row["path"]).read_bytes()).hexdigest() == row["currentHash"]
    resources = read("resources.json")
    assert resources["status"] == "USER_CONFIRMED_NONE"
    assert len(resources["resources"]) == 4
    assert all(r["presence"] == "NONE" and r["basis"] == "USER_DECLARATION"
               and r["declaration"] == "실제 운영 자료 없음 — 새 DB와 개발 fixture로 진행"
               for r in resources["resources"])
    assert resources["newOntologyV1V2BackupRestore"] == "MANDATORY_NOT_RUN"
    assert resources["existingVolumeDisposition"] == "PRESERVE_NO_DELETION_AUTHORIZED"
    assert resources["noDatabaseQueryNoConfigReadNoMutation"] is True
    assert all(c["exitCode"] == 0 for c in read("commands.json")["commands"])
    print(json.dumps({"status": "PASS", "checks": ["file-inventory-fields",
                     "archive-commit-reachability", "seven-document-sha256",
                     "owned-file-sha256", "R3-user-declaration-separate-from-docker",
                     "no-legacy-live-migration-new-upgrade-still-required"],
                     "runtimeRestore": "NOT_RUN"}))


if __name__ == "__main__":
    main()
