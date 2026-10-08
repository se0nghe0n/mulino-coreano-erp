"""Declared evidence subjects for authored S4 reconciliation commands.

matchSourceIdentity/linkCanonicalOccurrence publish exactly the claim's
subject (EvidenceReconciliationCommands.subjectBindings). The claim subject is
the authored original's binding subjectKind/subjectId, so the declared
subjectRefs are derived from that input, never from a product response.
"""
NOUNS={'ITEM':'TradeItem','LOT':'ManufacturingLot','SEGMENT':'QuantitySegment','WORK':'Work','PLACE':'Place',
 'PURCHASE_ORDER':'PurchaseOrder','SALES_ORDER':'SalesOrder','SALES_ORDER_LINE':'SalesOrderLine','DISPATCH':'Dispatch',
 'CARGO_SCOPE':'CargoScope','DELIVERY':'Delivery','DELIVERY_OBSERVATION':'DeliveryObservation','RETURN':'Return',
 'RECALL':'Recall','RECALL_SCOPE':'RecallScope','SETTLEMENT':'Settlement','INVOICE':'Invoice','PURCHASE_ORDER_LINE':'PurchaseOrderLine'}
def declare(value):
    if not isinstance(value,dict) or not isinstance(value.get('actions'),list):return value
    originals={a['id']:a['binding'] for a in value['actions'] if a.get('type')=='original' and 'subjectKind' in a.get('binding',{})}
    for a in value['actions']:
        if a.get('type')!='command' or a.get('capability') not in ('matchSourceIdentity','linkCanonicalOccurrence'):continue
        slots=a['request']['slots'];ref=slots.get('claimId') or slots.get('reconciliationId')
        if not isinstance(ref,str) or not ref.startswith('$'):continue
        binding=originals.get(ref[1:].rsplit('.',1)[0])
        if binding:a['request']['subjectRefs']=[{'type':NOUNS[binding['subjectKind']],'id':binding['subjectId']}]
    return value
