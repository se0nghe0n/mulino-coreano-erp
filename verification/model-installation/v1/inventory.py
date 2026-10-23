#!/usr/bin/env python3
"""Inspect immutable M fixtures and emit missing source choices. Never writes corpus/bindings/fixtures."""
import argparse, collections, hashlib, json, pathlib
ROOT = pathlib.Path(__file__).resolve().parents[3]
HERE = pathlib.Path(__file__).resolve().parent

def sha(p): return hashlib.sha256(p.read_bytes()).hexdigest()
def verify():
    lock = json.loads((HERE/'original-input-lock.json').read_text())
    drift = [x['path'] for x in lock['artifacts'] if sha(ROOT/x['path']) != x['sha256']]
    if drift: raise SystemExit('Original input drift: '+', '.join(drift))
    return len(lock['artifacts'])
def main():
    parser=argparse.ArgumentParser(); parser.add_argument('--output',type=pathlib.Path); args=parser.parse_args()
    count=verify(); corpus=json.loads((ROOT/'verification/model-corpus/corpus.json').read_text()); cases={x['id']:x for x in corpus['cases']}
    result={'schemaVersion':'model-input-inventory-v1','baseline':'a7f18cc8e7124b9fd1cc8bf811687ed7230a4981','originalArtifactsVerified':count,'actualModelCalls':0,'acceptanceStatus':'NOT_RUN','cases':[]}
    kinds=collections.Counter(); shapes=collections.defaultdict(set)
    for file in sorted((ROOT/'verification/model-binding/cases').glob('M*/fixture.json')):
        cid=file.parent.name; f=json.loads(file.read_text()); d=f['baseline']['domainFacts']; binding=json.loads((file.parent/'binding.json').read_text()); source=cases[cid]
        turns=[]
        for i,(b,t) in enumerate(zip(binding['turns'],source['turns'])):
            intent=t['expectedIntent']; cap=b['capabilityMapping']['public']; profile=f['actors'][b['commandActorRef']]; grant_alias=profile['grant']['scope']['grantAlias']; principal=next((alias for alias,p in f['baseline']['principalFacts'].items() if p['subject']==profile['subject']),None)
            roles=f['baseline']['principalFacts'][principal].get('roles',[]); grant=f['baseline']['grantFacts'][grant_alias]
            turns.append({'turn':i+1,'publicCapability':cap,'bindingPointer':f'/turns/{i}/capabilityMapping','oraclePointer':f"/cases/{int(cid[1:])-1}/turns/{i}/oracle",'fixedOutcome':t['oracle']['outcome'],'commandProfile':b['commandActorRef'],'principalAlias':principal,'grantAlias':grant_alias,'originalExactRoleMissing':cap not in roles,'originalExactActionMissing':cap not in grant['actions'],'originalSlots':intent['slots'],'fixedAssertions':t['oracle'].get('assertions',[])})
        doc=[]
        for alias,e in d['evidence'].items():
            refs=[]
            for i,t in enumerate(source['turns']):
                for key,slot in t['expectedIntent']['slots'].items():
                    value=slot.get('value'); values=value if isinstance(value,list) else [value]
                    if alias in values: refs.append({'turn':i+1,'pointer':f"/cases/{int(cid[1:])-1}/turns/{i}/expectedIntent/slots/{key}/value",'publicCapability':turns[i]['publicCapability'],'fixedOutcome':turns[i]['fixedOutcome']})
            doc.append({'pointer':'/baseline/domainFacts/evidence/'+alias,'fixedSha256':e['sha256'],'authoredAvailable':e['available'],'originalBytesProvided':False,'installedAvailabilityIfMetadataCompleted':'UNKNOWN','directOriginalTurnUses':refs,'sourceSnippet':e})
        for obj in d['objects'].values():kinds[obj['type']]+=1
        for key,value in d.items():shapes[key].add(type(value).__name__)
        result['cases'].append({'caseId':cid,'fixtureSha256':sha(file),'authorityCorrectionRequiredBeforeContext':True,'contextCapability':'getObject','documents':doc,'turns':turns,'coordinatorSelectedCommonCompletions':{'inputProvidedByOriginal':False,'originalAuthorityClaimed':False,'operatingApprovalClaimed':False,'source':'build_inputs.py + selected_cases.py','preservesOriginalBytes':True},
            'caseSpecificOriginalFacts':{k:v for k,v in d.items() if k not in ('organizationId','objects','sources','evidence','definition','policy','obligations','approvalFacts','baselineEffects')}})
    current=ROOT/'.local/model-fixture-probe/all-corrected-inventories-final.json'
    if current.exists():
        static={c['caseId']:c for c in json.loads(current.read_text())}
        for c in result['cases']:
            report=static.get(c['caseId'],{});c['mappingIssues']=report.get('issues',[])
            c['mappedRows']=len(report.get('mapping',[]));c['originalUnverifiedClaims']=report.get('unverifiedOriginalClaims',[])
            for issue in c['mappingIssues']:
                issue['requiredByActualTurns']=[{'turn':t['turn'],'publicCapability':t['publicCapability'],'fixedOutcome':t['fixedOutcome'],'oraclePointer':t['oraclePointer']} for t in c['turns']]
                # Select the full merged case body, never a shared F template.
                parts=issue['pointer'].strip('/').split('/');value=c.get('caseSpecificOriginalFacts',{}).get(parts[2] if len(parts)>2 else '',None)
                if value is None:
                    f=json.loads((ROOT/f"verification/model-binding/cases/{c['caseId']}/fixture.json").read_text());value=f
                    try:
                        for part in parts:value=value[int(part)] if isinstance(value,list) else value[part]
                    except (KeyError,IndexError,ValueError,TypeError):value=f['baseline']['domainFacts']
                issue['originalSourceSnippet']=value
        result['objectTypeCounts']=dict(kinds);result['domainFactShapes']={k:sorted(v) for k,v in shapes.items()};verify()
    text=json.dumps(result,ensure_ascii=False,indent=2)+'\n'
    if args.output: args.output.parent.mkdir(parents=True,exist_ok=True);args.output.write_text(text)
    else: print(text,end='')
if __name__=='__main__':main()
