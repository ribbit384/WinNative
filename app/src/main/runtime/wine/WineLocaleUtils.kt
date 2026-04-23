package com.winlator.cmod.runtime.wine

import java.util.Locale

object WineLocaleUtils {
    private const val UTF8_ENCODING = "UTF-8"

    data class Option(
        val value: String,
        val label: String,
    )

    val options: List<Option> =
        listOf(
            option("ar_EG", "العربية (مصر)"),
            option("ar_SA", "العربية (السعودية)"),
            option("de_AT", "Deutsch (Österreich)"),
            option("de_CH", "Deutsch (Schweiz)"),
            option("de_DE", "Deutsch (Deutschland)"),
            option("de_LU", "Deutsch (Luxemburg)"),
            option("en_AU", "English (Australia)"),
            option("en_CA", "English (Canada)"),
            option("en_GB", "English (United Kingdom)"),
            option("en_IN", "English (India)"),
            option("en_US", "English (United States)"),
            option("es_AR", "Español (Argentina)"),
            option("es_ES", "Español (España)"),
            option("es_MX", "Español (México)"),
            option("fr_BE", "Français (Belgique)"),
            option("fr_CA", "Français (Canada)"),
            option("fr_CH", "Français (Suisse)"),
            option("fr_FR", "Français (France)"),
            option("it_CH", "Italiano (Svizzera)"),
            option("it_IT", "Italiano (Italia)"),
            option("ja_JP", "日本語 (日本)"),
            option("ko_KR", "한국어 (대한민국)"),
            option("pt_BR", "Português (Brasil)"),
            option("pt_PT", "Português (Portugal)"),
            option("ru_RU", "Русский (Россия)"),
            option("ru_UA", "Русский (Украина)"),
            option("zh_CN", "简体中文 (中国)"),
            option("zh_HK", "繁體中文 (香港)"),
            option("zh_SG", "简体中文 (新加坡)"),
            option("zh_TW", "繁體中文 (台灣)"),
        )

    @JvmStatic
    fun defaultLocale(): String {
        val systemLocale = Locale.getDefault()
        val language = systemLocale.language.ifBlank { "en" }.lowercase(Locale.ROOT)
        val country = systemLocale.country.ifBlank { "US" }.uppercase(Locale.ROOT)
        return "${language}_${country}.${UTF8_ENCODING}"
    }

    @JvmStatic
    fun normalize(localeValue: String?): String {
        val trimmed = localeValue?.trim().orEmpty()
        if (trimmed.isEmpty()) return defaultLocale()

        val canonical = trimmed.replace('-', '_')
        val baseWithModifier = canonical.substringBefore('.', canonical)
        val encoding = canonical.substringAfter('.', "")
        val base = baseWithModifier.substringBefore('@', baseWithModifier)
        val modifier = baseWithModifier.substringAfter('@', "")
        val normalizedBase = normalizeBase(base)

        if (normalizedBase.equals("C", ignoreCase = true) || normalizedBase.equals("POSIX", ignoreCase = true)) {
            return if (modifier.isNotEmpty()) {
                "${normalizedBase.uppercase(Locale.ROOT)}@$modifier"
            } else {
                normalizedBase.uppercase(Locale.ROOT)
            }
        }

        val normalizedEncoding =
            when {
                encoding.isBlank() -> UTF8_ENCODING
                encoding.equals("utf8", ignoreCase = true) -> UTF8_ENCODING
                encoding.equals("utf-8", ignoreCase = true) -> UTF8_ENCODING
                else -> encoding
            }

        val normalizedLocale =
            if (modifier.isNotEmpty()) {
                "$normalizedBase@$modifier"
            } else {
                normalizedBase
            }

        return "$normalizedLocale.$normalizedEncoding"
    }

    private fun normalizeBase(base: String): String {
        val canonical = base.trim().replace('-', '_')
        val parts = canonical.split('_')
        if (parts.size == 2 && parts[0].length in 2..3 && parts[1].length in 2..3) {
            val language = parts[0].lowercase(Locale.ROOT)
            val country = parts[1].uppercase(Locale.ROOT)
            return "${language}_${country}"
        }
        return canonical
    }

    private fun option(value: String, label: String): Option = Option(normalize(value), label)
}
