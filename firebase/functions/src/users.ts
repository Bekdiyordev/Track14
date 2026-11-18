import * as functionsV1 from "firebase-functions/v1";
import { getFirestore } from "firebase-admin/firestore";
import { loadEconomy } from "./economy";
import { applyEntry, readEntry } from "./ledger";
import { ledgerKey } from "./rules";
import { REGION } from "./options";

/** First sign-in: create the wallet and pay the one-time welcome bonus. */
export const onUserCreated = functionsV1
  .region(REGION)
  .auth.user()
  .onCreate(async (user) => {
    const economy = await loadEconomy();
    const key = ledgerKey(user.uid, "welcome");
    await getFirestore().runTransaction(async (tx) => {
      const existing = await readEntry(tx, key);
      applyEntry(tx, existing, {
        key,
        uid: user.uid,
        amount: economy.welcomeBonus,
        type: "welcome",
        refId: user.uid,
      });
    });
  });
