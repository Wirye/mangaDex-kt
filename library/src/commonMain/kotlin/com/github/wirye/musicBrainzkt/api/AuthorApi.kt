package com.github.wirye.musicBrainzkt.api

import com.github.wirye.musicBrainzkt.MangaDexRateLimiter
import com.github.wirye.musicBrainzkt.exception.MangaDexException
import com.github.wirye.musicBrainzkt.model.Author
import com.github.wirye.musicBrainzkt.model.AuthorOrderField
import com.github.wirye.musicBrainzkt.model.CollectionResponse
import com.github.wirye.musicBrainzkt.model.EntityResponse
import com.github.wirye.musicBrainzkt.model.Order
import com.github.wirye.musicBrainzkt.model.PagedResult
import com.github.wirye.musicBrainzkt.model.ReferenceExpansion
import com.github.wirye.musicBrainzkt.model.decode
import com.github.wirye.musicBrainzkt.model.requirePaging
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode

class AuthorApi internal constructor(private val httpClient: HttpClient) {

    /**
     * Поиск авторов и художников по имени.
     * Чтобы найти мангу автора, возьмите id отсюда и передайте в searchManga(authorIds = ...).
     *
     * @param limit 1..100, по умолчанию сервер берёт 10; offset + limit не больше 10000
     */
    suspend fun searchAuthors(
        name: String? = null,
        ids: Set<String>? = null,
        order: List<Order<AuthorOrderField>> = emptyList(),
        include: Set<ReferenceExpansion> = emptySet(),
        limit: Int? = null,
        offset: Int? = null,
    ): Result<PagedResult<Author>> = runCatching {
        requirePaging(limit, offset)
        MangaDexRateLimiter.acquire()

        val result =
            httpClient.get("https://api.mangadex.org/author") {
                name?.let { parameter("name", it) }
                ids?.forEach { parameter("ids[]", it) }
                order.forEach { parameter("order[${it.field.value}]", it.direction.value) }
                include.forEach { parameter("includes[]", it.value) }
                limit?.let { parameter("limit", it) }
                offset?.let { parameter("offset", it) }
            }

        when (result.status) {
            HttpStatusCode.OK -> {
                val body = result.decode<CollectionResponse<Author>>()
                PagedResult(body.total, body.offset, body.limit, body.data)
            }

            HttpStatusCode.NotFound -> throw MangaDexException.NotFoundException()
            HttpStatusCode.BadRequest -> throw MangaDexException.ValidationErrorException(result.bodyAsText())
            HttpStatusCode.Forbidden -> throw MangaDexException.ForbiddenException()
            HttpStatusCode.TooManyRequests -> throw MangaDexException.RateLimitException()
            else -> throw MangaDexException.ServerErrorException(result.status.value, result.headers["X-Request-ID"])
        }
    }

    /**
     * Один автор по id. С ReferenceExpansion.MANGA в relationships появляются его работы
     * (id манги всегда приходят в relationships).
     */
    suspend fun getAuthor(
        id: String,
        include: Set<ReferenceExpansion> = emptySet(),
    ): Result<Author> = runCatching {
        MangaDexRateLimiter.acquire()

        val result =
            httpClient.get("https://api.mangadex.org/author/$id") {
                include.forEach { parameter("includes[]", it.value) }
            }

        when (result.status) {
            HttpStatusCode.OK -> {
                result.decode<EntityResponse<Author>>().data
            }

            HttpStatusCode.NotFound -> throw MangaDexException.NotFoundException()
            HttpStatusCode.BadRequest -> throw MangaDexException.ValidationErrorException(result.bodyAsText())
            HttpStatusCode.Forbidden -> throw MangaDexException.ForbiddenException()
            HttpStatusCode.TooManyRequests -> throw MangaDexException.RateLimitException()
            else -> throw MangaDexException.ServerErrorException(result.status.value, result.headers["X-Request-ID"])
        }
    }
}
