# Open Health Pipeline — Task List

This task list is ordered to minimize wasted implementation effort. Do not begin later stages until the acceptance gate for the current stage passes.

---

# Stage A — Android + Health Connect Proof

## A1. Bootstrap the Android project

- [x] Create a native Android application module in Kotlin.
- [x] Use Gradle Kotlin DSL.
- [x] Configure a reasonable package name for Open Health Pipeline.
- [x] Configure minimum/target SDK values compatible with the selected Health Connect SDK.
- [x] Add Jetpack Compose.
- [x] Add AndroidX Health Connect dependency using a current supported version.
- [x] Add only dependencies needed for Stage A.
- [ ] Verify the project with a Gradle build.
- [ ] Fix all compile/configuration errors before proceeding.

### Acceptance gate

```text
./gradlew build
```

or the appropriate Windows Gradle wrapper command completes successfully.

---

## A2. Implement Health Connect availability detection

- [x] Create `HealthConnectRepository` or equivalent.
- [x] Use the official Health Connect availability API.
- [x] Represent availability as an internal app state.
- [x] Handle:
  - [x] available;
  - [x] provider install/update required;
  - [x] unavailable.
- [x] Surface the current state to the UI.
- [x] Re-check availability when appropriate after returning to the app.

### Acceptance gate

The app launches without crashing and displays a meaningful Health Connect availability state.

---

## A3. Implement the Stage A permission flow

- [x] Add manifest permissions required for `StepsRecord`.
- [x] Implement the official Health Connect runtime permission request.
- [x] Add a `Request permissions` button.
- [x] Detect whether Steps read permission is already granted.
- [x] Display granted/missing state.
- [x] Handle denial without crashing.

### Acceptance gate

On a physical phone, the app can open the Health Connect permission UI and detect whether Steps permission was granted.

---

## A4. Implement paginated raw `StepsRecord` reading

- [x] Add a `HealthRecordReader` abstraction or a minimal equivalent.
- [x] Implement a `StepsRecord` reader first.
- [x] Determine a legal historical time range.
- [x] Call `HealthConnectClient.readRecords`.
- [x] Handle `pageToken`.
- [x] Continue until all pages are read.
- [x] Do not aggregate Steps data.
- [x] Do not load the complete history into memory before processing.
- [x] Return:
  - [x] number of pages;
  - [x] number of records;
  - [x] structured error state if reading fails.

### Acceptance gate

Real raw `StepsRecord` data can be read from the user's phone and the app reports the number of records found.

---

## A5. Add a lossless Steps mapper

- [x] Create `RawHealthRecord`.
- [x] Preserve common metadata exposed by Health Connect.
- [x] Map:
  - [x] Health Connect record ID;
  - [x] record type;
  - [x] start time;
  - [x] end time;
  - [x] zone offsets when available;
  - [x] data origin;
  - [x] last modified time;
  - [x] client record ID/version;
  - [x] device metadata;
  - [x] step count.
- [x] Do not destructively normalize or discard the source information.
- [x] Add unit tests for the mapper if practical.

### Acceptance gate

At least one real `StepsRecord` can be transformed into `RawHealthRecord` while preserving its relevant metadata and count.

---

## A6. Build the minimal Compose UI

- [x] Display Health Connect availability.
- [x] Display permission state.
- [x] Add `Request permissions`.
- [x] Add `Read Health Connect`.
- [x] Display idle/loading/success/error state.
- [x] Display record count.
- [x] Display page count if useful for debugging.
- [x] Do not display sensitive individual measurement values.

### Acceptance gate

A non-developer can open the app, grant permission, press Read, and see whether real records were found.

---

## A7. Add privacy-safe diagnostic logging

- [x] Log sync/read start and end.
- [x] Log record type.
- [x] Log page number.
- [x] Log count per page.
- [x] Log error category.
- [x] Never log authentication secrets.
- [x] Avoid logging raw health values.

### Acceptance gate

A developer can diagnose a failed Steps read from logs without exposing raw health payloads.

---

## A8. Test Stage A on the physical Android phone

- [ ] Build the APK/debug app.
- [ ] Install on the physical phone.
- [ ] Confirm Health Connect is detected.
- [ ] Grant Steps permission.
- [ ] Trigger read.
- [ ] Confirm real records are found.
- [ ] Trigger read a second time.
- [ ] Confirm stable behavior.
- [ ] Record any SDK/device-specific behavior in README.

### Stage A completion gate

Do not continue until:

```text
Physical Android phone
→ Health Connect
→ Open Health Pipeline
→ raw Steps records successfully read
```

works.

---

# Stage B — Representative Health Connect Types

## B1. Centralize supported type descriptors

- [~] Introduce a small record-type registry.
- [ ] Each descriptor identifies:
  - [~] type name;
  - [~] Kotlin record class;
  - [~] read permission;
  - [~] feature dependency if relevant;
  - [~] reader;
  - [~] mapper.
- [~] Keep orchestration generic while mapping remains type-specific.

---

## B2. Add `HeartRateRecord`

- [~] Request permission.
- [~] Read with pagination.
- [~] Preserve samples.
- [~] Preserve metadata.
- [~] Add mapper tests where practical.
- [~] Test using real phone data.

---

## B3. Add `RestingHeartRateRecord`

- [ ] Request permission.
- [ ] Read with pagination.
- [~] Preserve metadata/value.
- [~] Test on device.

---

## B4. Add `SleepSessionRecord`

- [ ] Request permission.
- [~] Read sessions.
- [~] Preserve session metadata.
- [~] Preserve sleep stages.
- [ ] Test on device.

---

## B5. Add `ExerciseSessionRecord`

- [ ] Request permission.
- [ ] Read sessions.
- [~] Preserve session metadata and segments/laps when exposed.
- [~] Treat exercise route access separately.
- [~] Do not make route access a prerequisite for importing the session itself.
- [ ] Test on device.

---

## B6. Add `WeightRecord`

- [ ] Request permission.
- [~] Read records.
- [~] Preserve typed measurement meaning and source unit representation.
- [ ] Test on device.

### Stage B completion gate

At least one instantaneous, interval, sample/series, and session-oriented record pattern has been validated.

---

# Stage C — Supabase/PostgreSQL Persistence

## C1. Create Supabase project

- [~] Create the Supabase project.
- [~] Record project URL locally.
- [~] Use a publishable/anon client key only.
- [~] Never store a service-role key in the Android project.
- [~] Add secrets/local configuration to `.gitignore`.

---

## C2. Add version-controlled SQL migrations

- [~] Create migration folder.
- [~] Add `health_records`.
- [~] Add `sync_state`.
- [ ] Add unique constraint:
  - [~] `(user_id, record_type, hc_record_id)`.
- [~] Add useful time/type index.
- [~] Add `updated_at` behavior if desired.
- [~] Do not add speculative indexes without a demonstrated need.

---

## C3. Configure Supabase Auth

- [~] Select the simplest secure prototype auth flow.
- [~] Ensure the app receives a stable authenticated `user_id`.
- [~] Confirm requests carry an authenticated JWT.
- [~] Handle missing/expired session.

---

## C4. Configure Row Level Security

- [~] Enable RLS on `health_records`.
- [~] Enable RLS on `sync_state`.
- [~] Add policies based on `auth.uid() = user_id`.
- [~] Support required SELECT/INSERT/UPDATE operations.
- [~] Verify one user cannot access another user's records using integration tests.

---

## C5. Add `HealthRecordSink`

- [~] Define persistence interface independent of Supabase.
- [~] Implement Supabase sink.
- [~] Accept `RawHealthRecord`.
- [~] Convert common metadata to relational fields.
- [~] Persist type-specific content to JSONB.
- [~] Keep Health Connect code independent from Supabase SDK classes.

---

## C6. Implement idempotent upsert

- [~] Upsert using the compound logical identity.
- [~] Re-running the same records must not create duplicates.
- [~] Avoid allowing stale incoming records to replace newer state.
- [~] Clear `deleted_at` if a newer upsertion restores a record.
- [~] Add integration tests.

### Acceptance gate

Run the same Steps import twice and verify that the database contains one logical row per Health Connect record.

---

## C7. Add paged streaming upload

- [~] Send each mapped Health Connect page to the sink.
- [~] Upload in configurable sub-batches if required.
- [~] Avoid accumulating the complete history in memory.
- [~] Implement bounded retries for transient backend/network failures.
- [~] Do not silently discard a failed batch.
- [~] Return partial/failure state clearly.

### Stage C completion gate

```text
Phone
→ Health Connect
→ Kotlin collector
→ Supabase/PostgreSQL
```

works for real data and a repeated sync does not duplicate rows.

---

# Stage D — Broad Health Connect Coverage

## D1. Audit current Health Connect SDK record types

- [~] Compare the project registry with the current official SDK.
- [~] Identify stable, feature-gated, experimental, or unavailable types.
- [~] Document unsupported types.
- [~] Do not reference classes that do not exist in the selected SDK.

---

## D2. Implement remaining supported simple types

Add readers/mappers for the applicable categories:

- [~] activity/fitness;
- [~] body measurements;
- [~] vitals;
- [~] hydration/nutrition;
- [~] cycle tracking;
- [~] wellness;
- [~] planned exercise.

---

## D3. Validate complex/nested payloads

- [~] Preserve all exposed nested samples.
- [~] Preserve stages/segments.
- [~] Preserve enum meanings.
- [ ] Preserve metadata.
- [~] Do not apply arbitrary truncation.
- [~] Add representative serialization tests.

---

## D4. Add per-type synchronization summary

- [~] records read;
- [~] records persisted;
- [~] pages;
- [~] skipped because permission missing;
- [~] skipped because feature unavailable;
- [~] failed with error category.

### Stage D completion gate

The registry represents the maximum practical set of record types supported by the selected SDK/provider and unavailable types fail gracefully.

---

# Stage E — Incremental Synchronization

## E1. Implement sync-state persistence

- [~] Store a stable scope.
- [~] Store changes token.
- [~] Store last successful sync.
- [~] Store backfill completion state.
- [~] Never replace a known-safe token before persistence succeeds.

---

## E2. Make initial backfill race-safe

- [~] Establish an appropriate initial changes position before/around backfill.
- [~] Perform historical import.
- [~] Reconcile changes that occurred during backfill.
- [~] Persist the resulting safe token only after reconciliation succeeds.
- [~] Add tests or reproducible test procedure.

---

## E3. Implement changes API

- [~] Fetch changes using stored token.
- [~] Process upsertion changes.
- [~] Process deletion changes.
- [~] Follow paginated changes responses.
- [~] Persist records/deletions.
- [~] Advance token only after successful persistence.

---

## E4. Implement soft deletion

- [~] Find record using compound logical identity.
- [~] Set `deleted_at`.
- [~] Keep prior payload.
- [~] Handle deletion of an unknown local record without crashing.

---

## E5. Handle expired/invalid change tokens

- [~] Detect unusable token state.
- [~] Trigger reconciliation/backfill.
- [~] Establish new safe token.
- [~] Never silently mark the dataset current.

### Stage E completion gate

New, changed, and deleted Health Connect records propagate correctly without re-reading the full history during normal operation.

---

# Stage F — Background Synchronization

## F1. Add background-read permission

- [~] Check provider/feature support.
- [~] Declare/request the required permission.
- [~] Do not block foreground sync if permission is denied.

---

## F2. Add WorkManager

- [~] Create periodic sync worker.
- [~] Respect Android background limitations.
- [~] Apply network constraints where useful.
- [~] Reuse the same `SyncCoordinator`.
- [~] Avoid creating a second independent sync implementation.

---

## F3. Prevent overlapping syncs

- [~] Ensure manual and background sync do not corrupt shared state.
- [~] Use unique work or equivalent coordination.
- [~] Preserve idempotency.

### Stage F completion gate

The app can update the backend in the background when Android and Health Connect permissions allow it, while manual sync remains reliable.

---

# Repository / Quality Tasks

## Q1. Keep documentation current

- [~] README explains project goal.
- [~] README explains current implemented stage.
- [~] README lists tested device/Android/provider versions.
- [~] README lists current supported record types.
- [~] README documents known Health Connect limitations.
- [~] README explains how to build and test.

---

## Q2. Keep secrets out of Git

- [~] Verify `.gitignore`.
- [~] Never commit Supabase service-role key.
- [~] Never commit private tokens.
- [~] Review repository history if a secret is accidentally added.

---

## Q3. Continuous build verification

After every meaningful implementation batch:

- [~] Run Gradle build.
- [~] Fix compiler errors immediately.
- [~] Do not stack multiple unverified implementation stages.
- [~] Commit only buildable checkpoints whenever practical.

---

# Immediate Next Task for Kiro

Kiro should execute only Stage A first.

The next implementation session should:

1. bootstrap/fix the Android project;
2. add the official Health Connect dependency;
3. implement availability detection;
4. implement Steps read permission;
5. implement paginated raw `StepsRecord` reading;
6. map Steps into the lossless internal representation;
7. create the minimal Compose UI;
8. run the Gradle build and fix errors;
9. stop and report the exact steps required to test on the physical phone.

Do not implement Supabase or later stages in the same session.
