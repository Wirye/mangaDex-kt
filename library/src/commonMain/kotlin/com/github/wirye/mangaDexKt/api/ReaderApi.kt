package com.github.wirye.mangaDexKt.api

import com.github.wirye.mangaDexKt.MangaDexRateLimiter
import com.github.wirye.mangaDexKt.exception.MangaDexException
import com.github.wirye.mangaDexKt.model.AtHomeResponse
import com.github.wirye.mangaDexKt.model.ChapterPages
import com.github.wirye.mangaDexKt.model.ImageQuality
import com.github.wirye.mangaDexKt.model.decode
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentLength
import io.ktor.http.content.TextContent
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.time.TimeSource

/**
 * Чтение и скачивание глав. Картинки отдаёт сеть MangaDex@Home, а не сам api.mangadex.org.
 *
 * Правила MangaDex, которые здесь соблюдаются автоматически:
 *  - адрес сервера берётся только из /at-home/server и используется как есть;
 *  - не больше 40 запросов в минуту на /at-home/server;
 *  - успехи и ошибки загрузки картинок отправляются на api.mangadex.network/report;
 *  - при ошибке адрес сервера запрашивается заново;
 *  - картинки грузятся по одной, без заголовка авторизации.
 *
 * Правила, которые должно соблюдать приложение: указать MangaDex и группы-переводчики,
 * не показывать рекламу и не продавать доступ, выполнять запросы групп на удаление контента.
 */
class ReaderApi internal constructor(private val httpClient: HttpClient) {

    /**
     * Адреса страниц главы. Действуют минимум 15 минут, затем нужно запросить заново (см. ChapterPages.isLikelyExpired).
     * Для глав, которые читаются на внешнем сайте (externalUrl), страниц в MangaDex нет.
     */
    suspend fun getChapterPages(chapterId: String): Result<ChapterPages> = runCatching {
        MangaDexRateLimiter.acquire()

        val result = httpClient.get("https://api.mangadex.org/at-home/server/$chapterId")

        when (result.status) {
            HttpStatusCode.OK -> {
                val body = result.decode<AtHomeResponse>()
                ChapterPages(
                    chapterId = chapterId,
                    baseUrl = body.baseUrl,
                    hash = body.chapter.hash,
                    data = body.chapter.data,
                    dataSaver = body.chapter.dataSaver,
                )
            }

            HttpStatusCode.NotFound -> throw MangaDexException.NotFoundException()
            HttpStatusCode.BadRequest -> throw MangaDexException.ValidationErrorException(result.bodyAsText())
            HttpStatusCode.Forbidden -> throw MangaDexException.ForbiddenException()
            HttpStatusCode.TooManyRequests -> throw MangaDexException.RateLimitException()
            else -> throw MangaDexException.ServerErrorException(result.status.value, result.headers["X-Request-ID"])
        }
    }

    /**
     * Загружает одну страницу и возвращает байты картинки (сохранить в файл или показать решает приложение).
     * Результат загрузки отправляется в MangaDex@Home, как они просят. Если картинка не загрузилась,
     * вызовите [getChapterPages] заново и повторите (так делает [downloadChapter]).
     */
    suspend fun downloadPage(
        pages: ChapterPages,
        index: Int,
        quality: ImageQuality = ImageQuality.DATA_SAVER,
    ): Result<ByteArray> = runCatching {
        val url = pages.pageUrl(index, quality)

        val started = TimeSource.Monotonic.markNow()
        var success = false
        var bytes = 0L
        var cached = false

        try {
            val response = httpClient.get(url)
            cached = response.headers["X-Cache"]?.startsWith("HIT") == true

            if (response.status != HttpStatusCode.OK) {
                bytes = response.contentLength() ?: 0L
                throw MangaDexException.ImageLoadException(url, response.status.value)
            }

            val data: ByteArray = response.body()
            bytes = data.size.toLong()
            success = true
            data
        } finally {
            if (!pages.baseUrl.contains("mangadex.org")) {
                report(url, success, bytes, started.elapsedNow().inWholeMilliseconds, cached)
            }
        }
    }

    /**
     * Скачивает всю главу по одной странице, последовательно. Каждую страницу отдаёт [onPage]:
     * сохраните её куда нужно. Возвращает число страниц.
     *
     * Если страница не загрузилась (или адрес сервера протух), адрес запрашивается заново,
     * и страница повторяется один раз.
     *
     * Не скачивайте разом десятки глав подряд: на /at-home/server лимит 40 запросов в минуту, а
     * «500 запросов вместо одного» MangaDex считает злоупотреблением.
     */
    suspend fun downloadChapter(
        chapterId: String,
        quality: ImageQuality = ImageQuality.DATA_SAVER,
        onPage: suspend (index: Int, fileName: String, bytes: ByteArray) -> Unit,
    ): Result<Int> = runCatching {
        var pages = getChapterPages(chapterId).getOrThrow()
        val fileNames = pages.fileNames(quality)

        for (index in fileNames.indices) {
            if (pages.isLikelyExpired) pages = getChapterPages(chapterId).getOrThrow()

            val bytes = downloadPage(pages, index, quality).getOrElse {
                pages = getChapterPages(chapterId).getOrThrow()
                downloadPage(pages, index, quality).getOrThrow()
            }
            onPage(index, fileNames[index], bytes)
        }
        fileNames.size
    }

    /** Отчёт о загрузке картинки. Ошибки отчёта игнорируются: он не должен ломать чтение. */
    private suspend fun report(url: String, success: Boolean, bytes: Long, durationMs: Long, cached: Boolean) {
        runCatching {
            val json = buildJsonObject {
                put("url", url)
                put("success", success)
                put("cached", cached)
                put("bytes", bytes)
                put("duration", durationMs)
            }.toString()

            httpClient.post("https://api.mangadex.network/report") {
                setBody(TextContent(json, ContentType.Application.Json))
            }
        }
    }
}
