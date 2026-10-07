package io.openhealthpipeline.app.health.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*

/**
 * Custom kotlinx.serialization serializer for heterogeneous [Any?] values
 * stored in [RawHealthRecord.payload].
 *
 * Handles the full range of types that Health Connect field values can take:
 * - null → JsonNull
 * - Boolean → JsonPrimitive
 * - Number (Long, Int, Double, Float) → JsonPrimitive
 * - String → JsonPrimitive
 * - List<*> → JsonArray (elements serialized recursively)
 * - Map<*, *> → JsonObject (keys coerced to String, values serialized recursively)
 * - Any other type → JsonPrimitive via toString() as a safe fallback
 *
 * Deserialization reconstructs the closest Kotlin type:
 * - JsonNull → null
 * - string primitives → String
 * - boolean primitives → Boolean
 * - number primitives with '.' → Double
 * - number primitives without '.' → Long
 * - JsonArray → List<Any?>
 * - JsonObject → Map<String, Any?>
 *
 * This serializer requires a [JsonEncoder]/[JsonDecoder] and must only be
 * used inside a Json serialization context.
 */
object AnySerializer : KSerializer<Any?> {

    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("kotlin.Any")

    override fun serialize(encoder: Encoder, value: Any?) {
        val jsonEncoder = encoder as? JsonEncoder
            ?: error("AnySerializer requires a JsonEncoder. Found: ${encoder::class.simpleName}")
        jsonEncoder.encodeJsonElement(toJsonElement(value))
    }

    override fun deserialize(decoder: Decoder): Any? {
        val jsonDecoder = decoder as? JsonDecoder
            ?: error("AnySerializer requires a JsonDecoder. Found: ${decoder::class.simpleName}")
        return fromJsonElement(jsonDecoder.decodeJsonElement())
    }

    // -------------------------------------------------------------------------
    // Kotlin → JsonElement
    // -------------------------------------------------------------------------

    fun toJsonElement(value: Any?): JsonElement = when (value) {
        null -> JsonNull
        is Boolean -> JsonPrimitive(value)
        is Long -> JsonPrimitive(value)
        is Int -> JsonPrimitive(value)
        is Double -> JsonPrimitive(value)
        is Float -> JsonPrimitive(value)
        is Number -> JsonPrimitive(value)          // covers Byte, Short, BigDecimal, etc.
        is String -> JsonPrimitive(value)
        is List<*> -> JsonArray(value.map { toJsonElement(it) })
        is Map<*, *> -> JsonObject(
            value.entries.associate { (k, v) -> k.toString() to toJsonElement(v) }
        )
        else -> JsonPrimitive(value.toString())   // safe fallback — preserve as string
    }

    // -------------------------------------------------------------------------
    // JsonElement → Kotlin
    // -------------------------------------------------------------------------

    fun fromJsonElement(element: JsonElement): Any? = when (element) {
        is JsonNull -> null
        is JsonPrimitive -> when {
            element.isString -> element.content
            element.content == "true" -> true
            element.content == "false" -> false
            element.content.contains('.') || element.content.contains('e', ignoreCase = true) ->
                element.doubleOrNull ?: element.content
            else ->
                element.longOrNull ?: element.content
        }
        is JsonArray -> element.map { fromJsonElement(it) }
        is JsonObject -> element.entries.associate { (k, v) -> k to fromJsonElement(v) }
    }
}
