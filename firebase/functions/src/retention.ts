/** Audit entries are removed by a Firestore TTL policy on `expireAt` (privacy policy: up to 1 year). */
export const AUDIT_RETENTION_MS = 365 * 86_400_000;
export const auditExpiry = (now = Date.now()) => new Date(now + AUDIT_RETENTION_MS);
