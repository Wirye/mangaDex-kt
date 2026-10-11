package com.github.wirye.musicBrainzkt.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/** Страница результатов, которую получает пользователь библиотеки. */
data class PagedResult<T>(
    /** Сколько всего элементов подходит под запрос (не длина [items]) */
    val total: Int,
    val offset: Int,
    val limit: Int,
    val items: List<T>,
) {
    val hasMore: Boolean get() = offset + items.size < total
}

/**
 * Связь с другой сущностью. [attributes] есть только если запрошен reference expansion
 * (см. [ReferenceExpansion]); читать их удобно через Relationship.authorAttributes() и подобные.
 */
@Serializable
data class Relationship(
    val id: String,
    /** "author", "artist", "cover_art", "scanlation_group", "manga", "user"... */
    val type: String,
    val related: String? = null,
    val attributes: JsonObject? = null,
)

// ---------- Manga ----------

@Serializable
data class Manga(
    val id: String,
    val attributes: MangaAttributes = MangaAttributes(),
    val relationships: List<Relationship> = emptyList(),
)

@Serializable
data class MangaAttributes(
    val title: LocalizedString = LocalizedString(),
    val altTitles: List<LocalizedString> = emptyList(),
    val description: LocalizedString = LocalizedString(),
    val isLocked: Boolean? = null,
    /** Ключи странные ("al" это AniList, "mal" это MyAnimeList и т. д.), расшифровка в документации "Static Data" */
    val links: Map<String, String>? = null,
    val originalLanguage: String? = null,
    val lastVolume: String? = null,
    val lastChapter: String? = null,
    /** shounen, shoujo, josei, seinen */
    val publicationDemographic: String? = null,
    /** ongoing, completed, hiatus, cancelled */
    val status: String? = null,
    val year: Int? = null,
    /** safe, suggestive, erotica, pornographic */
    val contentRating: String? = null,
    val tags: List<Tag> = emptyList(),
    val state: String? = null,
    val chapterNumbersResetOnNewVolume: Boolean? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val version: Int? = null,
    val availableTranslatedLanguages: List<String> = emptyList(),
    /** Id последней загруженной главы */
    val latestUploadedChapter: String? = null,
)

@Serializable
data class Tag(
    val id: String,
    val attributes: TagAttributes = TagAttributes(),
)

@Serializable
data class TagAttributes(
    val name: LocalizedString = LocalizedString(),
    val description: LocalizedString = LocalizedString(),
    /** "genre", "theme", "format", "content" */
    val group: String? = null,
    val version: Int? = null,
)

// ---------- Chapter ----------

@Serializable
data class Chapter(
    val id: String,
    val attributes: ChapterAttributes = ChapterAttributes(),
    val relationships: List<Relationship> = emptyList(),
)

@Serializable
data class ChapterAttributes(
    val title: String? = null,
    /** Строка, не число: бывает null или "1" */
    val volume: String? = null,
    /** Строка, не число: бывает "10.5" или null (например, у ваншота) */
    val chapter: String? = null,
    val pages: Int? = null,
    val translatedLanguage: String? = null,
    /** Если не null, глава читается на внешнем сайте, и страниц в MangaDex у неё нет */
    val externalUrl: String? = null,
    val isUnavailable: Boolean? = null,
    val publishAt: String? = null,
    val readableAt: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val version: Int? = null,
)

/** Группа-переводчик: приходит в relationships главы при ReferenceExpansion.SCANLATION_GROUP. */
@Serializable
data class ScanlationGroupAttributes(
    val name: String? = null,
    val website: String? = null,
    val discord: String? = null,
    val twitter: String? = null,
    val description: String? = null,
    val focusedLanguages: List<String>? = null,
    val official: Boolean? = null,
    val verified: Boolean? = null,
    val inactive: Boolean? = null,
)

// ---------- Cover ----------

@Serializable
data class Cover(
    val id: String,
    val attributes: CoverAttributes = CoverAttributes(),
    val relationships: List<Relationship> = emptyList(),
)

@Serializable
data class CoverAttributes(
    val volume: String? = null,
    /** Имя файла для ссылки на картинку, см. coverUrl() */
    val fileName: String? = null,
    val description: String? = null,
    val locale: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val version: Int? = null,
)

// ---------- Author ----------

/** Автор или художник: это одна и та же сущность, роль определяется связью с мангой (author / artist). */
@Serializable
data class Author(
    val id: String,
    val attributes: AuthorAttributes = AuthorAttributes(),
    val relationships: List<Relationship> = emptyList(),
)

@Serializable
data class AuthorAttributes(
    val name: String? = null,
    val imageUrl: String? = null,
    val biography: LocalizedString = LocalizedString(),
    val twitter: String? = null,
    val pixiv: String? = null,
    val melonBook: String? = null,
    val fanBox: String? = null,
    val booth: String? = null,
    val nicoVideo: String? = null,
    val skeb: String? = null,
    val fantia: String? = null,
    val tumblr: String? = null,
    val youtube: String? = null,
    val weibo: String? = null,
    val naver: String? = null,
    val namicomi: String? = null,
    val website: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val version: Int? = null,
)
