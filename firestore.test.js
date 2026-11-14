const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ADMIN_UID = "admin_uid_1";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) await testEnv.cleanup();
});

beforeEach(async () => {
  if (testEnv) await testEnv.clearFirestore();
});

function validTweakPayload(uid) {
  return {
    name: "Synax TikTok Zero",
    carrierTag: "MTN / CellC",
    notes: "TLS 443 SNI",
    protocol: "TCP",
    serverId: "za1",
    customHost: "1.1.1.1",
    customPort: 443,
    useSniHost: true,
    sniHost: "m.tiktok.com",
    usePayload: false,
    payload: "",
    usePayloadAfterTls: false,
    payloadAfterTls: "",
    useProxy: false,
    proxyHost: "",
    proxyPort: 8080,
    obfuscatedTlsCh: true,
    tcpFastOpen: true,
    preferIpv6: false,
    createdBy: uid,
    isActive: true,
    createdAt: new Date(),
    updatedAt: new Date(),
  };
}

function validCloudConfigPayload(uid, shareCode, isPublic = true) {
  return {
    shareCode,
    ownerId: uid,
    name: "Synax Fast Config",
    author: "Alice",
    notes: "Cloud export",
    protocol: "TCP",
    serverId: "sg1",
    customHost: "1.1.1.1",
    customPort: 443,
    useSniHost: true,
    sniHost: "speed.cloudflare.com",
    usePayload: false,
    payload: "",
    usePayloadAfterTls: false,
    payloadAfterTls: "",
    useProxy: false,
    proxyHost: "",
    proxyPort: 8080,
    obfuscatedTlsCh: false,
    tcpFastOpen: false,
    preferIpv6: false,
    isPublic,
    createdAt: new Date(),
    updatedAt: new Date(),
  };
}

test("Unauthenticated caller cannot read tweaks, notifications, or cloud configs", async () => {
  const db = testEnv.unauthenticatedContext().firestore();
  await assertFails(db.collection("cloud_tweaks").where("isActive", "==", true).get());
  await assertFails(db.collection("admin_notifications").where("isActive", "==", true).get());
  await assertFails(db.collection("cloud_configs").doc("SYNAX-1234").get());
});

test("Email spoofing with email_verified=false cannot write admin tweaks", async () => {
  const spoofedAdminDb = testEnv.authenticatedContext("spoof_uid", {
    email: "zululand118@gmail.com",
    email_verified: false,
  }).firestore();
  await assertFails(
    spoofedAdminDb.collection("cloud_tweaks").doc("tweak_1").set(validTweakPayload("spoof_uid"))
  );
});

test("Regular user cannot create admin tweaks or notifications", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@example.com",
    email_verified: true,
  }).firestore();
  await assertFails(
    aliceDb.collection("cloud_tweaks").doc("tweak_1").set(validTweakPayload(ALICE_UID))
  );
  await assertFails(
    aliceDb.collection("admin_notifications").doc("notif_1").set({
      title: "Fake",
      message: "Test",
      createdBy: ALICE_UID,
      isActive: true,
      createdAt: new Date(),
    })
  );
});

test("Verified admin can create tweaks & notifications and signed-in users can read active ones", async () => {
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: "zululand118@gmail.com",
    email_verified: true,
  }).firestore();

  await assertSucceeds(
    adminDb.collection("cloud_tweaks").doc("tweak_1").set(validTweakPayload(ADMIN_UID))
  );
  await assertSucceeds(
    adminDb.collection("admin_notifications").doc("notif_1").set({
      title: "New SG1 Node",
      message: "Use TikTok SNI tweak v2",
      actionUrl: "https://t.me/synaxtunnel",
      createdBy: ADMIN_UID,
      isActive: true,
      createdAt: new Date(),
    })
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(bobDb.collection("cloud_tweaks").where("isActive", "==", true).get());
  await assertSucceeds(bobDb.collection("admin_notifications").where("isActive", "==", true).get());
});

test("Shadow update with ghost field is rejected on cloud_configs", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const payload = validCloudConfigPayload(ALICE_UID, "SYNAX-9001", true);
  payload.isVerifiedAdmin = true;
  await assertFails(aliceDb.collection("cloud_configs").doc("SYNAX-9001").set(payload));
});

test("Cloud configs enforce private isolation and allow share-code public import", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("cloud_configs").doc("SYNAX-PRIV").set(validCloudConfigPayload(ALICE_UID, "SYNAX-PRIV", false))
  );
  await assertSucceeds(
    aliceDb.collection("cloud_configs").doc("SYNAX-PUB1").set(validCloudConfigPayload(ALICE_UID, "SYNAX-PUB1", true))
  );

  // Bob cannot read Alice's private config, CAN read public config by shareCode
  await assertFails(bobDb.collection("cloud_configs").doc("SYNAX-PRIV").get());
  await assertSucceeds(bobDb.collection("cloud_configs").doc("SYNAX-PUB1").get());
  // Bob cannot delete Alice's public config
  await assertFails(bobDb.collection("cloud_configs").doc("SYNAX-PUB1").delete());
});
