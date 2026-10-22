"""Preparation-only detection of explicit lease denials, separate from grant denials.

Recognizes absent references, a literal forged ID, or a real issue action followed
by a clock advance to its expiry. Does not infer product outcomes or skip any
runtime assertion. READ-grant tests with valid references are never exempted.
"""
from datetime import datetime, timedelta

def explicit_lease_denial(action, actions, fixture_clock):
    if action.get('route') != 'worker':
        return False
    claim=(action.get('harness') or {}).get('workerClaim')
    if claim is None:
        return True
    if isinstance(claim,dict) and claim.get('claimId')=='00000000-0000-0000-0000-000000000001':
        return True
    ref=claim.get('$result') if isinstance(claim,dict) else None
    if not ref or ref.get('pointer')!='/response/claim':
        return False
    now=fixture_clock
    expires=None
    for a in actions:
        if a.get('id')==action.get('id'):
            return expires is not None and datetime.fromisoformat(now.replace('Z','+00:00'))>=expires
        control=a.get('control') or {};p=control.get('parameters') or {}
        if control.get('type')=='clock':
            now=next((p[k] for k in ('instant','asOf','knownAt') if isinstance(p.get(k),str)),now)
        if a.get('id')==ref.get('actionId') and control.get('type')=='workerClaim' and control.get('operation')=='issue':
            expires=datetime.fromisoformat(now.replace('Z','+00:00'))+timedelta(seconds=p['ttlSeconds'])
    return False
