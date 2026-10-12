package com.github.wirye.mangaDexKt.api

import com.github.wirye.mangaDexKt.MangaDexRateLimiter
import com.github.wirye.mangaDexKt.exception.MangaDexException
import com.github.wirye.mangaDexKt.model.Chapter
import com.github.wirye.mangaDexKt.model.ChapterOrderField
import com.github.wirye.mangaDexKt.model.CollectionResponse
import com.github.wirye.mangaDexKt.model.ContentRating
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

class ChapterApi internal constructor(private val httpClient: HttpClient) {

    /**
     * Список глав манги (feed).
     *
     * @param translatedLanguage языки перевода, например setOf("ru", "en"); без фильтра придут главы на всех языках
     * @param include по умолчанию подгружаются группы-переводчики: MangaDex требует указывать их,
     * если приложение даёт читать главы (имена потом в chapter.scanlationGroupNames)
     * @param includeExternalUrl false: только главы, которые читаются в MangaDex (без ссылки на внешний сайт).
     * Параметр API называется «include», но false означает «исключить»
     * @param includeEmptyPages false: только главы со страницами
     * @param limit 1..500, по умолчанию сервер берёт 100; offset + limit не больше 10000
     */
    suspend fun getMangaFeed(
        mangaId: String,
        translatedLanguage: Set<String>? = null,
        originalLanguage: Set<String>? = null,
        contentRating: Set<ContentRating>? = null,
        groupIds: Set<String>? = null,
        order: List<Order<ChapterOrderField>> = listOf(
            Order(ChapterOrderField.VOLUME), Order(ChapterOrderField.CHAPTER),
        ),
        include: Set<ReferenceExpansion> = setOf(ReferenceExpansion.SCANLATION_GROUP),
        includeExternalUrl: Boolean? = false,
        includeEmptyPages: Boolean? = false,
        limit: Int? = null,
        offset: Int? = null,
    ): Result<PagedResult<Chapter>> = runCatching {
        requirePaging(limit, offset, maxLimit = 500)
        MangaDexRateLimiter.acquire()

        val result =
            httpClient.get("https://api.mangadex.org/manga/$mangaId/feed") {
                translatedLanguage?.forEach { parameter("translatedLanguage[]", it) }
                originalLanguage?.forEach { parameter("originalLanguage[]", it) }
                contentRating?.forEach { parameter("contentRating[]", it.value) }
                groupIds?.forEach { parameter("groups[]", it) }
                order.forEach { parameter("order[${it.field.value}]", it.direction.value) }
                include.forEach { parameter("includes[]", it.value) }
                includeExternalUrl?.let { parameter("includeExternalUrl", if (it) 1 else 0) }
                includeEmptyPages?.let { parameter("includeEmptyPages", if (it) 1 else 0) }
                limit?.let { parameter("limit", it) }
                offset?.let { parameter("offset", it) }
            }

        when (result.status) {
            HttpStatusCode.OK -> {
                val body = result.decode<CollectionResponse<Chapter>>()
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
     * Поиск глав по всему каталогу (например, последние обновления).
     * Для глав одной манги лучше подходит [getMangaFeed].
     *
     * @param limit 1..100, по умолчанию сервер берёт 10; offset + limit не больше 10000
     */
    suspend fun searchChapters(
        mangaId: String? = null,
        title: String? = null,
        groupIds: Set<String>? = null,
        volume: Set<String>? = null,
        chapter: Set<String>? = null,
        translatedLanguage: Set<String>? = null,
        originalLanguage: Set<String>? = null,
        contentRating: Set<ContentRating>? = null,
        order: List<Order<ChapterOrderField>> = emptyList(),
        include: Set<ReferenceExpansion> = setOf(ReferenceExpansion.SCANLATION_GROUP),
        includeExternalUrl: Boolean? = false,
        includeEmptyPages: Boolean? = false,
        limit: Int? = null,
        offset: Int? = null,
    ): Result<PagedResult<Chapter>> = runCatching {
        requirePaging(limit, offset)
        MangaDexRateLimiter.acquire()

        val result =
            httpClient.get("https://api.mangadex.org/chapter") {
                mangaId?.let { parameter("manga", it) }
                title?.let { parameter("title", it) }
                groupIds?.forEach { parameter("groups[]", it) }
                volume?.forEach { parameter("volume[]", it) }
                chapter?.forEach { parameter("chapter[]", it) }
                translatedLanguage?.forEach { parameter("translatedLanguage[]", it) }
                originalLanguage?.forEach { parameter("originalLanguage[]", it) }
                contentRating?.forEach { parameter("contentRating[]", it.value) }
                order.forEach { parameter("order[${it.field.value}]", it.direction.value) }
                include.forEach { parameter("includes[]", it.value) }
                includeExternalUrl?.let { parameter("includeExternalUrl", if (it) 1 else 0) }
                includeEmptyPages?.let { parameter("includeEmptyPages", if (it) 1 else 0) }
                limit?.let { parameter("limit", it) }
                offset?.let { parameter("offset", it) }
            }

        when (result.status) {
            HttpStatusCode.OK -> {
                val body = result.decode<CollectionResponse<Chapter>>()
                PagedResult(body.total, body.offset, body.limit, body.data)
            }

            HttpStatusCode.NotFound -> throw MangaDexException.NotFoundException()
            HttpStatusCode.BadRequest -> throw MangaDexException.ValidationErrorException(result.bodyAsText())
            HttpStatusCode.Forbidden -> throw MangaDexException.ForbiddenException()
            HttpStatusCode.TooManyRequests -> throw MangaDexException.RateLimitException()
            else -> throw MangaDexException.ServerErrorException(result.status.value, result.headers["X-Request-ID"])
        }
    }

    /** Одна глава по id. Страницы для чтения запрашиваются отдельно: client.reader.getChapterPages(id). */
    suspend fun getChapter(
        id: String,
        include: Set<ReferenceExpansion> = setOf(ReferenceExpansion.SCANLATION_GROUP),
    ): Result<Chapter> = runCatching {
        MangaDexRateLimiter.acquire()

        val result =
            httpClient.get("https://api.mangadex.org/chapter/$id") {
                include.forEach { parameter("includes[]", it.value) }
            }

        when (result.status) {
            HttpStatusCode.OK -> {
                result.decode<EntityResponse<Chapter>>().data
            }

            HttpStatusCode.NotFound -> throw MangaDexException.NotFoundException()
            HttpStatusCode.BadRequest -> throw MangaDexException.ValidationErrorException(result.bodyAsText())
            HttpStatusCode.Forbidden -> throw MangaDexException.ForbiddenException()
            HttpStatusCode.TooManyRequests -> throw MangaDexException.RateLimitException()
            else -> throw MangaDexException.ServerErrorException(result.status.value, result.headers["X-Request-ID"])
        }
    }
}
