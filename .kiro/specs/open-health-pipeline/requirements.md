# Open Health Pipeline — Requirements Document

## 1. Introduction

Open Health Pipeline is an open-source Android application whose first milestone has one primary purpose:

> Read the maximum amount of health and fitness data that the user authorizes through Android Health Connect and persist that data as faithfully as possible in a Supabase/PostgreSQL backend.

Milestone 1 is a data-ingestion pipeline.

It is not intended to provide:

- dashboards;
- analytics;
- AI features;
- recommendations;
- charts;
- coaching;
- social features;
- polished consumer-facing UI.

The architecture is:

Health Connect → Android/Kotlin collector → Supabase/PostgreSQL

The Android application SHALL use Kotlin, native Android APIs, and the official AndroidX Health Connect SDK wherever reasonably possible.

The system SHOULD prioritize:

1. data completeness;
2. lossless preservation;
3. traceability;
4. idempotency;
5. reliability;
6. simplicity.

---

## 2. Milestone 1 Definition of Done

Milestone 1 is considered complete when:

1. The Android application can be installed on a physical compatible Android phone.
2. The application detects Health Connect availability correctly.
3. The user can grant Health Connect read permissions.
4. The application can read real Health Connect records from the phone.
5. The application can upload those records to Supabase/PostgreSQL.
6. Health Connect metadata and record-specific data are preserved.
7. Re-running synchronization does not create duplicate records.
8. Updated records can update their existing database representation.
9. Deleted Health Connect records can eventually be reflected in the backend.
10. Failures affecting one record type do not unnecessarily prevent other record types from syncing.

---

## 3. Guiding Principles

### 3.1 Preserve first, transform later

The ingestion pipeline SHALL preserve the source information as faithfully as reasonably possible.

Analytics-oriented normalization MUST NOT destroy or replace source data.

If normalized or derived values are added in the future, they SHALL exist in addition to the original representation.

### 3.2 Do not silently discard data

The application SHALL NOT intentionally truncate samples, sub-records, stages, routes, or other record contents solely because they are large.

If an API, network, storage, or platform limitation makes a complete record impossible to persist, the application SHALL surface that limitation explicitly rather than silently discarding part of the record.

### 3.3 Prefer official APIs and samples

Implementation SHOULD prioritize:

1. official Android Health Connect documentation;
2. official Android Health Connect sample repositories;
3. official Supabase documentation;
4. mature third-party open-source projects as secondary architectural references.

Third-party source code SHALL NOT be copied unless its license is compatible with this project's licensing decisions.

---

# 4. Health Connect Availability

## Requirement 1 — Availability Detection

The application SHALL determine whether Health Connect is available before attempting Health Connect operations.

The application SHALL correctly handle the availability states exposed by the installed Health Connect SDK.

When Health Connect is available:

- Health Connect functionality MAY proceed.

When Health Connect requires installation or an update:

- the application SHALL explain this state;
- the application SHOULD provide an appropriate action to install or update Health Connect when supported.

When Health Connect is unavailable on the device:

- Health Connect-dependent functionality SHALL be disabled;
- the user SHALL receive a clear explanation.

The application SHOULD re-evaluate Health Connect availability when returning to the foreground when appropriate.

The implementation SHALL use the official Health Connect SDK availability APIs rather than relying only on Android OS version checks.

---

# 5. Permissions

## Requirement 2 — Health Connect Permissions

The application SHALL declare the appropriate read permissions for every Health Connect record type it attempts to read.

The application SHALL support requesting Health Connect permissions at runtime using the official permission APIs.

The application SHOULD request:

- record-specific read permissions;
- historical-read permission when supported and useful;
- background-read permission only when background synchronization is implemented.

The application SHALL continue operating with the subset of record permissions that the user grants.

A denied permission for one record type SHALL NOT cause all synchronization to fail.

The application SHALL clearly identify which data categories cannot be synchronized because permission was not granted.

The application SHALL NOT request write permissions during Milestone 1 unless a future requirement explicitly introduces Health Connect writing.

---

# 6. Supported Record Types

## Requirement 3 — Maximum Practical Record Coverage

The system SHALL be designed to ingest the broadest practical set of Health Connect health and fitness record types supported by the SDK version used by the project.

The implementation SHALL NOT assume that every record class exists or is available on every device or SDK version.

Record support MAY depend on:

- Health Connect SDK version;
- Android version;
- Health Connect provider version;
- feature availability;
- feature flags;
- user permissions.

The application SHALL check feature availability where the SDK requires it.

The initial target set SHOULD include, when supported:

### Activity and fitness

- `ActiveCaloriesBurnedRecord`
- `ActivityIntensityRecord`
- `CyclingPedalingCadenceRecord`
- `DistanceRecord`
- `ElevationGainedRecord`
- `ExerciseSessionRecord`
- `FloorsClimbedRecord`
- `PowerRecord`
- `SpeedRecord`
- `StepsCadenceRecord`
- `StepsRecord`
- `TotalCaloriesBurnedRecord`
- `Vo2MaxRecord`
- `WheelchairPushesRecord`

### Body measurements

- `BasalMetabolicRateRecord`
- `BodyFatRecord`
- `BodyWaterMassRecord`
- `BoneMassRecord`
- `HeightRecord`
- `LeanBodyMassRecord`
- `WeightRecord`

### Vitals

- `BloodGlucoseRecord`
- `BloodPressureRecord`
- `BodyTemperatureRecord`
- `HeartRateRecord`
- `HeartRateVariabilityRmssdRecord`
- `OxygenSaturationRecord`
- `RespiratoryRateRecord`
- `RestingHeartRateRecord`
- `SkinTemperatureRecord`

### Nutrition and hydration

- `HydrationRecord`
- `NutritionRecord`

### Sleep

- `SleepSessionRecord`

### Cycle tracking

- `BasalBodyTemperatureRecord`
- `CervicalMucusRecord`
- `IntermenstrualBleedingRecord`
- `MenstruationFlowRecord`
- `MenstruationPeriodRecord`
- `OvulationTestRecord`
- `SexualActivityRecord`

### Wellness

- `MindfulnessSessionRecord`

### Planned exercise

- `PlannedExerciseSessionRecord`

This list is a target list, not a hard-coded guarantee.

Before implementation of each type, the project SHALL verify that the record type exists and is usable with the Health Connect SDK version selected by the project.

Unsupported or unavailable record types SHALL be skipped safely and reported.

---

# 7. Raw Record Preservation

## Requirement 4 — Lossless Record Representation

Every successfully read record SHALL preserve as much source information as the Health Connect API exposes.

Common metadata SHOULD be stored in dedicated relational columns.

Record-type-specific contents SHOULD be stored in a flexible representation such as PostgreSQL `jsonb`.

The pipeline SHALL preserve, where available:

- Health Connect record ID;
- record type;
- start time;
- end time;
- instantaneous time;
- zone offsets;
- data origin;
- last modified time;
- client record ID;
- client record version;
- device type;
- device manufacturer;
- device model;
- device display name if exposed;
- device identifiers if exposed and appropriate to retain;
- record-specific measurements;
- enum values;
- notes or descriptive fields;
- nested structures;
- samples;
- stages;
- sessions;
- laps;
- segments;
- routes where accessible and authorized.

The application SHALL NOT intentionally convert source measurements into a different canonical unit in a way that discards the source representation.

If unit normalization is later added, both the original/source representation and normalized representation SHOULD remain recoverable.

---

# 8. Historical Import

## Requirement 5 — Initial Historical Synchronization

When a record type has never been synchronized, the application SHALL perform an initial historical import.

When historical access beyond the default Health Connect read window has been granted, the application SHOULD attempt to read the maximum available historical range supported by Health Connect.

Without historical permission, the application SHALL respect the standard Health Connect historical-access restriction.

The implementation SHALL NOT assume that Health Connect can return an unlimited dataset in one request.

Historical reads SHALL use pagination where applicable.

Each record type SHALL be processed independently.

The synchronization design SHALL minimize the possibility of missing changes that occur while a large historical backfill is running.

The implementation SHOULD establish an appropriate change-tracking position around the initial import so that records added or modified during the backfill can subsequently be reconciled.

---

# 9. Pagination and Large Datasets

## Requirement 6 — Pagination

The application SHALL correctly process paginated Health Connect responses.

When a page token indicates additional data:

- the application SHALL continue requesting pages until the dataset is exhausted or synchronization is safely suspended.

The application SHOULD avoid retaining an entire historical dataset in memory.

Data SHOULD be processed incrementally.

Page size SHALL be treated as an implementation parameter rather than a fixed product requirement.

The implementation MAY tune page sizes based on:

- SDK behavior;
- memory usage;
- performance;
- backend batch size;
- device capabilities.

No record SHALL be silently omitted merely because it exists beyond the first page.

---

# 10. Incremental Synchronization

## Requirement 7 — Change Tracking

After the initial import, the application SHOULD use the official Health Connect changes mechanism where appropriate.

The application SHALL handle:

- inserted records;
- updated records;
- deleted records;
- paginated change responses;
- expired or invalid change tokens;
- partial failures.

Change tokens SHALL only be advanced after the corresponding changes have been safely persisted.

If a synchronization attempt fails before persistence is complete, the previous safe synchronization position SHALL remain recoverable.

The implementation MAY use:

- one token covering multiple compatible record types; or
- separate tokens per logical group or type;

provided that the chosen strategy does not cause one failing record type to unnecessarily block unrelated record types.

If a change token expires or becomes unusable:

- the application SHALL recover through an appropriate reconciliation or backfill process;
- the application SHALL NOT simply assume that no changes occurred.

---

# 11. Updates, Deletions and Idempotency

## Requirement 8 — Idempotent Storage

Synchronization SHALL be idempotent.

Running the same synchronization more than once SHALL NOT create duplicate logical records.

A Health Connect record SHALL be uniquely identifiable in the context of the user and record type.

The database SHOULD enforce uniqueness using a compound identity such as:

`(user_id, record_type, hc_record_id)`

rather than assuming that a Health Connect record ID is globally unique across every user and every possible record domain.

When an existing Health Connect record is received again:

- the stored representation SHOULD be updated when the incoming version represents a newer state;
- the pipeline SHALL NOT insert a second logical copy.

When a deletion is reported:

- the system SHOULD retain enough information to represent that the original Health Connect record was deleted.

Soft deletion MAY be used.

The implementation SHALL avoid losing a valid existing row because a stale version of that record arrives later.

---

# 12. Database

## Requirement 9 — PostgreSQL/Supabase Storage

Milestone 1 SHALL use Supabase/PostgreSQL as the remote persistence layer.

The primary storage model SHOULD include at least:

## `health_records`

Suggested columns:

- `id uuid primary key`
- `user_id uuid not null`
- `hc_record_id text not null`
- `record_type text not null`
- `start_time timestamptz null`
- `end_time timestamptz null`
- `time timestamptz null`
- `start_zone_offset text null`
- `end_zone_offset text null`
- `zone_offset text null`
- `data_origin text null`
- `device_type integer null`
- `device_manufacturer text null`
- `device_model text null`
- `device_display_name text null`
- `last_modified_time timestamptz null`
- `client_record_id text null`
- `client_record_version bigint null`
- `payload jsonb not null`
- `deleted_at timestamptz null`
- `created_at timestamptz not null default now()`
- `updated_at timestamptz not null default now()`

A unique constraint SHOULD exist on:

`(user_id, record_type, hc_record_id)`

The system SHOULD include an index suitable for common queries such as:

`(user_id, record_type, start_time)`

A GIN index on `payload` MAY be added if justified by actual query needs.

It is not required solely for Milestone 1 ingestion.

---

## `sync_state`

Suggested columns:

- `id uuid primary key`
- `user_id uuid not null`
- `scope text not null`
- `changes_token text null`
- `last_successful_sync_at timestamptz null`
- `backfill_completed_at timestamptz null`
- `created_at timestamptz not null default now()`
- `updated_at timestamptz not null default now()`

The exact synchronization scope MAY represent:

- a record type;
- a group of record types;
- another stable synchronization unit.

The implementation SHALL document whichever approach is selected.

Database migrations SHALL be version-controlled in the Git repository.

---

# 13. Supabase Security

## Requirement 10 — Authentication and Authorization

The Android application SHALL NOT contain a Supabase service-role key.

The application MAY contain Supabase credentials designed to be public in client applications, such as a project URL and publishable/anon key, provided that database authorization is enforced through authentication and Row Level Security.

Supabase Auth SHALL provide a user-scoped authenticated identity before private health records are stored.

Row Level Security SHALL be enabled on health-data tables.

Policies SHALL ensure that an authenticated user can access only rows belonging to that user.

For `health_records`, the policies required by the implementation SHALL support at minimum:

- `SELECT`;
- `INSERT`;
- `UPDATE`;

and any other operation required by the final deletion strategy.

For `sync_state`, policies SHALL support the operations needed to create, read, and update synchronization state.

The backend SHALL NOT rely on secrecy of the client publishable/anon key for data protection.

All network communication with Supabase SHALL use HTTPS.

---

# 14. Upload Strategy

## Requirement 11 — Reliable Upload

Health Connect records SHALL be uploaded to Supabase in reasonably sized batches.

Batch size SHALL be an implementation parameter rather than a fixed product requirement.

The uploader SHALL:

- support retries for transient failures;
- distinguish retryable and non-retryable failures where practical;
- avoid losing records when a temporary network error occurs;
- avoid advancing synchronization state until the related records are safely persisted.

Retry timing SHALL use an appropriate backoff policy.

Exact retry counts and delays SHALL be implementation details chosen and documented by the implementation.

Failed uploads SHALL be surfaced in synchronization status.

---

# 15. Error Isolation

## Requirement 12 — Partial Failure Handling

The application SHALL process data in a way that prevents an error affecting one record type from unnecessarily terminating synchronization for every other type.

Expected error categories include:

- missing permission;
- permission revoked during synchronization;
- unsupported feature;
- unavailable record type;
- Health Connect service failure;
- rate limiting;
- serialization failure;
- network loss;
- authentication failure;
- Supabase failure;
- invalid or expired change token.

Failures SHALL be recorded with enough non-sensitive diagnostic context to support debugging.

The pipeline SHALL make it clear whether a synchronization was:

- fully successful;
- partially successful;
- unsuccessful.

---

# 16. Logging and Observability

## Requirement 13 — Privacy-Safe Logging

The application SHALL produce useful development and synchronization logs.

Logs MAY include:

- synchronization start/end;
- record type;
- page number;
- number of records read;
- number of records uploaded;
- skipped record types;
- missing permissions;
- retry attempts;
- error categories;
- synchronization state transitions.

Logs SHOULD NOT contain raw health measurements by default.

Logs SHALL NOT unnecessarily expose:

- medical measurements;
- health values;
- authentication tokens;
- credentials;
- full JSON health payloads;
- personally identifying information.

Debugging facilities MAY provide additional details during local development provided sensitive information is handled intentionally.

---

# 17. Minimal User Interface

## Requirement 14 — Milestone 1 UI

Milestone 1 SHALL contain only the UI needed to operate and diagnose the ingestion pipeline.

The UI SHOULD display:

- Health Connect availability;
- permission status;
- backend/authentication status;
- synchronization state;
- last successful synchronization time;
- record counts;
- failed/skipped record types;
- errors;
- a permission request action;
- a `Sync Now` action.

The UI SHALL NOT require:

- charts;
- dashboards;
- health insights;
- recommendations;
- AI;
- visualization of individual health measurements.

Visual polish is not a Milestone 1 priority.

---

# 18. Background Synchronization

## Requirement 15 — Background Sync

Background synchronization is desirable but is not required for the first working vertical slice.

The project SHALL first demonstrate successful manual synchronization.

After manual synchronization is reliable, background synchronization MAY be implemented using appropriate Android mechanisms such as WorkManager together with the required Health Connect background-read permission.

Background synchronization SHALL respect:

- Android background execution limits;
- Health Connect permissions;
- battery constraints;
- network availability;
- synchronization idempotency.

---

# 19. Exercise Routes and Special Access

## Requirement 16 — Restricted or Special Data

Some Health Connect data may require additional consent, permissions, or feature-specific handling.

The application SHALL NOT assume that obtaining ordinary exercise permission automatically grants access to every associated data structure.

Exercise route data and other specially protected data SHALL be handled according to the current Health Connect APIs and permission model.

If specific data cannot be accessed automatically:

- the application SHALL preserve the rest of the record;
- the inaccessible portion SHALL be reported as unavailable;
- the application SHALL NOT treat the entire synchronization as failed solely for that reason.

---

# 20. Feature Availability and SDK Evolution

## Requirement 17 — SDK Compatibility

Health Connect evolves over time.

The application SHALL NOT hard-code assumptions that every record type exists in every Health Connect version.

Where a Health Connect API exposes feature-status checks, the application SHALL use them before relying on feature-gated record types or functionality.

The project SHOULD keep the supported record-type registry centralized so that new Health Connect record types can be added without redesigning the ingestion pipeline.

The README SHOULD document:

- Health Connect SDK version;
- minimum Android SDK;
- tested Android versions;
- tested Health Connect provider versions where useful;
- currently implemented record types;
- unavailable or feature-gated record types.

---

# 21. Testing

## Requirement 18 — Testing Strategy

The implementation SHALL be testable in layers.

Health Connect access, record serialization, synchronization orchestration, and Supabase persistence SHOULD be sufficiently separated to allow independent testing.

Tests SHOULD include:

### Unit tests

- record mapping;
- serialization;
- deduplication identifiers;
- synchronization-state handling;
- retry logic where practical.

### Integration tests

- Supabase insert/upsert behavior;
- RLS behavior;
- database constraints;
- synchronization-state persistence.

### Physical-device testing

The primary end-to-end validation SHALL use a physical Android device containing real Health Connect data.

The first vertical slice SHALL verify at least:

1. Health Connect availability.
2. Permission request.
3. Reading real `StepsRecord` data.
4. Displaying the number of records read.

The next slice SHALL expand to representative complex types such as:

- `HeartRateRecord`;
- `SleepSessionRecord`;
- `ExerciseSessionRecord`.

The backend integration SHALL be introduced only after local Health Connect reading is proven to work reliably.

---

# 22. Source Code and Repository

## Requirement 19 — GitHub as Source of Truth

The GitHub repository SHALL be the authoritative source for the project.

All important project assets SHALL be version-controlled, including:

- Kotlin source;
- Gradle configuration;
- Android manifest;
- SQL migrations;
- documentation;
- tests;
- synchronization code.

Secrets SHALL NOT be committed.

Environment-specific or secret configuration SHALL be excluded using `.gitignore` and appropriate local configuration mechanisms.

The project SHOULD remain buildable independently of any specific AI coding tool.

Kiro, Android Studio, VS Code, Cursor, or other development tools SHALL be treated as interchangeable development environments rather than as proprietary dependencies of the project.

---

# 23. Implementation Sequence

To reduce risk and avoid unnecessary implementation complexity, development SHOULD proceed in vertical slices.

## Stage A — Android + Health Connect proof

Implement:

- Android/Kotlin project;
- Health Connect availability;
- permissions;
- `StepsRecord`;
- pagination;
- basic raw representation;
- minimal UI;
- successful Gradle build;
- test on physical phone.

## Stage B — Broader Health Connect ingestion

Expand support to:

- heart rate;
- resting heart rate;
- sleep;
- exercises;
- weight;
- other supported record types.

Add robust type-specific serialization.

## Stage C — Supabase persistence

Implement:

- authentication;
- database migrations;
- RLS;
- `health_records`;
- `sync_state`;
- batch upserts;
- idempotency.

## Stage D — Incremental synchronization

Implement:

- change tokens;
- updates;
- deletions;
- token recovery;
- reconciliation.

## Stage E — Background synchronization

Implement:

- WorkManager;
- background Health Connect permission;
- periodic synchronization.

No later stage SHALL block completion or testing of an earlier stage.

---

# 24. Known Platform Constraints

The project SHALL document important Health Connect limitations and behaviors.

These include, at minimum:

- Health Connect availability depends on Android/device/provider compatibility.
- Historical access is restricted unless the user grants the appropriate history permission.
- Background health-data reads require additional permission/support.
- Not every user will have data for every Health Connect record type.
- Some record types or features are gated by SDK/provider feature availability.
- Health Connect access depends on explicit user permissions.
- Permissions can be revoked at any time.
- Large datasets require pagination and incremental processing.
- Change tracking must safely handle token invalidation/expiration.
- Some sensitive or specialized data may require additional consent or special handling.

The README SHALL be updated as implementation reveals additional confirmed limitations.

---

# 25. Explicit Non-Goals for Milestone 1

The following are explicitly outside Milestone 1:

- health dashboards;
- AI health analysis;
- coaching;
- medical advice;
- recommendations;
- user-facing trend analysis;
- wearables connected directly to the backend;
- Apple Health;
- Garmin APIs;
- Fitbit APIs;
- Strava APIs;
- Google Fit APIs;
- web dashboards;
- social features;
- sharing features;
- production-grade UI design.

These may be considered only after the Health Connect → Supabase ingestion pipeline is proven reliable.

---

# 26. Milestone 1 Success Test

The final practical test for Milestone 1 is:

1. Install Open Health Pipeline on a physical Android phone.
2. Confirm Health Connect is available.
3. Grant the requested Health Connect read permissions.
4. Press `Sync Now`.
5. Confirm real records were read from Health Connect.
6. Confirm the records appear in Supabase/PostgreSQL.
7. Inspect a stored record and confirm its Health Connect metadata and type-specific payload were preserved.
8. Press `Sync Now` again.
9. Confirm duplicate logical records were not created.
10. Modify/add health data through an existing source app.
11. Sync again.
12. Confirm the backend reflects the new or updated data.

At that point, Milestone 1 is complete.