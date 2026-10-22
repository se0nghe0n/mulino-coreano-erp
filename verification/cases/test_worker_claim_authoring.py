import copy
import importlib.util
import json
from pathlib import Path
import unittest
spec=importlib.util.spec_from_file_location('author_worker_claims',Path(__file__).with_name('author_worker_claims.py'))
author=importlib.util.module_from_spec(spec);spec.loader.exec_module(author)
class Authoring(unittest.TestCase):
    def test_idempotent_and_every_worker_call_has_explicit_matching_lease(self):
        for cid in ('C3','V4','V7'):
            case=json.loads((author.ROOT/f'verification/cases/{cid}/case.json').read_text())
            self.assertEqual(case,author.patch(copy.deepcopy(case)))
            for sub in case['subcases']:
                prior={}
                for action in sub['actions']:
                    call=action.get('call',action)
                    if call.get('route')=='worker' and not call['id'].startswith('worker-negative-'):
                        ref=call['harness']['workerClaim']['$result']
                        params=prior[ref['actionId']]['control']['parameters']
                        self.assertEqual(params['actorRef'],call['actorRef'])
                        self.assertEqual(params['capabilityId'],call['capabilityId'])
                        self.assertEqual(params['commandId'],call['request']['commandIdempotencyKey'])
                        self.assertEqual('/response/claim',ref['pointer'])
                    prior[action['id']]=action
    def test_authoring_preserves_original_oracles(self):
        case=json.loads((author.ROOT/'verification/cases/C3/case.json').read_text())
        original=[(s['id'],a) for s in case['subcases'] for a in s['assertions'] if not a['id'].startswith('worker-claim-')]
        after=author.patch(copy.deepcopy(case))
        self.assertEqual(original,[(s['id'],a) for s in after['subcases'] for a in s['assertions'] if not a['id'].startswith('worker-claim-')])
if __name__=='__main__':unittest.main()
