#!/usr/bin/env python3
"""Validate reference structure/drift only; never declare runtime coverage PASS."""
import argparse,hashlib,json,pathlib,re,sys
ROOT=pathlib.Path(__file__).resolve().parents[2]
HERE=pathlib.Path(__file__).resolve().parent
EXPECTED_CASES={f'T{i:02}' for i in range(1,27)}|{f'C{i}' for i in range(1,6)}|{f'V{i}' for i in range(1,9)}|{'E1','E2'}
EXPECTED_REQUIREMENTS={f'D{i:02}' for i in range(1,27)}
class CatalogError(ValueError):pass
def require(condition,message):
 if not condition:raise CatalogError(message)
def check_schema(value,schema,path='$'):
 """Deliberate stdlib subset sufficient for the committed schema, no network."""
 if 'const' in schema:require(value==schema['const'],f'{path}: wrong constant')
 if 'enum' in schema:require(value in schema['enum'],f'{path}: invalid enum')
 typ=schema.get('type')
 kinds={'object':dict,'array':list,'string':str}
 if typ in kinds:require(isinstance(value,kinds[typ]),f'{path}: expected {typ}')
 if isinstance(value,str) and 'pattern' in schema:require(re.fullmatch(schema['pattern'],value) is not None,f'{path}: pattern mismatch')
 if isinstance(value,dict):
  for name in schema.get('required',[]):require(name in value,f'{path}: missing {name}')
  props=schema.get('properties',{})
  if schema.get('additionalProperties') is False:require(not(set(value)-set(props)),f'{path}: unknown fields {set(value)-set(props)}')
  for name,item in value.items():
   if name in props:check_schema(item,props[name],f'{path}.{name}')
 if isinstance(value,list):
  require(len(value)>=schema.get('minItems',0),f'{path}: too few items')
  require(len(value)<=schema.get('maxItems',len(value)),f'{path}: too many items')
  if schema.get('uniqueItems'):require(len({json.dumps(x,sort_keys=True) for x in value})==len(value),f'{path}: duplicate items')
  for i,item in enumerate(value):check_schema(item,schema.get('items',{}),f'{path}[{i}]')
def digest(o):return hashlib.sha256(json.dumps(o,ensure_ascii=False,sort_keys=True,separators=(',',':')).encode()).hexdigest()
def validate(catalog,lock=None,root=ROOT):
 schema=json.loads((HERE/'mandatory-oracles.schema.json').read_text())
 check_schema(catalog,schema)
 lock=lock or json.loads((HERE/'normative-contract-lock.json').read_text())
 require(set(catalog['requiredCaseIds'])==EXPECTED_CASES,'required case IDs must be exactly41 independently named cases')
 require(set(catalog['requirementIds'])==EXPECTED_REQUIREMENTS,'requirements must be D01-D26')
 require(catalog['sourceFiles']==lock['sourceFiles'],'source manifest drift')
 source_map={s['path']:s['sha256'] for s in catalog['sourceFiles']}
 require(len(source_map)==len(catalog['sourceFiles']),'duplicate source paths')
 for path,sha in source_map.items():
  file=(root/path).resolve()
  require(file.is_relative_to(root.resolve()),'source path escapes repository')
  require(file.is_file(),f'missing source {path}')
  require(hashlib.sha256(file.read_bytes()).hexdigest()==sha,f'normative source hash drift: {path}')
 oracles=catalog['oracles']; ids=[o['oracleId'] for o in oracles]
 require(len(set(ids))==len(ids),'duplicate oracle ID')
 require(set(ids)==set(lock['oracleContracts']),'oracle omissions/additions require independent normative review and lock update')
 cases=set();requirements=set();observation_count=0
 for oracle in oracles:
  oid=oracle['oracleId'];case=oracle['caseId'];cases.add(case);requirements.update(oracle['requirementIds'])
  require(case in EXPECTED_CASES and oid.startswith(case+'.'),f'{oid}: case mismatch')
  require(set(oracle['requirementIds'])<=EXPECTED_REQUIREMENTS,f'{oid}: unknown requirement')
  if case.startswith('T'):require(f'D{case[1:]}' in oracle['requirementIds'],f'{oid}: missing direct D/T mapping')
  require(oracle['fixture'],f'{oid}: empty fixture')
  for key in ('requiredCapabilities','controlPrerequisites'):require(all(isinstance(x,str) and x.strip() for x in oracle[key]),f'{oid}: empty {key}')
  for ref in oracle['sourceRefs']:
   require(source_map.get(ref['path'])==ref['sha256'],f'{oid}: source ref mismatch')
   require(ref['section'].strip() and ref['clause'].strip(),f'{oid}: untraceable source clause')
  for shared in oracle['sharedOracleRefs']:require(shared in ids and shared!=oid,f'{oid}: invalid shared ref {shared}')
  observations=oracle['expectedObservations'];names=[o['name'] for o in observations]
  require(len(set(names))==len(names),f'{oid}: duplicate observation names')
  for item in observations:
   require(item['name'].strip() and item['scope'].strip(),f'{oid}: empty observation')
   require(item['expected'] is not None,f'{oid}: null expected weakens oracle')
   require(all(isinstance(x,str) and x.strip() for x in item['artifactKinds']),f'{oid}: empty artifact kind')
   if item['type']=='quantity':
    x=item['expected'];require(isinstance(x,dict) and set(x)=={'value','unit'},f'{oid}: quantity needs exact decimal and unit')
    require(isinstance(x['value'],str) and re.fullmatch(r'-?\d+(?:\.\d+)?',x['value']),f'{oid}: non-decimal quantity')
    require(isinstance(x['unit'],str) and x['unit'],f'{oid}: missing quantity unit')
   if item['type']=='responsibility':
    x=item['expected'];require(isinstance(x,dict) and {'owner','nextAction','nextCheckAt','currentAssignmentCount'}<=set(x),f'{oid}: owner/action/check responsibility incomplete')
   if item['operator']=='satisfies':require(isinstance(item['expected'],dict) and item['expected'],f'{oid}: empty predicate contract')
  contract=lock['oracleContracts'][oid]
  require(contract['caseId']==case and contract['observationNames']==names,f'{oid}: named observation omission/drift')
  require(contract['contractSha256']==digest(oracle),f'{oid}: fixed contract changed or weakened')
  observation_count+=len(observations)
 require(cases==EXPECTED_CASES,'missing case implementation contract')
 require(requirements==EXPECTED_REQUIREMENTS,'missing normative requirement mapping')
 # Cross-reference acyclicity keeps closure finite and reviewable.
 edges={o['oracleId']:o['sharedOracleRefs'] for o in oracles}
 def visit(key,active,done):
  require(key not in active,'shared oracle cycle')
  if key in done:return
  active.add(key)
  for child in edges[key]:visit(child,active,done)
  active.remove(key);done.add(key)
 done=set()
 for key in edges:visit(key,set(),done)
 return {'catalogStatus':'NOT_RUN','structure':'VALID','oracleCount':len(oracles),'observationCount':observation_count,'caseCount':len(cases),'requirementCount':len(requirements),'runtimeCoverage':'NOT_RUN','semanticCompleteness':'REQUIRES_QA_SOURCE_REVIEW'}
def main():
 parser=argparse.ArgumentParser(description=__doc__)
 parser.add_argument('catalog',nargs='?',type=pathlib.Path,default=HERE/'mandatory-oracles.json')
 args=parser.parse_args()
 try:print(json.dumps(validate(json.loads(args.catalog.read_text())),ensure_ascii=False));return 0
 except (CatalogError,ValueError,KeyError,OSError,TypeError) as error:
  print(f'INVALID reference catalog: {error}',file=sys.stderr);return 1
if __name__=='__main__':sys.exit(main())
