"""Bound source requests, never fixture verification flags or scenario execution.

The case runner currently cannot serialize resolved UUIDs into an EVENT payload.
receipt_chain therefore takes real, already-resolved bindings. Its output is an
integration contract for that missing runner boundary, not an acceptance result.
"""
import base64
import copy
import hashlib
import json
from decimal import Decimal
from uuid import UUID
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
CASES = ("T09", "T11", "T12")


def result(action, field="id"):
    return {"$result": {"actionId": action, "pointer": "/response/" + field}}


def command(prefix, actor, capability, slots, kind="COMMAND", revision=1):
    return {"id": prefix, "kind": "invoke", "actorRef": actor, "route": "api",
            "capabilityId": capability, "evidenceRefs": [prefix + ":actual-artifact"],
            "request": {"intentKind": kind, "definitionVersion": "definition-v1",
                        "capabilityId": capability, "subjectRefs": [], "slots": slots,
                        "provenance": {k: "CONTEXT" for k in slots},
                        "expectedRevision": revision, "commandIdempotencyKey": prefix}}


def document_and_event(prefix, actor, subject, kind, original, namespace,
                       external_id, version, effective_from, quantity=None,
                       unit=None, effective_until=None, document=True):
    """A source observation is KNOWN as a claim, never pre-VERIFIED as a fact."""
    payload = json.dumps(original, ensure_ascii=False, sort_keys=True, separators=(",", ":"))
    actions = []
    slots = {"subject": subject, "kind": kind, "sourceNamespace": namespace,
             "externalEventId": external_id, "sourceVersion": version,
             "effectiveFrom": effective_from, "timeZone": "UTC",
             "timePrecision": "SECOND", "valueState": "KNOWN", "payload": payload}
    if effective_until is not None:
        slots["effectiveUntil"] = effective_until
    if quantity is not None:
        slots.update(quantity=quantity, unit=unit)
    if document:
        data = payload.encode()
        actions.append(command(prefix + "-document", actor, "attachEvidence", {
            "subject": copy.deepcopy(subject), "sourceNamespace": namespace,
            "sourceReference": external_id, "mediaType": "application/json",
            "expectedHash": hashlib.sha256(data).hexdigest(),
            "provenance": "SYNTHETIC source observation; no legal approval or verification",
            "contentBase64": base64.b64encode(data).decode()}, "RECORD"))
        slots.update(documentId=result(prefix + "-document"),
                     assertion="Synthetic original observation; subject to source reconciliation")
    actions.append(command(prefix, actor, "recordActivity", slots, "RECORD"))
    return actions


def receipt_chain(prefix, actor, binding, quantity, occurred_at, *,
                  transit_segment_id=None, receiving_custodian_id=None,
                  purchase_line_id=None, start_quantity="0", lot_required=True):
    """Build the published source→observation→match→canonical→receipt graph.

    UUIDs must come from actual prior product/fixture results. A missing LOT is
    retained for the negative confirm probe; it is never inferred from an item.
    """
    required = ("itemId", "placeId", "workId", "rangeRootId", "ownerId", "supervisorId")
    for key in required:
        UUID(binding[key])
    if lot_required:
        UUID(binding["lotId"])
    if Decimal(quantity) <= 0 or Decimal(start_quantity) < 0:
        raise ValueError("Positive physical quantity and nonnegative range start required")
    if transit_segment_id and receiving_custodian_id:
        raise ValueError("Transit custody cannot be replaced by a receiving custodian")
    original = {k: binding[k] for k in ("itemId", "placeId", "workId", "rangeRootId")}
    original.update(startQuantity=start_quantity, quantity=quantity, unit="BOX", occurredAt=occurred_at)
    if binding.get("lotId"):
        original["lotId"] = binding["lotId"]
    if receiving_custodian_id:
        original["receivingCustodianId"] = receiving_custodian_id
    namespace = binding.get("sourceNamespace", "synthetic-warehouse")
    actions = document_and_event(prefix + "-event", actor,
        {"kind": "WORK", "id": binding["workId"]}, "PHYSICAL_RECEIPT", original,
        namespace, prefix, "1", occurred_at, quantity, "BOX")
    observation = dict(original)
    observation.pop("receivingCustodianId", None)
    observation.update(eventId=result(prefix + "-event"), ownerId=binding["ownerId"],
        supervisorId=binding["supervisorId"], nextAction="원천 수령 범위와 증빙을 대조한다",
        nextCheckAt=binding["nextCheckAt"])
    if transit_segment_id:
        observation["transitSegmentId"] = transit_segment_id
    if purchase_line_id:
        observation["purchaseLineId"] = purchase_line_id
    actions.append(command(prefix + "-provisional", actor, "receiveProvisional", observation, "RECORD"))
    # Missing-LOT negative remains before canonical verification: ReceiptEvidenceScopes
    # deliberately cannot verify an unidentified receipt range.
    if not lot_required:
        actions.append(command(prefix, actor, "confirmReceipt", {
            "receiptId": result(prefix + "-provisional", "effects/receiptId")}))
        return actions
    actions.append(command(prefix + "-match", actor, "matchSourceIdentity", {
        "claimId": result(prefix + "-event", "claimId"),
        "basisDocumentId": result(prefix + "-event-document"),
        "physicalScopeId": binding["rangeRootId"],
        "policyVersion": binding["policyVersion"],
        "sourceIdentity": namespace + ":" + prefix + ":1", "quantity": quantity,
        "unit": "BOX", "effectiveFrom": occurred_at,
        "reason": "원문·관측·실제 수령 범위의 동일성을 대조한다"}))
    actions.append(command(prefix + "-canonical", actor, "linkCanonicalOccurrence", {
        "reconciliationId": result(prefix + "-match")}))
    confirmed = {"receiptId": result(prefix + "-provisional", "effects/receiptId"),
                 "lotId": binding["lotId"], "canonicalOccurrenceId": result(prefix + "-canonical")}
    if receiving_custodian_id:
        confirmed["receivingCustodianId"] = receiving_custodian_id
    actions.append(command(prefix, actor, "confirmReceipt", confirmed))
    return actions


def repair_inputs(case, fixture):
    """Only replace supported input shapes; leave unresolved positive receipts visible."""
    for sub in case["subcases"]:
        output = []
        f = fixture(sub["fixtureRef"])
        for action in sub["actions"]:
            cap = action.get("capabilityId")
            request = action.get("request", {})
            slots = request.get("slots", {})
            if cap in ("createWork", "createDraft") and "period" in slots:
                slots["period"].setdefault("startInclusive", True)
            if cap == "createWork" and "interval" in slots:
                # The old interval field was the intended state comparison range.
                # Public WorkLifecycle calls the same value period.
                slots["period"] = slots.pop("interval")
                slots["period"].setdefault("startInclusive", True)
                request["provenance"].pop("interval", None)
                request["provenance"]["period"] = "USER"
            if cap == "createSalesOrder":
                f["aliases"].setdefault("CUSTOMER_PLACE", {
                    "type": "Place", "kind": "CUSTOMER", "name": "고객 인도 장소"})
                slots["destinationId"] = {"$alias": "CUSTOMER_PLACE"}
                slots.setdefault("packageTerms", "STANDARD")
                request["provenance"].update(destinationId="CONTEXT", packageTerms="CONTEXT")
            if cap == "recordActivity" and "activityKind" in slots:
                original = copy.deepcopy(slots)
                subject = ({"kind": "WORK", "id": slots["workId"]} if "workId" in slots
                           else {"kind": "SEGMENT", "id": slots["segmentId"]})
                # These are source statements in fixture vocabulary, not resolved
                # database IDs. They are intentionally not canonical receipt proof.
                def source_value(value):
                    if isinstance(value, dict) and set(value) == {"$alias"}:
                        return {"fixtureAlias": value["$alias"]}
                    if isinstance(value, dict) and set(value) == {"$result"}:
                        return {"priorSourceAction": value["$result"]["actionId"]}
                    if isinstance(value, dict):
                        return {k: source_value(v) for k, v in value.items()}
                    if isinstance(value, list):
                        return [source_value(v) for v in value]
                    return value
                no_document = case["caseId"] == "T12" and sub["id"] == "no-receipt-evidence"
                chain = document_and_event(action["id"], action["actorRef"], subject,
                    slots["activityKind"], source_value(original), slots["sourceNamespace"],
                    slots.get("externalEventId", case["caseId"] + "-" + sub["id"] + "-" + action["id"]),
                    slots["sourceVersion"], slots["occurredAt"], slots.get("quantity"), slots.get("unit"),
                    # A point observation does not imply coverage through the next point.
                    document=not no_document)
                action["request"] = chain[-1]["request"]
                action["request"]["commandIdempotencyKey"] = request["commandIdempotencyKey"]
                action["request"]["evidenceRefs"] = [] if no_document else [result(action["id"] + "-document")]
                output.extend(chain[:-1])
                namespace = slots["sourceNamespace"]
                profiles = f["baseline"].setdefault("sourceProfiles", [])
                if not any(p["namespace"] == namespace for p in profiles):
                    profiles.append({"namespace": namespace, "policyVersion": "fixture-v1",
                                     "reconciliationOwnerAlias": "A"})
                if not no_document:
                    # Both the recording actor and its actual delegator need this
                    # capability. Scope composition stays the existing all-of rule.
                    who = {action["actorRef"], f["actors"][action["actorRef"]]["grant"]["delegatorAlias"]}
                    for name in who:
                        for caps in (f["actors"][name]["roleCapabilities"], f["actors"][name]["grant"]["actions"]):
                            if "attachEvidence" not in caps:
                                caps.append("attachEvidence")
            output.append(action)
        sub["actions"] = output
    return case


def main():
    """Reapply owned repairs without touching T13 or executing any scenario."""
    fixtures = {}
    def fixture(ref):
        if ref not in fixtures:
            fixtures[ref] = json.loads((ROOT / ref).read_text())
        return fixtures[ref]
    for name in CASES:
        path = ROOT / "verification/cases" / name / "case.json"
        case = repair_inputs(json.loads(path.read_text()), fixture)
        path.write_text(json.dumps(case, ensure_ascii=False, indent=2) + "\n")
        # Insert only added actions into the hand-authored Korean feature. Existing
        # assertion lines are not rewritten or generated from observed results.
        feature = path.with_name("scenario.feature")
        text = feature.read_text()
        for sub in case["subcases"]:
            marker = f'"{sub["id"]}"를 준비한다'
            start = text.index(marker)
            end = text.find('\n  시나리오:', start)
            if end < 0:
                end = len(text)
            block = text[start:end]
            for action in sub["actions"]:
                if action["id"].endswith("-document"):
                    following = action["id"][:-9]
                    line = f'    만일 "{action["actorRef"]}" 역할이 "{action["id"]}" 행동을 수행한다\n'
                    target = f'    만일 "{action["actorRef"]}" 역할이 "{following}" 행동을 수행한다\n'
                    # Repeated IDs occur in separate subcases; replace only those
                    # missing the immediate document action.
                    block = block.replace(line + target, target).replace(target, line + target)
            text = text[:start] + block + text[end:]
        feature.write_text(text)
    for ref, content in fixtures.items():
        (ROOT / ref).write_text(json.dumps(content, ensure_ascii=False, indent=2) + "\n")


if __name__ == "__main__":
    main()
