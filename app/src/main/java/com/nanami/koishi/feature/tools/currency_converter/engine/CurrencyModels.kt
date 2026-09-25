package com.nanami.koishi.feature.tools.currency_converter.engine

import kotlinx.serialization.Serializable

@Serializable
data class CurrencyInfo(
    val code: String,
    val name: String,
    val symbol: String? = null,
    val isPreciousMetal: Boolean = false,
    val flag: String = CurrencyConstants.getFlagEmoji(code)
)

object CurrencyConstants {
    val PRECIOUS_METALS = setOf("XAU", "XAG", "XPT", "XPD")

    val DEFAULT_SELECTED_CURRENCIES = listOf("CNY", "USD", "EUR", "JPY", "GBP", "HKD", "XAU")

    private val CHINESE_NAMES = mapOf(
        "CNY" to "人民币",
        "USD" to "美元",
        "EUR" to "欧元",
        "JPY" to "日元",
        "GBP" to "英镑",
        "HKD" to "港币",
        "TWD" to "新台币",
        "KRW" to "韩元",
        "AUD" to "澳大利亚元",
        "CAD" to "加拿大元",
        "CHF" to "瑞士法郎",
        "SGD" to "新加坡元",
        "NZD" to "新西兰元",
        "THB" to "泰铢",
        "MYR" to "马来西亚林吉特",
        "PHP" to "菲律宾比索",
        "IDR" to "印尼卢比",
        "VND" to "越南盾",
        "INR" to "印度卢比",
        "RUB" to "俄罗斯卢布",
        "BRL" to "巴西雷亚尔",
        "MXN" to "墨西哥比索",
        "ZAR" to "南非兰特",
        "TRY" to "土耳其里拉",
        "AED" to "阿联酋迪拉姆",
        "SAR" to "沙特里亚尔",
        "SEK" to "瑞典克朗",
        "NOK" to "挪威克朗",
        "DKK" to "丹麦克朗",
        "PLN" to "波兰兹罗提",
        "CZK" to "捷克克朗",
        "HUF" to "匈牙利福林",
        "ILS" to "以色列新谢克尔",
        "CLP" to "智利比索",
        "COP" to "哥伦比亚比索",
        "EGP" to "埃及镑",
        "KWD" to "科威特第纳尔",
        "QAR" to "卡塔尔里亚尔",
        "ARS" to "阿根廷比索",
        "XAU" to "黄金 (金衡盎司)",
        "XAG" to "白银 (金衡盎司)",
        "XPT" to "铂金 (金衡盎司)",
        "XPD" to "钯金 (金衡盎司)"
    )

    private val DEFAULT_SYMBOLS = mapOf(
        "CNY" to "¥",
        "USD" to "$",
        "EUR" to "€",
        "JPY" to "¥",
        "GBP" to "£",
        "HKD" to "HK$",
        "TWD" to "NT$",
        "KRW" to "₩",
        "AUD" to "A$",
        "CAD" to "C$",
        "CHF" to "Fr",
        "SGD" to "S$",
        "NZD" to "NZ$",
        "THB" to "฿",
        "INR" to "₹",
        "RUB" to "₽",
        "XAU" to "oz t",
        "XAG" to "oz t",
        "XPT" to "oz t",
        "XPD" to "oz t"
    )

    fun getDisplayName(code: String, fallbackName: String, isZh: Boolean): String {
        return if (isZh) {
            CHINESE_NAMES[code] ?: fallbackName
        } else {
            fallbackName
        }
    }

    fun resolveSymbol(code: String, apiSymbol: String?): String {
        if (!apiSymbol.isNullOrBlank()) return apiSymbol
        return DEFAULT_SYMBOLS[code] ?: code
    }

    private val SPECIAL_FLAGS = mapOf(
        "EUR" to "🇪🇺",
        "GBP" to "🇬🇧",
        "CNH" to "🇨🇳",
        "ANG" to "🇨🇼",
        "XCD" to "🇦🇬",
        "XAF" to "🇨🇲",
        "XOF" to "🇸🇳",
        "XPF" to "🇵🇫",
        "WST" to "🇼🇸",
        "STN" to "🇸🇹",
        "SSP" to "🇸🇸",
        "SLE" to "🇸🇱",
        "VES" to "🇻🇪",
        "VUV" to "🇻🇺",
        "ZMW" to "🇿🇲",
        "ZWG" to "🇿🇼",
        "CMD" to "🌍",
        "XDR" to "🌐",
        "XAU" to "🪙",
        "XAG" to "🥈",
        "XPT" to "💍",
        "XPD" to "⚙️"
    )

    fun getFlagEmoji(code: String): String {
        val upper = code.uppercase()
        val special = SPECIAL_FLAGS[upper]
        if (special != null) return special

        if (upper.length >= 2) {
            val c1 = upper[0]
            val c2 = upper[1]
            if (c1 in 'A'..'Z' && c2 in 'A'..'Z') {
                val cp1 = 0x1F1E6 + (c1 - 'A')
                val cp2 = 0x1F1E6 + (c2 - 'A')
                return String(Character.toChars(cp1)) + String(Character.toChars(cp2))
            }
        }
        return "🪙"
    }

    fun isMetal(code: String): Boolean = PRECIOUS_METALS.contains(code.uppercase())
}
