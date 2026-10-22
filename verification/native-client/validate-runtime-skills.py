#!/usr/bin/env python3
"""Validate new runtime packages and direct discovery links without invoking a model."""
import argparse
import hashlib
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
MANIFEST = ROOT / 'agents/runtime-package-manifest.json'
NAMES = ['ontology-' + name for name in (
    'work-coordinator', 'procurement-transport', 'import-qc',
    'sales-returns-recall', 'settlement', 'definition-authoring')]


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def digest(value):
    return hashlib.sha256(json.dumps(value, ensure_ascii=False,
                                    separators=(',', ':')).encode()).hexdigest()


def validate(refresh=False):
    manifest = json.loads(MANIFEST.read_text())
    assert [p['name'] for p in manifest['packages']] == NAMES, 'PACKAGE_SET_CHANGED'
    slots = json.loads((ROOT / 'contracts/capability-slots.json').read_text())['capabilities']
    all_files = {}
    sources = set(manifest['schemaRefs'])
    sources.add(manifest['definitionCompatibility']['definitionSchema'])
    for package in manifest['packages']:
        name = package['name']
        base = ROOT / 'agents/skills' / name
        assert not base.is_symlink() and base.is_dir(), 'PACKAGE_NOT_REAL_DIRECTORY'
        assert package['path'] == str(base.relative_to(ROOT)), 'PACKAGE_PATH_CHANGED'
        body = (base / 'SKILL.md').read_text()
        assert body.startswith('---\nname: ' + name + '\ndescription: '), 'METADATA_INVALID'
        assert 'load_operational_skill' in body, 'LOADING_INSTRUCTION_MISSING'
        assert 'agents/skills/ontology-work-coordinator/references/tool-contract.md' in body
        for link_path in package['discoveryLinks']:
            link = ROOT / link_path
            assert link_path in ('.agents/skills/' + name, '.claude/skills/' + name)
            assert link.is_symlink() and link.resolve() == base.resolve(), 'DISCOVERY_LINK_INVALID'
            assert link.readlink() == Path('../../agents/skills/' + name), 'INDIRECT_DISCOVERY_LINK'
        assert len(package['discoveryLinks']) == 2
        files = {}
        for path in sorted(base.rglob('*')):
            assert not path.is_symlink(), 'PACKAGE_SYMLINK_UNEXPECTED'
            if path.is_file():
                assert path.resolve().is_relative_to(base.resolve()), 'PACKAGE_FILE_ESCAPE'
                files[str(path.relative_to(ROOT))] = sha(path)
        ref = json.loads((base / 'references/capabilities.json').read_text())
        assert package['capabilities'] == list(ref['queries']) + list(ref['commands'])
        for capability, contract in ref['commands'].items():
            assert {k: v for k, v in contract.items() if k != 'provider'} == slots[capability], 'SLOT_CONTRACT_DRIFT'
        for capability, contract in (ref['queries'] | ref['commands']).items():
            path = ROOT / contract['provider']
            assert path.is_relative_to(ROOT / 'backend/src/main/java/com/mulino')
            assert '"' + capability + '"' in path.read_text(), 'PROVIDER_CAPABILITY_MISSING'
            sources.add(contract['provider'])
        if refresh:
            package['files'] = files
            package['sha256'] = digest(files)
        assert package['files'] == files, 'PACKAGE_FILE_DRIFT'
        assert package['sha256'] == digest(files), 'PACKAGE_HASH_DRIFT'
        all_files.update(files)
    source_hashes = {name: sha(ROOT / name) for name in sorted(sources)}
    if refresh:
        manifest['sourceHashes'] = source_hashes
        manifest['skillSha256'] = digest(all_files)
        MANIFEST.write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + '\n')
    assert manifest['sourceHashes'] == source_hashes, 'PUBLIC_CONTRACT_DRIFT'
    assert manifest['skillSha256'] == digest(all_files), 'SKILL_HASH_DRIFT'
    return {'status': 'PASS', 'evidenceClass': 'STATIC_PACKAGE_DISCOVERY',
            'packages': len(NAMES), 'discoveryLinks': 2 * len(NAMES),
            'files': len(all_files), 'skillSha256': digest(all_files),
            'manifestSha256': sha(MANIFEST), 'actualModelCalls': 0,
            'nativeBodyLoading': 'NOT_RUN', 'nativeToolExecution': 'NOT_RUN'}


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--refresh', action='store_true',
                        help='Explicitly refresh hashes after reviewing package/contract changes')
    args = parser.parse_args()
    print(json.dumps(validate(args.refresh), ensure_ascii=False, indent=2))
