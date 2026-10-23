#!/usr/bin/env python3
"""Read only owned probe rows after real HTTP. It never reads corpus/expected/oracle."""
import argparse,json,pathlib,subprocess,uuid,os
from inventory import ROOT,sha
p=argparse.ArgumentParser();p.add_argument('case',choices=['M05','M50']);p.add_argument('database');p.add_argument('installation',type=pathlib.Path);p.add_argument('output',type=pathlib.Path);a=p.parse_args()
if not a.database.startswith('model_') or not a.database.replace('_','').isalnum():raise SystemExit('Owned model probe database name required')
j=json.loads(a.installation.read_text());org=str(uuid.UUID(j['originalAliasMap']['org']));sqls={}
if a.case=='M50':
 cmd=str(uuid.UUID(j['priorReceiptProof']['actualFirstCommit']['response']['commandId']))
 sqls['commandAudit']=f"SELECT to_jsonb(a)||jsonb_build_object('xminTransactionId',a.xmin::text) FROM mulino_commands_CommandAudits a WHERE a.organizationId='{org}' AND a.commandId='{cmd}'"
 sqls['dutyRoots']=f"SELECT to_jsonb(r)||jsonb_build_object('xminTransactionId',r.xmin::text) FROM mulino_responsibility_Roots r WHERE r.organizationId='{org}' AND r.sourceId='{cmd}'"
 sqls['dutyScopes']=f"SELECT to_jsonb(s)||jsonb_build_object('xminTransactionId',s.xmin::text) FROM mulino_responsibility_Scopes s JOIN mulino_responsibility_Roots r ON r.organizationId=s.organizationId AND r.ID=s.rootId WHERE r.organizationId='{org}' AND r.sourceId='{cmd}'"
 sqls['dutyAssignments']=f"SELECT to_jsonb(s)||jsonb_build_object('xminTransactionId',s.xmin::text) FROM mulino_work_read_ObligationReferences s JOIN mulino_responsibility_Roots r ON r.organizationId=s.organizationId AND r.ID=s.rootId WHERE r.organizationId='{org}' AND r.sourceId='{cmd}'"
 sqls['movements']=f"SELECT to_jsonb(m)||jsonb_build_object('xminTransactionId',m.xmin::text) FROM mulino_inventory_QuantityMovements m WHERE m.organizationId='{org}' AND m.commandId='{cmd}'"
else:
 sh=str(uuid.UUID(j['originalAliasMap']['SH1']))
 sqls['shipmentSourceChain']=f"SELECT jsonb_build_object('shipment',to_jsonb(s),'cargo',to_jsonb(c),'observation',to_jsonb(e),'actualConfirmedPlace',to_jsonb(p),'canonical',to_jsonb(o),'verification',to_jsonb(v),'sourceDocument',to_jsonb(d)) FROM mulino_trade_shipment_Shipments s JOIN mulino_trade_shipment_ShipmentCargo c ON c.organizationId=s.organizationId AND c.shipmentId=s.ID JOIN mulino_trade_shipment_LegEvents e ON e.organizationId=s.organizationId AND e.cargoId=c.ID JOIN mulino_inventory_Places p ON p.organizationId=e.organizationId AND p.ID=e.placeId JOIN mulino_evidence_CanonicalOccurrences o ON o.organizationId=e.organizationId AND o.ID=e.occurrenceId JOIN mulino_evidence_Verifications v ON v.organizationId=o.organizationId AND v.canonicalOccurrenceId=o.ID JOIN mulino_evidence_DocumentVersions d ON d.organizationId=v.organizationId AND d.ID=v.basisDocumentId WHERE s.organizationId='{org}' AND s.ID='{sh}'"
result=dict(evidenceClass='ACTUAL_FOCUSED_PG_CUSTODY_READBACK',independentBusinessActorReadClaimed=False,actualModelCalls=0,caseId=a.case,installationId=j['installationId'],fixtureSha256=j['fixtureSha256'],installationReceiptSha256=sha(a.installation),database=a.database,queries={})
env=dict(os.environ,DOCKER_HOST='unix:///Users/se0nghe0n/.orbstack/run/docker.sock')
for name,sql in sqls.items():
 proc=subprocess.run(['docker','exec','mulino-model-fixture-a7-94ae2786','psql','-U','postgres','-d',a.database,'-v','ON_ERROR_STOP=1','-At','-c',sql],env=env,text=True,capture_output=True,check=True)
 result['queries'][name]=dict(sql=sql,actualRows=[json.loads(row) for row in proc.stdout.splitlines() if row])
a.output.parent.mkdir(parents=True,exist_ok=True);a.output.write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n');print(a.case,{k:len(v['actualRows']) for k,v in result['queries'].items()})
