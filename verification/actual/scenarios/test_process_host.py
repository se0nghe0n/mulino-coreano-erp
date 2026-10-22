"""Focused lifecycle checks, including real local OS processes (not product acceptance)."""
import argparse
import contextlib
import importlib.util
import io
import json
import pathlib
import socket
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import patch

spec = importlib.util.spec_from_file_location('process_host', pathlib.Path(__file__).with_name('process_host.py'))
host = importlib.util.module_from_spec(spec)
spec.loader.exec_module(host)


class ProcessHostTest(unittest.TestCase):
    def setUp(self):
        self.children = []
        original = subprocess.Popen
        def spawn(*args, **kwargs):
            child = original(*args, **kwargs)
            self.children.append(child)
            return child
        self.spawns = patch.object(host.subprocess, 'Popen', side_effect=spawn)
        self.spawns.start()
        self.temp = tempfile.TemporaryDirectory()
        self.root = pathlib.Path(self.temp.name)
        with socket.socket() as sock:
            sock.bind(('127.0.0.1', 0))
            self.port = sock.getsockname()[1]
        # A real HTTP process accepting the backend launcher argument, deliberately not an ontology mock.
        app = self.root / 'http_process.py'
        app.write_text('import http.server, sys\n'
                       'port = int(sys.argv[-1].split("=")[1])\n'
                       'http.server.HTTPServer(("127.0.0.1", port), http.server.BaseHTTPRequestHandler).serve_forever()\n')
        self.args = argparse.Namespace(op='start', process_id='api', pid_file=str(self.root / 'pid'),
            argv_json=json.dumps([sys.executable, str(app)]), port=self.port, log=str(self.root / 'log'),
            scope_json='{"test":"real-local-process"}', out=str(self.root / 'report.json'),
            before_json=None, quiet_ensure=False)

    def tearDown(self):
        if host.alive(host.pid_of(self.args.pid_file)):
            host.stop(self.args.pid_file, self.port)
        for child in self.children:
            child.wait(timeout=5)
        self.spawns.stop()
        self.temp.cleanup()

    def run_control(self, op, process_id='api'):
        self.args.op, self.args.process_id = op, process_id
        with contextlib.redirect_stdout(io.StringIO()):
            code = host.execute(self.args)
        report = json.loads(pathlib.Path(self.args.out).read_text())
        return code, report

    def test_real_process_launch_attach_restart_stop(self):
        code, first = self.run_control('start')
        self.assertEqual(0, code)
        self.assertEqual('STARTED', first['rows']['processAction'])
        pid = first['processObservation']['after']['instanceId']
        for role in ('scheduler', 'worker-a', 'worker-b', 'due-sweeper'):
            code, attached = self.run_control('start', role)
            self.assertEqual(0, code)
            self.assertEqual('ATTACHED_EXISTING', attached['rows']['processAction'])
            self.assertEqual(pid, attached['processObservation']['before']['instanceId'])
            self.assertEqual(pid, attached['processObservation']['after']['instanceId'])
        code, restarted = self.run_control('restart')
        self.assertEqual(0, code)
        self.assertEqual('RESTARTED', restarted['rows']['processAction'])
        self.assertNotEqual(pid, restarted['processObservation']['after']['instanceId'])
        self.assertFalse(host.alive(int(pid)))
        code, stopped = self.run_control('stop')
        self.assertEqual(0, code)
        self.assertEqual('STOPPED', stopped['processObservation']['after']['status'])
        self.assertFalse(host.answers(self.port))
        for op in ('stop', 'restart'):
            code, failed = self.run_control(op)
            self.assertEqual(3, code)
            self.assertEqual('FAILED', failed['processObservation']['terminalStatus'])

    def test_concurrent_role_starts_share_one_application(self):
        commands = []
        for role in ('api', 'scheduler'):
            report = self.root / (role + '.json')
            commands.append(subprocess.Popen([
                sys.executable, str(pathlib.Path(host.__file__)), '--op', 'start',
                '--process-id', role, '--pid-file', self.args.pid_file,
                '--argv-json', self.args.argv_json, '--port', str(self.port),
                '--log', self.args.log, '--scope-json', self.args.scope_json,
                '--out', str(report)], stdout=subprocess.PIPE, stderr=subprocess.PIPE))
        for command in commands:
            out, err = command.communicate(timeout=10)
            self.assertEqual(0, command.returncode, (out, err))
        reports = [json.loads((self.root / (role + '.json')).read_text()) for role in ('api', 'scheduler')]
        self.assertEqual({'STARTED', 'ATTACHED_EXISTING'}, {r['rows']['processAction'] for r in reports})
        self.assertEqual(1, len({r['processObservation']['after']['instanceId'] for r in reports}))

    def test_existing_fixture_instance_is_attached_without_relaunch(self):
        self.args.quiet_ensure = True
        with contextlib.redirect_stdout(io.StringIO()):
            self.assertEqual(0, host.execute(self.args))
        pid = host.pid_of(self.args.pid_file)
        self.args.quiet_ensure = False
        code, attached = self.run_control('start')
        self.assertEqual(0, code)
        self.assertEqual('ATTACHED_EXISTING', attached['rows']['processAction'])
        self.assertEqual(str(pid), attached['processObservation']['after']['instanceId'])

    def test_stale_pid_observation_fails_without_touching_live_instance(self):
        self.run_control('start')
        stale = host.state('api', self.args.pid_file)
        stale['instanceId'] = 'stale-pid'
        self.args.before_json = json.dumps(stale)
        code, failed = self.run_control('restart')
        self.assertEqual(3, code)
        self.assertIn('identity or state changed', failed['rows']['reason'])
        self.assertEqual('RUNNING', failed['processObservation']['after']['status'])
        self.assertEqual('stale-pid', failed['processObservation']['before']['instanceId'])

    def test_stopped_clock_is_intent_only_and_launch_uses_it(self):
        self.args.instant = '2026-10-07T09:00:11Z'
        self.args.op = 'stage-clock'
        with contextlib.redirect_stdout(io.StringIO()) as out:
            self.assertEqual(0, host.execute(self.args))
        pending = json.loads(out.getvalue())
        self.assertEqual('PENDING_RESTART', pending['status'])
        self.assertFalse(pending['authorityClockApplied'])
        self.assertFalse(host.answers(self.port))
        # Capture the real launch arguments; the generic HTTP smoke app need not interpret a clock.
        with patch.object(host.subprocess, 'Popen', side_effect=RuntimeError('captured')) as spawn:
            with self.assertRaisesRegex(RuntimeError, 'captured'):
                host.start(json.loads(self.args.argv_json), self.port, self.args.log, self.args.pid_file)
        self.assertIn('--mulino.verification.instant=' + self.args.instant, spawn.call_args.args[0])

    def test_running_clock_cannot_be_staged_as_applied(self):
        self.run_control('start')
        self.args.op, self.args.instant = 'stage-clock', '2026-10-07T09:00:11Z'
        with contextlib.redirect_stdout(io.StringIO()) as out:
            self.assertEqual(4, host.execute(self.args))
        self.assertEqual('RUNNING', json.loads(out.getvalue())['status'])
        self.assertFalse(pathlib.Path(self.args.pid_file + '.clock.json').exists())

    def test_start_failure_keeps_terminal_evidence(self):
        self.args.argv_json = json.dumps([sys.executable, '-c', 'raise SystemExit(7)'])
        code, failed = self.run_control('start')
        self.assertEqual(3, code)
        self.assertIn('exited during start (7)', failed['rows']['reason'])
        self.assertEqual('STOPPED', failed['processObservation']['after']['status'])

    def test_running_unready_process_is_not_confirmed(self):
        self.run_control('start')
        with patch.object(host, 'answers', return_value=False):
            code, failed = self.run_control('start')
        self.assertEqual(3, code)
        self.assertIn('does not answer', failed['rows']['reason'])

    def test_port_conflict_is_not_misreported_as_new_process(self):
        self.run_control('start')
        with self.assertRaisesRegex(RuntimeError, 'already answers'):
            host.start(json.loads(self.args.argv_json), self.port, self.args.log, str(self.root / 'other-pid'))
        self.assertFalse((self.root / 'other-pid').exists())


if __name__ == '__main__':
    unittest.main(verbosity=2)
