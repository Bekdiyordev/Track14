/** Region close to Uzbekistan with Cloud Functions gen 2 support. Keep equal to the Firestore location region. */
export const REGION = "europe-central2";

/** App Check is enforced in production; the local emulator has no attestation. */
export const ENFORCE_APP_CHECK = process.env.FUNCTIONS_EMULATOR !== "true";

export const callableOptions = {
  region: REGION,
  enforceAppCheck: ENFORCE_APP_CHECK,
  // Replay protection on the money paths is added together with limited-use tokens on the client.
  consumeAppCheckToken: false,
  memory: "256MiB" as const,
  maxInstances: 20,
};
