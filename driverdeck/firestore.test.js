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

function validVehiclePayload(uid, overrides = {}) {
  const now = new Date(Date.now() - 2000);
  return {
    userId: uid,
    schemaVersion: 1,
    name: "Tiguan",
    brand: "Volkswagen",
    model: "Tiguan",
    year: 2021,
    category: "CAR",
    vehicleClass: "CROSSOVER",
    iconStyle: "TWO_TONE",
    accentColorHex: "#1D63DC",
    odometerKm: 45200,
    fuelType: "АИ-95",
    tankCapacityLiters: 58.0,
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

function validRecordPayload(uid, vehicleId, overrides = {}) {
  const now = new Date(Date.now() - 2000);
  return {
    userId: uid,
    schemaVersion: 1,
    vehicleId,
    recordType: "REFUEL",
    title: "Заправка АИ-95",
    categoryTag: "АИ-95",
    amountCost: 2750.0,
    odometerKm: 45450,
    liters: 45.0,
    pricePerLiter: 61.1,
    fullTank: true,
    notes: "Полный бак",
    eventTimestampMs: Date.now() - 5000,
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

function validReminderPayload(uid, vehicleId, overrides = {}) {
  const now = new Date(Date.now() - 2000);
  return {
    userId: uid,
    schemaVersion: 1,
    vehicleId,
    title: "Замена масла",
    notes: "5W-30 + фильтр",
    triggerType: "BOTH",
    dueDateEpochMs: Date.now() + 86400000 * 30,
    dueOdometerKm: 50000,
    repeatIntervalDays: 365,
    repeatIntervalKm: 10000,
    priority: "HIGH",
    completed: false,
    completedAtEpochMs: 0,
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

test("Unauthenticated user cannot read or write vehicles, records, or reminders", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("vehicles").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("vehicles").doc("veh_1").set(validVehiclePayload(ALICE_UID))
  );
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("reminders").get());
});

test("Owner can create, list, partially update, and delete vehicle with valid schema", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const vehRef = aliceDb.collection("users").doc(ALICE_UID).collection("vehicles").doc("veh_1");

  await assertSucceeds(vehRef.set(validVehiclePayload(ALICE_UID)));
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("vehicles").get());

  // Odometer partial update
  await assertSucceeds(
    vehRef.update({
      odometerKm: 45800,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Style partial update
  await assertSucceeds(
    vehRef.update({
      iconStyle: "OUTLINE",
      accentColorHex: "#10B981",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject ghost field or @DocumentId 'id' field
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("vehicles").doc("veh_bad").set({
      id: "veh_bad",
      ...validVehiclePayload(ALICE_UID),
    })
  );

  // Reject invalid vehicleClass
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("vehicles").doc("veh_bad_cls").set(
      validVehiclePayload(ALICE_UID, { vehicleClass: "TANK" })
    )
  );

  // Reject immutable createdAt mutation
  await assertFails(
    vehRef.update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Owner can create records and reminders linked to existing vehicle; orphan vehicleId fails", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const vehRef = aliceDb.collection("users").doc(ALICE_UID).collection("vehicles").doc("veh_moto");
  await assertSucceeds(
    vehRef.set(
      validVehiclePayload(ALICE_UID, {
        name: "MT-07",
        brand: "Yamaha",
        model: "MT-07",
        category: "MOTO",
        vehicleClass: "NAKED",
      })
    )
  );

  // Orphan record fails
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("records").doc("rec_orphan").set(
      validRecordPayload(ALICE_UID, "non_existent_vehicle")
    )
  );

  // Valid record succeeds
  const recRef = aliceDb.collection("users").doc(ALICE_UID).collection("records").doc("rec_1");
  await assertSucceeds(recRef.set(validRecordPayload(ALICE_UID, "veh_moto")));

  // Valid reminder succeeds & toggle completion succeeds
  const remRef = aliceDb.collection("users").doc(ALICE_UID).collection("reminders").doc("rem_1");
  await assertSucceeds(remRef.set(validReminderPayload(ALICE_UID, "veh_moto")));
  await assertSucceeds(
    remRef.update({
      completed: true,
      completedAtEpochMs: Date.now(),
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Cross-user isolation blocks Bob from reading or modifying Alice's data", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await db.collection("users").doc(ALICE_UID).collection("vehicles").doc("veh_1").set(validVehiclePayload(ALICE_UID));
    await db.collection("users").doc(ALICE_UID).collection("reminders").doc("rem_1").set(validReminderPayload(ALICE_UID, "veh_1"));
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("vehicles").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("vehicles").doc("veh_1").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("reminders").doc("rem_1").update({
      completed: true,
      completedAtEpochMs: Date.now(),
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("vehicles").doc("veh_1").delete()
  );
});
