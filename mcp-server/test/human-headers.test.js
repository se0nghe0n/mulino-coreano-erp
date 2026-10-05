import test from 'node:test';
import assert from 'node:assert/strict';
import { humanHeaders, safeHumanError } from '../src/human-headers.js';
test('Human transport key comes only from host environment and cannot appear in returned errors', () => {
  const env={MULINO_LOCAL_HUMAN_SECRET:'host-human-sentinel',MULINO_LOCAL_ROLE:'QC'};
  assert.deepEqual(humanHeaders(env), {'X-Mulino-Local-Role':'QC','X-Mulino-Local-Human':'host-human-sentinel'});
  assert.deepEqual(humanHeaders({}), {'X-Mulino-Local-Role':'OPERATOR'});
  assert.doesNotMatch(safeHumanError('server echoed host-human-sentinel',env), /host-human-sentinel/);
});
