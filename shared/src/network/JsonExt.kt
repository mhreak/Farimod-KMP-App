package ir.farimod.app.network

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

/**
 * JS-like accessors for dynamic JSON so API code can be ported 1:1 from React Native:
 *   RN:  result.Data?.LikeCount          Kotlin: result["Data"]["LikeCount"].int
 *   RN:  data.Message || data.message    Kotlin: data.str("Message") ?: data.str("message")
 */
operator fun JsonElement?.get(key: String): JsonElement? =
    (this as? JsonObject)?.get(key)?.takeIf { it !is JsonNull }

operator fun JsonElement?.get(index: Int): JsonElement? =
    (this as? JsonArray)?.getOrNull(index)?.takeIf { it !is JsonNull }

/** String value of a primitive (numbers/booleans are stringified). null for JsonNull / objects / arrays. */
val JsonElement?.str: String?
    get() = (this as? JsonPrimitive)?.takeIf { it !is JsonNull }?.contentOrNull

fun JsonElement?.str(key: String): String? = this[key].str

val JsonElement?.int: Int?
    get() = (this as? JsonPrimitive)?.let { it.longOrNull?.toInt() ?: it.doubleOrNull?.toInt() ?: it.contentOrNull?.trim()?.toIntOrNull() }

fun JsonElement?.int(key: String): Int? = this[key].int

val JsonElement?.long: Long?
    get() = (this as? JsonPrimitive)?.let { it.longOrNull ?: it.doubleOrNull?.toLong() ?: it.contentOrNull?.trim()?.toLongOrNull() }

fun JsonElement?.long(key: String): Long? = this[key].long

val JsonElement?.double: Double?
    get() = (this as? JsonPrimitive)?.let { it.doubleOrNull ?: it.contentOrNull?.trim()?.toDoubleOrNull() }

fun JsonElement?.double(key: String): Double? = this[key].double

/** JS truthiness for booleans coming from the API (true / "true" / 1). */
val JsonElement?.bool: Boolean
    get() {
        val p = this as? JsonPrimitive ?: return false
        if (p is JsonNull) return false
        p.booleanOrNull?.let { return it }
        val n = p.longOrNull ?: p.doubleOrNull?.toLong()
        if (n != null) return n != 0L
        val c = p.contentOrNull ?: return false
        return c.isNotEmpty() && !c.equals("false", true)
    }

fun JsonElement?.bool(key: String): Boolean = this[key].bool

/** Elements of a JSON array (empty list for anything else). */
val JsonElement?.list: List<JsonElement>
    get() = (this as? JsonArray)?.toList() ?: emptyList()

fun JsonElement?.list(key: String): List<JsonElement> = this[key].list

val JsonElement?.isNullish: Boolean get() = this == null || this is JsonNull

/** Build a JSON object quickly: jsonOf("Mobile" to "0912", "Count" to 3, "Flag" to true, "X" to null). */
fun jsonOf(vararg pairs: Pair<String, Any?>): JsonObject = buildJsonObject {
    for ((k, v) in pairs) {
        when (v) {
            null -> put(k, JsonNull)
            is JsonElement -> put(k, v)
            is String -> put(k, v)
            is Boolean -> put(k, v)
            is Int -> put(k, v)
            is Long -> put(k, v)
            is Double -> put(k, v)
            is Float -> put(k, v.toDouble())
            is List<*> -> put(k, buildJsonArray {
                for (item in v) {
                    when (item) {
                        null -> add(JsonNull)
                        is JsonElement -> add(item)
                        is String -> add(JsonPrimitive(item))
                        is Boolean -> add(JsonPrimitive(item))
                        is Int -> add(JsonPrimitive(item))
                        is Long -> add(JsonPrimitive(item))
                        is Double -> add(JsonPrimitive(item))
                        else -> add(JsonPrimitive(item.toString()))
                    }
                }
            })
            else -> put(k, v.toString())
        }
    }
}

/** Copy of this object with changed/added keys (React Native: `{ ...obj, key: value }`). */
fun JsonObject.with(vararg pairs: Pair<String, Any?>): JsonObject {
    val extra = jsonOf(*pairs)
    return JsonObject(this + extra)
}

fun JsonObject.without(vararg keys: String): JsonObject = JsonObject(this.filterKeys { it !in keys })

val rnJson: Json = Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true; coerceInputValues = true }
