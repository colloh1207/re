package com.sdd.marketplace.core.util

object SanitizationUtils {

    private val HTML_TAG_REGEX = Regex("<[^>]*>")
    private val JS_PROTO_REGEX = Regex("javascript\\s*:", RegexOption.IGNORE_CASE)
    private val DATA_PROTO_REGEX = Regex("data\\s*:", RegexOption.IGNORE_CASE)
    private val ON_EVENT_REGEX = Regex("on[a-z]+\\s*=", RegexOption.IGNORE_CASE)
    private val SQL_COMMENT_REGEX = Regex("(--|#\\s|/\\*|\\*/|\\bxp_|\\bEXEC\\b|\\bDROP\\b|\\bTRUNCATE\\b|\\bINSERT\\b|\\bUPDATE\\b|\\bDELETE\\b|\\bSELECT\\b\\s+\\*)", RegexOption.IGNORE_CASE)
    private val NULL_BYTE_REGEX = Regex("\u0000")
    private val UNICODE_DIRECTION_REGEX = Regex("[\u200E\u200F\u202A-\u202E\u2066-\u2069\uFEFF]")

    fun sanitizeText(input: String): String = input
        .replace(NULL_BYTE_REGEX, "")
        .replace(UNICODE_DIRECTION_REGEX, "")
        .replace(HTML_TAG_REGEX, "")
        .replace(JS_PROTO_REGEX, "")
        .replace(DATA_PROTO_REGEX, "")
        .replace(ON_EVENT_REGEX, "")
        .trim()

    fun sanitizeProductTitle(input: String): String =
        sanitizeText(input).take(120)

    fun sanitizeDescription(input: String): String =
        sanitizeText(input).take(5000)

    fun sanitizeBrand(input: String): String =
        sanitizeText(input).take(80)

    fun sanitizeTag(input: String): String =
        sanitizeText(input)
            .replace(Regex("[^a-zA-Z0-9\\s\\-_]"), "")
            .take(50)

    fun sanitizePriceInput(input: String): String =
        input.replace(Regex("[^0-9.]"), "").take(14)

    fun sanitizeChatMessage(input: String): String =
        input.replace(NULL_BYTE_REGEX, "")
            .replace(UNICODE_DIRECTION_REGEX, "")
            .trim()
            .take(4000)

    fun sanitizeUrl(input: String): String {
        val trimmed = input.trim()
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) return ""
        return trimmed.replace(HTML_TAG_REGEX, "").take(2048)
    }

    fun sanitizeSearchQuery(input: String): String =
        input.replace(SQL_COMMENT_REGEX, "")
            .replace(NULL_BYTE_REGEX, "")
            .trim()
            .take(200)
}
