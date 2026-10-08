#!/usr/bin/env python3
"""Hash stamps of every committed observation-bindings.json must match the current inputs; never a runtime PASS.

Each verification/cases/<ID>/observation-bindings.json is derived data stamped with the sha256 of the
case.json it was derived from (caseHash) and of the normative catalog it was bound against
(catalogSha256). A stale stamp means the derived file was not regenerated after its input changed.
The per-case generators' --check modes cover content; this check covers the stamps of every bindings
file, including C3 whose post-processor refreshes caseHash and catalogSha256.

A recorded KNOWN_OPEN entry (owner, reason, closeWhen) is listed but does not fail; an entry that no
longer matches a stale stamp fails, so the list stays exact.
Exit 0: every stamp current or recorded. Exit 1: an unrecorded stale stamp or a stale entry.
"""
import argparse, hashlib, json, pathlib, sys

ROOT = pathlib.Path(__file__).resolve().parents[2]
CATALOG = 'verification/requirements/mandatory-oracles.json'
# Recorded open stamp gaps: {(caseId, field): {'owner', 'reason', 'closeWhen'}}. Empty since the C3 generator
# recomputes catalogSha256 (step2r round 4); a new entry needs an owner and a closing condition.
KNOWN_OPEN = {}


def sha256(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def review(root):
    catalog = sha256(root / CATALOG)
    rows, problems, matched = [], [], set()
    for path in sorted((root / 'verification/cases').glob('*/observation-bindings.json')):
        case_id = path.parent.name
        value = json.loads(path.read_text())
        wanted = {'caseHash': sha256(path.parent / 'case.json'), 'catalogSha256': catalog}
        for field, expected in wanted.items():
            if value.get(field) == expected:
                rows.append(('CURRENT', case_id, field, None))
                continue
            entry = KNOWN_OPEN.get((case_id, field))
            if entry:
                matched.add((case_id, field))
                rows.append(('KNOWN_OPEN', case_id, field, entry['owner']))
            else:
                rows.append(('STALE', case_id, field, None))
                problems.append(f'{case_id} observation-bindings.json {field} {value.get(field)} differs from current input {expected}; rerun its generator')
    for key in sorted(set(KNOWN_OPEN) - matched):
        problems.append(f'stale known-open entry (stamp is current; delete it): {key[0]} {key[1]}')
    return rows, problems


def main():
    parser = argparse.ArgumentParser(description=__doc__.split('\n')[0])
    parser.add_argument('--root', type=pathlib.Path, default=ROOT)
    args = parser.parse_args()
    rows, problems = review(args.root)
    for status, case_id, field, owner in rows:
        if status != 'CURRENT':
            print(f'{status}{" " + owner if owner else ""}\t{case_id}\t{field}')
    for problem in problems:
        print('PROBLEM\t' + problem)
    print(json.dumps({'status': 'FAIL' if problems else 'VALID', 'files': len({r[1] for r in rows}),
                      'knownOpen': sum(r[0] == 'KNOWN_OPEN' for r in rows), 'problems': len(problems)}))
    return 1 if problems else 0


if __name__ == '__main__':
    sys.exit(main())
