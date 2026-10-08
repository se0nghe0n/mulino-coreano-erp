# Claude 인계 뒤 사용자 Step 1·2 재검토

2026-10-08 사용자가 매 Step의 adversarial reviewer를 Claude Opus
`xhigh`와 Claude Fable `low`로 다시 지정했다. Codex 시기에 Sol xhigh와
Astra low가 닫은 Step 1(skills)과 Step 2(tests)를 같은 기준으로 다시
검토했다. 대상은 Task HEAD `d0ae28ca`~`7b540e8f`의 skills·contracts·
harness·coverage·model·고위험 case다. Step 3 actual adapter는 제외했다.

## 방법

- slice 5개(skills, contracts-harness, catalog-coverage-model,
  cases-high-risk-a: C3/T20/T25/T26, cases-high-risk-b: T17/V2/V3/V8/
  C1/C4/E1/E2)마다 Opus xhigh와 Fable low가 독립 검토했다.
- P0–P2 지적마다 Opus xhigh skeptic이 반박을 시도했다. 반박되지 않은
  지적만 수정 대상으로 확정했다. P3은 검증 없이 각 worker에게 참고로
  전달했다.
- 첫 실행은 사용량 한도로 40개 agent가 실패했고 같은 script를 resume해
  나머지를 실행했다. 최종 81 agent 완료, 오류0이다.
- 결과: slice별 판정은 10개 모두 FAIL이다. 확정 지적 54건(P0 1, P1
  18, P2 29, P3 6), 반박 17건, 미검증 P3 15건이다. 확정 지적의 원문,
  반박 근거는 [evidence](evidence/claude-rereview/)에 보존했다.

## 확정 지적과 수정 소유자

수정은 해당 사용자 Step의 지정 모델로 수행한다. Step 1은 GPT-6.1 Sol
high, Step 2는 Claude Opus high다. 모든 worker는 `7b540e8f`에서 만든
독립 worktree에서 작업하고 coordinator만 통합한다.

### step1 — `step1r/skills` / GPT-6.1 Sol high (T3 delegate)

| ID | 판정 등급 | 원 등급 | reviewer | 지적 | 위치 |
|---|---|---|---|---|---|
| step1-00 | P2 | P1 | opus-xhigh | The skills send execution evidence to a manifest path that doesn't exist and to a template the repo's validator never reads. The template has no evidence-class field, so stub or selftest output can be recorded as PASS. | `.agents/skills/ontology-scenario-testing/SKILL.md:24` |
| step1-01 | P2 | P2 | opus-xhigh | The scenario skill, its Gherkin template and the @pending rule don't match the repo's executable harness. Gherkin written to the harness ends up opaque, which breaks the skill's own business-readable 'then' rule. | `.agents/skills/ontology-scenario-testing/SKILL.md:22` |
| step1-02 | P2 | P2 | opus-xhigh | The V4 'every path' oracle uses a fixed list of path types and never requires enumerating the write surfaces actually exposed. With CAP adopted, any CDS entity a developer later exposes gets generic CREATE/UPDATE/DELETE handlers that the fixed list would not catch. | `.agents/skills/ontology-implementation/references/implementation-contracts.md:50` |
| step1-03 | P2 | P2 | fable-low | Scenario Gherkin template is not executable by the delivered harness grammar; new cases written from it cannot run | `.agents/skills/ontology-scenario-testing/assets/scenario.feature.template:9` |
| step1-04 | P2 | P2 | fable-low | evidence.json template does not match the delivered evidence schema; hand-filled copies are not consumed by coverage and can masquerade as results | `.agents/skills/ontology-scenario-testing/assets/evidence.json:6` |
| step1-05 | P3 | P2 | opus-xhigh | stage-gates.md still has the old Sol/Astra model and reviewer table, which contradicts the AGENTS.md table from d0ae28ca. Its S-gate index also drops required cases. | `.agents/skills/ontology-implementation/references/stage-gates.md:10` |
| step1-06 | P3 | P2 | fable-low | stage-gates.md still carries the superseded Sol/Astra model table, contradicting AGENTS.md d0ae28ca | `.agents/skills/ontology-implementation/references/stage-gates.md:10` |

### harness — `step2r/harness` / Claude Opus high

| ID | 판정 등급 | 원 등급 | reviewer | 지적 | 위치 |
|---|---|---|---|---|---|
| harness-00 | P1 | P1 | opus-xhigh | Main always uses AgentRunner.Scripted, but all 30 C3 agent actions have intent:{}, so any actual profile that includes C3 aborts with exit 3 and writes no report | `verification/harness/src/main/java/org/mulino/verification/Main.java:46` |
| harness-01 | P1 | P1 | opus-xhigh | The T08 'verified auth' oracle reads the harness driver's own provenance, so a server that trusts the payload actor still passes payload-actor | `contracts/acceptance-driver.schema.json:38` |
| harness-02 | P2 | P1 | opus-xhigh | Every case observe requires the observer's DB snapshot id to equal a token taken from the SUT, so only an observer that echoes the token can pass | `verification/harness/src/main/java/org/mulino/verification/CaseRunner.java:102` |
| harness-03 | P2 | P2 | opus-xhigh | Process-control evidence labelled CAPTURED_SELFTEST is accepted in product profile runs, and no process subcase asserts ACTUAL_HOST | `verification/harness/src/main/java/org/mulino/verification/HostObservationValidator.java:24` |
| harness-04 | P2 | P2 | opus-xhigh | Observation `data` is free-form and not bound to raw rows, yet the guide's template and the primary quantity oracles (V7, V2, T23, V8, T26) assert on /data/data | `contracts/acceptance-observation.schema.json:87` |
| harness-05 | P3 | P2 | opus-xhigh | The plan's `./verify model --manifest X` and `deployment --manifest X` entrypoints misparse the manifest as a case file; with --actual also refused, these gates have no runnable path | `verify:26` |
| harness-06 | P3 | P2 | opus-xhigh | Catalog artifactKinds are never enforced: 25 observations that require db_snapshot pass on API responses alone | `verification/harness/src/main/java/org/mulino/verification/CatalogLinkValidator.java:28` |

### coverage — `step2r/coverage` / Claude Opus high

| ID | 판정 등급 | 원 등급 | reviewer | 지적 | 위치 |
|---|---|---|---|---|---|
| coverage-00 | P1 | P1 | opus-xhigh | 79 named observations (including all of E1/E2) can never PASS because case profiles omit profiles that the oracle's requiredLayers demand | `verification/coverage/assemble.py:685` |
| coverage-01 | P1 | P1 | fable-low | 21 catalog oracles require layers/profiles their case never declares; preparation still reports PREPARED while those observations are structurally unreachable | `verification/coverage/assemble.py:686` |
| coverage-02 | P1 | P1 | opus-xhigh | 79 of 499 named observations, including every E1 and E2 observation, can never reach PASS because no case covers the required profile; preparation does not detect this | `verification/coverage/assemble.py:224` |
| coverage-03 | P2 | P1 | opus-xhigh | An empty or null normative lock turns off lock enforcement in the coverage assembler | `verification/coverage/assemble.py:125` |
| coverage-04 | P2 | P1 | opus-xhigh | Model-corpus oracles can be weakened without detection: generate.py rewrites every hash and no check runs the corpus validator | `verification/model-binding/generate.py:9` |
| coverage-05 | P2 | P2 | opus-xhigh | Coverage can report PASS from a dirty working tree, so the recorded code commit does not identify the tested code | `verification/coverage/assemble.py:694` |
| coverage-06 | P2 | P2 | opus-xhigh | UAT QUERY turns pass without the model actually answering, because the harness's own context read satisfies the response oracles | `verification/harness/src/main/java/org/mulino/verification/modelbinding/ModelBindingRunner.java:161` |
| coverage-07 | P2 | P2 | opus-xhigh | The §13.3 'clear requests structured correctly ≥95%' threshold is mis-encoded: exact-match hard FAIL in the runner, narrowed denominator in the docs, and no producer of the rate | `verification/harness/src/main/java/org/mulino/verification/modelbinding/ModelBindingRunner.java:200` |
| coverage-08 | P3 | P2 | opus-xhigh | The assembler accepts the runner's PASS without re-checking observed values against expected, and never compares op or filters | `verification/coverage/assemble.py:403` |

### cases-a — `step2r/cases-a` / Claude Opus high

| ID | 판정 등급 | 원 등급 | reviewer | 지적 | 위치 |
|---|---|---|---|---|---|
| cases-a-00 | P1 | P1 | opus-xhigh | T25 (cases, outside slice A) fixes Step-2 NOT_RUN as the expected result and contradicts itself on the same runtime manifest, and its runtime links never require PASS | `verification/cases/T25/case.json:92303` |
| cases-a-01 | P1 | P0 | opus-xhigh | C3 no-effect oracle never observes the primary effect tables of most of the 93 write capabilities | `verification/cases/C3/case.json:2018` |
| cases-a-02 | P1 | P1 | opus-xhigh | T25 hard-codes the current NOT_RUN state as expected values, so T25 can never PASS on a completed system | `verification/cases/T25/case.json:4128` |
| cases-a-03 | P1 | P1 | opus-xhigh | T26 safe-retry has the caller supply originalActorId and canonicalRequestHash, the oracle reads them back, and no forged-actor case exists | `verification/cases/T26/case.json:6638` |
| cases-a-04 | P1 | P2 | opus-xhigh | The six T20 skill subcases are identical and do not test any skill's required procedure | `verification/cases/T20/case.json:45505` |
| cases-a-05 | P1 | P2 | opus-xhigh | T26 never tests autonomous scheduling; every recovery is triggered by the harness | `verification/cases/T26/case.json:9299` |
| cases-a-06 | P1 | P1 | fable-low | T25 coverage-none hard-codes the current NOT_RUN state as the expected value | `verification/cases/T25/case.json:4026` |
| cases-a-07 | P1 | P1 | fable-low | C3 closeRecall positive counter-call expects APPLIED on a PROPOSED recall with no recovery/ADMIN closure chain | `verification/cases/C3/case.json:2926` |
| cases-a-08 | P2 | P2 | opus-xhigh | Post-Step-2 T20 edit (4233e7ec) drifted from its generator and left the tools/list call almost unasserted; the repair note misstates this | `verification/cases/T20/case.json:6701` |
| cases-a-09 | P2 | P2 | opus-xhigh | T20 error-path oracles contradict the S0 wire contract and its real responses, and do not check the official error codes | `verification/cases/T20/case.json:14768` |
| cases-a-10 | P2 | P2 | opus-xhigh | T20 host-* boundary checks count only COMMAND tool calls, so RECORD writes induced by a malicious document or synonym go undetected | `verification/cases/T20/case.json:53746` |
| cases-a-11 | P2 | P2 | opus-xhigh | C3 model-query checks pass when the client never had MCP tools | `verification/cases/C3/case.json:14353` |
| cases-a-12 | P2 | P2 | fable-low | C3 model-query oracle is vacuous: a model that calls no tool at all passes | `verification/cases/C3/case.json:14340` |
| cases-a-13 | P2 | P2 | fable-low | T20 host-allowed-tools-write never exercises a write attempt, so the 'allowed-tools is not server authorization' oracle cannot discriminate | `verification/cases/T20/case.json:53244` |
| cases-a-14 | P2 | P2 | fable-low | T20 MRTR negative variants assert only outcome REJECTED; expiry TTL is unspecified | `verification/cases/T20/case.json:30046` |
| cases-a-15 | P3 | P2 | fable-low | C3 denied attempt and authorized counter-call reuse one commandIdempotencyKey across different principals | `verification/cases/C3/author_prerequisites.py:163` |

### cases-b — `step2r/cases-b` / Claude Opus high

| ID | 판정 등급 | 원 등급 | reviewer | 지적 | 위치 |
|---|---|---|---|---|---|
| cases-b-00 | P0 | P0 | opus-xhigh | E2: the check that releasing only QC20 does not allow dispatch60 is vacuous, and the false-close probe is malformed | `verification/cases/E2/case.json:610` |
| cases-b-01 | P1 | P1 | opus-xhigh | V2/V3 race oracles pass under sequential execution; nothing proves the contender's initial read came before the winner's commit, and V2 has no lock/fence evidence | `verification/cases/V2/case.json:1293` |
| cases-b-02 | P1 | P1 | opus-xhigh | V2 'new reservation 20' targets an order line already fully covered (ORDER qty 40 = ALLOC 40), so the oracle requires over-reservation and the race effect check is non-diagnostic | `verification/cases/V2/fixtures/reserve-commits-first.json:447` |
| cases-b-03 | P1 | P1 | fable-low | V3 dispatch-first demands a stale-revision placeHold be APPLIED, contradicting the revision contract and V2 | `verification/cases/V3/case.json:1392` |
| cases-b-04 | P1 | P1 | fable-low | E2 overlapping-holds: dispatch60 has no outcome/errorCode assertion and carries a foreign revision, so a system that ignores the recall hold passes | `verification/cases/E2/case.json:610` |
| cases-b-05 | P1 | P1 | fable-low | C4 and E1 encode contradictory semantics of obligations.current for resolved obligations | `verification/cases/E1/case.json:7164` |
| cases-b-06 | P2 | P2 | opus-xhigh | C4: the assessment check meant to show the return does not overwrite the delivery runs before the return happens | `verification/cases/C4/case.json:1355` |
| cases-b-07 | P2 | P2 | opus-xhigh | E1 agency-unverified-held filters by a per-axis regulatoryStatus that cannot equal 30 in a correct ledger | `verification/cases/E1/case.json:2520` |
| cases-b-08 | P2 | P2 | opus-xhigh | V2 actual50: the bounds ≤50 and ≥10 are never tied together, so lost or double-counted promise quantity passes | `verification/cases/V2/case.json:3716` |
| cases-b-09 | P2 | P2 | opus-xhigh | Error-code pointers differ across cases and conflict with the command-response schema | `verification/cases/V2/case.json:1` |
| cases-b-10 | P2 | P2 | opus-xhigh | E1 noun/verb cross-check compares the two entry points only with each other, never with the independent ledger | `verification/cases/E1/case.json:6777` |
| cases-b-11 | P2 | P1 | fable-low | Error-code pointer shape differs across cases; no wire contract defines it | `verification/cases/E2/case.json:5565` |
| cases-b-12 | P2 | P2 | fable-low | C4 return-not-correction asserts the pre-return assessment, never the post-return one | `verification/cases/C4/case.json:1355` |
| cases-b-13 | P2 | P2 | fable-low | V2 bounds admit degenerate outcomes (API/DB mismatch and whole-allocation suspension) | `verification/cases/V2/case.json:890` |
| cases-b-14 | P2 | P2 | fable-low | testBarrier request slot and self-reported transactions/locks sources are undocumented; V2/V3 are ordering races, not lock races | `verification/cases/V2/case.json:147` |

## 반박된 지적

- [skills/opus-xhigh] Skills never tie PASS evidence to the exact integration commit that was built, and the 'don't repeat checks' rule has no minimum. A 23-commit stretch that never compiled went through with PASS-style claims.
- [skills/opus-xhigh] The skill's approval list leaves out half of the plan §7.1 approval rules, while also telling agents not to add approvals.
- [skills/opus-xhigh] The E1 worked example weakens the duplicate-evidence oracle and gives approval-relevant steps to the wrong roles.
- [skills/fable-low] Skills describe ./verify only as a not-yet-existing plan contract; they omit that plan-named profiles always return NOT_RUN without --actual and that real execution lives in actual-s1..s4
- [contracts-harness/opus-xhigh] The plan-named command applyObservedDelivery is missing from the capability registry, so no case can test it
- [contracts-harness/fable-low] Process-control cases can PASS on CAPTURED_SELFTEST host evidence; no runtime gate requires ACTUAL_HOST or command exit
- [contracts-harness/fable-low] CatalogLinkValidator ignores oracle artifactKinds/requiredLayers: db_snapshot observations are satisfied by the product's own API response
- [contracts-harness/fable-low] Product-side contract violations abort the whole profile run as exit 3 'environment error' with no evidence report
- [contracts-harness/fable-low] `where` filter compares JSON node identity without type check, so a representation mismatch silently yields zero rows and vacuous 'no effect' PASS
- [contracts-harness/fable-low] DB observation artifacts are only existence-checked; rawRows/snapshot are never bound to artifact content or hash
- [catalog-coverage-model/fable-low] Model runner executes each corpus case once and hard-codes attempts=[]; §13.3's 60x3 repeats and per-attempt usage/cost cannot be produced by the shipped harness, yet the coverage consumer requires exactly that
- [catalog-coverage-model/fable-low] Plan §13.3 metrics (clear-structured ratio >=95% over 20x3, p50/p95 latency, clarification-question ratio) are never computed or carried by any report/schema; the assembler substitutes an all-or-nothing 180/180 PASS
- [catalog-coverage-model/fable-low] The 'after' snapshot used for effect-delta and preflight row-equality is taken at a revision reported by the step under test (agent/client result), so a stale revision yields zero delta and passes effect=0 / unchanged-row oracles
- [cases-high-risk-a/opus-xhigh] T20 fixture gives the proposer (writer) approvePurchase, and nothing tests purchase self-approval
- [cases-high-risk-a/fable-low] T26 *-expiry-delayed-guard oracles are satisfied by the seeded responsibility row and fail a correct system
- [cases-high-risk-b/opus-xhigh] C1 revoked-basis: invalid new-reserve probe, no pre-revocation positive control, and the revocation itself is unasserted
- [cases-high-risk-b/fable-low] C1 custody-not-sale: CUS60 is ineligible because disposition is UNKNOWN, so ownership/custody is never the deciding factor

## 종료 조건

모든 확정 지적이 FIXED 또는 근거 있는 NOT_A_DEFECT가 되고, 통합본에서
`./verify harness`, `./verify prepare`, 관련 validator가 통과하고, 같은
두 reviewer의 closure 검토가 PASS해야 Step 1·2를 다시 닫는다. 이
검토는 runtime PASS를 뜻하지 않는다.
