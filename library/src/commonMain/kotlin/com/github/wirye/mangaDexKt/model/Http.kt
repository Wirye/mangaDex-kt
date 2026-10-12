// Поправьте package под свой проект (во всех файлах).
package com.github.wirye.mangaDexKt.model

import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.serializer

/**
 * Разбор JSON живёт в самой библиотеке, поэтому пользователю не нужен ContentNegotiation.
 * coerceInputValues: null вместо значения у поля с дефолтом даёт дефолт, а не ошибку.
 */
internal val mdJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
}

internal suspend inline fun <reified T> HttpResponse.decode(): T =
    mdJson.decodeFromString(serializer<T>(), bodyAsText())

internal inline fun <reified T> JsonObject.decodeOrNull(): T? =
    runCatching { mdJson.decodeFromJsonElement(serializer<T>(), this) }.getOrNull()

/**
 * offset + limit не может быть больше 10 000 (правило MangaDex), limit обычно 1..100,
 * у feed-эндпоинтов до 500. Если limit не указан, сервер берёт 10.
 */
internal fun requirePaging(limit: Int?, offset: Int?, maxLimit: Int = 100) {
    require(limit == null || limit in 1..maxLimit) { "limit должен быть от 1 до $maxLimit" }
    require(offset == null || offset >= 0) { "offset не может быть отрицательным" }
    require((offset ?: 0) + (limit ?: 10) <= 10_000) { "offset + limit не может превышать 10000" }
}
