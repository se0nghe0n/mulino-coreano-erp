#!/usr/bin/env python3
"""Regression checks for the committed M01-M60 installation input archive.

Run from the repository root with:
  python3 -m unittest verification.model-installation.v1.test_archive_contract_pins -v
or directly with:
  python3 verification/model-installation/v1/test_archive_contract_pins.py
"""

import copy
import gzip
import hashlib
import io
import json
import pathlib
import tarfile
import unittest


ROOT = pathlib.Path(__file__).resolve().parents[3]
HERE = pathlib.Path(__file__).resolve().parent
CONTRACT_PATH = ROOT / "contracts/acceptance-capabilities.json"
CONVENTIONS_PATH = ROOT / "verification/actual/scenarios/installer-conventions.json"
ARCHIVE_PATH = HERE / "installation-inputs.tar.gz"
INDEX_PATH = HERE / "generated-input-index.json"
LOCK_PATH = HERE / "original-input-lock.json"
PREFIX = "verification/model-installation/v1/"
EXPECTED_IDS = tuple(f"M{i:02d}" for i in range(1, 61))
FLAGS = (
    "originalInputProvided",
    "originalAuthorityClaimed",
    "operatingApprovalClaimed",
)


def sha_bytes(value: bytes) -> str:
    return hashlib.sha256(value).hexdigest()


def sha_file(path: pathlib.Path) -> str:
    return sha_bytes(path.read_bytes())


def _load_json(raw: bytes, label: str):
    try:
        return json.loads(raw)
    except (UnicodeDecodeError, json.JSONDecodeError) as exc:
        raise ValueError(f"invalid JSON: {label}") from exc


def _expected_fixture_hashes(lock_bytes: bytes):
    lock = _load_json(lock_bytes, "original-input-lock.json")
    if lock.get("baseline") != "a7f18cc8e7124b9fd1cc8bf811687ed7230a4981":
        raise ValueError("unexpected original fixture baseline")
    artifacts = lock.get("artifacts")
    if not isinstance(artifacts, list) or len(artifacts) != 218:
        raise ValueError("original-input-lock must retain all 218 artifacts")
    result = {}
    for cid in EXPECTED_IDS:
        path = f"verification/model-binding/cases/{cid}/fixture.json"
        matches = [row for row in artifacts if row.get("path") == path]
        if len(matches) != 1:
            raise ValueError(f"original fixture lock missing or duplicated: {cid}")
        result[cid] = matches[0]["sha256"]
    return result


def validate_archive(archive_bytes: bytes, index_bytes: bytes, lock_bytes: bytes,
                     current_contract_sha: str, current_template_sha: str,
                     archive_sources=None):
    """Validate the full package against the live contracts and original lock.

    archive_sources is optional because generated/ is intentionally ignored and
    absent from a clean checkout. When present, every archive source must match
    the regenerated source bytes exactly.
    """
    index = _load_json(index_bytes, "generated-input-index.json")
    if index.get("schemaVersion") != "model-installation-input-index-v1":
        raise ValueError("unexpected generated input index schema")
    if index.get("fixtureCount") != 60:
        raise ValueError("generated input index must declare all 60 fixtures")
    if index.get("archiveSha256") != sha_bytes(archive_bytes):
        raise ValueError("archive SHA-256 does not match index")

    expected_fixtures = _expected_fixture_hashes(lock_bytes)
    try:
        with tarfile.open(fileobj=io.BytesIO(archive_bytes), mode="r:gz") as tar:
            members = tar.getmembers()
            names = [member.name for member in members]
            if len(names) != len(set(names)):
                raise ValueError("archive contains duplicate paths")
            if any(not member.isfile() for member in members):
                raise ValueError("archive must contain only regular files")
            if any(not name.startswith(PREFIX) for name in names):
                raise ValueError("archive contains an out-of-scope path")
            contents = {}
            for member in members:
                stream = tar.extractfile(member)
                if stream is None:
                    raise ValueError(f"unreadable archive member: {member.name}")
                contents[member.name] = stream.read()
    except (tarfile.TarError, OSError) as exc:
        raise ValueError("installation archive is corrupt") from exc

    expected_names = {f"{PREFIX}inputs/{cid}-isolated.json" for cid in EXPECTED_IDS}
    actual_inputs = {name for name in contents if name.startswith(f"{PREFIX}inputs/")}
    if actual_inputs != expected_names:
        missing = sorted(expected_names - actual_inputs)
        extra = sorted(actual_inputs - expected_names)
        raise ValueError(f"archive input set mismatch (missing={missing}, extra={extra})")

    rows = index.get("inputs")
    if not isinstance(rows, list) or len(rows) != 60:
        raise ValueError("index must contain exactly 60 input rows")
    row_ids = [row.get("fixtureId") for row in rows]
    expected_row_ids = [f"{cid}-isolated" for cid in EXPECTED_IDS]
    if row_ids != expected_row_ids:
        raise ValueError("index input rows must cover M01-M60 exactly once in order")

    for cid, row in zip(EXPECTED_IDS, rows):
        name = f"{PREFIX}inputs/{cid}-isolated.json"
        raw = contents[name]
        item = _load_json(raw, name)
        if row.get("inputSha256") != sha_bytes(raw):
            raise ValueError(f"input SHA-256 does not match index: {cid}")
        if item.get("schemaVersion") != "model-installation-inputs-v1":
            raise ValueError(f"unexpected input schema: {cid}")
        if item.get("fixtureSha256") != expected_fixtures[cid] or row.get("fixtureSha256") != expected_fixtures[cid]:
            raise ValueError(f"original fixture pin drift: {cid}")
        if item.get("authorityCorrection") != {
            "version": "model-authority-correction-v1",
            "capabilityContractSha256": current_contract_sha,
            "supplementalRead": True,
        }:
            raise ValueError(f"stale or malformed authority contract pin: {cid}")
        if any(item.get(flag) is not False for flag in FLAGS):
            raise ValueError(f"synthetic input flags must remain false: {cid}")
        artifacts = item.get("templateArtifacts")
        expected_artifacts = [
            {"path": str(CONVENTIONS_PATH.relative_to(ROOT)), "sha256": current_template_sha},
            {"path": "contracts/acceptance-capabilities.json", "sha256": current_contract_sha},
        ]
        if artifacts != expected_artifacts:
            raise ValueError(f"stale, missing, or unexpected template artifact pin: {cid}")
        if row.get("recordCount") != len(item.get("records", [])):
            raise ValueError(f"record count mismatch: {cid}")

    source_names = set(contents) - expected_names
    if any(not name.startswith(f"{PREFIX}generated/M") or not name.endswith(".json") for name in source_names):
        raise ValueError("archive contains missing or unexpected generated source paths")
    if index.get("generatedSourceCount") != len(source_names):
        raise ValueError("generated source count does not match archive")
    if not source_names:
        raise ValueError("archive is missing generated source originals")
    if archive_sources is not None:
        if set(archive_sources) != source_names:
            raise ValueError("archive generated source set differs from regenerated sources")
        for name, raw in archive_sources.items():
            if contents[name] != raw:
                raise ValueError(f"generated source bytes changed: {name}")
    return contents, index


def _repackage(contents):
    raw_tar = io.BytesIO()
    with tarfile.open(fileobj=raw_tar, mode="w") as tar:
        for name in sorted(contents):
            data = contents[name]
            info = tarfile.TarInfo(name)
            info.size = len(data)
            info.mode = 0o444
            info.mtime = 0
            tar.addfile(info, io.BytesIO(data))
    return gzip.compress(raw_tar.getvalue(), mtime=0)


class ModelInstallationArchivePinsTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.archive = ARCHIVE_PATH.read_bytes()
        cls.index = json.loads(INDEX_PATH.read_text())
        cls.index_bytes = INDEX_PATH.read_bytes()
        cls.lock_bytes = LOCK_PATH.read_bytes()
        cls.contract_sha = sha_file(CONTRACT_PATH)
        cls.template_sha = sha_file(CONVENTIONS_PATH)
        generated_dir = HERE / "generated"
        cls.regenerated_sources = None
        if generated_dir.exists():
            cls.regenerated_sources = {
                f"{PREFIX}{path.relative_to(HERE).as_posix()}": path.read_bytes()
                for path in generated_dir.rglob("*.json")
            }

    def assert_valid(self, archive=None, index=None):
        archive = self.archive if archive is None else archive
        index = self.index_bytes if index is None else index
        return validate_archive(
            archive, index, self.lock_bytes, self.contract_sha, self.template_sha,
            self.regenerated_sources,
        )

    def rewrite_package(self, mutate_input):
        contents, _ = self.assert_valid()
        name = f"{PREFIX}inputs/M01-isolated.json"
        item = json.loads(contents[name])
        mutate_input(item)
        contents[name] = json.dumps(item, ensure_ascii=False, indent=2).encode() + b"\n"
        altered = _repackage(contents)
        index = copy.deepcopy(self.index)
        index["archiveSha256"] = sha_bytes(altered)
        index["inputs"][0]["inputSha256"] = sha_bytes(contents[name])
        return altered, json.dumps(index).encode()

    def test_committed_archive_and_index_pin_all_current_inputs(self):
        contents, index = self.assert_valid()
        self.assertEqual(index["fixtureCount"], 60)
        self.assertEqual(len([n for n in contents if "/inputs/" in n]), 60)
        self.assertEqual(index["archiveSha256"], sha_bytes(self.archive))

    def test_original_218_artifact_lock_still_matches_checkout(self):
        lock = json.loads(self.lock_bytes)
        self.assertEqual(len(lock["artifacts"]), 218)
        for artifact in lock["artifacts"]:
            path = ROOT / artifact["path"]
            self.assertTrue(path.is_file(), artifact["path"])
            self.assertEqual(path.stat().st_size, artifact["sizeBytes"], artifact["path"])
            self.assertEqual(sha_file(path), artifact["sha256"], artifact["path"])

    def test_stale_authority_pin_is_rejected_even_if_all_hashes_are_updated(self):
        stale = "dd4987f1ce7050dd510da42e4ab4cf5706d19413e6181bbc29924a251505c3a4"
        altered, index = self.rewrite_package(
            lambda item: item["authorityCorrection"].update(capabilityContractSha256=stale)
        )
        with self.assertRaisesRegex(ValueError, "stale or malformed authority contract pin"):
            self.assert_valid(altered, index)

    def test_stale_template_pin_is_rejected_even_if_all_hashes_are_updated(self):
        stale = "dd4987f1ce7050dd510da42e4ab4cf5706d19413e6181bbc29924a251505c3a4"
        altered, index = self.rewrite_package(
            lambda item: item["templateArtifacts"][1].update(sha256=stale)
        )
        with self.assertRaisesRegex(ValueError, "stale, missing, or unexpected template artifact pin"):
            self.assert_valid(altered, index)

    def test_missing_fixture_is_rejected_even_if_archive_hash_is_rewritten(self):
        contents, _ = self.assert_valid()
        contents.pop(f"{PREFIX}inputs/M60-isolated.json")
        altered = _repackage(contents)
        index = copy.deepcopy(self.index)
        index["archiveSha256"] = sha_bytes(altered)
        with self.assertRaisesRegex(ValueError, "archive input set mismatch"):
            self.assert_valid(altered, json.dumps(index).encode())

    def test_corrupt_input_is_rejected_when_index_is_rewritten(self):
        contents, _ = self.assert_valid()
        name = f"{PREFIX}inputs/M01-isolated.json"
        contents[name] = b"{not-json"
        altered = _repackage(contents)
        index = copy.deepcopy(self.index)
        index["archiveSha256"] = sha_bytes(altered)
        index["inputs"][0]["inputSha256"] = sha_bytes(contents[name])
        with self.assertRaisesRegex(ValueError, "invalid JSON"):
            self.assert_valid(altered, json.dumps(index).encode())


if __name__ == "__main__":
    unittest.main(verbosity=2)
