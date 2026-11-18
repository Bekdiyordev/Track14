import { strict as assert } from "node:assert";
import { describe, it } from "node:test";
import { accountIdOf, checkPurchase, COIN_PACKS, tokenId, voidedTokenOf } from "../purchases";

describe("purchase checks", () => {
  const owner = { obfuscatedExternalAccountId: accountIdOf("u1"), productId: "coins_1000" };
  it("accepts a purchase bound to the caller", () => {
    assert.equal(checkPurchase({ ...owner, purchaseState: 0 }, "u1", "coins_1000"), "ok");
  });
  it("rejects other accounts, cancelled, pending and swapped products", () => {
    assert.equal(checkPurchase({ ...owner, purchaseState: 0 }, "u2", "coins_1000"), "wrong_account");
    assert.equal(checkPurchase({ purchaseState: 0, productId: "coins_1000" }, "u1", "coins_1000"), "wrong_account");
    assert.equal(checkPurchase({ ...owner, purchaseState: 1 }, "u1", "coins_1000"), "not_purchased");
    assert.equal(checkPurchase({ ...owner, purchaseState: 2 }, "u1", "coins_1000"), "pending");
    assert.equal(checkPurchase({ ...owner, purchaseState: 0 }, "u1", "coins_10000"), "wrong_product");
  });
  it("account id matches the Android side (sha256 hex of uid)", () => {
    assert.equal(accountIdOf("abc"), "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
  });
  it("token ids are stable and Firestore safe", () => {
    assert.equal(tokenId("t"), tokenId("t"));
    assert.match(tokenId("x".repeat(500)), /^[0-9a-f]{40}$/);
  });
  it("packs", () => assert.equal(COIN_PACKS.coins_2000, 2000));
});

describe("RTDN parsing", () => {
  it("picks voided one-time purchases of this app only", () => {
    const m = { packageName: "uz.beko404.track14", voidedPurchaseNotification: { purchaseToken: "tok", productType: 2 } };
    assert.equal(voidedTokenOf(m), "tok");
    assert.equal(voidedTokenOf({ ...m, packageName: "other.app" }), null);
    assert.equal(voidedTokenOf({ ...m, voidedPurchaseNotification: { purchaseToken: "tok", productType: 1 } }), null);
    assert.equal(voidedTokenOf({ packageName: "uz.beko404.track14" }), null);
  });
});
