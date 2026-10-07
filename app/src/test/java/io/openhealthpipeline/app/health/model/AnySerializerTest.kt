package io.openhealthpipeline.app.health.model

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [AnySerializer].
 *
 * Verifies that the full round-trip for each supported payload type works
 * correctly without a real Health Connect device.
 */
class AnySerializerTest {

    // ── toJsonElement ─────────────────────────────────────────────────────────

    @Test
    fun `null serializes to JsonNull`() {
        assertEquals(JsonNull, AnySerializer.toJsonElement(null))
    }

    @Test
    fun `Boolean true serializes to JsonPrimitive true`() {
        assertEquals(JsonPrimitive(true), AnySerializer.toJsonElement(true))
    }

    @Test
    fun `Long serializes to JsonPrimitive number`() {
        assertEquals(JsonPrimitive(1234L), AnySerializer.toJsonElement(1234L))
    }

    @Test
    fun `Int serializes to JsonPrimitive number`() {
        assertEquals(JsonPrimitive(42), AnySerializer.toJsonElement(42))
    }

    @Test
    fun `Double serializes to JsonPrimitive number`() {
        assertEquals(JsonPrimitive(72.4), AnySerializer.toJsonElement(72.4))
    }

    @Test
    fun `String serializes to JsonPrimitive string`() {
        assertEquals(JsonPrimitive("hello"), AnySerializer.toJsonElement("hello"))
    }

    @Test
    fun `List serializes to JsonArray`() {
        val result = AnySerializer.toJsonElement(listOf(1L, 2L, 3L))
        assertTrue(result is JsonArray)
        assertEquals(3, (result as JsonArray).size)
    }

    @Test
    fun `Map serializes to JsonObject`() {
        val result = AnySerializer.toJsonElement(mapOf("count" to 1234L))
        assertTrue(result is JsonObject)
        assertEquals(JsonPrimitive(1234L), (result as JsonObject)["count"])
    }

    @Test
    fun `nested Map in List serializes correctly`() {
        val input = listOf(mapOf("bpm" to 72L, "time" to "2024-01-01T00:00:00Z"))
        val result = AnySerializer.toJsonElement(input)
        assertTrue(result is JsonArray)
        val firstElement = (result as JsonArray)[0]
        assertTrue(firstElement is JsonObject)
        assertEquals(JsonPrimitive(72L), (firstElement as JsonObject)["bpm"])
    }

    // ── fromJsonElement ───────────────────────────────────────────────────────

    @Test
    fun `JsonNull deserializes to null`() {
        assertNull(AnySerializer.fromJsonElement(JsonNull))
    }

    @Test
    fun `JsonPrimitive string deserializes to String`() {
        val result = AnySerializer.fromJsonElement(JsonPrimitive("hello"))
        assertEquals("hello", result)
        assertTrue(result is String)
    }

    @Test
    fun `JsonPrimitive boolean deserializes to Boolean`() {
        val result = AnySerializer.fromJsonElement(JsonPrimitive(true))
        assertEquals(true, result)
        assertTrue(result is Boolean)
    }

    @Test
    fun `JsonPrimitive integer deserializes to Long`() {
        val result = AnySerializer.fromJsonElement(JsonPrimitive(1234))
        assertTrue(result is Long)
        assertEquals(1234L, result)
    }

    @Test
    fun `JsonPrimitive decimal deserializes to Double`() {
        val result = AnySerializer.fromJsonElement(JsonPrimitive(72.4))
        assertTrue(result is Double)
        assertEquals(72.4, result as Double, 0.001)
    }

    @Test
    fun `JsonArray deserializes to List`() {
        val input = JsonArray(listOf(JsonPrimitive(1), JsonPrimitive(2)))
        val result = AnySerializer.fromJsonElement(input)
        assertTrue(result is List<*>)
        assertEquals(2, (result as List<*>).size)
    }

    @Test
    fun `JsonObject deserializes to Map`() {
        val input = JsonObject(mapOf("count" to JsonPrimitive(1234)))
        val result = AnySerializer.fromJsonElement(input)
        assertTrue(result is Map<*, *>)
        assertEquals(1234L, (result as Map<*, *>)["count"])
    }

    // ── StepsRecord payload shape ─────────────────────────────────────────────

    @Test
    fun `StepsRecord payload round-trips correctly`() {
        val payload: Map<String, Any?> = mapOf("count" to 5000L)
        val jsonElement = AnySerializer.toJsonElement(payload)
        assertTrue(jsonElement is JsonObject)
        val count = (jsonElement as JsonObject)["count"]
        assertEquals(JsonPrimitive(5000L), count)
    }

    // ── RawHealthRecord full serialization ────────────────────────────────────

    @Test
    fun `RawHealthRecord with Steps payload serializes and deserializes`() {
        val record = RawHealthRecord(
            hcRecordId = "test-id-001",
            recordType = "StepsRecord",
            startTime = "2024-01-15T08:00:00Z",
            endTime = "2024-01-15T09:00:00Z",
            startZoneOffset = "+00:00",
            endZoneOffset = "+00:00",
            dataOrigin = "com.example.fitnessapp",
            payload = mapOf("count" to 1234L)
        )

        val json = Json.encodeToString(RawHealthRecord.serializer(), record)
        val decoded = Json.decodeFromString(RawHealthRecord.serializer(), json)

        assertEquals(record.hcRecordId, decoded.hcRecordId)
        assertEquals(record.recordType, decoded.recordType)
        assertEquals(record.startTime, decoded.startTime)
        assertEquals(record.endTime, decoded.endTime)
        assertEquals(record.dataOrigin, decoded.dataOrigin)
        // Payload count round-trips as Long
        assertEquals(1234L, decoded.payload["count"])
    }

    @Test
    fun `RawHealthRecord with null optional fields serializes correctly`() {
        val record = RawHealthRecord(
            hcRecordId = "test-id-002",
            recordType = "StepsRecord",
            payload = mapOf("count" to 0L)
        )

        val json = Json.encodeToString(RawHealthRecord.serializer(), record)
        val decoded = Json.decodeFromString(RawHealthRecord.serializer(), json)

        assertEquals("test-id-002", decoded.hcRecordId)
        assertNull(decoded.startTime)
        assertNull(decoded.endTime)
        assertNull(decoded.dataOrigin)
        assertNull(decoded.deviceType)
    }
}
