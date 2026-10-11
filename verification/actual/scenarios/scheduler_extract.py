#!/usr/bin/env python3
"""Independent read-only extractor of the scheduler process controls.

Usage: scheduler_extract.py --root REPO --host HOST.json --out ROWS.json [--snapshot SNAPSHOT.json]

Reads the host descriptor the adapter wrote before this command started and, read-only over GET, the verification
scheduler's own records (MULINO_BASE_URL, MULINO_CONTROL_TOKEN from the environment). It never posts a tick. rawRows are
the scheduler-recorded rows of the observed task, selected by the typed identity in operationEvidence (taskId, or the
requested taskId/invocationHandle of an await): schedulerSubmissions, schedulerCycles and tasks. For an await it also
re-reads the terminal snapshot file as runtimeTaskSnapshot. The descriptor fields are copied as read so the harness can
compare them with the host observation.
"""
import argparse, hashlib, json, os, pathlib, urllib.parse, urllib.request


def inside(root, ref):
    path = (root / ref).resolve()
    if not path.is_file() or not path.is_relative_to(root):
        raise SystemExit(f'not a repository file: {ref}')
    return path


def records(scheduler):
    base = os.environ['MULINO_BASE_URL'].rstrip('/')
    request = urllib.request.Request(base + '/__verification/runtime/scheduler/records?' + urllib.parse.urlencode({'schedulerId': scheduler}),
                                     headers={'Authorization': 'Bearer ' + os.environ['MULINO_CONTROL_TOKEN']})
    with urllib.request.urlopen(request, timeout=30) as response:
        return json.loads(response.read())


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--root', type=pathlib.Path, required=True)
    parser.add_argument('--host', required=True)
    parser.add_argument('--out', required=True)
    parser.add_argument('--snapshot')
    args = parser.parse_args()
    root = args.root.resolve()
    host = json.loads(inside(root, args.host).read_text())
    identity = host['operationEvidence']
    selector = host.get('taskSelector') or {}
    task_id = identity.get('taskId') or selector.get('taskId')
    handle = identity.get('invocationHandle') or selector.get('invocationHandle')
    read = records(identity['schedulerId'])

    def mine(row):
        return (task_id is None or row.get('taskId') == task_id) and (handle is None or row.get('invocationHandle') == handle)
    tasks = [t for t in read.get('tasks', []) if mine(t)]
    runs = {(t.get('tickId'), t.get('sweepId')) for t in tasks}
    rows = {
        'schedulerSubmissions': [s for s in read.get('submissions', []) if mine(s)],
        'schedulerCycles': [c for c in read.get('cycles', []) if (c.get('tickId'), c.get('sweepId')) in runs],
        'tasks': tasks,
    }
    if args.snapshot:
        data = inside(root, args.snapshot).read_bytes()
        rows['runtimeTaskSnapshot'] = dict(json.loads(data), sha256=hashlib.sha256(data).hexdigest(), artifactRef=args.snapshot)
    for key in ('operation', 'command', 'environment', 'scope', 'operationEvidence', 'evidenceClass', 'driverProvenance'):
        rows[key] = host[key]
    (root / args.out).write_text(json.dumps(rows, ensure_ascii=False, sort_keys=True) + '\n')
    print(json.dumps({'tasks': len(tasks), 'out': args.out}))


if __name__ == '__main__':
    main()
