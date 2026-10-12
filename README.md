# SpamShield

SpamShield is a Kotlin + Jetpack Compose Android app for collecting message records into a local Room database, marking spam candidates, flagging or unflagging them, and maintaining a list of blocked or deleted threads.

## What it does

- Reads messages from Android's default SMS/MMS provider when the app is granted the SMS-related permissions.
- Imports call history (caller, call type, time, and duration) from Android's call log after the user grants permission.
- Stores data in a Room database so it can be reviewed offline.
- Lets the user mark a message as spam or not spam.
- Lets the user flag or unflag messages, mark threads as blocked in the app, and delete a thread's local records.
- Lets the user flag call records, mark them as spam, or delete them from the local database.

## Important Android restriction

Direct reads of Google Messages or Samsung Messages databases are not generally available to normal Android apps. Those apps store data in app-private storage or OS-managed containers and do not expose a supported public API for third-party apps to read message content. This app therefore handles:

- default SMS/MMS support via Android's ContentResolver API, and
- user-provided export/import data for Google Messages or Samsung Messages records.

This app does not currently import Google Messages or Samsung Messages exports. Any such import requires a supported export format and a dedicated import flow.

## Download

### Download the SpamShield Android APK

- [Download the latest APK from `jwgmpi-wolff/phone-spam-list`](https://github.com/jwgmpi-wolff/phone-spam-list/releases/latest/download/app-debug.apk)
- [Download the latest APK from `jerrywolff_microsoft/phone-spam-list`](https://github.com/jerrywolff_microsoft/phone-spam-list/releases/latest/download/app-debug.apk)

Each APK is also available as **app-debug.apk** under Assets on its [latest release page](https://github.com/jwgmpi-wolff/phone-spam-list/releases/latest) or [the other latest release page](https://github.com/jerrywolff_microsoft/phone-spam-list/releases/latest). Both repositories and releases are private, so GitHub sign-in and repository access are required.

## Local run

Once the Android SDK is installed and `local.properties` is generated, run:

```bash
./gradlew assembleDebug
```

## Permissions

The app requests:

- `READ_SMS`
- `RECEIVE_SMS`
- `SEND_SMS`
- `READ_CALL_LOG` (requested when importing call history)
- `READ_EXTERNAL_STORAGE` (Android 12 and lower)

Call-log and SMS permissions expose sensitive personal data. The app requests the corresponding permission only when the user starts that import. Call-log permissions are subject to Google Play policy requirements. This is intended for local review and data collection on a device the user controls.
