package com.sdd.marketplace.core.util

import java.util.Locale
import kotlin.math.roundToInt

object CurrencyUtils {

    data class CurrencyInfo(
        val code: String,
        val symbol: String,
        val name: String,
        val usdRate: Double,
        val decimalPlaces: Int = 2,
        val flag: String = ""
    )

    val supported = listOf(
        CurrencyInfo("USD", "$",    "US Dollar",               1.0,      2, "🇺🇸"),
        CurrencyInfo("EUR", "€",    "Euro",                    0.92,     2, "🇪🇺"),
        CurrencyInfo("GBP", "£",    "British Pound",           0.79,     2, "🇬🇧"),
        CurrencyInfo("JPY", "¥",    "Japanese Yen",            150.0,    0, "🇯🇵"),
        CurrencyInfo("CAD", "CA$",  "Canadian Dollar",         1.36,     2, "🇨🇦"),
        CurrencyInfo("AUD", "A$",   "Australian Dollar",       1.53,     2, "🇦🇺"),
        CurrencyInfo("CHF", "CHF",  "Swiss Franc",             0.90,     2, "🇨🇭"),
        CurrencyInfo("CNY", "¥",    "Chinese Yuan",            7.24,     2, "🇨🇳"),
        CurrencyInfo("HKD", "HK$",  "Hong Kong Dollar",        7.83,     2, "🇭🇰"),
        CurrencyInfo("SGD", "S$",   "Singapore Dollar",        1.35,     2, "🇸🇬"),
        CurrencyInfo("KRW", "₩",    "South Korean Won",        1330.0,   0, "🇰🇷"),
        CurrencyInfo("INR", "₹",    "Indian Rupee",            83.5,     0, "🇮🇳"),
        CurrencyInfo("NGN", "₦",    "Nigerian Naira",          1550.0,   0, "🇳🇬"),
        CurrencyInfo("GHS", "GH₵",  "Ghanaian Cedi",           15.8,     2, "🇬🇭"),
        CurrencyInfo("KES", "KSh",  "Kenyan Shilling",         129.0,    0, "🇰🇪"),
        CurrencyInfo("ZAR", "R",    "South African Rand",      18.7,     2, "🇿🇦"),
        CurrencyInfo("EGP", "E£",   "Egyptian Pound",          48.0,     0, "🇪🇬"),
        CurrencyInfo("UGX", "USh",  "Ugandan Shilling",        3740.0,   0, "🇺🇬"),
        CurrencyInfo("TZS", "TSh",  "Tanzanian Shilling",      2640.0,   0, "🇹🇿"),
        CurrencyInfo("XOF", "CFA",  "West African CFA Franc",  603.0,    0, "🌍"),
        CurrencyInfo("XAF", "FCFA", "Central African CFA",     603.0,    0, "🌍"),
        CurrencyInfo("ZMW", "ZK",   "Zambian Kwacha",          27.0,     2, "🇿🇲"),
        CurrencyInfo("RWF", "RF",   "Rwandan Franc",           1310.0,   0, "🇷🇼"),
        CurrencyInfo("ETB", "Br",   "Ethiopian Birr",          56.0,     2, "🇪🇹"),
        CurrencyInfo("DZD", "DA",   "Algerian Dinar",          134.0,    0, "🇩🇿"),
        CurrencyInfo("MAD", "MAD",  "Moroccan Dirham",         10.0,     2, "🇲🇦"),
        CurrencyInfo("TND", "DT",   "Tunisian Dinar",          3.10,     3, "🇹🇳"),
        CurrencyInfo("MZN", "MT",   "Mozambican Metical",      63.0,     2, "🇲🇿"),
        CurrencyInfo("AOA", "Kz",   "Angolan Kwanza",          820.0,    2, "🇦🇴"),
        CurrencyInfo("BRL", "R$",   "Brazilian Real",          5.0,      2, "🇧🇷"),
        CurrencyInfo("MXN", "MX$",  "Mexican Peso",            17.2,     2, "🇲🇽"),
        CurrencyInfo("ARS", "AR$",  "Argentine Peso",          870.0,    0, "🇦🇷"),
        CurrencyInfo("CLP", "CLP$", "Chilean Peso",            900.0,    0, "🇨🇱"),
        CurrencyInfo("COP", "COP$", "Colombian Peso",          3900.0,   0, "🇨🇴"),
        CurrencyInfo("PEN", "S/",   "Peruvian Sol",            3.7,      2, "🇵🇪"),
        CurrencyInfo("VES", "Bs.",  "Venezuelan Bolívar",      36.0,     2, "🇻🇪"),
        CurrencyInfo("UYU", "$U",   "Uruguayan Peso",          38.0,     2, "🇺🇾"),
        CurrencyInfo("BOB", "Bs.",  "Bolivian Boliviano",      6.9,      2, "🇧🇴"),
        CurrencyInfo("PYG", "₲",    "Paraguayan Guaraní",      7300.0,   0, "🇵🇾"),
        CurrencyInfo("GTQ", "Q",    "Guatemalan Quetzal",      7.8,      2, "🇬🇹"),
        CurrencyInfo("HNL", "L",    "Honduran Lempira",        24.6,     2, "🇭🇳"),
        CurrencyInfo("CRC", "₡",    "Costa Rican Colón",       520.0,    0, "🇨🇷"),
        CurrencyInfo("DOP", "RD$",  "Dominican Peso",          58.0,     2, "🇩🇴"),
        CurrencyInfo("RUB", "₽",    "Russian Ruble",           90.0,     2, "🇷🇺"),
        CurrencyInfo("TRY", "₺",    "Turkish Lira",            32.0,     2, "🇹🇷"),
        CurrencyInfo("UAH", "₴",    "Ukrainian Hryvnia",       39.0,     2, "🇺🇦"),
        CurrencyInfo("PLN", "zł",   "Polish Zloty",            3.98,     2, "🇵🇱"),
        CurrencyInfo("CZK", "Kč",   "Czech Koruna",            23.0,     2, "🇨🇿"),
        CurrencyInfo("HUF", "Ft",   "Hungarian Forint",        360.0,    0, "🇭🇺"),
        CurrencyInfo("RON", "lei",  "Romanian Leu",            4.58,     2, "🇷🇴"),
        CurrencyInfo("BGN", "лв",   "Bulgarian Lev",           1.80,     2, "🇧🇬"),
        CurrencyInfo("HRK", "kn",   "Croatian Kuna",           6.94,     2, "🇭🇷"),
        CurrencyInfo("DKK", "kr",   "Danish Krone",            6.89,     2, "🇩🇰"),
        CurrencyInfo("SEK", "kr",   "Swedish Krona",           10.5,     2, "🇸🇪"),
        CurrencyInfo("NOK", "kr",   "Norwegian Krone",         10.7,     2, "🇳🇴"),
        CurrencyInfo("ISK", "kr",   "Icelandic Krona",         138.0,    0, "🇮🇸"),
        CurrencyInfo("SAR", "﷼",    "Saudi Riyal",             3.75,     2, "🇸🇦"),
        CurrencyInfo("AED", "د.إ",  "UAE Dirham",              3.67,     2, "🇦🇪"),
        CurrencyInfo("QAR", "﷼",    "Qatari Riyal",            3.64,     2, "🇶🇦"),
        CurrencyInfo("KWD", "KD",   "Kuwaiti Dinar",           0.31,     3, "🇰🇼"),
        CurrencyInfo("BHD", "BD",   "Bahraini Dinar",          0.376,    3, "🇧🇭"),
        CurrencyInfo("OMR", "﷼",    "Omani Rial",              0.385,    3, "🇴🇲"),
        CurrencyInfo("JOD", "JD",   "Jordanian Dinar",         0.709,    3, "🇯🇴"),
        CurrencyInfo("LBP", "LL",   "Lebanese Pound",          89500.0,  0, "🇱🇧"),
        CurrencyInfo("IQD", "ع.د",  "Iraqi Dinar",             1310.0,   0, "🇮🇶"),
        CurrencyInfo("IRR", "﷼",    "Iranian Rial",            42000.0,  0, "🇮🇷"),
        CurrencyInfo("ILS", "₪",    "Israeli Shekel",          3.70,     2, "🇮🇱"),
        CurrencyInfo("PKR", "₨",    "Pakistani Rupee",         278.0,    0, "🇵🇰"),
        CurrencyInfo("BDT", "৳",    "Bangladeshi Taka",        110.0,    2, "🇧🇩"),
        CurrencyInfo("LKR", "₨",    "Sri Lankan Rupee",        305.0,    2, "🇱🇰"),
        CurrencyInfo("NPR", "₨",    "Nepalese Rupee",          133.0,    2, "🇳🇵"),
        CurrencyInfo("MMK", "K",    "Myanmar Kyat",            2100.0,   0, "🇲🇲"),
        CurrencyInfo("THB", "฿",    "Thai Baht",               35.0,     2, "🇹🇭"),
        CurrencyInfo("VND", "₫",    "Vietnamese Dong",         24400.0,  0, "🇻🇳"),
        CurrencyInfo("IDR", "Rp",   "Indonesian Rupiah",       15700.0,  0, "🇮🇩"),
        CurrencyInfo("MYR", "RM",   "Malaysian Ringgit",       4.7,      2, "🇲🇾"),
        CurrencyInfo("PHP", "₱",    "Philippine Peso",         56.0,     2, "🇵🇭"),
        CurrencyInfo("TWD", "NT$",  "Taiwan Dollar",           32.0,     2, "🇹🇼"),
        CurrencyInfo("NZD", "NZ$",  "New Zealand Dollar",      1.63,     2, "🇳🇿"),
        CurrencyInfo("KZT", "₸",    "Kazakhstani Tenge",       450.0,    2, "🇰🇿"),
        CurrencyInfo("UZS", "so'm", "Uzbekistani Som",         12400.0,  0, "🇺🇿"),
        CurrencyInfo("GEL", "₾",    "Georgian Lari",           2.65,     2, "🇬🇪"),
        CurrencyInfo("AMD", "֏",    "Armenian Dram",           387.0,    0, "🇦🇲"),
        CurrencyInfo("AZN", "₼",    "Azerbaijani Manat",       1.70,     2, "🇦🇿"),
        CurrencyInfo("AFN", "؋",    "Afghan Afghani",          70.0,     2, "🇦🇫"),
        CurrencyInfo("MNT", "₮",    "Mongolian Tugrik",        3450.0,   0, "🇲🇳"),
        CurrencyInfo("KHR", "₭",    "Cambodian Riel",          4100.0,   0, "🇰🇭"),
        CurrencyInfo("LAK", "₭",    "Lao Kip",                 21000.0,  0, "🇱🇦"),
        CurrencyInfo("SYP", "S£",   "Syrian Pound",            13000.0,  0, "🇸🇾"),
        CurrencyInfo("YER", "﷼",    "Yemeni Rial",             250.0,    0, "🇾🇪"),
        CurrencyInfo("SDG", "SDG",  "Sudanese Pound",          600.0,    2, "🇸🇩"),
        CurrencyInfo("LYD", "LD",   "Libyan Dinar",            4.85,     3, "🇱🇾"),
        CurrencyInfo("SOS", "Sh",   "Somali Shilling",         571.0,    2, "🇸🇴"),
        CurrencyInfo("MDL", "L",    "Moldovan Leu",            17.8,     2, "🇲🇩"),
        CurrencyInfo("MKD", "ден",  "Macedonian Denar",        56.5,     2, "🇲🇰"),
        CurrencyInfo("ALL", "L",    "Albanian Lek",            95.0,     2, "🇦🇱"),
        CurrencyInfo("BAM", "KM",   "Bosnia Convertible Mark",  1.80,    2, "🇧🇦"),
        CurrencyInfo("RSD", "din",  "Serbian Dinar",           108.0,    2, "🇷🇸"),
        CurrencyInfo("BYN", "Br",   "Belarusian Ruble",        3.25,     2, "🇧🇾"),
        CurrencyInfo("MWK", "MK",   "Malawian Kwacha",         1730.0,   2, "🇲🇼"),
        CurrencyInfo("BWP", "P",    "Botswana Pula",           13.5,     2, "🇧🇼"),
        CurrencyInfo("NAD", "N$",   "Namibian Dollar",         18.7,     2, "🇳🇦"),
        CurrencyInfo("ZWL", "Z$",   "Zimbabwean Dollar",       322.0,    2, "🇿🇼"),
        CurrencyInfo("MUR", "₨",    "Mauritian Rupee",         46.0,     2, "🇲🇺"),
        CurrencyInfo("SCR", "₨",    "Seychellois Rupee",       13.5,     2, "🇸🇨"),
        CurrencyInfo("CDF", "FC",   "Congolese Franc",         2820.0,   2, "🇨🇩"),
        CurrencyInfo("XCD", "EC$",  "East Caribbean Dollar",   2.70,     2, "🇦🇬"),
        CurrencyInfo("JMD", "J$",   "Jamaican Dollar",         156.0,    2, "🇯🇲"),
        CurrencyInfo("TTD", "TT$",  "Trinidad and Tobago Dollar", 6.77,  2, "🇹🇹"),
        CurrencyInfo("BBD", "Bds$", "Barbadian Dollar",        2.0,      2, "🇧🇧"),
        CurrencyInfo("KYD", "CI$",  "Cayman Islands Dollar",   0.83,     2, "🇰🇾"),
        CurrencyInfo("BMD", "BD$",  "Bermudian Dollar",        1.0,      2, "🇧🇲"),
        CurrencyInfo("FJD", "FJ$",  "Fijian Dollar",           2.26,     2, "🇫🇯"),
        CurrencyInfo("PGK", "K",    "Papua New Guinean Kina",  3.78,     2, "🇵🇬"),
        CurrencyInfo("WST", "T",    "Samoan Tala",             2.77,     2, "🇼🇸"),
        CurrencyInfo("TOP", "T$",   "Tongan Paʻanga",          2.36,     2, "🇹🇴"),
        CurrencyInfo("SBD", "SI$",  "Solomon Islands Dollar",  8.45,     2, "🇸🇧"),
        CurrencyInfo("VUV", "VT",   "Vanuatu Vatu",            119.0,    0, "🇻🇺")
    )

    private val byCode: Map<String, CurrencyInfo> = supported.associateBy { it.code }

    private val countryToCurrency = mapOf(
        "US" to "USD", "GB" to "GBP", "NG" to "NGN", "GH" to "GHS",
        "KE" to "KES", "ZA" to "ZAR", "IN" to "INR", "EG" to "EGP",
        "UG" to "UGX", "TZ" to "TZS", "SN" to "XOF", "CI" to "XOF",
        "ML" to "XOF", "BF" to "XOF", "NE" to "XOF", "TG" to "XOF",
        "BJ" to "XOF", "CA" to "CAD", "AU" to "AUD", "ZM" to "ZMW",
        "RW" to "RWF", "DE" to "EUR", "FR" to "EUR", "IT" to "EUR",
        "ES" to "EUR", "PT" to "EUR", "NL" to "EUR", "BE" to "EUR",
        "AT" to "EUR", "IE" to "EUR", "FI" to "EUR", "GR" to "EUR",
        "JP" to "JPY", "CH" to "CHF", "CN" to "CNY", "HK" to "HKD",
        "SG" to "SGD", "KR" to "KRW", "MX" to "MXN", "BR" to "BRL",
        "AR" to "ARS", "CL" to "CLP", "CO" to "COP", "PE" to "PEN",
        "SA" to "SAR", "AE" to "AED", "QA" to "QAR", "KW" to "KWD",
        "BH" to "BHD", "OM" to "OMR", "JO" to "JOD", "LB" to "LBP",
        "IQ" to "IQD", "IL" to "ILS", "PK" to "PKR", "BD" to "BDT",
        "LK" to "LKR", "NP" to "NPR", "MM" to "MMK", "TH" to "THB",
        "VN" to "VND", "ID" to "IDR", "MY" to "MYR", "PH" to "PHP",
        "TW" to "TWD", "NZ" to "NZD", "RU" to "RUB", "TR" to "TRY",
        "UA" to "UAH", "PL" to "PLN", "CZ" to "CZK", "HU" to "HUF",
        "RO" to "RON", "BG" to "BGN", "DK" to "DKK", "SE" to "SEK",
        "NO" to "NOK", "IS" to "ISK", "KZ" to "KZT", "GE" to "GEL",
        "AM" to "AMD", "AZ" to "AZN", "ET" to "ETB", "DZ" to "DZD",
        "MA" to "MAD", "TN" to "TND", "MZ" to "MZN", "AO" to "AOA",
        "MW" to "MWK", "BW" to "BWP", "NA" to "NAD", "ZW" to "ZWL",
        "MU" to "MUR", "SC" to "SCR", "CD" to "CDF", "CM" to "XAF",
        "CF" to "XAF", "TD" to "XAF", "CG" to "XAF", "GA" to "XAF",
        "GQ" to "XAF", "MN" to "MNT", "KH" to "KHR", "LA" to "LAK",
        "SD" to "SDG", "LY" to "LYD", "SO" to "SOS", "YE" to "YER",
        "JM" to "JMD", "TT" to "TTD", "BB" to "BBD", "FJ" to "FJD",
        "PG" to "PGK", "WS" to "WST", "TO" to "TOP", "SB" to "SBD",
        "VU" to "VUV", "UY" to "UYU", "BO" to "BOB", "PY" to "PYG",
        "GT" to "GTQ", "HN" to "HNL", "CR" to "CRC", "DO" to "DOP"
    )

    fun detectCurrency(): CurrencyInfo {
        val countryCode = Locale.getDefault().country.uppercase()
        val currencyCode = countryToCurrency[countryCode] ?: "USD"
        return byCode[currencyCode] ?: byCode["USD"]!!
    }

    fun getInfo(code: String): CurrencyInfo = byCode[code] ?: byCode["USD"]!!

    fun convertFromUsd(usdAmount: Double, targetCode: String): String {
        val info = getInfo(targetCode)
        val local = usdAmount * info.usdRate
        return if (info.decimalPlaces == 0) {
            "${info.symbol}${local.roundToInt().formatWithCommas()}"
        } else {
            "${info.symbol}${"%.${info.decimalPlaces}f".format(local)}"
        }
    }

    fun usdToLocal(usdAmount: Double, targetCode: String): Double {
        val info = getInfo(targetCode)
        return usdAmount * info.usdRate
    }

    fun format(amount: Double, currencyCode: String): String {
        val info = getInfo(currencyCode)
        return if (info.decimalPlaces == 0) {
            "${info.symbol}${amount.roundToInt().formatWithCommas()}"
        } else {
            "${info.symbol}${"%.${info.decimalPlaces}f".format(amount)}"
        }
    }

    private fun Int.formatWithCommas(): String = "%,d".format(this)
}
