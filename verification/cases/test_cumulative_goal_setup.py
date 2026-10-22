"""Setup input regression checks; no backend/scenario execution or runtime PASS claim.

Executes the current WorkLifecycle public-slot mapping fragment and actual GoalInput
with javac/java 21, using the published installer definition/context defaults.
The locked pre-fix document hashes protect every assertion and unselected action.
"""
import copy
import hashlib
import json
import subprocess
import tempfile
import unittest
from pathlib import Path

from author_cumulative_goal_setup import ROOT, SELECTION, repair
from request_contract import Contracts

BASELINE_HASHES = {'T09': '81c675f9714cb6709799d846f9bd1403e91293344081b2597c3d2274acb2161a', 'T11': '1a2af9f1cbe362ff6a7c5ac8684d3d5eb2c9eabfb3202024f495c2e36be3d987', 'T12': 'bba001ff8f9c22e41fb158e3d9375fd7b6eb6ccf562e617ca652ae6aa42ef29d', 'T13': 'cce3d8adaf965c5d53f717ad6b7d061054fd561840f3f540b9828f34621f6302'}


def digest(value):
    return hashlib.sha256(json.dumps(value, sort_keys=True, ensure_ascii=False).encode()).hexdigest()


def java(value):
    if isinstance(value, bool):
        return str(value).lower()
    if isinstance(value, str):
        return json.dumps(value, ensure_ascii=True)
    if isinstance(value, list):
        return 'List.of(' + ','.join(map(java, value)) + ')'
    if isinstance(value, dict):
        return 'Map.ofEntries(' + ','.join('Map.entry(' + java(k) + ',' + java(v) + ')' for k, v in value.items()) + ')'
    raise TypeError(value)


class CumulativeGoalSetup(unittest.TestCase):
    def setUp(self):
        self.cases = {c: json.loads((ROOT / f'verification/cases/{c}/case.json').read_text()) for c in SELECTION}

    def targets(self):
        for c, case in self.cases.items():
            for sub in case['subcases']:
                for action in sub['actions']:
                    if action['id'] in SELECTION[c].get(sub['id'], ()):
                        yield c, sub, action

    def test_only_selected_setup_inputs_changed(self):
        for c, case in self.cases.items():
            old = copy.deepcopy(case)
            for sub in old['subcases']:
                for action in sub['actions']:
                    if action['id'] not in SELECTION[c].get(sub['id'], ()):
                        continue
                    req = action['request']
                    if c == 'T13':
                        for key in ('period', 'eventKinds', 'distinctContributionScope'):
                            del req['slots'][key]
                            del req['provenance'][key]
                    else:
                        del req['slots']['period']['startInclusive']
            self.assertEqual(BASELINE_HASHES[c], digest(old), c)

    def test_authoring_fixed_point_and_reproduction(self):
        for c, case in self.cases.items():
            self.assertEqual(case, repair(copy.deepcopy(case)))
            old = copy.deepcopy(case)
            for sub in old['subcases']:
                for a in sub['actions']:
                    if a['id'] in SELECTION[c].get(sub['id'], ()):
                        if c == 'T13':
                            for key in ('period', 'eventKinds', 'distinctContributionScope'):
                                del a['request']['slots'][key]
                                del a['request']['provenance'][key]
                        else:
                            del a['request']['slots']['period']['startInclusive']
            self.assertEqual(case, repair(old))

    def test_selected_public_requests(self):
        contract = Contracts(ROOT)
        for c, sub, action in self.targets():
            problems, known = contract.command_problems(action, sub)
            self.assertEqual([], problems, (c, sub['id'], action['id'], problems))
            self.assertEqual([], known, (c, sub['id'], action['id'], known))

    def test_actual_java_mapping_and_goal_validation(self):
        convention = json.loads((ROOT / 'verification/actual/scenarios/installer-conventions.json').read_text())
        source = (ROOT / 'backend/src/main/java/com/mulino/application/work/WorkLifecycle.java').read_text()
        # Execute the exact product mapping for quantity/period/events/contribution scope,
        # not a second Python implementation of it. Defaults below are fixture-published.
        fragment = source.split('if(slots.containsKey("quantity")){var q=map(slots.get("quantity"));', 1)[1]
        fragment = 'if(slots.containsKey("quantity")){var q=map(slots.get("quantity"));' + fragment.split('p.put("goal",g);return p;', 1)[0]
        tests = []
        for c, sub, action in self.targets():
            slots = action['request']['slots']
            defaults = {k: v for k, v in slots.items() if k in ('dueAt', 'endpoint', 'quantityMode', 'evidencePolicyVersion', 'timezone', 'conditions', 'evaluatorVersion')}
            defaults.setdefault('timezone', next(p['content']['timezone'] for p in convention['policies'] if p['kind'] == 'GOAL_CONTEXT'))
            defaults.setdefault('evaluatorVersion', convention['definition']['evaluatorVersion'])
            templates = [t for t in convention['definition']['goals'] if t['endpoint'] == slots['endpoint'] and t['quantityMode'] == slots['quantityMode']]
            self.assertEqual(1, len(templates))
            defaults.setdefault('conditions', [{'id': templates[0]['name']}])
            defaults['scope'] = {'itemId': 'fixture-item'}
            tests.append('check(' + java(slots) + ',' + java(defaults) + ');')
        body = '''import java.util.*;
import com.mulino.domain.work.GoalInput;
import com.mulino.application.core.DomainError;
public class GoalSetupCheck {
 @SuppressWarnings("unchecked") static Map<String,Object> map(Object o){return (Map<String,Object>)o;}
 static void check(Map<String,Object> slots, Map<String,Object> defaults){
 var g=new LinkedHashMap<String,Object>(defaults);
 FRAGMENT
 GoalInput.validate(g,true);
 // Presence, interval and type failures must remain real validation failures.
 for(String key:List.of("periodStart","periodStartInclusive","eventKind","contributionScope","deduplication")){
 var bad=new LinkedHashMap<String,Object>(g);bad.remove(key);
 try{GoalInput.validate(bad,true);throw new AssertionError("accepted missing "+key);}
 catch(DomainError e){if(!e.code().equals("GOAL_SLOT_MISSING"))throw e;}
 }
 var reversed=new LinkedHashMap<String,Object>(g);reversed.put("periodStart",g.get("periodEnd"));
 try{GoalInput.validate(reversed,true);throw new AssertionError("accepted empty period");}catch(DomainError expected){}
 var badFlag=new LinkedHashMap<String,Object>(g);badFlag.put("periodStartInclusive","true");
 try{GoalInput.validate(badFlag,true);throw new AssertionError("accepted string flag");}catch(DomainError expected){}
 }
 public static void main(String[] args){ TESTS System.out.println("validated COUNT selected setup goals and negative controls"); }
}'''.replace('FRAGMENT', fragment).replace('TESTS', '\n'.join(tests)).replace('COUNT', str(len(tests)))
        with tempfile.TemporaryDirectory() as td:
            path = Path(td)
            (path / 'GoalSetupCheck.java').write_text(body)
            sources = [ROOT / f'backend/src/main/java/com/mulino/{p}.java' for p in ('domain/work/GoalInput', 'application/core/DomainError', 'application/core/DecimalValue')]
            for cmd in (['javac', '--release', '21', '-d', td, *map(str, sources), str(path / 'GoalSetupCheck.java')], ['java', '-cp', td, 'GoalSetupCheck']):
                run = subprocess.run(cmd, capture_output=True, text=True)
                self.assertEqual(0, run.returncode, run.stdout + run.stderr)
                if run.stdout:
                    print(run.stdout.strip())


if __name__ == '__main__':
    unittest.main()
