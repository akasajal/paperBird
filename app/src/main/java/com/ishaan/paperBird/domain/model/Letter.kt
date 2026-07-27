package com.ishaan.paperBird.domain.model

import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.format.DateTimeFormatter

private fun Long.toIso(): String =
    DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(this))

private fun String.fromIso(): Long =
    Instant.parse(this).toEpochMilli()

private fun parseDate(json: JSONObject, key: String): Long {
    val value = json.opt(key)
    return when (value) {
        is String -> runCatching { value.fromIso() }.getOrElse { System.currentTimeMillis() }
        is Long -> value // backwards compat with old epoch exports
        is Int -> value.toLong()
        else -> System.currentTimeMillis()
    }
}

data class Letter(
    val id: Long = 0,
    val title: String = "",
    val body: String = "",
    val category: String = "Today",
    val favorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toJsonObject(): JSONObject {
        val json = JSONObject()
        json.put("title", title)
        json.put("body", body)
        json.put("category", category)
        json.put("favorite", favorite)
        json.put("createdAt", createdAt.toIso())
        json.put("updatedAt", updatedAt.toIso())
        return json
    }

    fun toJson(): String = toJsonObject().toString(4)

    companion object {
        fun fromJsonObject(json: JSONObject): Letter {
            return Letter(
                title = json.optString("title", ""),
                body = json.optString("body", ""),
                category = json.optString("category", "Today"),
                favorite = json.optBoolean("favorite", false),
                createdAt = parseDate(json, "createdAt"),
                updatedAt = parseDate(json, "updatedAt")
            )
        }

        fun fromJson(jsonString: String): Letter = fromJsonObject(JSONObject(jsonString))

        fun fromJsonArray(jsonString: String): List<Letter> {
            val array = JSONArray(jsonString)
            val list = mutableListOf<Letter>()
            for (i in 0 until array.length()) {
                list.add(fromJsonObject(array.getJSONObject(i)))
            }
            return list
        }

        fun toJsonArray(letters: List<Letter>): String {
            val array = JSONArray()
            letters.forEach { array.put(it.toJsonObject()) }
            return array.toString(4)
        }
    }
}

data class Attachment(
    val id: Long = 0,
    val letterId: Long,
    val filename: String,
    val mimeType: String,
    val uriPath: String,
    val createdAt: Long = System.currentTimeMillis()
)

val DEFAULT_CATEGORIES = listOf(
    "Love", "Gratitude", "Achievement", "Grief", "Memory", "Dream", "Today"
)

val CATEGORY_COLORS = mapOf(
    "Love" to 0xFFD77FA1,
    "Achievement" to 0xFF7BC47F,
    "Gratitude" to 0xFFF2C14E,
    "Memory" to 0xFF9D8DF1,
    "Dream" to 0xFF61C0BF,
    "Grief" to 0xFF6E7FA8,
    "Today" to 0xFF9A9A9A
)