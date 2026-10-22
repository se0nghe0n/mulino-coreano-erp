import importlib.util
from pathlib import Path
import unittest

spec=importlib.util.spec_from_file_location('worker_claim_contract',Path(__file__).with_name('worker_claim_contract.py'))
module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)
class WorkerClaims(unittest.TestCase):
    def test_real_reference_does_not_exempt_read_grant(self):
        issue={'id':'lease','control':{'type':'workerClaim','operation':'issue','parameters':{'ttlSeconds':5}}}
        call={'id':'call','route':'worker','harness':{'workerClaim':{'$result':{'actionId':'lease','pointer':'/response/claim'}}}}
        self.assertFalse(module.explicit_lease_denial(call,[issue,call],'2026-10-07T09:00:00Z'))
        clock={'id':'clock','control':{'type':'clock','parameters':{'instant':'2026-10-07T09:00:05Z'}}}
        self.assertTrue(module.explicit_lease_denial(call,[issue,clock,call],'2026-10-07T09:00:00Z'))
        self.assertFalse(module.explicit_lease_denial(call,[issue,call,clock],'2026-10-07T09:00:00Z'))
    def test_missing_or_explicit_forged_ref_is_only_worker_denial(self):
        self.assertTrue(module.explicit_lease_denial({'route':'worker'},[],'2026-10-07T09:00:00Z'))
        self.assertFalse(module.explicit_lease_denial({'route':'api'},[],'2026-10-07T09:00:00Z'))
        call={'route':'worker','harness':{'workerClaim':{'claimId':'00000000-0000-0000-0000-000000000001','fencingToken':1}}}
        self.assertTrue(module.explicit_lease_denial(call,[],'2026-10-07T09:00:00Z'))
        call['harness']['workerClaim']['claimId']='another-reference'
        self.assertFalse(module.explicit_lease_denial(call,[],'2026-10-07T09:00:00Z'))
if __name__=='__main__':unittest.main()
