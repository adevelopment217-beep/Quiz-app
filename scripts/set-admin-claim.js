/**
 * Script to grant or revoke custom claim { admin: true } on a Firebase user account.
 *
 * Requirements:
 * 1. Download service account JSON from Firebase Console:
 *    Project Settings -> Service accounts -> Generate new private key
 * 2. Save as service-account.json in the project root or provide via GOOGLE_APPLICATION_CREDENTIALS
 * 3. Run: node scripts/set-admin-claim.js <email-or-uid> [--revoke]
 *
 * Example:
 * node scripts/set-admin-claim.js admin@medhaquiz.com
 */

const admin = require("firebase-admin");
const path = require("path");
const fs = require("fs");

const serviceAccountPath = process.env.GOOGLE_APPLICATION_CREDENTIALS ||
  path.join(__dirname, "..", "service-account.json");

if (!fs.existsSync(serviceAccountPath)) {
  console.error("❌ Service account JSON file not found at: " + serviceAccountPath);
  console.error("Please download your service account key from Firebase Console:");
  console.error("Firebase Console -> Project Settings -> Service accounts -> Generate new private key");
  console.error("Save it as service-account.json in the project root directory.");
  process.exit(1);
}

const serviceAccount = require(serviceAccountPath);

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

async function setAdminClaim() {
  const args = process.argv.slice(2);
  if (args.length === 0) {
    console.log("Usage: node scripts/set-admin-claim.js <user_email_or_uid> [--revoke]");
    console.log("Example: node scripts/set-admin-claim.js admin@medhaquiz.com");
    process.exit(1);
  }

  const identifier = args[0].trim();
  const revoke = args.includes("--revoke");
  const makeAdmin = !revoke;

  try {
    let userRecord;
    if (identifier.includes("@")) {
      userRecord = await admin.auth().getUserByEmail(identifier);
    } else {
      userRecord = await admin.auth().getUser(identifier);
    }

    console.log(`Found user: ${userRecord.email} (UID: ${userRecord.uid})`);

    // Set custom claim
    await admin.auth().setCustomUserClaims(userRecord.uid, {
      admin: makeAdmin
    });

    // Update Firestore profile
    await admin.firestore().collection("users").document(userRecord.uid).set({
      role: makeAdmin ? "admin" : "user",
      updatedAt: admin.firestore.FieldValue.serverTimestamp()
    }, { merge: true });

    console.log(`✅ Success! Custom claim { admin: ${makeAdmin} } applied to ${userRecord.email}.`);
    if (makeAdmin) {
      console.log(`User now has full Administrator access in the MedhaQuiz Admin Panel.`);
    } else {
      console.log(`User admin privileges successfully revoked.`);
    }
    process.exit(0);
  } catch (error) {
    console.error("❌ Error setting admin claim:", error.message);
    process.exit(1);
  }
}

setAdminClaim();
