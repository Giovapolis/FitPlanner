package com.fitplanner.ai.core.data

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.time.LocalDate

class LocalDateAdapter : JsonAdapter<LocalDate>() {
    override fun fromJson(reader: com.squareup.moshi.JsonReader): LocalDate? {
        if (!reader.hasNext()) return null
        return runCatching { LocalDate.parse(reader.nextString()) }.getOrNull()
    }

    override fun toJson(writer: com.squareup.moshi.JsonWriter, value: LocalDate?) {
        if (value == null) writer.nullValue() else writer.value(value.toString())
    }
}

val AppMoshi: Moshi = Moshi.Builder()
    .add(LocalDate::class.java, LocalDateAdapter())
    .add(KotlinJsonAdapterFactory())
    .build()

inline fun <reified T> Moshi.readJson(json: String): T? =
    runCatching { adapter(T::class.java).fromJson(json) }.getOrNull()

inline fun <reified T> Moshi.writeJson(value: T): String =
    adapter(T::class.java).toJson(value)

@Suppress("unused")
inline fun <reified T> Moshi.listAdapter(): JsonAdapter<List<T>> =
    adapter(Types.newParameterizedType(List::class.java, T::class.java))
