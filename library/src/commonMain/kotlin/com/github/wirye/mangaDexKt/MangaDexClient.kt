package com.github.wirye.mangaDexKt

import com.github.wirye.mangaDexKt.api.AuthorApi
import com.github.wirye.mangaDexKt.api.ChapterApi
import com.github.wirye.mangaDexKt.api.CoverApi
import com.github.wirye.mangaDexKt.api.MangaApi
import com.github.wirye.mangaDexKt.api.ReaderApi
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

internal object MangaDexRateLimiter {
    private val minInterval = 500.milliseconds
    private val mutex = Mutex()
    private var nextSlot = TimeSource.Monotonic.markNow()

    suspend fun acquire() = mutex.withLock {
        val wait = -nextSlot.elapsedNow()
        if (wait.isPositive()) delay(wait)
        nextSlot = TimeSource.Monotonic.markNow() + minInterval
    }
}

/**
 * @param userAgent a string in the format "MyApp/1.0 ( me@example.com )": app name, version, and contact.
 * Required for MangaDex. If you provide your own [customHttpClient], you do not need to set the User-Agent in it:
 * the library will add this one.
 */
class MangaDexClient(
    userAgent: String,
    customHttpClient: HttpClient? = null,
) {
    init {
        require(userAgent.isNotBlank()) { "User-Agent is required for MangaDex API" }
    }

    private val httpClient: HttpClient = run {
        val setup: HttpClientConfig<*>.() -> Unit = {
            install(UserAgent) { agent = userAgent }
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
        }
        customHttpClient?.config(setup) ?: HttpClient(setup)
    }

    val author: AuthorApi = AuthorApi(httpClient)
    val chapter: ChapterApi = ChapterApi(httpClient)
    val cover: CoverApi = CoverApi(httpClient)
    val manga: MangaApi = MangaApi(httpClient)
    val reader: ReaderApi = ReaderApi(httpClient)
}