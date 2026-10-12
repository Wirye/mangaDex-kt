package com.github.wirye.mangaDexKt.exception

sealed class MangaDexException(message: String) : Exception(message) {
    class NotFoundException : MangaDexException("Ничего не найдено (404)")

    class ValidationErrorException(details: String? = null) :
        MangaDexException("Ошибка валидации (неверные параметры запроса)" + details?.let { ": $it" }.orEmpty())

    class ForbiddenException(details: String? = null) :
        MangaDexException("Доступ запрещён (403)" + details?.let { ": $it" }.orEmpty())

    class RateLimitException :
        MangaDexException("Слишком много запросов (429)")

    class ServerErrorException(val code: Int, val requestId: String? = null) :
        MangaDexException("Ошибка сервера MangaDex ($code)" + requestId?.let { ", X-Request-ID: $it" }.orEmpty())

    class ImageLoadException(val url: String, val code: Int? = null) :
        MangaDexException("Не удалось загрузить изображение" + code?.let { " (код $it)" }.orEmpty() + ": $url")
}
