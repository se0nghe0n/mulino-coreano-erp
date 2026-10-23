"""Bound source requests, never fixture verification flags or scenario execution.

The case runner resolves explicit INPUT encodings before sending public commands.
No source labels stand in for database identities and no verification is seeded.
"""
import copy
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


def encoded(source, encoding="json"):
    return {"$encode": {"source": copy.deepcopy(source), "encoding": encoding}}


def document_and_event(prefix, actor, subject, kind, original, namespace,
                       external_id, version, effective_from, quantity=None,
                       unit=None, effective_until=None, document=True):
    """A source observation is KNOWN as a claim, never pre-VERIFIED as a fact."""
    payload = encoded(original)
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
        actions.append(command(prefix + "-document", actor, "attachEvidence", {
            "subject": copy.deepcopy(subject), "sourceNamespace": namespace,
            "sourceReference": external_id, "mediaType": "application/json",
            "expectedHash": encoded(original, "sha256"),
            "provenance": "SYNTHETIC source observation; no legal approval or verification",
            "contentBase64": encoded(original, "base64")}, "RECORD"))
        slots.update(documentId=result(prefix + "-document"),
                     assertion="Synthetic original observation; subject to source reconciliation")
    actions.append(command(prefix, actor, "recordActivity", slots, "RECORD"))
    return actions



def identity_input(value):
    if isinstance(value, str):
        UUID(value)
    elif isinstance(value, dict) and set(value) == {"$alias"}:
        if not isinstance(value["$alias"], str) or not value["$alias"]:
            raise ValueError("Nonempty actual fixture alias required")
    elif isinstance(value, dict) and set(value) == {"$result"}:
        ref = value["$result"]
        if not isinstance(ref, dict) or set(ref) != {"actionId", "pointer"} or not all(
                isinstance(ref[k], str) and ref[k] for k in ref) or not ref["pointer"].startswith("/"):
            raise ValueError("Strict actual result reference required")
    else:
        raise ValueError("Actual UUID or strict alias/result identity required")

def receipt_chain(prefix, actor, binding, quantity, occurred_at, *,
                  transit_segment_id=None, receiving_custodian_id=None,
                  purchase_line_id=None, start_quantity="0", lot_required=True,
                  source_actor=None, original_metadata=None):
    """Build the published source→observation→match→canonical→receipt graph.

    UUIDs must come from actual prior product/fixture results. A missing LOT is
    retained for the negative confirm probe; it is never inferred from an item.
    """
    required = ("itemId", "placeId", "workId", "rangeRootId", "ownerId", "supervisorId")
    for key in required:
        identity_input(binding[key])
    if lot_required:
        identity_input(binding["lotId"])
    if Decimal(quantity) <= 0 or Decimal(start_quantity) < 0:
        raise ValueError("Positive physical quantity and nonnegative range start required")
    if transit_segment_id and receiving_custodian_id:
        raise ValueError("Transit custody cannot be replaced by a receiving custodian")
    original = copy.deepcopy(original_metadata or {})
    original.update({k: binding[k] for k in ("itemId", "placeId", "workId", "rangeRootId")})
    original.update(startQuantity=start_quantity, quantity=quantity, unit="BOX", occurredAt=occurred_at)
    if binding.get("lotId"):
        original["lotId"] = binding["lotId"]
    if receiving_custodian_id:
        original["receivingCustodianId"] = receiving_custodian_id
    namespace = binding.get("sourceNamespace", "synthetic-warehouse")
    source_actor = source_actor or actor
    actions = document_and_event(prefix + "-event", source_actor,
        {"kind": "WORK", "id": binding["workId"]}, "PHYSICAL_RECEIPT", original,
        namespace, prefix, "1", occurred_at, quantity, "BOX")
    observation = {k: original[k] for k in ("itemId", "placeId", "workId", "rangeRootId",
        "startQuantity", "quantity", "unit", "occurredAt", "lotId") if k in original}
    observation.update(eventId=result(prefix + "-event"), ownerId=binding["ownerId"],
        supervisorId=binding["supervisorId"], nextAction="원천 수령 범위와 증빙을 대조한다",
        nextCheckAt=binding["nextCheckAt"])
    if transit_segment_id:
        observation["transitSegmentId"] = transit_segment_id
    if purchase_line_id:
        observation["purchaseLineId"] = purchase_line_id
    actions.append(command(prefix + "-provisional", source_actor, "receiveProvisional", observation, "RECORD"))
    # Missing-LOT negative remains before canonical verification: ReceiptEvidenceScopes
    # deliberately cannot verify an unidentified receipt range.
    if not lot_required:
        actions.append(command(prefix, actor, "confirmReceipt", {
            "receiptId": result(prefix + "-provisional", "effects/receiptId")}))
        return actions
    actions.append(command(prefix + "-match", source_actor, "matchSourceIdentity", {
        "claimId": result(prefix + "-event", "claimId"),
        "basisDocumentId": result(prefix + "-event-document"),
        "physicalScopeId": binding["rangeRootId"],
        "policyVersion": binding["policyVersion"],
        "sourceIdentity": namespace + ":" + prefix + ":1", "quantity": quantity,
        "unit": "BOX", "effectiveFrom": occurred_at,
        "reason": "원문·관측·실제 수령 범위의 동일성을 대조한다"},
        revision=result(prefix + "-event", "revision")))
    actions.append(command(prefix + "-canonical", source_actor, "linkCanonicalOccurrence", {
        "reconciliationId": result(prefix + "-match")},
        revision=result(prefix + "-match", "revision")))
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
                no_document = case["caseId"] == "T12" and sub["id"] == "no-receipt-evidence"
                chain = document_and_event(action["id"], action["actorRef"], subject,
                    slots["activityKind"], original, slots["sourceNamespace"],
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
    return connect_sources(case, fixture)


# Each receipt keeps its original final action ID. These are authoritative Work
# inputs, not a guess from observed goal/quantity output.
RECEIPT_WORKS = {
    ("T09", "cumulative-versus-state"): "cumulative",
    ("T09", "receipt-missing-lot"): "draft",
    ("T11", "parent90-cancelled"): "parent",
    ("T11", "parent90-impossible"): "parent",
    ("T11", "parent90-superseded"): "parent",
    ("T11", "shared-distinct60"): "parent-one",
    ("T12", "wrong-warehouse"): "work",
    ("T12", "deadline-revision-and-correction"): "work",
}
SOURCE_ACTOR = "receiptSource"
SOURCE_CAPABILITIES = ["attachEvidence", "recordActivity", "receiveProvisional",
                       "matchSourceIdentity", "linkCanonicalOccurrence", "getEvidence"]


def install_source_actor(fixture, policy):
    """One synthetic source reviewer. Existing business actors/scopes are untouched."""
    fixture["aliases"][SOURCE_ACTOR] = {"type": "Human"}
    basis = fixture["actors"]["A"]
    fixture["actors"][SOURCE_ACTOR] = {
        "issuer": basis["issuer"], "subject": fixture["fixtureId"] + "-receipt-source",
        "audience": basis["audience"], "organizationAlias": "ORG",
        "roleCapabilities": list(SOURCE_CAPABILITIES),
        "grant": {"delegatorAlias": SOURCE_ACTOR, "actions": list(SOURCE_CAPABILITIES),
                  "scope": {"organizationAlias": "ORG", "itemAliases": ["P"]},
                  "validFrom": basis["grant"]["validFrom"],
                  "validUntil": basis["grant"]["validUntil"], "revision": 1}}
    profiles = fixture["baseline"].setdefault("sourceProfiles", [])
    profile = {"namespace": "synthetic-receipt", "policyVersion": policy,
               "reconciliationOwnerAlias": SOURCE_ACTOR}
    old = next((p for p in profiles if p["namespace"] == profile["namespace"]), None)
    if old is not None:
        assert old == profile
    else:
        profiles.append(profile)


def live_source_references(node):
    """Undo source-label placeholders only; never derive facts from results."""
    if isinstance(node, dict):
        if set(node) == {"fixtureAlias"}:
            return {"$alias": node["fixtureAlias"]}
        if set(node) == {"priorSourceAction"}:
            return result(node["priorSourceAction"])
        return {k: live_source_references(v) for k, v in node.items()}
    if isinstance(node, list):
        return [live_source_references(v) for v in node]
    return node


def connect_sources(case, fixture):
    frontier = json.loads((ROOT / "verification/cases" / case["caseId"] / "source-contract-frontier.json").read_text())
    recipes = {(r["subcase"], r["action"]): r["originalPhysicalInput"] for r in frontier["unresolvedReceiptInputs"]}
    for sub in case["subcases"]:
        f = fixture(sub["fixtureRef"])
        output = []
        for action in sub["actions"]:
            key = (sub["id"], action["id"])
            if key not in recipes or "receiptId" in action.get("request", {}).get("slots", {}):
                output.append(action)
                continue
            original = copy.deepcopy(recipes[key])
            work = original.get("workId")
            if work is None:
                work = result(RECEIPT_WORKS[(case["caseId"], sub["id"])], "workId")
            if "$result" in work:
                owner_action = next(a for a in sub["actions"] if a["id"] == work["$result"]["actionId"])
                goal = owner_action["request"]["slots"]
                owner, supervisor = goal["ownerId"], goal["supervisorId"]
            else:
                existing = next(w for w in f["baseline"]["works"] if w["alias"] == work["$alias"])
                goal = existing["goal"]
                owner, supervisor = {"$alias": existing["ownerAlias"]}, {"$alias": existing["supervisorAlias"]}
            policy = goal["evidencePolicyVersion"]
            install_source_actor(f, policy)
            quantity = original["quantity"]["value"]
            binding = {"itemId": original.get("itemId", {"$alias": "P"}),
                "placeId": original["placeId"], "workId": work,
                "rangeRootId": original.get("segmentId", {"$alias": "Q"}),
                "ownerId": owner, "supervisorId": supervisor,
                "sourceNamespace": "synthetic-receipt", "policyVersion": policy,
                "nextCheckAt": "2026-10-08T09:00:00Z"}
            if "lotId" in original:
                binding["lotId"] = original["lotId"]
            chain = receipt_chain(action["id"], action["actorRef"], binding, quantity,
                original["occurredAt"], transit_segment_id=original.get("segmentId"),
                source_actor=SOURCE_ACTOR, lot_required="lotId" in original,
                original_metadata=original)
            # The public receipt observation stores revision 1 although its response
            # revision is 0 (no stock yet). confirmReceipt compares that row revision.
            chain[-1]["request"]["commandIdempotencyKey"] = action["request"]["commandIdempotencyKey"]
            chain[-1]["evidenceRefs"] = action["evidenceRefs"]
            output.extend(chain)
        sub["actions"] = output
        # Existing observation claims retain their missing document, reported scope,
        # conflicting amounts and timestamps while binding actual referenced IDs.
        for action in output:
            slots = action.get("request", {}).get("slots", {})
            if action.get("capabilityId") == "recordActivity" and isinstance(slots.get("payload"), str):
                original = live_source_references(json.loads(slots["payload"]))
                slots["payload"] = encoded(original)
                document = next((a for a in output if a["id"] == action["id"] + "-document"), None)
                if document:
                    ds = document["request"]["slots"]
                    ds["contentBase64"], ds["expectedHash"] = encoded(original, "base64"), encoded(original, "sha256")
        # Response shapes are public product fields; no hidden flattening adapter.
        # Only request bindings change, never assertion source/expected values.
        all_receipts = {aid for (sid, aid) in recipes if sid == sub["id"]}
        def rebind(node):
            if isinstance(node, dict):
                ref = node.get("$result")
                if ref and ref["actionId"] in all_receipts:
                    aid, ptr = ref["actionId"], ref["pointer"]
                    if ptr == "/response/segmentId":
                        ref["pointer"] = "/response/effects/segmentId"
                    elif ptr in ("/response/occurrenceId", "/response/evidenceId"):
                        ref.update(actionId=aid + "-canonical", pointer="/response/id")
                    elif ptr == "/response/activityId":
                        ref.update(actionId=aid + "-event", pointer="/response/id")
                for value in node.values():
                    rebind(value)
            elif isinstance(node, list):
                for value in node:
                    rebind(value)
        for action in output:
            rebind(action.get("request", {}))
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
        # Add missing auxiliary actions at the original action's position. Keep
        # every original scenario line and assertion byte-for-byte.
        feature = path.with_name("scenario.feature")
        text = feature.read_text()
        for sub in case["subcases"]:
            marker = f'"{sub["id"]}"를 준비한다'
            start = text.index(marker)
            end = text.find('\n  시나리오:', start)
            if end < 0:
                end = len(text)
            block = text[start:end]
            pending = []
            for action in sub["actions"]:
                if action["kind"] == "installFixture":
                    continue
                line = f'    만일 "{action.get("actorRef", "A")}" 역할이 "{action["id"]}" 행동을 수행한다\n'
                if f'"{action["id"]}" 행동을 수행한다' in block:
                    if pending:
                        anchor = next(x for x in block.splitlines(keepends=True) if f'"{action["id"]}" 행동을 수행한다' in x)
                        block = block.replace(anchor, ''.join(pending) + anchor, 1)
                        pending = []
                else:
                    pending.append(line)
            if pending:
                raise ValueError('Auxiliary actions have no original feature anchor')
            text = text[:start] + block + text[end:]
        feature.write_text(text)
    for ref, content in fixtures.items():
        (ROOT / ref).write_text(json.dumps(content, ensure_ascii=False, indent=2) + "\n")


if __name__ == "__main__":
    main()
