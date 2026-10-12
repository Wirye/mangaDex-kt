package com.github.wirye.mangaDexKt.model

/** Значения для параметров запроса. В ответах те же поля приходят обычными строками, чтобы новое значение не ломало разбор. */

enum class MangaStatus(val value: String) {
    ONGOING("ongoing"), COMPLETED("completed"), HIATUS("hiatus"), CANCELLED("cancelled")
}

enum class ContentRating(val value: String) {
    SAFE("safe"), SUGGESTIVE("suggestive"), EROTICA("erotica"), PORNOGRAPHIC("pornographic")
}

enum class PublicationDemographic(val value: String) {
    SHOUNEN("shounen"), SHOUJO("shoujo"), JOSEI("josei"), SEINEN("seinen")
}

/**
 * Reference expansion: в relationships приходят не только id, но и атрибуты связанных сущностей,
 * так что одним запросом можно получить и обложку, и авторов. Подходит не для каждого запроса:
 * манга принимает COVER_ART, AUTHOR, ARTIST (и другие), глава принимает SCANLATION_GROUP, MANGA, USER,
 * обложка принимает MANGA, USER, автор принимает MANGA. Если значение не подходит, сервер ответит 400.
 */
enum class ReferenceExpansion(val value: String) {
    MANGA("manga"),
    COVER_ART("cover_art"),
    AUTHOR("author"),
    ARTIST("artist"),
    SCANLATION_GROUP("scanlation_group"),
    USER("user"),
}

enum class SortDirection(val value: String) { ASC("asc"), DESC("desc") }

interface OrderField { val value: String }

/** Сортировка: можно передать несколько, например volume, затем chapter. */
data class Order<F : OrderField>(val field: F, val direction: SortDirection = SortDirection.ASC)

enum class MangaOrderField(override val value: String) : OrderField {
    TITLE("title"), YEAR("year"), CREATED_AT("createdAt"), UPDATED_AT("updatedAt"),
    LATEST_UPLOADED_CHAPTER("latestUploadedChapter"), FOLLOWED_COUNT("followedCount"), RELEVANCE("relevance")
}

enum class ChapterOrderField(override val value: String) : OrderField {
    CREATED_AT("createdAt"), UPDATED_AT("updatedAt"), PUBLISH_AT("publishAt"),
    READABLE_AT("readableAt"), VOLUME("volume"), CHAPTER("chapter")
}

enum class CoverOrderField(override val value: String) : OrderField {
    CREATED_AT("createdAt"), UPDATED_AT("updatedAt"), VOLUME("volume")
}

enum class AuthorOrderField(override val value: String) : OrderField {
    NAME("name"), CREATED_AT("createdAt"), UPDATED_AT("updatedAt")
}

/** DATA это оригинальное качество, DATA_SAVER сжатое: оно намного легче и экономит трафик и сервера MangaDex. */
enum class ImageQuality(val path: String) { DATA("data"), DATA_SAVER("data-saver") }

/** Размер обложки: оригинал или готовые превью шириной 256 и 512 пикселей. */
enum class CoverSize(val suffix: String) { ORIGINAL(""), W256(".256.jpg"), W512(".512.jpg") }
