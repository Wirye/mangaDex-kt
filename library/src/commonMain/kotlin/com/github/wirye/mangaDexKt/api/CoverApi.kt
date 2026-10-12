package com.github.wirye.mangaDexKt.api

import com.github.wirye.mangaDexKt.MangaDexRateLimiter
import com.github.wirye.mangaDexKt.exception.MangaDexException
import com.github.wirye.mangaDexKt.model.CollectionResponse
import com.github.wirye.mangaDexKt.model.Cover
import com.github.wirye.mangaDexKt.model.CoverOrderField
import com.github.wirye.mangaDexKt.model.EntityResponse
import com.github.wirye.mangaDexKt.model.Order
import com.github.wirye.mangaDexKt.model.PagedResult
import com.github.wirye.mangaDexKt.model.ReferenceExpansion
import com.github.wirye.mangaDexKt.model.decode
import com.github.wirye.mangaDexKt.model.requirePaging
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode

class CoverApi internal constructor(private val httpClient: HttpClient) {

    /**
     * Обложки. Главную обложку манги можно получить и без этого запроса: она приходит в relationships манги
     * (ReferenceExpansion.COVER_ART). Этот запрос нужен, чтобы получить ВСЕ обложки манги (по томам).
     *
     * @param mangaIds id манги, чьи обложки нужны
     * @param limit 1..100, по умолчанию сервер берёт 10; offset + limit не больше 10000
     */
    suspend fun searchCovers(
        mangaIds: Set<String>? = null,
        coverIds: Set<String>? = null,
        uploaderIds: Set<String>? = null,
        locales: Set<String>? = null,
        order: List<Order<CoverOrderField>> = emptyList(),
        include: Set<ReferenceExpansion> = emptySet(),
        limit: Int? = null,
        offset: Int? = null,
    ): Result<PagedResult<Cover>> = runCatching {
        requirePaging(limit, offset)
        MangaDexRateLimiter.acquire()

        val result =
            httpClient.get("https://api.mangadex.org/cover") {
                mangaIds?.forEach { parameter("manga[]", it) }
                coverIds?.forEach { parameter("ids[]", it) }
                uploaderIds?.forEach { parameter("uploaders[]", it) }
                locales?.forEach { parameter("locales[]", it) }
                order.forEach { parameter("order[${it.field.value}]", it.direction.value) }
                include.forEach { parameter("includes[]", it.value) }
                limit?.let { parameter("limit", it) }
                offset?.let { parameter("offset", it) }
            }

        when (result.status) {
            HttpStatusCode.OK -> {
                val body = result.decode<CollectionResponse<Cover>>()
                PagedResult(body.total, body.offset, body.limit, body.data)
            }

            HttpStatusCode.NotFound -> throw MangaDexException.NotFoundException()
            HttpStatusCode.BadRequest -> throw MangaDexException.ValidationErrorException(result.bodyAsText())
            HttpStatusCode.Forbidden -> throw MangaDexException.ForbiddenException()
            HttpStatusCode.TooManyRequests -> throw MangaDexException.RateLimitException()
            else -> throw MangaDexException.ServerErrorException(result.status.value, result.headers["X-Request-ID"])
        }
    }

    suspend fun getCover(
        id: String,
        include: Set<ReferenceExpansion> = emptySet(),
    ): Result<Cover> = runCatching {
        MangaDexRateLimiter.acquire()

        val result =
            httpClient.get("https://api.mangadex.org/cover/$id") {
                include.forEach { parameter("includes[]", it.value) }
            }

        when (result.status) {
            HttpStatusCode.OK -> {
                result.decode<EntityResponse<Cover>>().data
            }

            HttpStatusCode.NotFound -> throw MangaDexException.NotFoundException()
            HttpStatusCode.BadRequest -> throw MangaDexException.ValidationErrorException(result.bodyAsText())
            HttpStatusCode.Forbidden -> throw MangaDexException.ForbiddenException()
            HttpStatusCode.TooManyRequests -> throw MangaDexException.RateLimitException()
            else -> throw MangaDexException.ServerErrorException(result.status.value, result.headers["X-Request-ID"])
        }
    }
}
