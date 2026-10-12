package com.github.wirye.mangaDexKt.api

import com.github.wirye.mangaDexKt.MangaDexRateLimiter
import com.github.wirye.mangaDexKt.exception.MangaDexException
import com.github.wirye.mangaDexKt.model.CollectionResponse
import com.github.wirye.mangaDexKt.model.ContentRating
import com.github.wirye.mangaDexKt.model.EntityResponse
import com.github.wirye.mangaDexKt.model.Manga
import com.github.wirye.mangaDexKt.model.MangaOrderField
import com.github.wirye.mangaDexKt.model.MangaStatus
import com.github.wirye.mangaDexKt.model.Order
import com.github.wirye.mangaDexKt.model.PagedResult
import com.github.wirye.mangaDexKt.model.PublicationDemographic
import com.github.wirye.mangaDexKt.model.ReferenceExpansion
import com.github.wirye.mangaDexKt.model.decode
import com.github.wirye.mangaDexKt.model.requirePaging
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode

class MangaApi internal constructor(private val httpClient: HttpClient) {

    /**
     * Поиск манги.
     *
     * @param title часть названия
     * @param authorIds id авторов (их можно получить из searchAuthors)
     * @param includedTagIds id тегов, которые обязательно есть (id тегов в API: GET /manga/tag)
     * @param contentRating если не указан, сервер, насколько я знаю, не отдаёт pornographic; чтобы быть уверенными, задайте явно
     * @param include по умолчанию сразу подгружаются обложка, авторы и художники: одного запроса достаточно,
     * чтобы показать список. MangaDex просит не делать сотни запросов там, где хватит одного
     * @param limit 1..100, по умолчанию сервер берёт 10; offset + limit не больше 10000
     */
    suspend fun searchManga(
        title: String? = null,
        authorIds: Set<String>? = null,
        artistIds: Set<String>? = null,
        year: Int? = null,
        includedTagIds: Set<String>? = null,
        excludedTagIds: Set<String>? = null,
        status: Set<MangaStatus>? = null,
        originalLanguage: Set<String>? = null,
        availableTranslatedLanguage: Set<String>? = null,
        publicationDemographic: Set<PublicationDemographic>? = null,
        contentRating: Set<ContentRating>? = null,
        order: List<Order<MangaOrderField>> = emptyList(),
        include: Set<ReferenceExpansion> = setOf(
            ReferenceExpansion.COVER_ART, ReferenceExpansion.AUTHOR, ReferenceExpansion.ARTIST,
        ),
        limit: Int? = null,
        offset: Int? = null,
    ): Result<PagedResult<Manga>> = runCatching {
        requirePaging(limit, offset)
        MangaDexRateLimiter.acquire()

        val result =
            httpClient.get("https://api.mangadex.org/manga") {
                title?.let { parameter("title", it) }
                authorIds?.forEach { parameter("authors[]", it) }
                artistIds?.forEach { parameter("artists[]", it) }
                year?.let { parameter("year", it) }
                includedTagIds?.forEach { parameter("includedTags[]", it) }
                excludedTagIds?.forEach { parameter("excludedTags[]", it) }
                status?.forEach { parameter("status[]", it.value) }
                originalLanguage?.forEach { parameter("originalLanguage[]", it) }
                availableTranslatedLanguage?.forEach { parameter("availableTranslatedLanguage[]", it) }
                publicationDemographic?.forEach { parameter("publicationDemographic[]", it.value) }
                contentRating?.forEach { parameter("contentRating[]", it.value) }
                order.forEach { parameter("order[${it.field.value}]", it.direction.value) }
                include.forEach { parameter("includes[]", it.value) }
                limit?.let { parameter("limit", it) }
                offset?.let { parameter("offset", it) }
            }

        when (result.status) {
            HttpStatusCode.OK -> {
                val body = result.decode<CollectionResponse<Manga>>()
                PagedResult(body.total, body.offset, body.limit, body.data)
            }

            HttpStatusCode.NotFound -> throw MangaDexException.NotFoundException()
            HttpStatusCode.BadRequest -> throw MangaDexException.ValidationErrorException(result.bodyAsText())
            HttpStatusCode.Forbidden -> throw MangaDexException.ForbiddenException()
            HttpStatusCode.TooManyRequests -> throw MangaDexException.RateLimitException()
            else -> throw MangaDexException.ServerErrorException(result.status.value, result.headers["X-Request-ID"])
        }
    }

    /** Одна манга по id. По умолчанию подгружаются обложка, авторы и художники. */
    suspend fun getManga(
        id: String,
        include: Set<ReferenceExpansion> = setOf(
            ReferenceExpansion.COVER_ART, ReferenceExpansion.AUTHOR, ReferenceExpansion.ARTIST,
        ),
    ): Result<Manga> = runCatching {
        MangaDexRateLimiter.acquire()

        val result =
            httpClient.get("https://api.mangadex.org/manga/$id") {
                include.forEach { parameter("includes[]", it.value) }
            }

        when (result.status) {
            HttpStatusCode.OK -> {
                result.decode<EntityResponse<Manga>>().data
            }

            HttpStatusCode.NotFound -> throw MangaDexException.NotFoundException()
            HttpStatusCode.BadRequest -> throw MangaDexException.ValidationErrorException(result.bodyAsText())
            HttpStatusCode.Forbidden -> throw MangaDexException.ForbiddenException()
            HttpStatusCode.TooManyRequests -> throw MangaDexException.RateLimitException()
            else -> throw MangaDexException.ServerErrorException(result.status.value, result.headers["X-Request-ID"])
        }
    }
}
