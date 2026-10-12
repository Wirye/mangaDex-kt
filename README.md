# mangadex-kt

Библиотека для работы с [API MangaDex](https://api.mangadex.org/docs/swagger.html#/). 
Не связана с MangaDex.
Здесь нет логики для работы с аккаунтом.

## Установка

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories { maven("https://jitpack.io") }
}

// build.gradle.kts
dependencies {
    implementation("com.github.Wirye.mangaDex-kt:mangaDex-kt:1.0.1")
}
```

## Быстрый старт

> **Важно:** MangaDex требует осмысленный User-Agent: название приложения,
> версию и контакт. Без него запросы могут блокироваться.

```kotlin
val client = MangaDexClient(userAgent = "MyApp/1.0.0 ( me@example.com )")

client.manga.searchManga(title = "Звёздное дитя", limit = 5)
    .onSuccess { page ->
        page.items.forEach { println("${it.displayTitle("ru", "en")} ${it.coverUrl(CoverSize.W256)}") }
    }
```

Клиент создаётся один раз. В нём:

- `client.author`
- `client.chapter`
- `client.cover`
- `client.manga`
- `client.reader` - чтение и скачивание глав

## Обязательные правила MangaDex для приложений

Библиотека ничего не хранит и не показывает, но приложение, которое её использует, обязано соблюдать
([Acceptable Usage Policy](https://api.mangadex.org/docs/))

## Ошибки

Все функции возвращают `Result<T>`. Исключения: `NotFoundException`, `ValidationErrorException` (с текстом от сервера),
`ForbiddenException` (временный бан IP или истёкший адрес сервера картинок), `RateLimitException` (429),
`ServerErrorException` (с `X-Request-ID`, его просят указывать при сообщении об ошибках), `ImageLoadException`.

## Лимит запросов

MangaDex допускает не больше 5 запросов в секунду с одного IP. В библиотеке есть логика под это ограничение, вам не надо писать его самим
