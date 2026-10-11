#!/usr/bin/env python3
"""Host command for test-profile process lifecycle controls (start, stop, restart) of the scenario suite backend.

Usage: process_host.py --op start|stop|restart --process-id ID --pid-file F --argv-json JSON --port P --log L
                       --scope-json JSON --out REPORT --before-json STATE [--quiet-ensure]

The scenario suite runs one backend JVM (run.sh) that hosts every case role (api, scheduler, worker-*, application) on one
disposable database. This command acts on that JVM: stop sends SIGTERM (SIGKILL after 20 s) and waits until the port is
free; start relaunches the exported command on the same port and waits until it answers HTTP. It records the observed
process state before and after (instanceId = OS pid) and an honest terminal status: start of a running JVM and stop of a
stopped one are FAILED (exit 3) without acting, because a co-hosted role cannot start or stop on its own. --quiet-ensure
starts a stopped JVM before a fixture installation and writes no report.
"""
import argparse, datetime, json, os, signal, subprocess, sys, time, urllib.error, urllib.request


def now():
    return datetime.datetime.now(datetime.timezone.utc).isoformat().replace('+00:00', 'Z')


def pid_of(path):
    try:
        text = open(path).read().strip()
        return int(text) if text else None
    except FileNotFoundError:
        return None


def alive(pid):
    if pid is None:
        return False
    try:
        os.kill(pid, 0)
    except ProcessLookupError:
        return False
    except PermissionError:
        return True
    # A zombie child of nobody here still answers kill 0; check the state where ps is available.
    try:
        state = subprocess.run(['ps', '-o', 'stat=', '-p', str(pid)], capture_output=True, text=True).stdout.strip()
        return bool(state) and not state.startswith('Z')
    except OSError:
        return True


def state(process_id, pid_file):
    pid = pid_of(pid_file)
    running = alive(pid)
    return {'processId': process_id, 'instanceId': str(pid) if running else None,
            'status': 'RUNNING' if running else 'STOPPED', 'observedAt': now()}


def answers(port):
    try:
        urllib.request.urlopen(f'http://127.0.0.1:{port}/api/ontology/queries/getInventory', timeout=2)
        return True
    except urllib.error.HTTPError:
        return True
    except (urllib.error.URLError, OSError):
        return False


def stop(pid_file, port):
    pid = pid_of(pid_file)
    os.kill(pid, signal.SIGTERM)
    deadline = time.time() + 20
    while alive(pid) and time.time() < deadline:
        time.sleep(0.1)
    if alive(pid):
        os.kill(pid, signal.SIGKILL)
        deadline = time.time() + 10
        while alive(pid) and time.time() < deadline:
            time.sleep(0.1)
    if alive(pid):
        raise SystemExit('backend did not terminate')
    deadline = time.time() + 10
    while answers(port) and time.time() < deadline:
        time.sleep(0.1)


def start(argv, port, log, pid_file):
    argv = [a for a in argv if not a.startswith('--server.port=')] + [f'--server.port={port}']
    out = open(log, 'ab')
    out.write(f'\n# process_host.py start {now()}\n'.encode())
    out.flush()
    child = subprocess.Popen(argv, stdout=out, stderr=subprocess.STDOUT, stdin=subprocess.DEVNULL, start_new_session=True)
    with open(pid_file, 'w') as f:
        f.write(str(child.pid))
    deadline = time.time() + 120
    while time.time() < deadline:
        if child.poll() is not None:
            raise SystemExit(f'backend exited during start ({child.returncode})')
        if answers(port):
            return
        time.sleep(0.25)
    raise SystemExit('backend did not answer within 120 s')


def main():
    p = argparse.ArgumentParser()
    for name in ('--op', '--process-id', '--pid-file', '--argv-json', '--port', '--log', '--scope-json', '--out', '--before-json'):
        p.add_argument(name, required=name not in ('--out', '--scope-json', '--process-id', '--before-json'))
    p.add_argument('--quiet-ensure', action='store_true')
    a = p.parse_args()
    argv = json.loads(a.argv_json)
    if a.quiet_ensure:
        if not alive(pid_of(a.pid_file)):
            start(argv, a.port, a.log, a.pid_file)
            print('started')
        return 0
    # The adapter observed the state before this command started (host-observation-guide.md); act on the real state.
    before = json.loads(a.before_json) if a.before_json else state(a.process_id, a.pid_file)
    if state(a.process_id, a.pid_file)['status'] != before['status']:
        raise SystemExit('process state changed between observation and command')
    terminal, reason = 'SUCCEEDED', None
    if a.op == 'start' and before['status'] == 'RUNNING':
        terminal, reason = 'FAILED', 'the backend JVM that hosts this role is already running; a co-hosted role cannot start on its own'
    elif a.op == 'stop' and before['status'] != 'RUNNING':
        terminal, reason = 'FAILED', 'the backend JVM that hosts this role is not running'
    else:
        if a.op in ('stop', 'restart') and before['status'] == 'RUNNING':
            stop(a.pid_file, a.port)
        if a.op in ('start', 'restart'):
            start(argv, a.port, a.log, a.pid_file)
    completed = now()
    after = state(a.process_id, a.pid_file)
    if terminal == 'SUCCEEDED' and (after['status'] != ('STOPPED' if a.op == 'stop' else 'RUNNING')
                                    or a.op == 'restart' and after['instanceId'] == before['instanceId']):
        terminal, reason = 'FAILED', 'observed final process state differs from the requested operation'
    observation = {'requestedProcessId': a.process_id, 'before': before, 'after': after, 'terminalStatus': terminal,
                   'completedAt': completed, 'artifactRef': a.out}
    report = {'evidenceClass': 'ACTUAL_HOST', 'scope': json.loads(a.scope_json), 'operation': a.op,
              'processObservation': observation, 'rows': {'processObservation': observation, 'reason': reason}}
    with open(a.out, 'w') as f:
        f.write(json.dumps(report, ensure_ascii=False, sort_keys=True) + '\n')
    print(json.dumps({'terminalStatus': terminal, 'reason': reason, 'before': before['status'], 'after': after['status']}))
    return 0 if terminal == 'SUCCEEDED' else 3


if __name__ == '__main__':
    sys.exit(main())
