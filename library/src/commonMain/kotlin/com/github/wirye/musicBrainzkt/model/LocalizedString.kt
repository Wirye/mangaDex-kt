package com.github.wirye.musicBrainzkt.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * Строка на нескольких языках: {"en": "...", "ja-ro": "..."}.
 * Язык это ISO 639-1 ("en", "ru"), иногда с регионом ("pt-br") или с суффиксом -ro для романизации ("ja-ro").
 */
@Serializable(with = LocalizedStringSerializer::class)
data class LocalizedString(val values: Map<String, String> = emptyMap()) {
    operator fun get(language: String): String? = values[language]

    /** Первая найденная строка из предпочитаемых языков, иначе любая доступная. */
    fun pick(vararg preferred: String): String? =
        preferred.firstNotNullOfOrNull { values[it] } ?: values.values.firstOrNull()
}

/** Принимает и объект, и пустой массив/null (у API бывают такие значения): тогда получится пустая строка. */
object LocalizedStringSerializer : KSerializer<LocalizedString> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("LocalizedString")

    override fun deserialize(decoder: Decoder): LocalizedString {
        val json = decoder as? JsonDecoder ?: error("LocalizedString читается только из JSON")
        val element = json.decodeJsonElement()
        val map = (element as? JsonObject)
            ?.mapNotNull { (key, value) -> (value as? JsonPrimitive)?.contentOrNull?.let { key to it } }
            ?.toMap()
            .orEmpty()
        return LocalizedString(map)
    }

    override fun serialize(encoder: Encoder, value: LocalizedString) {
        val json = encoder as? JsonEncoder ?: error("LocalizedString пишется только в JSON")
        json.encodeJsonElement(JsonObject(value.values.mapValues { JsonPrimitive(it.value) }))
    }
}
