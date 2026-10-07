package io.openhealthpipeline.app.health.model

import kotlinx.serialization.Serializable

/**
 * Lossless internal representation of a Health Connect record.
 *
 * Common metadata fields are typed relational columns. The record-type-specific
 * payload is stored as a Map keyed by String with values handled by [AnySerializer],
 * which round-trips correctly through kotlinx.serialization JSON.
 *
 * For Stage C, this map serializes directly to a PostgreSQL JSONB column.
 *
 * Examples:
 *   StepsRecord      → payload = {"count": 1234}
 *   HeartRateRecord  → payload = {"samples": [{"time": "...", "bpm": 72}, ...]}
 *   WeightRecord     → payload = {"value": 72.4, "unit": "kilograms"}
 *
 * Sensitive health values are NOT included in log output anywhere in the pipeline.
 */
@Serializable
data class RawHealthRecord(

    /** Health Connect-assigned stable record UUID. Never null for real records. */
    val hcRecordId: String,

    /** Unqualified record class name, e.g. "StepsRecord", "HeartRateRecord". */
    val recordType: String,

    /** ISO-8601 UTC start time for interval records (null for instantaneous). */
    val startTime: String? = null,

    /** ISO-8601 UTC end time for interval records (null for instantaneous). */
    val endTime: String? = null,

    /** ISO-8601 UTC time for instantaneous records (null for interval). */
    val time: String? = null,

    /** Zone offset string for startTime, e.g. "+05:30", "-08:00". */
    val startZoneOffset: String? = null,

    /** Zone offset string for endTime. */
    val endZoneOffset: String? = null,

    /** Zone offset string for instantaneous records. */
    val zoneOffset: String? = null,

    /** Package name of the app that originally wrote this record to Health Connect. */
    val dataOrigin: String? = null,

    /** ISO-8601 UTC timestamp of the last modification to this record's metadata. */
    val lastModifiedTime: String? = null,

    /** Client-assigned record identifier from Metadata, or null if not set. */
    val clientRecordId: String? = null,

    /** Client-assigned record version from Metadata, or null if not set / is 0. */
    val clientRecordVersion: Long? = null,

    /**
     * Device type integer from Metadata.Device.
     * Maps to constants defined in androidx.health.connect.client.records.metadata.Device:
     *   TYPE_UNKNOWN=0, TYPE_WATCH=1, TYPE_PHONE=2, TYPE_SCALE=3, TYPE_RING=4,
     *   TYPE_HEAD_MOUNTED=5, TYPE_FITNESS_BAND=6, TYPE_CHEST_STRAP=7, TYPE_SMART_DISPLAY=8
     */
    val deviceType: Int? = null,

    /** Device manufacturer string from Metadata.Device, or null. */
    val deviceManufacturer: String? = null,

    /** Device model string from Metadata.Device, or null. */
    val deviceModel: String? = null,

    /**
     * Record-type-specific payload.
     *
     * Each entry is a primitive, list, or nested map that represents the
     * Health Connect fields specific to this record type. The [AnySerializer]
     * handles serialization to/from JSON primitives, arrays, and objects.
     *
     * The payload preserves source units and nested structures without
     * destructive normalization.
     */
    val payload: Map<String, @Serializable(with = AnySerializer::class) Any?> = emptyMap()
)
