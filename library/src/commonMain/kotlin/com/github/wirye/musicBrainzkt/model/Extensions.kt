package com.github.wirye.musicBrainzkt.model

import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeSource

// ---------- Картинки ----------

/**
 * Ссылка на обложку. Нужны id манги и имя файла обложки (cover.attributes.fileName).
 * Картинки отдаются без авторизации; о User-Agent см. README.
 */
fun coverUrl(mangaId: String, fileName: String, size: CoverSize = CoverSize.ORIGINAL): String =
    "https://uploads.mangadex.org/covers/$mangaId/$fileName${size.suffix}"

// ---------- Данные из relationships (работают, если запрошен ReferenceExpansion) ----------

fun Relationship.authorAttributes(): AuthorAttributes? =
    if (type == "author" || type == "artist") attributes?.decodeOrNull() else null

fun Relationship.coverAttributes(): CoverAttributes? =
    if (type == "cover_art") attributes?.decodeOrNull() else null

fun Relationship.groupAttributes(): ScanlationGroupAttributes? =
    if (type == "scanlation_group") attributes?.decodeOrNull() else null

// ---------- Manga ----------

/** Название: первое из предпочитаемых языков, иначе любое, иначе из альтернативных названий. */
fun Manga.displayTitle(vararg preferredLanguages: String): String? =
    attributes.title.pick(*preferredLanguages)
        ?: attributes.altTitles.firstNotNullOfOrNull { it.pick(*preferredLanguages) }

fun Manga.description(vararg preferredLanguages: String): String? =
    attributes.description.pick(*preferredLanguages)

/** Имена авторов; нужен ReferenceExpansion.AUTHOR. */
val Manga.authorNames: List<String>
    get() = relationships.filter { it.type == "author" }.mapNotNull { it.authorAttributes()?.name }

/** Имена художников; нужен ReferenceExpansion.ARTIST. */
val Manga.artistNames: List<String>
    get() = relationships.filter { it.type == "artist" }.mapNotNull { it.authorAttributes()?.name }

/** Id авторов и художников: есть всегда, даже без reference expansion. */
val Manga.authorIds: List<String>
    get() = relationships.filter { it.type == "author" || it.type == "artist" }.map { it.id }.distinct()

/** Имя файла главной обложки; нужен ReferenceExpansion.COVER_ART. */
val Manga.coverFileName: String?
    get() = relationships.firstOrNull { it.type == "cover_art" }?.coverAttributes()?.fileName

/** Ссылка на главную обложку или null, если cover_art не был запрошен. */
fun Manga.coverUrl(size: CoverSize = CoverSize.ORIGINAL): String? =
    coverFileName?.let { coverUrl(id, it, size) }

// ---------- Chapter ----------

/** Id манги, к которой относится глава. */
val Chapter.mangaId: String?
    get() = relationships.firstOrNull { it.type == "manga" }?.id

/**
 * Группы-переводчики главы. Нужен ReferenceExpansion.SCANLATION_GROUP.
 * MangaDex требует указывать их, если приложение позволяет читать главы.
 */
val Chapter.scanlationGroups: List<ScanlationGroupAttributes>
    get() = relationships.mapNotNull { it.groupAttributes() }

val Chapter.scanlationGroupNames: List<String>
    get() = scanlationGroups.mapNotNull { it.name }

/** Читается ли глава в MangaDex (а не на внешнем сайте). */
val Chapter.isReadableInMangaDex: Boolean
    get() = attributes.externalUrl == null && (attributes.pages ?: 0) > 0

// ---------- Cover ----------

val Cover.mangaId: String?
    get() = relationships.firstOrNull { it.type == "manga" }?.id

fun Cover.url(size: CoverSize = CoverSize.ORIGINAL): String? {
    val manga = mangaId ?: return null
    val file = attributes.fileName ?: return null
    return coverUrl(manga, file, size)
}

// ---------- Страницы главы ----------

/** Адреса страниц главы. Действуют ограниченное время, см. [isLikelyExpired]. */
class ChapterPages internal constructor(
    val chapterId: String,
    val baseUrl: String,
    val hash: String,
    private val data: List<String>,
    private val dataSaver: List<String>,
) {
    private val receivedAt = TimeSource.Monotonic.markNow()

    /** Адрес сервера действует минимум 15 минут: после этого нужно запросить его заново. */
    val isLikelyExpired: Boolean get() = receivedAt.elapsedNow() > 14.minutes

    fun fileNames(quality: ImageQuality): List<String> =
        if (quality == ImageQuality.DATA) data else dataSaver

    val pageCount: Int get() = dataSaver.size.coerceAtLeast(data.size)

    fun pageUrl(index: Int, quality: ImageQuality = ImageQuality.DATA_SAVER): String {
        val files = fileNames(quality)
        require(index in files.indices) { "Нет страницы $index: в главе ${files.size} страниц" }
        // baseUrl используется как есть: это не обязательно адрес вида https://домен
        return "${baseUrl.trimEnd('/')}/${quality.path}/$hash/${files[index]}"
    }

    fun pageUrls(quality: ImageQuality = ImageQuality.DATA_SAVER): List<String> =
        fileNames(quality).indices.map { pageUrl(it, quality) }
}
