# Open Health Pipeline — Tech Design

## 1. Purpose

Open Health Pipeline is an Android/Kotlin ingestion pipeline whose Milestone 1 goal is to read the maximum practical amount of authorized raw data from Android Health Connect and persist it faithfully in Supabase/PostgreSQL.

The architecture is intentionally staged:

```text
Health Connect
    ↓
Android/Kotlin collector
    ↓
lossless internal representation
    ↓
Supabase/PostgreSQL
```

The first working vertical slice stops before Supabase and proves that the Android app can access Health Connect on a physical phone, obtain permission, read real raw records, paginate correctly, and preserve metadata.

## 2. Design Principles

1. Preserve source data before transforming it.
2. Prefer official Android/Health Connect APIs and samples.
3. Keep Health Connect access, mapping, synchronization, and persistence separate.
4. Make synchronization idempotent.
5. Never advance sync state before the corresponding data is safely persisted.
6. Fail per record type whenever possible rather than failing the entire sync.
7. Keep the Android UI minimal.
8. Keep GitHub as the source of truth.
9. Avoid coupling the project to Kiro or any other AI coding environment.
10. Treat constants such as page size, batch size, retry delays, and timeouts as implementation parameters rather than product requirements.

---

## 3. Technology Stack

### Android client

- Kotlin
- Native Android
- Jetpack Compose
- AndroidX Health Connect SDK
- Kotlin coroutines
- WorkManager only after manual sync is proven
- Gradle Kotlin DSL

### Backend

- Supabase
- PostgreSQL
- Supabase Auth
- PostgreSQL Row Level Security
- SQL migrations committed to the repository

### Serialization

- Kotlin serialization or an equivalent well-supported Kotlin serialization layer
- JSONB for record-specific payloads
- Typed relational columns for common metadata

### Repository

- GitHub
- Public repository: `open-health-pipeline`

---

## 4. High-Level Components

```text
┌────────────────────────────┐
│        Compose UI          │
│ availability / permissions │
│ status / Sync Now          │
└─────────────┬──────────────┘
              │
              ▼
┌────────────────────────────┐
│      SyncCoordinator       │
│ orchestrates one sync run  │
└──────┬──────────────┬──────┘
       │              │
       ▼              ▼
┌──────────────┐  ┌────────────────┐
│HealthConnect │  │ SyncStateStore │
│Repository    │  │ tokens/cursors │
└──────┬───────┘  └────────────────┘
       │
       ▼
┌────────────────────────────┐
│ RecordReaderRegistry       │
│ readers per record type    │
└─────────────┬──────────────┘
              │
              ▼
┌────────────────────────────┐
│ HealthRecordMapper         │
│ lossless internal DTO      │
└─────────────┬──────────────┘
              │
              ▼
┌────────────────────────────┐
│ HealthRecordSink           │
│ local debug → Supabase     │
└────────────────────────────┘
```

For Stage A, `HealthRecordSink` can simply return counts to the UI and logs. Supabase is introduced only after Health Connect reading works on the physical device.

---

## 5. Proposed Android Project Structure

```text
app/
└── src/main/java/.../
    ├── MainActivity.kt
    ├── ui/
    │   ├── MainScreen.kt
    │   ├── MainViewModel.kt
    │   └── UiState.kt
    ├── health/
    │   ├── HealthConnectAvailability.kt
    │   ├── HealthConnectRepository.kt
    │   ├── HealthPermissions.kt
    │   ├── HealthRecordReader.kt
    │   ├── HealthRecordReaderRegistry.kt
    │   ├── HealthRecordMapper.kt
    │   └── model/
    │       ├── RawHealthRecord.kt
    │       └── SyncRecordResult.kt
    ├── sync/
    │   ├── SyncCoordinator.kt
    │   ├── SyncResult.kt
    │   └── SyncState.kt
    └── data/
        └── remote/
            ├── SupabaseClientProvider.kt
            ├── SupabaseHealthRecordSink.kt
            └── dto/
```

This is a target structure, not a requirement to create every file immediately. Stage A should remain minimal.

---

## 6. Health Connect Availability

The app uses the official Health Connect SDK availability API instead of inferring availability only from Android version.

The UI exposes three practical states:

- available;
- update/install required;
- unavailable.

Availability is checked on startup and can be re-checked when the app returns to the foreground.

No Health Connect operation proceeds when the provider is unavailable.

---

## 7. Permission Model

Permissions are generated from the record types currently enabled in the registry.

Stage A initially requests read access for:

- `StepsRecord`
- `HeartRateRecord`
- `RestingHeartRateRecord`
- `SleepSessionRecord`
- `ExerciseSessionRecord`
- `WeightRecord`

The architecture must make it easy to add further record types later.

Historical access is requested when the SDK/provider supports the relevant permission. Without it, the app respects the normal historical access window.

Background-read permission is not required for Stage A. It is added only when WorkManager/background synchronization is implemented.

Permission denial for one data type does not block record types whose permissions were granted.

---

## 8. Record Reader Registry

Health Connect records are typed Kotlin classes and cannot be safely treated as one fully generic record payload. Therefore the implementation uses a centralized registry of supported readers.

Conceptually:

```text
RecordTypeDescriptor
- stable type name
- Kotlin record class
- required read permission
- feature requirement, if any
- reader
- mapper
```

This allows the project to:

- iterate over supported record types;
- skip unavailable feature-gated types;
- isolate failures per type;
- add new Health Connect record types without rewriting sync orchestration.

Stage A fully implements `StepsRecord` first. The remaining five representative types are added after Steps successfully builds and reads real data.

---

## 9. Lossless Internal Representation

The ingestion boundary uses a common DTO for metadata plus a JSON-like payload for type-specific fields.

Conceptual model:

```text
RawHealthRecord
- hcRecordId
- recordType
- startTime?
- endTime?
- time?
- startZoneOffset?
- endZoneOffset?
- zoneOffset?
- dataOrigin?
- lastModifiedTime?
- clientRecordId?
- clientRecordVersion?
- deviceType?
- deviceManufacturer?
- deviceModel?
- deviceDisplayName?
- payload
```

### Payload rules

The mapper preserves the information exposed by Health Connect for that type.

It does not destructively normalize all units into SI.

Where the Health Connect Kotlin API represents values through typed unit classes, the payload should preserve enough information to reconstruct the source meaning. A practical representation is:

```json
{
  "count": 1234
}
```

or, for measurements:

```json
{
  "value": 72.4,
  "unit": "kg"
}
```

For series/session records, nested samples/stages/segments are preserved.

No arbitrary sample truncation is performed.

---

## 10. Stage A Reading Strategy

Stage A proves the pipeline with manual foreground reading.

### Steps vertical slice

1. Check Health Connect availability.
2. Request `StepsRecord` read permission.
3. Determine an allowed time range.
4. Call `readRecords`.
5. Follow every returned page token.
6. Map each record to the internal representation.
7. Return count and debug-safe metadata to the UI.
8. Build successfully.
9. Test on a physical phone.

The UI does not need to display individual health values.

### Historical range

The reader selects the broadest legal time range available to the app.

When extended history permission is unavailable or not granted, the reader stays within the Health Connect historical restriction.

Time boundaries must be generated centrally so that later readers use the same strategy.

---

## 11. Pagination

`readRecords` is always treated as potentially paginated.

The reader loops until no next page token exists.

Page size is configurable.

The reader processes one page at a time and does not accumulate the user's complete Health Connect history in memory.

For Stage A, page results may be mapped and counted immediately.

For Stage C, each mapped page is forwarded to the persistence sink before the next page is fetched.

---

## 12. Error Handling

Each record type is synchronized independently.

Expected categories include:

- permission denied;
- feature unavailable;
- provider unavailable;
- Health Connect exception;
- serialization/mapping failure;
- network failure;
- authentication failure;
- backend rejection;
- expired change token.

A record-type failure produces a structured result instead of crashing the entire run.

Conceptual result:

```text
RecordTypeSyncResult
- recordType
- status
- recordsRead
- recordsPersisted
- pagesRead
- errorCategory?
- errorMessage?
```

Sensitive health values are excluded from ordinary logs.

---

## 13. Supabase Integration

Supabase begins only after Stage A and representative local reads are working.

The Android client uses:

- Supabase project URL;
- publishable/anon key appropriate for client use;
- authenticated user session;
- RLS-protected tables.

A Supabase service-role key must never be shipped in the APK.

The app may use the documented Kotlin client or direct HTTPS APIs if later compatibility requirements justify it. Backend choice is isolated behind `HealthRecordSink`, so the Health Connect layer does not depend on Supabase implementation details.

---

## 14. Database Schema

### `health_records`

```sql
create table health_records (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references auth.users(id) on delete cascade,
    hc_record_id text not null,
    record_type text not null,

    start_time timestamptz,
    end_time timestamptz,
    time timestamptz,

    start_zone_offset text,
    end_zone_offset text,
    zone_offset text,

    data_origin text,

    device_type integer,
    device_manufacturer text,
    device_model text,
    device_display_name text,

    last_modified_time timestamptz,
    client_record_id text,
    client_record_version bigint,

    payload jsonb not null,

    deleted_at timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),

    unique (user_id, record_type, hc_record_id)
);
```

Recommended initial index:

```sql
create index health_records_user_type_time_idx
on health_records (user_id, record_type, start_time);
```

A GIN index on `payload` should be added later only if real query patterns justify it.

### `sync_state`

```sql
create table sync_state (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references auth.users(id) on delete cascade,
    scope text not null,
    changes_token text,
    last_successful_sync_at timestamptz,
    backfill_completed_at timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),

    unique (user_id, scope)
);
```

All schema changes are SQL migration files committed to GitHub.

---

## 15. RLS Strategy

Both tables have RLS enabled.

The authenticated user may operate only on rows where:

```sql
auth.uid() = user_id
```

`health_records` must support the operations required by the final persistence implementation, including `SELECT`, `INSERT`, and `UPDATE`.

`sync_state` must support reading, inserting, and updating the user's own synchronization state.

The anon/publishable key is not treated as a secret. RLS and authenticated identity provide authorization.

---

## 16. Idempotency

The logical unique key is:

```text
(user_id, record_type, hc_record_id)
```

The persistence layer performs an upsert against that unique key.

If an incoming record represents a newer version, mutable stored fields are updated.

If an incoming record is stale, it must not overwrite a newer stored state.

The exact comparison strategy is implemented defensively because not every record may expose identical metadata semantics.

Repeated full or partial syncs must converge on the same logical database state.

---

## 17. Initial Backfill and Change Tracking

A large initial import creates a race risk if changes happen during the backfill.

The implementation should establish change tracking at a safe point before or around the backfill, then reconcile changes after the historical scan.

A safe conceptual sequence is:

1. Obtain an initial changes token for the relevant sync scope when supported.
2. Perform historical paginated import.
3. Persist imported pages.
4. Consume changes from the previously established token.
5. Persist all resulting upserts/deletions.
6. Only then persist the new safe changes token as the current sync state.

This avoids a gap where records changed during a long backfill could be missed.

The exact token scope follows Health Connect guidance: independent data types should generally use independent tokens or stable logical groups.

---

## 18. Incremental Synchronization

After the backfill, normal sync uses Health Connect's changes API for the configured scope.

For each changes response:

- upsertion changes are mapped and upserted;
- deletion changes mark matching backend records deleted;
- additional pages are consumed while indicated;
- sync state is advanced only after persistence succeeds.

If the token expires or becomes invalid:

- do not assume synchronization is current;
- execute a recovery/reconciliation backfill;
- establish a new safe token.

A failure leaves the last known-safe token intact.

---

## 19. Deletions

Backend deletion is represented initially as a soft delete:

```text
deleted_at = current backend timestamp
```

The original payload remains available for auditing/debugging.

If an upsertion for the same logical record later appears, the row can be restored by clearing `deleted_at` and updating to the newer state.

---

## 20. Upload Strategy

Mapped records are uploaded in configurable batches.

A batch failure is classified as retryable or non-retryable where practical.

Transient failures use bounded exponential backoff with jitter where available.

No synchronization token is advanced until all data represented by that token position has been safely persisted.

The app reports partial success when some record types fail.

---

## 21. Authentication

Milestone 1 backend integration should use the simplest secure Supabase Auth flow suitable for a personal prototype.

Anonymous auth is acceptable for an initial single-user proof of concept if it provides a stable authenticated `user_id` and is protected by RLS.

Before a public release, authentication UX and account recovery must be reconsidered.

The Android app stores no privileged backend secret.

---

## 22. Minimal UI State

The main screen exposes:

```text
Health Connect: Available / Update Required / Unsupported
Permissions: Granted / Partial / Missing
Backend: Not configured / Authenticated / Error
Last sync: timestamp / Never
Current state: Idle / Syncing / Partial / Error
Records read: count
Records persisted: count
Failed/skipped types: count

[Request permissions]
[Sync Now]
```

Stage A can omit backend fields until Supabase is added.

---

## 23. Logging

INFO-level logs may include:

- sync start/end;
- type counts;
- success/failure summary.

DEBUG-level logs may include:

- record type;
- page number;
- page record count;
- retry attempt.

Logs must not contain:

- auth tokens;
- secrets;
- raw JSON payloads;
- unnecessary health values.

---

## 24. Testing Strategy

### Unit tests

- record mapper behavior;
- unique-key construction;
- serialization of representative record types;
- state transitions;
- retry policy.

### Database integration tests

- insert;
- conflict/upsert;
- RLS isolation;
- update;
- soft delete;
- sync-state persistence.

### Physical-device tests

A physical Android phone with existing Health Connect data is the primary end-to-end target.

Stage A acceptance test:

1. App installs.
2. Health Connect detected.
3. Permission request opens.
4. User grants Steps permission.
5. `Read Health Connect` reads real `StepsRecord` data.
6. Pagination completes.
7. UI displays record count.
8. App does not crash.
9. Gradle build succeeds.

---

## 25. Development Stages

### Stage A — Health Connect proof

Implement a buildable Android/Kotlin project, availability detection, permission flow, `StepsRecord`, pagination, lossless mapping, minimal UI, and physical-device testing.

### Stage B — Representative complex types

Add:

- `HeartRateRecord`
- `RestingHeartRateRecord`
- `SleepSessionRecord`
- `ExerciseSessionRecord`
- `WeightRecord`

Validate nested/sample/session serialization patterns.

### Stage C — Supabase

Add Auth, SQL migrations, RLS, record persistence, upsert behavior, and `sync_state`.

### Stage D — Broad type coverage

Expand the registry across all record types supported by the chosen Health Connect SDK/provider, using feature checks where required.

### Stage E — Incremental changes

Add changes tokens, updates, deletions, safe token advancement, and recovery.

### Stage F — Background synchronization

Add WorkManager and Health Connect background-read capability after manual sync is reliable.

---

## 26. Out of Scope

Not part of Milestone 1:

- dashboards;
- charts;
- AI;
- health recommendations;
- coaching;
- medical advice;
- Apple Health;
- direct Garmin/Fitbit/Strava ingestion;
- web frontend;
- social/sharing features;
- polished consumer UI.

---

## 27. Architecture Decision Summary

The project deliberately chooses:

- native Kotlin because Health Connect is an Android-native integration;
- official Health Connect APIs as the primary source of truth;
- a typed reader/mapper registry because Health Connect records are heterogeneous;
- JSONB for type-specific payloads to maximize preservation and future flexibility;
- common relational metadata for traceability and efficient queries;
- compound logical identity `(user_id, record_type, hc_record_id)` for idempotency;
- Supabase Auth + RLS instead of privileged credentials in the APK;
- manual foreground sync before background automation;
- staged implementation so a working Health Connect proof exists before backend complexity is introduced.

This design should remain understandable enough that a developer can inspect the repository and trace a record from Health Connect read → mapping → persistence without depending on a proprietary AI development tool.
