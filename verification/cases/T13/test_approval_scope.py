"""T13의 fixture 수정과 실제 PurchaseCommands/IdentityAuthorization을 검증한다.

제품을 먼저 현재 checkout에서 빌드한다(실제 시나리오 실행은 아니다).
  PATH="$PWD/backend/node_modules/.bin:$PATH" ./mvnw -f backend/pom.xml \
    -DskipTests package dependency:build-classpath \
    -Dmdep.includeScope=test -Dmdep.outputFile=<absolute-classpath-file>
  T13_BACKEND_CLASSPATH=<absolute-classpath-file> python3 -m unittest discover \
    -s verification/cases/T13 -p 'test_approval_scope.py' -v

Rows는 fixture/installer 계약에 맞춘 메모리 저장소다. 인가 판정은 빌드한
제품 클래스가 수행하며 DB 설치·transaction·HTTP·인수 완료는 증명하지 않는다.
"""
import copy
import json
import os
import subprocess
import tempfile
import unittest
from pathlib import Path

from author_approval_scope import BASELINE, DIRECTORY, repair, selected_fixtures
from test_contract_refs import assertion_bytes, request_contract

ROOT = DIRECTORY.parents[2]


def original(path):
    return subprocess.check_output(['git', 'show', f'{BASELINE}:{path}'], cwd=ROOT)


class ApprovalFixtureScope(unittest.TestCase):
    def test_only_management_prerequisite_changes_and_authoring_is_reproducible(self):
        case_path = 'verification/cases/T13/case.json'
        before = original(case_path)
        after = (ROOT / case_path).read_bytes()
        self.assertEqual(before, after)
        self.assertEqual(assertion_bytes(before), assertion_bytes(after))
        case = json.loads(after)
        self.assertEqual(123, sum(len(s['assertions']) for s in case['subcases']))
        selected = selected_fixtures(case)
        self.assertEqual(15, len(selected))
        for sub in case['subcases']:
            path = sub['fixtureRef']
            raw = original(path)
            old = json.loads(raw)
            new = json.loads((ROOT / path).read_text())
            if path not in selected:
                self.assertEqual(raw, (ROOT / path).read_bytes())
                continue
            self.assertEqual(new, repair(copy.deepcopy(old)), path)
            self.assertEqual(new, repair(copy.deepcopy(new)), path)
            stripped = copy.deepcopy(new)
            authorities = stripped['baseline'].pop('managementAuthorities')
            self.assertEqual(old, stripped, path)
            self.assertEqual(['P', 'P-alt'], [a['scopeAlias'] for a in authorities])
            self.assertTrue(all(a['scopeKind'] == 'ITEM' and
                                a['capabilityId'] == 'approvePurchase' and
                                a['actorAlias'] == 'manager' for a in authorities))

    def test_new_dimension_is_not_silently_discarded(self):
        fixture = json.loads(original('verification/cases/T13/fixtures/changed-quantity.json'))
        fixture['actors']['manager']['grant']['scope']['places'] = ['W']
        with self.assertRaises(AssertionError):
            repair(fixture)

    def test_existing_authority_is_not_overwritten(self):
        fixture = json.loads(original('verification/cases/T13/fixtures/changed-quantity.json'))
        fixture['baseline']['managementAuthorities'] = [{'actorAlias': 'other-manager'}]
        with self.assertRaises(AssertionError):
            repair(fixture)

    def test_scoped_requests_and_intent_schema(self):
        case = json.loads((DIRECTORY / 'case.json').read_text())
        problems, _ = request_contract.Contracts(ROOT).case_problems(case)
        self.assertEqual([], problems)

    def test_actual_product_authorization(self):
        dependency_file = Path(os.environ['T13_BACKEND_CLASSPATH'])
        classpath = str(ROOT / 'backend/target/classes') + os.pathsep + dependency_file.read_text().strip()
        with tempfile.TemporaryDirectory(prefix='t13-scope-') as temporary:
            subprocess.run(['javac', '--release', '21', '-cp', classpath, '-d', temporary,
                            str(DIRECTORY / 'ApprovalScopeProbe.java')], check=True)
            for reference in selected_fixtures(json.loads((DIRECTORY / 'case.json').read_text())):
                run = subprocess.run(['java', '-cp', temporary + os.pathsep + classpath,
                                      'ApprovalScopeProbe', str(ROOT / reference)],
                                     capture_output=True, text=True)
                self.assertEqual(0, run.returncode, run.stdout + run.stderr)
                print(run.stdout.strip())


if __name__ == '__main__':
    unittest.main()
