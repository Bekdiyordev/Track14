import { initializeApp } from "firebase-admin/app";

initializeApp();

export { onUserCreated } from "./users";
export { deleteAccount } from "./account";
export { createCampaign, featureApp, onAppWritten, onCampaignWritten, ownerSetStatus } from "./campaigns";
export { leaveTest, startTest, verifyDay } from "./tests";
export { dailyRollover } from "./rollover";
export { onPlayRtdn, verifyPurchase } from "./purchases";
export { onFeedbackCreated } from "./feedback";
export { dailyReminder } from "./reminders";
export {
  adminAdjustCoins,
  adminBlockUser,
  adminCancelPush,
  adminResolveReport,
  adminSendPush,
  adminSetConfig,
  adminSetRole,
  adminSuspendApp,
  dispatchPushes,
} from "./admin";
