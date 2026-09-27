const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();

/**
 * Callable Cloud Function: setAdminRole
 * Grants or revokes custom claim { admin: true } on target user.
 * STRICT SECURITY: Only existing administrators (caller with context.auth.token.admin === true)
 * can invoke this function. Normal users and unauthenticated callers are rejected immediately.
 */
exports.setAdminRole = functions.https.onCall(async (data, context) => {
  // 1. Verify caller authentication
  if (!context.auth) {
    throw new functions.https.HttpsError(
      "unauthenticated",
      "Request must be authenticated."
    );
  }

  // 2. Verify caller has admin custom claim
  if (context.auth.token.admin !== true) {
    throw new functions.https.HttpsError(
      "permission-denied",
      "Only verified administrators can assign admin privileges."
    );
  }

  const { targetUid, targetEmail, makeAdmin } = data;
  let uid = targetUid;

  if (!uid && targetEmail) {
    try {
      const user = await admin.auth().getUserByEmail(targetEmail.trim());
      uid = user.uid;
    } catch (err) {
      throw new functions.https.HttpsError(
        "not-found",
        `User with email ${targetEmail} was not found.`
      );
    }
  }

  if (!uid) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      "Either targetUid or targetEmail must be provided."
    );
  }

  const shouldBeAdmin = makeAdmin === true;

  // 3. Set Firebase Custom User Claims
  await admin.auth().setCustomUserClaims(uid, {
    admin: shouldBeAdmin,
  });

  // 4. Update the Firestore user document to mirror the role
  await admin
    .firestore()
    .collection("users")
    .document(uid)
    .set(
      {
        role: shouldBeAdmin ? "admin" : "user",
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      },
      { merge: true }
    );

  // 5. Audit log this operation
  await admin.firestore().collection("auditLogs").add({
    adminUid: context.auth.uid,
    action: shouldBeAdmin ? "ADMIN_CLAIM_GRANTED" : "ADMIN_CLAIM_REVOKED",
    targetUid: uid,
    details: `Admin role ${shouldBeAdmin ? "granted to" : "revoked from"} UID ${uid}`,
    timestamp: Date.now(),
  });

  return {
    success: true,
    uid: uid,
    admin: shouldBeAdmin,
    message: `User ${uid} custom claim successfully updated to admin: ${shouldBeAdmin}`,
  };
});

/**
 * Trigger: onUserCreated
 * Automatically initializes users/{uid} document with server timestamps when a user registers.
 */
exports.onUserCreated = functions.auth.user().onCreate(async (user) => {
  const userRef = admin.firestore().collection("users").document(user.uid);
  const doc = await userRef.get();

  if (!doc.exists) {
    await userRef.set({
      uid: user.uid,
      name: user.displayName || (user.email ? user.email.split("@")[0] : "Student"),
      email: user.email || "",
      role: "user", // Default normal user role, NEVER admin
      photoUrl: user.photoURL || "",
      selectedClassId: "class_9",
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      lastLoginAt: admin.firestore.FieldValue.serverTimestamp(),
    });
  }
});
