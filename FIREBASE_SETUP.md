# MedhaQuiz Firebase Backend Integration & Administrator Setup Guide

This guide describes how the real Firebase backend operates within MedhaQuiz, including Firebase Authentication, Cloud Firestore, Firebase Storage, Firebase Cloud Functions, Firebase App Check, and the custom claim authorization pipeline.

---

## 1. Architecture Overview

MedhaQuiz connects to a real Firebase backend using official Firebase SDKs:

* **Firebase Authentication:** Handles Email & Password registration, login, session persistence, and password resets.
* **Cloud Firestore:** Real-time database for:
  - `classes/{classId}` (Class 1 to 10 hierarchy)
  - `subjects/{subjectId}` (Subject curriculum with color & icon)
  - `chapters/{chapterId}` (Chapter divisions)
  - `quizzes/{quizId}` (Timed/untimed exams & practice quizzes)
  - `questions/{questionId}` (MCQ questions, options, explanations, images)
  - `users/{uid}` (User profile, selected class, timestamps)
  - `attempts/{attemptId}` (Student exam & practice attempts with scoring breakdown)
  - `favorites/{id}` (Bookmarked questions/quizzes)
  - `announcements/{id}` (Curriculum & exam notices)
  - `auditLogs/{id}` (Admin security audit log)
* **Firebase Storage:** Stores user profile avatars (`users/{uid}/profile.jpg`) and educational quiz media.
* **Firebase Cloud Functions:** Manages server-side admin role claims (`setAdminRole`) and automatic user record initialization (`onUserCreated`).
* **Firebase App Check:** Protects backend resources using DebugAppCheckProvider (debug builds) and Play Integrity (production).
* **Firebase Cloud Messaging (FCM):** Handles push notifications for curriculum updates and challenges.

---

## 2. Administrator Authorization Pipeline

MedhaQuiz strictly avoids client-side admin flags, local preferences, or email substring checks.

### How Admin Access is Verified:
1. When a user logs in, Firebase Auth validates credentials and signs in the user.
2. The Android client calls `getIdToken(true)` to force-refresh the Firebase ID token.
3. The app inspects the decoded token claims for:
   ```json
   {
     "admin": true
   }
   ```
4. If and only if `claims["admin"] === true`, the user is granted the `ADMIN` role and allowed into the Admin Panel.
5. All Firestore write actions on educational content are secured server-side via `firestore.rules`, which enforce `request.auth.token.admin == true`.

---

## 3. Initial Administrator Bootstrap

Because the Android client is strictly forbidden from granting itself admin privileges, use one of the following methods to establish the initial administrator:

### Method A: Using the Bootstrap Script (`scripts/set-admin-claim.js`)
1. Create your administrator user account in the app or Firebase Console (e.g. `admin@medhaquiz.com`).
2. Go to **Firebase Console** -> **Project Settings** -> **Service accounts** -> click **Generate new private key**.
3. Save the downloaded JSON file as `service-account.json` in the root of the project.
4. Run:
   ```bash
   node scripts/set-admin-claim.js admin@medhaquiz.com
   ```
   Output:
   ```
   ✅ Success! Custom claim { admin: true } applied to admin@medhaquiz.com.
   User now has full Administrator access in the MedhaQuiz Admin Panel.
   ```
5. Log in (or log out and log back in) on the Android device. The app reads the fresh ID token and immediately opens the Admin Panel.

### Method B: Using Firebase Cloud Functions (`functions/index.js`)
Once the initial administrator is established, that administrator can call the `setAdminRole` Cloud Function from the Admin Panel or Firebase CLI to grant or revoke admin claims for other users:
```javascript
const setAdminRole = firebase.functions().httpsCallable('setAdminRole');
await setAdminRole({ targetEmail: 'newadmin@medhaquiz.com', makeAdmin: true });
```

---

## 4. Deploying Security Rules & Functions

### Firestore & Storage Rules:
Deploy the production rules:
```bash
firebase deploy --only firestore:rules,storage
```

### Cloud Functions:
```bash
cd functions
npm install
firebase deploy --only functions
```

---

## 5. Offline-First Resilience

MedhaQuiz implements a hybrid architecture:
* When online: Real-time queries and updates synchronize with Cloud Firestore collections.
* When offline: The local Room database serves cached curriculum data and queues quiz attempts until network connectivity is restored.
