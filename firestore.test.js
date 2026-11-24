const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validProjectPayload(uid, overrides = {}) {
  const now = new Date(Date.now() - 2000);
  return {
    userId: uid,
    name: "Commerce Pro",
    packageName: "com.forge.commercepro",
    tagline: "Full-stack multi-vendor storefront",
    themePreset: "OBSIDIAN_CYAN",
    architecture: "CLEAN_MVVM",
    cloudRegion: "asia-southeast1",
    scalingTier: "SERVERLESS_AUTO",
    deploymentStatus: "DRAFT",
    activeVersion: "v1.0.0",
    screens: [
      {
        screenId: "home",
        title: "Home",
        route: "home",
        iconName: "Home",
        components: []
      }
    ],
    files: [
      {
        fileId: "main_activity",
        path: "app/MainActivity.kt",
        language: "kotlin",
        layer: "UI",
        content: "class MainActivity"
      }
    ],
    backend: {
      collections: [],
      endpoints: [],
      authGoogle: true,
      authEmail: false,
      appCheckEnabled: true,
      firestoreRules: "rules_version = '2';",
      envVars: []
    },
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

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
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read or write projects or deployments", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("projects").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("projects").doc("proj_1").set(validProjectPayload(ALICE_UID))
  );
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("deployments").get());
});

test("Authenticated owner can create, read, list, and partially update own project", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const projectRef = aliceDb.collection("users").doc(ALICE_UID).collection("projects").doc("proj_1");

  await assertSucceeds(projectRef.set(validProjectPayload(ALICE_UID)));
  await assertSucceeds(projectRef.get());
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("projects").get());

  // Action 1: Design/Code update
  await assertSucceeds(
    projectRef.update({
      name: "Commerce Pro v2",
      themePreset: "EMERALD_CLOUD",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Action 2: Cloud deployment update
  await assertSucceeds(
    projectRef.update({
      deploymentStatus: "DEPLOYED",
      activeVersion: "v1.1.0",
      cloudRegion: "us-central1",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Rejects shadow fields, @DocumentId id key, future timestamps, and immutable createdAt mutations", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  // Rejects 'id' field in payload
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("projects").doc("proj_bad_id")
      .set(validProjectPayload(ALICE_UID, { id: "proj_bad_id" }))
  );

  // Rejects future createdAt
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("projects").doc("proj_future")
      .set(validProjectPayload(ALICE_UID, { createdAt: new Date(Date.now() + 60000) }))
  );

  // Rejects ghost field
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("projects").doc("proj_ghost")
      .set(validProjectPayload(ALICE_UID, { isAdmin: true }))
  );

  // Create valid project then reject createdAt mutation
  const projRef = aliceDb.collection("users").doc(ALICE_UID).collection("projects").doc("proj_immut");
  await assertSucceeds(projRef.set(validProjectPayload(ALICE_UID)));
  await assertFails(
    projRef.update({
      createdAt: new Date(Date.now() - 100),
      updatedAt: new Date(Date.now() - 100),
    })
  );
});

test("Cross-user access is strictly denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("projects").doc("proj_alice").set(validProjectPayload(ALICE_UID))
  );

  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("projects").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("projects").doc("proj_alice").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("projects").doc("proj_alice").update({
      name: "Hijacked",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Deployments enforce parent project existence and terminal ROLLED_BACK lock", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("projects").doc("proj_1").set(validProjectPayload(ALICE_UID))
  );

  const depRef = aliceDb.collection("users").doc(ALICE_UID).collection("deployments").doc("dep_1");

  // Fails if projectId does not exist
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("deployments").doc("dep_orphan").set({
      userId: ALICE_UID,
      projectId: "non_existent",
      projectName: "Ghost",
      versionTag: "v1.0.0",
      environment: "PRODUCTION",
      cloudRegion: "asia-southeast1",
      status: "SUCCEEDED",
      endpointUrl: "https://api.forgestudio.app/ghost",
      summary: "Deployed",
      createdAt: new Date(Date.now() - 1000),
    })
  );

  // Succeeds when parent project exists
  await assertSucceeds(
    depRef.set({
      userId: ALICE_UID,
      projectId: "proj_1",
      projectName: "Commerce Pro",
      versionTag: "v1.0.0",
      environment: "PRODUCTION",
      cloudRegion: "asia-southeast1",
      status: "SUCCEEDED",
      endpointUrl: "https://commerce-pro.asia-southeast1.run.app",
      summary: "Initial production release",
      createdAt: new Date(Date.now() - 1000),
    })
  );

  // Rollback succeeds once
  await assertSucceeds(
    depRef.update({
      status: "ROLLED_BACK",
      summary: "Rolled back by owner",
    })
  );

  // Subsequent update blocked by terminal lock
  await assertFails(
    depRef.update({
      status: "ROLLED_BACK",
      summary: "Mutate after rollback",
    })
  );
});
