#!/usr/bin/env python3
"""Host command of the scheduler process controls (tickScheduler, sweepDue, awaitRuntimeTask).

Usage:
  scheduler_host.py tick --request WIRE.json --out SUBMISSION.json
  scheduler_host.py await --scheduler ID [--task-id ID] [--handle H] --timeout SECONDS --out SNAPSHOT.json

The product base URL and the control bearer come from MULINO_BASE_URL and MULINO_CONTROL_TOKEN (never argv, so the
transcript holds no credential). tick posts the wire body the adapter wrote to the verification scheduler, which runs one
scheduler cycle as one task, and writes the submission reply. await reads the scheduler's own task rows until the named
task is terminal (or the timeout passes) and writes the terminal snapshot; it never runs a cycle or a command itself.
"""
import argparse, datetime, json, os, pathlib, sys, time, urllib.error, urllib.parse, urllib.request, uuid


def now():
    return datetime.datetime.now(datetime.timezone.utc).isoformat(timespec='microseconds').replace('+00:00', 'Z')


def call(method, path, body=None):
    base = os.environ['MULINO_BASE_URL'].rstrip('/')
    data = None if body is None else json.dumps(body).encode()
    request = urllib.request.Request(base + path, data=data, method=method, headers={
        'Authorization': 'Bearer ' + os.environ['MULINO_CONTROL_TOKEN'], 'Content-Type': 'application/json'})
    try:
        with urllib.request.urlopen(request, timeout=60) as response:
            return response.status, json.loads(response.read() or b'{}')
    except urllib.error.HTTPError as failure:
        raw = failure.read()
        try:
            return failure.code, json.loads(raw or b'{}')
        except ValueError:
            return failure.code, {'body': raw.decode('utf-8', 'replace')[:2000]}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('mode', choices=['tick', 'await'])
    parser.add_argument('--request')
    parser.add_argument('--scheduler')
    parser.add_argument('--task-id')
    parser.add_argument('--handle')
    parser.add_argument('--timeout', type=int, default=30)
    parser.add_argument('--out', required=True)
    args = parser.parse_args()
    out = pathlib.Path(args.out)
    if args.mode == 'tick':
        status, reply = call('POST', '/__verification/runtime/scheduler/tick', json.loads(pathlib.Path(args.request).read_text()))
        out.write_text(json.dumps({'httpStatus': status, 'reply': reply}, sort_keys=True) + '\n')
        print(json.dumps({'httpStatus': status, 'submissionStatus': reply.get('submissionStatus')}))
        return 0 if status == 200 else 2
    deadline = time.monotonic() + max(1, min(args.timeout, 60))
    query = '/__verification/runtime/scheduler/records?' + urllib.parse.urlencode({'schedulerId': args.scheduler})
    while True:
        status, records = call('GET', query)
        if status != 200:
            print(json.dumps({'httpStatus': status}))
            return 2
        tasks = [t for t in records.get('tasks', []) if (args.task_id is None or t.get('taskId') == args.task_id)
                 and (args.handle is None or t.get('invocationHandle') == args.handle)]
        terminal = [t for t in tasks if t.get('terminalStatus') in ('SUCCEEDED', 'FAILED', 'CANCELLED')]
        if len(terminal) == 1:
            task = terminal[0]
            snapshot = {'evidenceClass': 'ACTUAL_HOST', 'schedulerId': args.scheduler, 'snapshotId': 'runtime-task-' + str(uuid.uuid4()),
                        'capturedAt': now()}
            for key in ('taskId', 'invocationHandle', 'origin', 'terminalStatus', 'completedAt', 'submittedAt', 'result', 'technicalCode'):
                if key in task:
                    snapshot[key] = task[key]
            out.write_text(json.dumps(snapshot, sort_keys=True) + '\n')
            print(json.dumps({'taskId': task['taskId'], 'terminalStatus': task['terminalStatus']}))
            return 0
        if len(tasks) > 1:
            print(json.dumps({'error': 'ambiguous task identity', 'matches': len(tasks)}))
            return 3
        if time.monotonic() > deadline:
            print(json.dumps({'error': 'no terminal before timeout', 'matches': len(tasks)}))
            return 4
        time.sleep(0.25)


if __name__ == '__main__':
    sys.exit(main())
