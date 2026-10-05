/** Host Human transport only. Credentials never belong to tool arguments or schemas. */
export function humanHeaders(env = process.env) {
  return { "X-Mulino-Local-Role": env.MULINO_LOCAL_ROLE ?? "OPERATOR",
    ...(env.MULINO_LOCAL_HUMAN_SECRET ? { "X-Mulino-Local-Human": env.MULINO_LOCAL_HUMAN_SECRET } : {}) };
}
export function safeHumanError(message, env = process.env) {
  const secret = env.MULINO_LOCAL_HUMAN_SECRET;
  return secret ? String(message).split(secret).join("[REDACTED]") : String(message);
}
