"""Reviewable observation-only corrections; original inputs and business oracles stay fixed."""
import copy


def apply(value):
    result = copy.deepcopy(value)
    for sub in result['subcases']:
        assertions = {a['id']: a for a in sub['assertions']}
        if sub['id'] == 'cumulative-versus-state':
            observe = next(a for a in sub['actions'] if a['id'] == 'after-db')['observation']
            assert 'receipts' not in observe['sources']
            observe['sources'].append('receipts')
            for aid in ('raw-receipt-sum100', 'arrival-distinct-identities'):
                a = assertions[aid]
                for key in ('source', 'unitSource'):
                    if key not in a:
                        continue
                    assert a[key]['pointer'] == '/data/rawRows/contributions'
                    a[key]['pointer'] = '/data/rawRows/receipts'
                if aid == 'arrival-distinct-identities':
                    assert a['source']['field'] == 'occurrenceId'
                    a['source']['field'] = 'canonicalOccurrenceId'
        if sub['id'] == 'throughout-complete':
            after = next(a for a in sub['actions'] if a['id'] == 'after-db')
            before = copy.deepcopy(after)
            before['id'] = 'movement-baseline'
            before['evidenceRefs'] = ['movement-baseline:actual-artifact']
            before['observation']['snapshotRef'] = 'CURRENT_COMMITTED'
            # Same authored boundary, before any business commands. The fixture's
            # INITIAL_BALANCE stays a real row in both independent observations.
            before['observation']['sources'] = ['movements']
            sub['actions'].insert(1, before)
            a = assertions['no-quantity-changing-events']
            assert a['op'] == 'count' and a['expected'] == 0 and 'baseline' not in a
            a['op'] = 'countAdded'
            a['baseline'] = dict(actionId='movement-baseline', pointer='/data/rawRows/movements')
            a['evidenceRefs'].append('movement-baseline:actual-artifact')
    return result


def restore(value):
    result = copy.deepcopy(value)
    for sub in result['subcases']:
        assertions = {a['id']: a for a in sub['assertions']}
        if sub['id'] == 'cumulative-versus-state':
            obs = next(a for a in sub['actions'] if a['id'] == 'after-db')['observation']
            assert obs['sources'][-1] == 'receipts'
            obs['sources'].pop()
            for aid in ('raw-receipt-sum100', 'arrival-distinct-identities'):
                a = assertions[aid]
                for key in ('source', 'unitSource'):
                    if key in a:
                        assert a[key]['pointer'] == '/data/rawRows/receipts'
                        a[key]['pointer'] = '/data/rawRows/contributions'
                if aid == 'arrival-distinct-identities':
                    assert a['source']['field'] == 'canonicalOccurrenceId'
                    a['source']['field'] = 'occurrenceId'
        if sub['id'] == 'throughout-complete':
            a = assertions['no-quantity-changing-events']
            assert a['op'] == 'countAdded' and a['expected'] == 0
            assert a.pop('baseline') == dict(actionId='movement-baseline', pointer='/data/rawRows/movements')
            assert a['evidenceRefs'].pop() == 'movement-baseline:actual-artifact'
            a['op'] = 'count'
            assert sub['actions'][1]['id'] == 'movement-baseline'
            sub['actions'].pop(1)
    # Check the entire round trip, including the newly added observation action.
    if apply(result) != value:
        raise ValueError('Undeclared T09 observer selector drift')
    return result
