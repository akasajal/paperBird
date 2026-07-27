package com.ishaan.paperBird.domain.model

import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
    timeZone = TimeZone.getTimeZone("UTC")
}

private fun parseDate(json: JSONObject, key: String): Long {
    val value = json.opt(key)
    return when (value) {
        is String -> isoFormat.parse(value)?.time ?: System.currentTimeMillis()
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
        json.put("createdAt", isoFormat.format(Date(createdAt)))
        json.put("updatedAt", isoFormat.format(Date(updatedAt)))
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