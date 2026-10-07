# Open Health Pipeline

Android Health Connect data ingestion pipeline to PostgreSQL/Supabase.

## Current Stage: Stage A — Health Connect Proof

This is the first working vertical slice of the pipeline. It proves the
Android application can access Health Connect on a physical phone, obtain
permission, read real raw StepsRecord data with full pagination, and display
record counts in the UI.

Supabase persistence begins in Stage C.

---

## Build Requirements

- No local Android Studio is required for the default workflow.
- GitHub Actions builds the debug APK in the cloud on every push, pull request,
  and manual workflow run.
- A physical Android phone running Android 9+ with Health Connect installed
  (built-in on Android 14+, available from Play Store on Android 9-13)

---

## How to Build in GitHub

1. Push this repository to GitHub.
2. Open the repository on GitHub.
3. Go to **Actions**.
4. Open **Android Build**.
5. Wait for the run to finish.
6. Download the `open-health-pipeline-debug-apk` artifact.

The artifact contains `app-debug.apk`.

The workflow also supports **Run workflow** from the Actions tab.

### Optional local command-line build

If you later install JDK 17 and the Android command-line SDK tools, you can build
without Android Studio:

```bash
./gradlew assembleDebug
```

The debug APK is output to:
```
app/build/outputs/apk/debug/app-debug.apk
```

---

## How to Test on a Physical Android Phone

### Prerequisites

- Health Connect is installed (check Settings > Apps or open Health Connect app).
- The phone has some health data (e.g., step data from Google Fit, Samsung Health,
  Garmin Connect, or any app that writes to Health Connect).

### Steps

1. Install the APK on your phone:
   - Download the GitHub Actions artifact on your computer or phone.
   - Extract it and open `app-debug.apk` on the phone.
   - If Android asks, allow installing apps from that source.
2. Open **Open Health Pipeline** on the phone.
3. The app shows **Health Connect: Available** if Health Connect is installed.
   If it shows **Update Required**, tap the prompt and install Health Connect
   from the Play Store.
4. Tap **Request Permissions**.
5. The Health Connect permission screen opens. Grant **Steps** read access.
6. Back in the app, **Steps Permission: Granted** appears.
7. Tap **Read Health Connect**.
8. The app reads your last 30 days of step data with full pagination.
9. The screen shows the record count and page count.
10. Tap **Read Health Connect** again — the count should be identical,
    confirming stable behavior.

### What to check in Logcat

Filter by tag `HealthRecordReader` or `SyncCoordinator`:

```
D/HealthRecordReader: StepsRecord time range: 2024-xx-xxTxx:xx:xxZ to 2024-xx-xxTxx:xx:xxZ
D/HealthRecordReader: Page 1 of StepsRecord: 42 records
I/HealthRecordReader: Completed read of StepsRecord: 42 records across 1 pages
I/SyncCoordinator: Sync completed in 312ms — attempted: 1, succeeded: 1, failed: 0, ...
```

No raw step count values appear at INFO level or above.

---

## Project Structure

```
app/src/main/java/io/openhealthpipeline/app/
├── MainActivity.kt              — single activity, permission launcher
├── ui/
│   ├── UiState.kt               — immutable UI state
│   ├── MainViewModel.kt         — bridges Health Connect layer <-> Compose
│   ├── MainScreen.kt            — minimal Compose screen
│   └── theme/
│       └── Theme.kt             — Material3 dynamic colour theme
├── health/
│   ├── HealthConnectAvailability.kt  — 3-state availability enum
│   ├── HealthConnectRepository.kt    — SDK status check, client lifecycle
│   ├── HealthPermissions.kt          — permission registry
│   ├── HealthRecordReader.kt         — paginated Steps reader + mapper
│   └── model/
│       ├── RawHealthRecord.kt        — lossless internal DTO
│       ├── AnySerializer.kt          — kotlinx.serialization for Any? payload
│       └── SyncRecordResult.kt       — per-type outcome
└── sync/
    ├── SyncCoordinator.kt            — orchestrates one sync run
    └── SyncResult.kt                 — aggregated sync outcome
```

---

## Known Limitations

- **Health Connect availability**: Health Connect is not available on all Android
  devices. It is built into Android 14+ and available from the Play Store for
  Android 9-13. Devices running Android 8 (minSdk) will see "Not Supported".

- **30-day history limit**: Without the `READ_HEALTH_DATA_HISTORY` permission,
  Health Connect limits reads to the past 30 days. Stage A uses this default
  window. Extended history access will be added in Stage B.

- **Foreground-only reads**: Health Connect requires the app to be in the
  foreground for reads unless `READ_HEALTH_DATA_IN_BACKGROUND` is granted.
  Background sync via WorkManager is Stage F.

- **Change token expiry**: Health Connect change tokens (used in Stage E for
  incremental sync) expire after up to 30 days of non-use. The app will need
  to handle token expiry with a fallback backfill.

- **Rate limits**: Repeated Health Connect read requests in rapid succession may
  result in a `RemoteException` with a rate-limit message. The reader applies
  back-off before retrying.

- **Exercise routes**: `ExerciseSessionRecord` route data requires the separate
  `READ_EXERCISE_ROUTE` permission and per-request user consent. Route access
  is not implemented in Stage A or B.

- **Device data availability**: Not all record types will have data on every
  device. The set of populated types depends on which apps the user has installed
  and authorized to write to Health Connect.

---

## Development Stages

| Stage | Status | Description |
|-------|--------|-------------|
| A     | In progress | Android project, Health Connect availability, Steps read, pagination, minimal UI. Needs GitHub build and phone validation. |
| B     | 🔜 Next | HeartRate, RestingHeartRate, Sleep, Exercise, Weight |
| C     | ⬜ Planned | Supabase Auth, SQL schema, RLS, upsert persistence |
| D     | ⬜ Planned | All Health Connect record types |
| E     | ⬜ Planned | Change tokens, incremental sync, deletions |
| F     | ⬜ Planned | WorkManager background sync |
