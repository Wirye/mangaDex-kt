package com.github.wirye.mangaDexKt.model

import kotlinx.serialization.Serializable

/** Обёртки ответов: коллекции и одиночные сущности устроены одинаково для всех типов. */

@Serializable
internal data class CollectionResponse<T>(
    val result: String? = null,
    val data: List<T> = emptyList(),
    val limit: Int = 0,
    val offset: Int = 0,
    val total: Int = 0,
)

@Serializable
internal data class EntityResponse<T>(
    val result: String? = null,
    val data: T,
)

/** GET /at-home/server/{chapterId} */
@Serializable
internal data class AtHomeResponse(
    val baseUrl: String,
    val chapter: AtHomeChapter,
)

@Serializable
internal data class AtHomeChapter(
    val hash: String,
    /** Имена файлов в оригинальном качестве */
    val data: List<String> = emptyList(),
    /** Имена файлов в сжатом качестве */
    val dataSaver: List<String> = emptyList(),
)
