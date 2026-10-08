#!/usr/bin/env python3
"""Regenerate docs/execution/s4-integration/schema-compatibility.json from a real
S1ReadIntegrationTest observation (backend/target/s4-compatibility-observed.json).

The observed artifact is written by
S1ReadIntegrationTest.freshFlywayMatchesAllCdsColumnsAndPrimaryKeysWithExplicitWidening
after it applies fresh Flyway and the compiled CDS DDL to one PostgreSQL container.
Counts, column inventories and widening/strengthening lists are copied from that
observation; nothing is typed by hand. Only the review reasons for columns added
after the previous inventory are authored here, and the script refuses to write
when an observed column has no reviewed entry or when the observation reports a
structural or primary-key difference.

Usage (repository root):
  python3 -I docs/execution/s4c-settlement/regenerate-schema-compatibility.py \
    <observed.json> <baseline-commit> <migration-range>
"""
import hashlib, json, pathlib, sys

COMPAT = pathlib.Path("docs/execution/s4-integration/schema-compatibility.json")
S3_COMPAT = pathlib.Path("docs/execution/s3-integration/schema-compatibility.json")

# Columns added after the 1863-column inventory (commit 4c1ea087).
REVIEWED = {
    "mulino_inventory_deliverytransfers.legitimatequantity": {
        "migration": "database/migrations/V24__fulfillment_ranges.sql",
        "cds": "backend/db/inventory.cds",
        "notNullStrengtheningReason": "관측 인도 범위 중 정당한 인도 기여량을 0 이상 원 수량 이하의 필수 값으로 보존한다(8eb247d8).",
    },
    "mulino_trade_sales_deliveries.legitimaterangesjson": {
        "migration": "database/migrations/V30__delivery_legitimate_ranges.sql",
        "cds": "backend/db/sales.cds",
        "nullableReason": "확정 인도의 정당한 실물 좌표를 불변 JSON 배열로 보존해 인도 정정이 정확한 범위를 교차하게 한다. V30 이전 행과 다른 모듈의 직접 fixture에는 좌표가 없어 nullable이며 없으면 보수적 하한을 쓴다(s4-sales-05).",
    },
    "mulino_trade_settlement_matches.scopedifference": {
        "migration": "database/migrations/V29__settlement_scope_difference.sql",
        "cds": "backend/db/settlement.cds",
        "notNullStrengtheningReason": "같은 수령·인도 기여에 대한 누적 송장 중복을 별도 정산 차이로 불변 보존한다(D19).",
    },
}


def main(observed_path, baseline, migrations):
    raw = pathlib.Path(observed_path).read_bytes()
    observed = json.loads(raw)
    if observed["structuralDifferences"] or observed["primaryKeyDifferences"]:
        sys.exit("observation has structural or primary-key differences; fix source, not inventory")
    columns = sorted(observed["columns"])
    if len(columns) != observed["columnCount"]:
        sys.exit("observed column list and count disagree")
    current = json.loads(COMPAT.read_text())
    s3_count = json.loads(S3_COMPAT.read_text())["observedColumnCount"]
    s3 = set(current["exactColumnInventory"]) - set(current["s4AddedColumnInventory"])
    if len(s3) != s3_count:
        sys.exit(f"S3 inventory derivation gives {len(s3)}, expected {s3_count}")
    if not s3 <= set(columns):
        sys.exit(f"S3 columns disappeared: {sorted(s3 - set(columns))}")
    reviewed = dict(current["reviewedAdditions"])
    for key, entry in REVIEWED.items():
        if key in columns and key not in reviewed:
            reviewed[key] = entry
    added = sorted(set(columns) - s3)
    missing = [c for c in added if c not in reviewed]
    if missing:
        sys.exit(f"unreviewed S4 columns: {missing}")
    stale = [c for c in reviewed if c not in columns]
    if stale:
        sys.exit(f"reviewed columns not observed: {stale}")
    n = len(columns)
    current["rationale"]["scope"] = (
        f"Fresh {migrations} PostgreSQL comparison: exact {n} column names, sizes, decimal scales, PKs; "
        "only explicit timestamp widening and NOT NULL strengthening.")
    current["timestampWidening"] = sorted(observed["timestampWidening"])
    current["notNullStrengthening"] = sorted(observed["notNullStrengthening"])
    current["baseline"] = baseline
    current["observedColumnCount"] = n
    current["observedArtifactSha256"] = hashlib.sha256(raw).hexdigest()
    current["scope"] = (
        f"S4 {migrations} exact current CDS/Flyway comparison; earlier S3 {s3_count} columns "
        f"plus exact {len(added)} additions")
    current["reviewedAdditions"] = reviewed
    current["exactColumnInventory"] = columns
    current["s4AddedColumnInventory"] = added
    COMPAT.write_text(json.dumps(current, indent=2, ensure_ascii=False) + "\n")
    print(json.dumps({"columns": n, "s3": s3_count, "s4Added": len(added),
                      "timestampWidening": len(current["timestampWidening"]),
                      "notNullStrengthening": len(current["notNullStrengthening"]),
                      "observedArtifactSha256": current["observedArtifactSha256"]}))


if __name__ == "__main__":
    main(*sys.argv[1:4])
