package com.github.wirye.mangaDexKt

import com.github.wirye.mangaDexKt.model.CoverSize
import com.github.wirye.mangaDexKt.model.ReferenceExpansion
import com.github.wirye.mangaDexKt.model.artistNames
import com.github.wirye.mangaDexKt.model.authorIds
import com.github.wirye.mangaDexKt.model.authorNames
import com.github.wirye.mangaDexKt.model.coverUrl
import com.github.wirye.mangaDexKt.model.displayTitle
import com.github.wirye.mangaDexKt.model.scanlationGroupNames
import com.github.wirye.mangaDexKt.model.url
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertTrue

class RealApiIntegrationTest {
    private val client = MangaDexClient("MangaDexKtLibary/1.1.4 ( https://github.com/Wirye/mangaDex-kt )")

    private val mangaId = "296cbc31-af1a-4b5b-a34b-fee2b4cad542"

    @Test
    fun `search manga test`(): Unit = runBlocking {
        val result = client.manga.searchManga(title = "Звёздное дитя", limit = 5)

        println(">>> Result isSuccess: ${result.isSuccess}")
        println(">>> Result: ${result.getOrNull()?.items?.size},  $result")
        println(">>> Exception type: ${result.exceptionOrNull()?.javaClass?.simpleName}")
        println(">>> Exception message: ${result.exceptionOrNull()?.message}")

        assertTrue(result.isSuccess)
    }

    @Test
    fun `get manga test`(): Unit = runBlocking {
        val result = client.manga.getManga(mangaId)

        println(">>> Result isSuccess: ${result.isSuccess}")
        println(">>> Result: $result")
        println(">>> Title: ${result.getOrNull()?.displayTitle("ru", "en")}")
        println(">>> Authors: ${result.getOrNull()?.authorNames}, artists: ${result.getOrNull()?.artistNames}")
        println(">>> Cover: ${result.getOrNull()?.coverUrl(CoverSize.W256)}")
        println(">>> Exception type: ${result.exceptionOrNull()?.javaClass?.simpleName}")
        println(">>> Exception message: ${result.exceptionOrNull()?.message}")

        assertTrue(result.isSuccess)
    }

    @Test
    fun `get manga feed test`(): Unit = runBlocking {
        val result = client.chapter.getMangaFeed(
            mangaId,
            translatedLanguage = setOf("en"),
            limit = 10,
        )

        println(">>> Result isSuccess: ${result.isSuccess}")
        println(">>> Result: ${result.getOrNull()?.items?.size},  $result")
        println(">>> Groups: ${result.getOrNull()?.items?.map { it.scanlationGroupNames }}")
        println(">>> Exception type: ${result.exceptionOrNull()?.javaClass?.simpleName}")
        println(">>> Exception message: ${result.exceptionOrNull()?.message}")

        assertTrue(result.isSuccess)
    }

    @Test
    fun `get chapter test`(): Unit = runBlocking {
        val chapterId = client.chapter.getMangaFeed(mangaId, limit = 1)
            .getOrThrow().items.first().id
        val result = client.chapter.getChapter(chapterId)

        println(">>> Result isSuccess: ${result.isSuccess}")
        println(">>> Result: $result")
        println(">>> Exception type: ${result.exceptionOrNull()?.javaClass?.simpleName}")
        println(">>> Exception message: ${result.exceptionOrNull()?.message}")

        assertTrue(result.isSuccess)
    }

    @Test
    fun `search chapters test`(): Unit = runBlocking {
        val result = client.chapter.searchChapters(
            mangaId = mangaId,
            translatedLanguage = setOf("en"),
            limit = 5,
        )

        println(">>> Result isSuccess: ${result.isSuccess}")
        println(">>> Result: ${result.getOrNull()?.items?.size},  $result")
        println(">>> Exception type: ${result.exceptionOrNull()?.javaClass?.simpleName}")
        println(">>> Exception message: ${result.exceptionOrNull()?.message}")

        assertTrue(result.isSuccess)
    }

    @Test
    fun `get chapter pages test`(): Unit = runBlocking {
        val chapterId = client.chapter.getMangaFeed(mangaId, translatedLanguage = setOf("en"), limit = 1)
            .getOrThrow().items.first().id
        val result = client.reader.getChapterPages(chapterId)

        println(">>> Result isSuccess: ${result.isSuccess}")
        println(">>> Pages: ${result.getOrNull()?.pageCount}, first: ${result.getOrNull()?.pageUrl(0)}")
        println(">>> Exception type: ${result.exceptionOrNull()?.javaClass?.simpleName}")
        println(">>> Exception message: ${result.exceptionOrNull()?.message}")

        assertTrue(result.isSuccess)
    }

    @Test
    fun `download page test`(): Unit = runBlocking {
        val chapterId = client.chapter.getMangaFeed(mangaId, translatedLanguage = setOf("en"), limit = 1)
            .getOrThrow().items.first().id
        val pages = client.reader.getChapterPages(chapterId).getOrThrow()
        val result = client.reader.downloadPage(pages, index = 0)

        println(">>> Result isSuccess: ${result.isSuccess}")
        println(">>> Bytes: ${result.getOrNull()?.size}")
        println(">>> Exception type: ${result.exceptionOrNull()?.javaClass?.simpleName}")
        println(">>> Exception message: ${result.exceptionOrNull()?.message}")

        assertTrue(result.isSuccess)
    }

    @Test
    fun `search covers test`(): Unit = runBlocking {
        val result = client.cover.searchCovers(mangaIds = setOf(mangaId), limit = 5)

        println(">>> Result isSuccess: ${result.isSuccess}")
        println(">>> Result: ${result.getOrNull()?.items?.size},  $result")
        println(">>> Urls: ${result.getOrNull()?.items?.map { it.url(CoverSize.W256) }}")
        println(">>> Exception type: ${result.exceptionOrNull()?.javaClass?.simpleName}")
        println(">>> Exception message: ${result.exceptionOrNull()?.message}")

        assertTrue(result.isSuccess)
    }

    @Test
    fun `get cover test`(): Unit = runBlocking {
        val coverId = client.cover.searchCovers(mangaIds = setOf(mangaId), limit = 1)
            .getOrThrow().items.first().id
        val result = client.cover.getCover(coverId)

        println(">>> Result isSuccess: ${result.isSuccess}")
        println(">>> Result: $result")
        println(">>> Exception type: ${result.exceptionOrNull()?.javaClass?.simpleName}")
        println(">>> Exception message: ${result.exceptionOrNull()?.message}")

        assertTrue(result.isSuccess)
    }

    @Test
    fun `search authors test`(): Unit = runBlocking {
        val result = client.author.searchAuthors(name = "Oda", limit = 5)

        println(">>> Result isSuccess: ${result.isSuccess}")
        println(">>> Result: ${result.getOrNull()?.items?.size},  $result")
        println(">>> Exception type: ${result.exceptionOrNull()?.javaClass?.simpleName}")
        println(">>> Exception message: ${result.exceptionOrNull()?.message}")

        assertTrue(result.isSuccess)
    }

    @Test
    fun `get author test`(): Unit = runBlocking {
        val authorId = client.manga.getManga(mangaId).getOrThrow().authorIds.first()
        val result = client.author.getAuthor(authorId, include = setOf(ReferenceExpansion.MANGA))

        println(">>> Result isSuccess: ${result.isSuccess}")
        println(">>> Result: $result")
        println(">>> Exception type: ${result.exceptionOrNull()?.javaClass?.simpleName}")
        println(">>> Exception message: ${result.exceptionOrNull()?.message}")

        assertTrue(result.isSuccess)
    }
}
