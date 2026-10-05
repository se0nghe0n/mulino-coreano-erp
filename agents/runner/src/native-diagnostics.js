import {createHash} from 'node:crypto';

/** Reduce native error metadata to fixed categories; never retain free-form child output. */
export function claudeDiagnostics(event) {
  const knownSubtypes = new Set(['success', 'error_max_turns', 'error_max_budget_usd',
    'error_max_structured_output_retries', 'error_during_execution']);
  const result = {};
  if (knownSubtypes.has(event.subtype)) result.nativeResultSubtype = event.subtype;
  if (!event.is_error && event.subtype === 'success') return result;
  const errors = Array.isArray(event.errors) ? event.errors : [];
  const status = [event.error?.status, ...errors.map(error => error?.status)]
    .find(value => Number.isInteger(value) && value >= 400 && value <= 599);
  if (status !== undefined) result.nativeHttpStatus = status;
  const text = [typeof event.result === 'string' ? event.result : '',
    typeof event.error?.type === 'string' ? event.error.type : '',
    ...errors.map(error => typeof error === 'string' ? error : (typeof error?.message === 'string' ? error.message : ''))]
    .join('\n').slice(0, 8192);
  Object.assign(result, nativeErrorShape(event, text));
  const subtypeCategory = { error_max_turns: 'TURN_LIMIT', error_max_budget_usd: 'BUDGET_LIMIT',
    error_max_structured_output_retries: 'SCHEMA_RETRIES' }[event.subtype];
  result.nativeFailureCategory = subtypeCategory
    ?? (/(?:json.?schema|structured output).{0,100}(?:invalid|unsupported|not supported|error)|(?:invalid|unsupported).{0,60}(?:json.?schema|structured output)/i.test(text) ? 'SCHEMA_CONFIGURATION' : undefined)
    ?? (/(?:allowed.?tools|tool configuration|tool choice|permission.mode).{0,80}(?:invalid|unsupported|not supported|error)|(?:invalid|unsupported).{0,60}(?:allowed.?tools|tool choice)/i.test(text) ? 'TOOL_CONFIGURATION' : undefined)
    ?? (/read.only file system|permission denied|EACCES|EROFS|cannot (?:create|write|open).{0,40}(?:file|directory)/i.test(text) ? 'FILESYSTEM' : undefined)
    ?? (/unknown (?:field|key)|unrecognized argument|unexpected argument|invalid.{0,40}config|config.{0,40}parse/i.test(text) ? 'CONFIGURATION' : undefined)
    ?? (/output.{0,50}(?:validation failed|schema mismatch)|structured output.{0,50}validation/i.test(text) ? 'OUTPUT_VALIDATION' : undefined)
    ?? (/context.{0,40}(?:too long|exceed|limit)|prompt.{0,40}too long|input.{0,40}too (?:long|large)|context_length_exceeded/i.test(text) ? 'CONTEXT_LIMIT' : undefined)
    ?? (/credit balance is too low|insufficient credits|billing_error/i.test(text) ? 'BILLING' : undefined)
    ?? (/hit your (usage )?limit|usage limit (reached|exceeded)|quota exceeded|insufficient_quota/i.test(text) ? 'USAGE_LIMIT' : undefined)
    ?? (/authentication_error|not logged in|invalid api key|oauth token.{0,30}expired/i.test(text) || status === 401 ? 'AUTHENTICATION' : undefined)
    ?? (/model_not_found|model.{0,80}(does not exist|do not have access)|not have access to.{0,80}model/i.test(text) ? 'MODEL_ACCESS' : undefined)
    ?? (/rate_limit_error|too many requests/i.test(text) || status === 429 ? 'RATE_LIMIT' : undefined)
    ?? (status === 403 ? 'FORBIDDEN' : undefined)
    ?? (status !== undefined && status >= 500 ? 'PROVIDER_SERVER' : undefined)
    ?? 'UNKNOWN_NATIVE_FAILURE';
  return result;
}

/** Only known key names, primitive counts and an irreversible error fingerprint leave memory. */
function nativeErrorShape(event, text) {
  const allowedKeys = new Set(['type','subtype','is_error','result','error','errors','usage','modelUsage',
    'total_cost_usd','num_turns','structured_output','stop_reason','duration_ms','duration_api_ms',
    'session_id','uuid','permission_denials']);
  const counts = {string:0,number:0,boolean:0,null:0,array:0,object:0};
  for (const value of Object.values(event)) {
    const kind = value === null ? 'null' : Array.isArray(value) ? 'array' : typeof value;
    if (Object.hasOwn(counts,kind)) counts[kind]++;
  }
  return {nativeErrorKeys:Object.keys(event).filter(key=>allowedKeys.has(key)).sort(),
    nativeErrorPrimitiveCounts:counts,nativeErrorFingerprint:createHash('sha256').update(text).digest('hex')};
}

/** Codex protocol errors are diagnostics, never an agent result or free-form log. */
export function codexDiagnostics(event) {
  const result = {nativeFrameKind:event.type === 'turn.failed' ? 'turn.failed' : 'error'};
  const error = event.error && typeof event.error === 'object' ? event.error : {};
  const knownCodes = new Set(['unauthorized','invalid_api_key','model_not_found','rate_limit_exceeded',
    'insufficient_quota','usage_limit_reached','workspace_routing_unauthorized','permission_denied','invalid_config']);
  const code = [error.code,event.code].find(value => knownCodes.has(value));
  if (code) result.nativeErrorCode = code;
  const text = [event.message,typeof event.error === 'string' ? event.error : '',error.message,error.error?.message,code]
    .filter(value => typeof value === 'string').join('\n').slice(0,8192);
  const structuredStatus = [error.status,error.http_status_code,event.status]
    .find(value => Number.isInteger(value) && value >= 400 && value <= 599);
  const statusText = text.match(/(?:HTTP(?: status)?|status(?: code)?|unauthorized|forbidden)[^0-9]{0,12}(401|403|429|5[0-9]{2})\b/i);
  const status = structuredStatus ?? (statusText ? Number(statusText[1]) : undefined);
  if (status !== undefined) result.nativeHttpStatus = status;
  result.nativeFailureCategory = /workspace.{0,40}routing|workspace_routing_unauthorized/i.test(text) ? 'WORKSPACE_ROUTING'
    : /quota|insufficient_quota|usage_limit_reached|hit your (usage )?limit|usage limit (reached|exceeded)/i.test(text) ? 'USAGE_LIMIT'
    : status === 401 || /authentication|unauthorized|invalid.api.key|not logged in|token.{0,40}(expired|invalid)/i.test(text) ? 'AUTHENTICATION'
    : /model_not_found|model.{0,80}(does not exist|not supported|do not have access|not available)/i.test(text) ? 'MODEL_ACCESS'
    : status === 429 || /rate.limit|too many requests/i.test(text) ? 'RATE_LIMIT'
    : status === 403 || /forbidden/i.test(text) ? 'FORBIDDEN'
    : /unknown (field|key)|unrecognized argument|unexpected argument|invalid.{0,40}config|config.{0,40}parse/i.test(text) ? 'CONFIGURATION'
    : /read.only file system|permission denied|EACCES|EROFS/i.test(text) ? 'FILESYSTEM'
    : status !== undefined && status >= 500 ? 'PROVIDER_SERVER'
    : /connection reset|stream disconnected|TLS|SSL|certificate verify|network error/i.test(text) ? 'TRANSPORT'
    : 'UNKNOWN_NATIVE_FAILURE';
  return result;
}
