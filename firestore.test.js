const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");
const pastTimestamp = () => new Date(Date.now() - 1000);

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const OWNER_UID = "owner_789";
const ADMIN_UID = "admin_001";
const ADMIN_EMAIL = "mustaphasaboogie@gmail.com";

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
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: cannot read or write profiles, config, turfs, or bookings", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("app_config").doc("owner_membership").get());
  await assertFails(unauthDb.collection("turfs").where("verificationStatus", "==", "verified").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("bookings").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("challenges").get());
});

test("Role escalation blocked: player cannot self-assign owner/admin or read admin config", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("app_config").doc("owner_membership").set({
      code: "TURF-GM-2026",
      active: true,
      updatedAt: pastTimestamp(),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const aliceRef = aliceDb.collection("users").doc(ALICE_UID);

  // Player cannot read admin-only membership code
  await assertFails(aliceDb.collection("app_config").doc("owner_membership").get());
  await assertFails(aliceDb.collection("app_config").get());

  // Player cannot create account directly as owner or admin
  await assertFails(aliceRef.set({
    userId: ALICE_UID,
    role: "owner",
    displayName: "Drako",
    handle: "@drako427",
    city: "Banjul",
    matchesPlayed: 24,
    wins: 16,
    trophies: 4,
    createdAt: pastTimestamp(),
  }));

  await assertFails(aliceRef.set({
    userId: ALICE_UID,
    role: "admin",
    displayName: "Drako",
    handle: "@drako427",
    city: "Banjul",
    matchesPlayed: 24,
    wins: 16,
    trophies: 4,
    createdAt: pastTimestamp(),
  }));

  // Player cannot claim owner_pending with a wrong membership code
  await assertFails(aliceRef.set({
    userId: ALICE_UID,
    role: "owner_pending",
    membershipCode: "WRONG-CODE",
    displayName: "Drako",
    handle: "@drako427",
    city: "Banjul",
    matchesPlayed: 24,
    wins: 16,
    trophies: 4,
    createdAt: pastTimestamp(),
  }));

  // Normal player profile creation succeeds
  await assertSucceeds(aliceRef.set({
    userId: ALICE_UID,
    role: "player",
    displayName: "Drako",
    handle: "@drako427",
    city: "Banjul",
    position: "Forward",
    favoriteTeamId: "brikama_warriors",
    onboardingCompleted: true,
    matchesPlayed: 24,
    wins: 16,
    trophies: 4,
    createdAt: pastTimestamp(),
    updatedAt: pastTimestamp(),
  }));

  // Player cannot escalate their existing role on update
  await assertFails(aliceRef.update({
    role: "owner",
    updatedAt: pastTimestamp(),
  }));
  await assertFails(aliceRef.update({
    role: "admin",
    updatedAt: pastTimestamp(),
  }));
  await assertFails(aliceRef.update({
    role: "owner_pending",
    updatedAt: pastTimestamp(),
  }));

  // Regular profile updates remain allowed
  await assertSucceeds(aliceRef.update({
    city: "Serrekunda",
    position: "Midfielder",
    updatedAt: pastTimestamp(),
  }));
});

test("Owner onboarding & turf verification: valid code creates pending owner+turf, admin verifies, players see only verified", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("app_config").doc("owner_membership").set({
      code: "GAMBIA-TURF-99",
      active: true,
      updatedAt: pastTimestamp(),
    });
  });

  const ownerDb = testEnv.authenticatedContext(OWNER_UID, { email: "owner@turf.gm", email_verified: true }).firestore();
  const playerDb = testEnv.authenticatedContext(ALICE_UID, { email: "player@gm.com", email_verified: true }).firestore();
  const spoofedAdminDb = testEnv.authenticatedContext("spoof", { email: ADMIN_EMAIL, email_verified: false }).firestore();
  const realAdminDb = testEnv.authenticatedContext(ADMIN_UID, { email: ADMIN_EMAIL, email_verified: true }).firestore();

  // Spoofed unverified admin email cannot read config
  await assertFails(spoofedAdminDb.collection("app_config").doc("owner_membership").get());
  // Real admin can read config
  await assertSucceeds(realAdminDb.collection("app_config").doc("owner_membership").get());

  // Atomic batch: owner creates owner_pending user doc + pending turf
  const batch = ownerDb.batch();
  batch.set(ownerDb.collection("users").doc(OWNER_UID), {
    userId: OWNER_UID,
    role: "owner_pending",
    membershipCode: "GAMBIA-TURF-99",
    primaryTurfId: "turf_kotu_1",
    contactPhone: "+220 777 1234",
    displayName: "Lamin",
    handle: "@lamin_turf",
    city: "Kotu",
    onboardingCompleted: true,
    matchesPlayed: 0,
    wins: 0,
    trophies: 0,
    createdAt: pastTimestamp(),
    updatedAt: pastTimestamp(),
  });
  batch.set(ownerDb.collection("turfs").doc("turf_kotu_1"), {
    ownerId: OWNER_UID,
    name: "Kotu Night Arena",
    area: "Kotu",
    city: "Kotu, The Gambia",
    latitude: 13.455,
    longitude: -16.705,
    pricePerHour: 450,
    primaryFormat: "7-a-side",
    amenities: ["7-a-side", "Floodlights"],
    timeSlots: ["17:00-18:00", "19:00-20:00"],
    description: "New 7-a-side pitch",
    contactPhone: "+220 777 1234",
    verificationStatus: "pending",
    isOpen: true,
    createdAt: pastTimestamp(),
    updatedAt: pastTimestamp(),
  });
  await assertSucceeds(batch.commit());

  // Player CANNOT create a turf
  await assertFails(playerDb.collection("turfs").doc("turf_fake").set({
    ownerId: ALICE_UID,
    name: "Fake Turf",
    area: "Banjul",
    city: "Banjul, The Gambia",
    latitude: 13.45,
    longitude: -16.57,
    pricePerHour: 300,
    primaryFormat: "5-a-side",
    amenities: ["5-a-side"],
    timeSlots: ["10:00-11:00"],
    verificationStatus: "pending",
    isOpen: true,
    createdAt: pastTimestamp(),
  }));

  // Player CANNOT read the pending turf directly or via unfiltered list
  await assertFails(playerDb.collection("turfs").doc("turf_kotu_1").get());
  await assertFails(playerDb.collection("turfs").get());

  // Player verified query returns empty while turf is pending
  const verifiedBefore = await assertSucceeds(
    playerDb.collection("turfs").where("verificationStatus", "==", "verified").get()
  );
  if (verifiedBefore.size !== 0) {
    throw new Error(`Expected 0 verified turfs before approval, got ${verifiedBefore.size}`);
  }

  // Owner CAN view own pending turf, CANNOT self-verify it
  await assertSucceeds(ownerDb.collection("turfs").doc("turf_kotu_1").get());
  await assertFails(ownerDb.collection("turfs").doc("turf_kotu_1").update({
    verificationStatus: "verified",
    updatedAt: pastTimestamp(),
  }));

  // Admin verifies turf and promotes owner_pending -> owner
  await assertSucceeds(realAdminDb.collection("turfs").doc("turf_kotu_1").update({
    verificationStatus: "verified",
    updatedAt: pastTimestamp(),
  }));
  await assertSucceeds(realAdminDb.collection("users").doc(OWNER_UID).update({
    role: "owner",
    updatedAt: pastTimestamp(),
  }));

  // Now players CAN see the verified turf
  const verifiedAfter = await assertSucceeds(
    playerDb.collection("turfs").where("verificationStatus", "==", "verified").get()
  );
  if (verifiedAfter.size !== 1) {
    throw new Error(`Expected 1 verified turf after approval, got ${verifiedAfter.size}`);
  }
});

test("Authenticated user: cannot read or write another user's profile or bookings", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(bobDb.collection("users").doc(BOB_UID).collection("bookings").doc("b1").set({
    userId: BOB_UID,
    turfId: "paradise_turf",
    turfName: "Paradise Turf",
    turfLocation: "Serrekunda, The Gambia",
    pricePerHour: 400,
    dateLabel: "Sat Nov 22",
    timeSlot: "11:00-12:00",
    pitchFormat: "7-a-side",
    status: "confirmed",
    createdAt: pastTimestamp(),
    updatedAt: pastTimestamp(),
  }));

  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("bookings").get());
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("bookings").doc("b1").get());
});

test("Shadow update & terminal state lock on bookings", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bookingRef = aliceDb.collection("users").doc(ALICE_UID).collection("bookings").doc("b1");

  await assertSucceeds(bookingRef.set({
    userId: ALICE_UID,
    turfId: "paradise_turf",
    turfName: "Paradise Turf",
    turfLocation: "Serrekunda, The Gambia",
    pricePerHour: 400,
    dateLabel: "Sat Nov 22",
    timeSlot: "11:00-12:00",
    pitchFormat: "7-a-side",
    status: "confirmed",
    createdAt: pastTimestamp(),
    updatedAt: pastTimestamp(),
  }));

  await assertFails(bookingRef.update({
    status: "cancelled",
    isAdmin: true,
    updatedAt: pastTimestamp(),
  }));

  await assertSucceeds(bookingRef.update({
    status: "cancelled",
    updatedAt: pastTimestamp(),
  }));

  await assertFails(bookingRef.update({
    status: "confirmed",
    updatedAt: pastTimestamp(),
  }));
});

test("Membership code check doc: succeeds only on active exact code, never readable or updatable", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("app_config").doc("owner_membership").set({
      code: "TURF-GM-2026",
      active: true,
      updatedAt: pastTimestamp(),
    });
  });

  const unauthDb = testEnv.unauthenticatedContext().firestore();

  // Wrong code rejected
  await assertFails(unauthDb.collection("code_checks").doc("chk_wrong").set({
    code: "WRONG-CODE",
    createdAt: pastTimestamp(),
  }));

  // Extra ghost field rejected
  await assertFails(unauthDb.collection("code_checks").doc("chk_ghost").set({
    code: "TURF-GM-2026",
    extra: true,
    createdAt: pastTimestamp(),
  }));

  // Matching active code succeeds
  await assertSucceeds(unauthDb.collection("code_checks").doc("chk_valid").set({
    code: "TURF-GM-2026",
    createdAt: pastTimestamp(),
  }));

  // Read/list/update/delete strictly forbidden
  await assertFails(unauthDb.collection("code_checks").doc("chk_valid").get());
  await assertFails(unauthDb.collection("code_checks").get());
  await assertFails(unauthDb.collection("code_checks").doc("chk_valid").update({ code: "TURF-GM-2026" }));
  await assertFails(unauthDb.collection("code_checks").doc("chk_valid").delete());

  // Inactive code rejected
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("app_config").doc("owner_membership").set({
      code: "TURF-GM-2026",
      active: false,
      updatedAt: pastTimestamp(),
    });
  });

  await assertFails(unauthDb.collection("code_checks").doc("chk_inactive").set({
    code: "TURF-GM-2026",
    createdAt: pastTimestamp(),
  }));
});

test("Turf slots & walk-in bookings: verified owner transitions work, players read-only, invalid jumps fail", async () => {
  const TURF_ID = "turf_verified_1";
  const PENDING_TURF_ID = "turf_pending_2";
  const OTHER_OWNER = "other_owner_999";
  const SLOT_ID = "2026-11-15_16:00";

  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("users").doc(OWNER_UID).set({
      userId: OWNER_UID,
      role: "owner",
      primaryTurfId: TURF_ID,
      displayName: "Lamin",
      handle: "@lamin_turf",
      city: "Kotu",
      matchesPlayed: 0,
      wins: 0,
      trophies: 0,
      createdAt: pastTimestamp(),
    });
    await db.collection("users").doc(OTHER_OWNER).set({
      userId: OTHER_OWNER,
      role: "owner_pending",
      primaryTurfId: PENDING_TURF_ID,
      displayName: "Other",
      handle: "@other_turf",
      city: "Bakau",
      matchesPlayed: 0,
      wins: 0,
      trophies: 0,
      createdAt: pastTimestamp(),
    });
    await db.collection("turfs").doc(TURF_ID).set({
      ownerId: OWNER_UID,
      name: "Kotu Night Arena",
      area: "Kotu",
      city: "Kotu, The Gambia",
      latitude: 13.455,
      longitude: -16.705,
      pricePerHour: 500,
      primaryFormat: "7-a-side",
      amenities: ["7-a-side", "Floodlights"],
      timeSlots: ["16:00-17:00"],
      openTime: "08:00",
      closeTime: "23:00",
      slotMinutes: 60,
      verificationStatus: "verified",
      isOpen: true,
      createdAt: pastTimestamp(),
    });
    await db.collection("turfs").doc(PENDING_TURF_ID).set({
      ownerId: OTHER_OWNER,
      name: "Bakau Pending Arena",
      area: "Bakau",
      city: "Bakau, The Gambia",
      latitude: 13.47,
      longitude: -16.68,
      pricePerHour: 400,
      primaryFormat: "5-a-side",
      amenities: ["5-a-side"],
      timeSlots: ["16:00-17:00"],
      openTime: "08:00",
      closeTime: "23:00",
      slotMinutes: 60,
      verificationStatus: "pending",
      isOpen: true,
      createdAt: pastTimestamp(),
    });
  });

  const ownerDb = testEnv.authenticatedContext(OWNER_UID, { email: "owner@turf.gm", email_verified: true }).firestore();
  const otherOwnerDb = testEnv.authenticatedContext(OTHER_OWNER, { email: "other@turf.gm", email_verified: true }).firestore();
  const playerDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@gm.com", email_verified: true }).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, { email: ADMIN_EMAIL, email_verified: true }).firestore();

  const slotRef = ownerDb.collection("turfs").doc(TURF_ID).collection("slots").doc(SLOT_ID);
  const bookingRef = ownerDb.collection("turfs").doc(TURF_ID).collection("bookings").doc("walkin_1");

  // Pending owner / pending turf CANNOT create slots
  await assertFails(otherOwnerDb.collection("turfs").doc(PENDING_TURF_ID).collection("slots").doc(SLOT_ID).set({
    date: "2026-11-15",
    startTime: "16:00",
    endTime: "17:00",
    status: "available",
    bookingId: null,
    startedAt: null,
    endsAt: null,
    createdAt: pastTimestamp(),
    updatedAt: pastTimestamp(),
  }));

  // Non-matching slot ID rejected
  await assertFails(ownerDb.collection("turfs").doc(TURF_ID).collection("slots").doc("wrong_id").set({
    date: "2026-11-15",
    startTime: "16:00",
    endTime: "17:00",
    status: "available",
    createdAt: pastTimestamp(),
    updatedAt: pastTimestamp(),
  }));

  // Verified owner creates slot batch
  await assertSucceeds(slotRef.set({
    date: "2026-11-15",
    startTime: "16:00",
    endTime: "17:00",
    status: "available",
    bookingId: null,
    startedAt: null,
    endsAt: null,
    createdAt: pastTimestamp(),
    updatedAt: pastTimestamp(),
  }));

  // Player CAN read verified turf slots, CANNOT write slots or read venue bookings
  await assertSucceeds(playerDb.collection("turfs").doc(TURF_ID).collection("slots").where("date", "==", "2026-11-15").get());
  await assertFails(playerDb.collection("turfs").doc(TURF_ID).collection("slots").doc(SLOT_ID).update({
    status: "blocked",
    updatedAt: pastTimestamp(),
  }));
  await assertFails(playerDb.collection("turfs").doc(TURF_ID).collection("bookings").get());

  // Player CANNOT read pending turf slots
  await assertFails(playerDb.collection("turfs").doc(PENDING_TURF_ID).collection("slots").get());

  // Invalid transition: available -> completed directly is REJECTED
  await assertFails(slotRef.update({
    status: "completed",
    updatedAt: pastTimestamp(),
  }));

  // Slot documents NEVER allow customerName or phone fields
  await assertFails(slotRef.update({
    status: "booked",
    customerName: "Young Stars FC",
    updatedAt: pastTimestamp(),
  }));

  // Valid transition: available -> blocked -> available
  await assertSucceeds(slotRef.update({ status: "blocked", updatedAt: pastTimestamp() }));
  await assertSucceeds(slotRef.update({ status: "available", updatedAt: pastTimestamp() }));

  // Valid multi-slot booking + Start Timer directly from available -> in_progress
  const SLOT_2_ID = "2026-11-15_17:00";
  const slot2Ref = ownerDb.collection("turfs").doc(TURF_ID).collection("slots").doc(SLOT_2_ID);
  await assertSucceeds(slot2Ref.set({
    date: "2026-11-15",
    startTime: "17:00",
    endTime: "18:00",
    status: "available",
    bookingId: null,
    startedAt: null,
    endsAt: null,
    createdAt: pastTimestamp(),
    updatedAt: pastTimestamp(),
  }));

  const futureEnd = new Date(Date.now() + 7200 * 1000);
  const startTimerBatch = ownerDb.batch();
  startTimerBatch.set(bookingRef, {
    source: "walk_in",
    customerName: "Young Stars FC",
    phone: "+220 7012345",
    date: "2026-11-15",
    startTime: "16:00",
    endTime: "18:00",
    hours: 2,
    totalPrice: 900,
    paymentMethod: "cash",
    paid: true,
    slotId: SLOT_ID,
    slotIds: [SLOT_ID, SLOT_2_ID],
    turfId: TURF_ID,
    ownerId: OWNER_UID,
    status: "in_progress",
    createdAt: pastTimestamp(),
    updatedAt: pastTimestamp(),
  });
  startTimerBatch.update(slotRef, {
    status: "in_progress",
    bookingId: "walkin_1",
    startedAt: pastTimestamp(),
    endsAt: futureEnd,
    updatedAt: pastTimestamp(),
  });
  startTimerBatch.update(slot2Ref, {
    status: "in_progress",
    bookingId: "walkin_1",
    startedAt: pastTimestamp(),
    endsAt: futureEnd,
    updatedAt: pastTimestamp(),
  });
  await assertSucceeds(startTimerBatch.commit());

  // End session: mark booking completed and free both slots back to available
  const endBatch = ownerDb.batch();
  endBatch.update(bookingRef, {
    status: "completed",
    updatedAt: pastTimestamp(),
  });
  endBatch.update(slotRef, {
    status: "available",
    bookingId: null,
    startedAt: null,
    endsAt: null,
    updatedAt: pastTimestamp(),
  });
  endBatch.update(slot2Ref, {
    status: "available",
    bookingId: null,
    startedAt: null,
    endsAt: null,
    updatedAt: pastTimestamp(),
  });
  await assertSucceeds(endBatch.commit());

  // Owner can save up to 6 photos and 24:00 closing time while verificationStatus stays unchanged
  await assertSucceeds(ownerDb.collection("turfs").doc(TURF_ID).update({
    closeTime: "24:00",
    photos: ["data:image/jpeg;base64,abc123"],
    updatedAt: pastTimestamp(),
  }));

  // Admin can update anything on slots
  await assertSucceeds(adminDb.collection("turfs").doc(TURF_ID).collection("slots").doc(SLOT_ID).update({
    status: "available",
    bookingId: null,
    startedAt: null,
    endsAt: null,
    updatedAt: pastTimestamp(),
  }));
});


