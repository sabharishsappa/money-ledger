const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_ledger_123";
const BOB_UID = "bob_ledger_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
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

function validTimestamp() {
  return new Date(Date.now() - 2000);
}

test("Unauthenticated users cannot read or write any ledger collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const ts = validTimestamp();

  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("transactions").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("accounts").doc("acc_1").set({
      userId: ALICE_UID,
      groupId: "grp_cash",
      name: "Cash",
      initialBalance: 0,
      accountType: "ASSET",
      sortOrder: 0,
      archived: false,
      createdAt: ts,
      updatedAt: ts,
    })
  );
});

test("UserSettings: valid Kotlin create & partial update succeed; ghost fields and cross-user access fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = validTimestamp();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      mainCurrencyCode: "INR",
      mainCurrencySymbol: "₹",
      subcategoryEnabled: true,
      startScreen: "DAILY",
      monthlyStartDate: 1,
      weeklyStartDay: "MONDAY",
      seeded: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Partial update matching LedgerRepository.updateSettings
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      mainCurrencyCode: "USD",
      mainCurrencySymbol: "$",
      subcategoryEnabled: false,
      startScreen: "CALENDAR",
      monthlyStartDate: 5,
      weeklyStartDay: "SUNDAY",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Cross-user read & write blocked
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      mainCurrencyCode: "EUR",
      updatedAt: validTimestamp(),
    })
  );

  // Shadow update with ghost field blocked
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: validTimestamp(),
    })
  );

  // Tampered future timestamp blocked
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      startScreen: "DAILY",
      updatedAt: new Date(Date.now() + 60000),
    })
  );
});

test("AccountGroups & Accounts: Kotlin create, partial update, archive, list, and cross-user isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = validTimestamp();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("accountGroups").doc("grp_bank").set({
      userId: ALICE_UID,
      name: "Bank Accounts",
      groupType: "ASSET",
      sortOrder: 1,
      archived: false,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("accounts").doc("acc_hdfc").set({
      userId: ALICE_UID,
      groupId: "grp_bank",
      name: "HDFC | Home",
      initialBalance: 150000.5,
      accountType: "ASSET",
      sortOrder: 0,
      archived: false,
      includeInNetWorth: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Mirror Kotlin partial update with null note and toggled includeInNetWorth permitted
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("accounts").doc("acc_hdfc").update({
      groupId: "grp_bank",
      name: "HDFC | Primary",
      initialBalance: 200000.0,
      accountType: "ASSET",
      note: null,
      sortOrder: 0,
      includeInNetWorth: false,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject non-boolean includeInNetWorth
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("accounts").doc("acc_hdfc").update({
      includeInNetWorth: "yes",
      updatedAt: new Date(Date.now() - 400),
    })
  );

  // Reject including @DocumentId 'id' inside write payload
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("accounts").doc("acc_bad").set({
      id: "acc_bad",
      userId: ALICE_UID,
      groupId: "grp_bank",
      name: "Bad Account",
      initialBalance: 0,
      accountType: "ASSET",
      sortOrder: 1,
      archived: false,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // List queries match Kotlin repository
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("accountGroups").get()
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("accounts").get()
  );

  // Cross-user list denied
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("accounts").get()
  );
});

test("Categories, Transactions, Budgets & FinanceNotes: full Kotlin payload parity and security boundaries", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = validTimestamp();

  // Category create + update
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("categories").doc("cat_food").set({
      userId: ALICE_UID,
      name: "Food",
      type: "EXPENSE",
      colorHex: "#F06458",
      subcategories: ["Groceries", "Dining Out"],
      sortOrder: 0,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Invalid colorHex rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("categories").doc("cat_bad").set({
      userId: ALICE_UID,
      name: "Bad",
      type: "EXPENSE",
      colorHex: "red",
      subcategories: [],
      sortOrder: 1,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Transaction create (Expense, null optionals filtered out just like Kotlin toCreateMap)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_1").set({
      userId: ALICE_UID,
      type: "EXPENSE",
      amount: 250.75,
      feeAmount: 0.0,
      epochDay: 20769,
      timestampMillis: Date.now() - 5000,
      accountId: "acc_hdfc",
      categoryId: "cat_food",
      subcategory: "Dining Out",
      note: "Lunch",
      tagging: "Prabhakar",
      tags: ["work"],
      favorite: false,
      repeatFrequency: "NONE",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Transaction update (Transfer with null categoryId/subcategory and updated tagging)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_1").update({
      type: "TRANSFER",
      amount: 1000.0,
      feeAmount: 5.0,
      epochDay: 20769,
      timestampMillis: Date.now() - 4000,
      accountId: "acc_hdfc",
      toAccountId: "acc_cash",
      categoryId: null,
      subcategory: null,
      note: "ATM Withdrawal",
      tagging: "Home",
      description: "Cash top-up",
      tags: [],
      favorite: true,
      repeatFrequency: "MONTHLY",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Oversized tagging (>60 chars) rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_1").update({
      tagging: "x".repeat(65),
      updatedAt: new Date(Date.now() - 200),
    })
  );

  // Negative amount rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_neg").set({
      userId: ALICE_UID,
      type: "EXPENSE",
      amount: -50,
      feeAmount: 0,
      epochDay: 20769,
      timestampMillis: Date.now() - 5000,
      accountId: "acc_hdfc",
      note: "Invalid",
      tags: [],
      favorite: false,
      repeatFrequency: "NONE",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Immutable createdAt mutation rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_1").update({
      createdAt: new Date(Date.now() - 100),
      updatedAt: new Date(Date.now() - 100),
    })
  );

  // Budget & Note CRUD
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("budgets").doc("budget_cat_food").set({
      userId: ALICE_UID,
      categoryId: "cat_food",
      monthlyLimit: 15000,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("notes").doc("note_1").set({
      userId: ALICE_UID,
      title: "Insurance Renewal",
      content: "Check policy due on 18th",
      pinned: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Cross-user delete blocked
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_1").delete()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("notes").doc("note_1").get()
  );
});
