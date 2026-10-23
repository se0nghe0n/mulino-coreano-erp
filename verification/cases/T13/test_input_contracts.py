"""Bounded input repair and real enqueue checks; actual acceptance is NOT_RUN.

Unchanged material frontiers:
* PolicyCommands consumes kind=COMMAND -> content.rules[capability] through
  PolicyCommandGuard.rule. There is no PURCHASE_APPROVAL/MANAGER_NEW_DECISION
  interpreter. create returns id/revision/effects.policyId, not policyHash.
  Inventing content, a waiver or an ignored `requires` field would hide that gap.
* cancel-unexecuted PO-A has only type=PurchaseOrder. Installer.proposalTerms
  cannot resolve work/destination/quantity from the scalar historicalFacts.purchase.
  Its current PurchaseOrder subject/purchaseId are correct for lineForOrder; no
  unambiguous installed OrderLine exists. Do not broaden actor authority.
* PurchaseCommands dispatch returns both orderId and objectId as the order ID.
  Existing supplier pointers are correct. Partial-case createShipment/confirmReceipt
  purchaseId inputs are unsupported slots, not evidence for substituting poLineId.
* Public domain-vocabulary.json explicitly maps the ten selected approval
  guards to HELD. Conditional unsupported selectors remain a separate failure.
  Original business values, scopes, zero-effects and audit identities remain.
"""
import copy
import json
import os
from pathlib import Path
import subprocess
import tempfile
import unittest
import uuid

from author_input_contracts import BASELINE, DIRECTORY, APPROVAL_CODES, external_operation_id, repair, repair_feature
from test_contract_refs import request_contract

ROOT = DIRECTORY.parents[2]
CASE = 'verification/cases/T13/case.json'


class InputContracts(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.original = subprocess.check_output(['git', 'show', f'{BASELINE}:{CASE}'], cwd=ROOT)
        cls.before = json.loads(cls.original)
        cls.raw = (ROOT / CASE).read_bytes()
        cls.current = json.loads(cls.raw)

    def test_only_bounded_input_changes_and_all_oracles_preserved(self):
        self.assertEqual(self.current, repair(copy.deepcopy(self.before)))
        self.assertEqual(self.current, repair(copy.deepcopy(self.current)))
        self.assertEqual(135, sum(len(s['assertions']) for s in self.current['subcases']))
        self.assert_semantic_preservation()
        changed = 0
        for old, new in zip(self.before['subcases'], self.current['subcases'], strict=True):
            for a, b in zip(old['actions'], new['actions'], strict=True):
                expected = copy.deepcopy(a)
                if a.get('capabilityId') == 'dispatchPurchaseOrder':
                    expected['request']['slots']['externalOperationId'] = external_operation_id(a['request']['slots']['externalOperationId'])
                    changed += 1
                if old['id']=='policy-rechecked' and a['id'] in ('policy-approved','policy-active'):
                    expected['request']['slots']['id']['$result']['pointer']='/response/id'
                    changed += 1
                if old['id']=='changed-itemId' and a['id']=='dispatch':
                    expected['request']['subjectRefs']=[{'type':'TradeItem','id':{'$alias':'P-alt'}}]
                if old['id'].startswith('changed-') and a['id']=='after':
                    expected['request']['id']={'$result':{'actionId':'revision','pointer':'/response/proposalId'}}
                    expected['request']['scope']['objectType']='PurchaseProposal'
                    if old['id']=='changed-itemId':expected['request']['scope']['itemId']={'$alias':'P-alt'}
                self.assertEqual(expected, b, (old['id'], a['id']))
            self.assertEqual({k:v for k,v in old.items() if k not in ('actions','assertions')},
                             {k:v for k,v in new.items() if k not in ('actions','assertions')})
        self.assertEqual(17, changed)
        for path in (DIRECTORY / 'fixtures').rglob('*'):
            if path.is_file():
                rel = str(path.relative_to(ROOT))
                self.assertEqual(subprocess.check_output(['git','show',f'{BASELINE}:{rel}'],cwd=ROOT),path.read_bytes(),rel)
        feature='verification/cases/T13/scenario.feature'
        original_feature=subprocess.check_output(['git','show',f'{BASELINE}:{feature}'],cwd=ROOT).decode()
        actual_feature=(ROOT/feature).read_text()
        self.assertEqual(repair_feature(original_feature,self.current),actual_feature)
        restored=''.join(line for line in actual_feature.splitlines(keepends=True)
                         if not any('"'+key+'" assertion' in line for key in
                                    ('dispatch-approval-code','changed-field-unit','changed-field-currency')))
        self.assertEqual(original_feature,restored)
        print('123 original semantic assertions preserved; 10 exact approval-code guards + 2 denomination assertions')

    def assert_semantic_preservation(self):
        vocabulary=json.loads((ROOT/'contracts/domain-vocabulary.json').read_text())
        codes={row['code']:row['outcomes'] for row in vocabulary['errorCodes']}
        total=0
        for old,new in zip(self.before['subcases'],self.current['subcases'],strict=True):
            actual={a['id']:a for a in new['assertions']}
            expected_ids=[a['id'] for a in old['assertions']]
            for original in old['assertions']:
                total+=1
                expected=copy.deepcopy(original)
                if old['id'] in APPROVAL_CODES:
                    self.assertEqual(['HELD'],codes[APPROVAL_CODES[old['id']]])
                    if expected['source']=={'actionId':'dispatch','pointer':'/response/outcome'}:
                        self.assertEqual('REJECTED',original['expected']);expected['expected']='HELD'
                    if expected['source'].get('pointer')=='/data/rawRows/audit':
                        if expected['source'].get('where',{}).get('outcome')=='REJECTED':
                            expected['source']['where']['outcome']='HELD'
                        if 'outcome' in expected['source'].get('field',[]):
                            column=expected['source']['field'].index('outcome')
                            for row in expected['expected']:
                                self.assertEqual('REJECTED',row[column]);row[column]='HELD'
                if original['id']=='changed-field-value':
                    field=old['id'].removeprefix('changed-')
                    expected['source']={'actionId':'after','pointer':'/response/data/'+field}
                    expected['evidenceRefs']=['after:actual-adapter-artifact']
                    if field in ('quantity','price'):
                        expected['op']='decimalEquals';expected['expected']=original['expected']['value']
                        dimension='unit' if field=='quantity' else 'currency'
                        unit=copy.deepcopy(expected);unit['id']='changed-field-'+dimension
                        unit['op']='equals';unit['source']['pointer']='/response/data/'+dimension
                        unit['expected']=original['expected'][dimension]
                        unit['oracleExplanation']='개정값의 원래 단위 또는 통화를 정확히 보존한다'
                        self.assertEqual(unit,actual[unit['id']]);expected_ids.append(unit['id'])
                self.assertEqual(expected,actual[original['id']],(old['id'],original['id']))
            if old['id'] in APPROVAL_CODES:
                outcome=next(a for a in old['assertions'] if a['source']=={'actionId':'dispatch','pointer':'/response/outcome'})
                guard=copy.deepcopy(outcome);guard['id']='dispatch-approval-code'
                guard['source']['pointer']='/response/error/code';guard['expected']=APPROVAL_CODES[old['id']]
                guard['oracleExplanation']='공개 승인 code로 의도한 승인 guard가 실행됐는지 확인한다'
                self.assertEqual(guard,actual[guard['id']]);expected_ids.append(guard['id'])
            self.assertEqual(set(expected_ids),set(actual))
            self.assertEqual([a['id'] for a in old['assertions']],
                             [a['id'] for a in new['assertions'] if a['id'] not in ('dispatch-approval-code','changed-field-unit','changed-field-currency')])
        self.assertEqual(123,total)

    def test_stable_uuid_mapping_not_attempt_or_payload_dependent(self):
        ids=[]
        for sub in self.current['subcases']:
            for action in sub['actions']:
                if action.get('capabilityId')!='dispatchPurchaseOrder':continue
                value=action['request']['slots']['externalOperationId'];ids.append(value)
                self.assertEqual(5,uuid.UUID(value).version)
                label='T13-'+sub['id']+'-external'
                self.assertEqual(str(uuid.uuid5(uuid.NAMESPACE_URL,'urn:mulino:verification:external-operation:'+label)),value)
                changed=copy.deepcopy({'caseId':'T13','subcases':[sub]})
                dispatch=next(a for a in changed['subcases'][0]['actions'] if a['id']=='dispatch')
                dispatch['request']['commandIdempotencyKey']='new-attempt'
                dispatch['request']['slots']['channel']='CHANGED_REQUEST'
                self.assertEqual(value, next(a for a in repair(changed)['subcases'][0]['actions'] if a['id']=='dispatch')['request']['slots']['externalOperationId'])
        self.assertEqual(15,len(set(ids)))

    def test_modified_requests_obey_public_slots_schema(self):
        contracts=request_contract.Contracts(ROOT)
        for sub in self.current['subcases']:
            for action in sub['actions']:
                if action.get('capabilityId')=='dispatchPurchaseOrder' or sub['id']=='policy-rechecked' and action['id']=='policy-active':
                    problems,_=contracts.command_problems(action,sub)
                    self.assertEqual([],problems)

    def test_real_compiled_purchase_projection_and_subject(self):
        dependency=Path(os.environ['T13_BACKEND_CLASSPATH'])
        cp=str(ROOT/'backend/target/classes')+os.pathsep+dependency.read_text().strip()
        with tempfile.TemporaryDirectory(prefix='t13-purchase-') as directory:
            subprocess.run(['javac','--release','21','-cp',cp,'-d',directory,str(DIRECTORY/'PurchaseInputProbe.java')],check=True)
            run=subprocess.run(['java','-cp',directory+os.pathsep+cp,'PurchaseInputProbe',str(ROOT/CASE)],capture_output=True,text=True)
            self.assertEqual(0,run.returncode,run.stdout+run.stderr)
            print(run.stdout.strip())

    def test_real_compiled_enqueue(self):
        dependency=Path(os.environ['T13_BACKEND_CLASSPATH'])
        cp=str(ROOT/'backend/target/classes')+os.pathsep+dependency.read_text().strip()
        ids=[a['request']['slots']['externalOperationId'] for s in self.current['subcases'] for a in s['actions'] if a.get('capabilityId')=='dispatchPurchaseOrder']
        with tempfile.TemporaryDirectory(prefix='t13-inputs-') as directory:
            subprocess.run(['javac','--release','21','-cp',cp,'-d',directory,str(DIRECTORY/'ExternalOperationProbe.java')],check=True)
            run=subprocess.run(['java','-cp',directory+os.pathsep+cp,'ExternalOperationProbe',*ids],capture_output=True,text=True)
            self.assertEqual(0,run.returncode,run.stdout+run.stderr)
            print(run.stdout.strip())


if __name__=='__main__':unittest.main()
