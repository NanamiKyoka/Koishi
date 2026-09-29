package com.nanami.koishi.feature.tools.worth_calculator.engine

import com.nanami.koishi.R

object WorthCountries {

    val supportedCountries: List<WorthCountry> = listOf(
        WorthCountry("CN", R.string.postal_country_cn, "¥", 4.19),
        WorthCountry("US", R.string.postal_country_us, "$", 1.00),
        WorthCountry("JP", R.string.postal_country_jp, "¥", 102.84),
        WorthCountry("DE", R.string.postal_country_de, "€", 0.75),
        WorthCountry("GB", R.string.worth_country_gb, "£", 0.70),
        WorthCountry("FR", R.string.postal_country_fr, "€", 0.73),
        WorthCountry("IT", R.string.postal_country_it, "€", 0.66),
        WorthCountry("ES", R.string.postal_country_es, "€", 0.62),
        WorthCountry("CA", R.string.worth_country_ca, "C$", 1.21),
        WorthCountry("AU", R.string.postal_country_au, "A$", 1.47),
        WorthCountry("SG", R.string.worth_country_sg, "S$", 0.84),
        WorthCountry("KR", R.string.worth_country_kr, "₩", 861.82),
        WorthCountry("CH", R.string.postal_country_ch, "CHF", 1.14),
        WorthCountry("NL", R.string.postal_country_nl, "€", 0.77),
        WorthCountry("SE", R.string.postal_country_se, "kr", 8.77),
        WorthCountry("DK", R.string.postal_country_dk, "kr", 6.60),
        WorthCountry("RU", R.string.postal_country_ru, "₽", 25.88),
        WorthCountry("IN", R.string.postal_country_in, "₹", 21.99),
        WorthCountry("MX", R.string.postal_country_mx, "Mex$", 9.52),
        WorthCountry("HK", R.string.worth_country_hk, "HK$", 6.07),
        WorthCountry("TW", R.string.worth_country_tw, "NT$", 13.85),
        WorthCountry("TH", R.string.worth_country_th, "฿", 12.34),
        WorthCountry("MY", R.string.worth_country_my, "RM", 1.57),
        WorthCountry("VN", R.string.worth_country_vn, "₫", 7473.67),
        WorthCountry("BR", R.string.worth_country_br, "R$", 2.36),
        WorthCountry("NZ", R.string.worth_country_nz, "NZ$", 1.45),
        WorthCountry("IE", R.string.worth_country_ie, "€", 0.78),
        WorthCountry("AT", R.string.postal_country_at, "€", 0.76),
        WorthCountry("NO", R.string.worth_country_no, "kr", 10.03),
        WorthCountry("FI", R.string.worth_country_fi, "€", 0.84),
        WorthCountry("PL", R.string.worth_country_pl, "zł", 1.78),
        WorthCountry("AE", R.string.worth_country_ae, "AED", 1.77),
        WorthCountry("SA", R.string.worth_country_sa, "SR", 1.61)
    )

    fun getByCode(code: String): WorthCountry {
        return supportedCountries.find { it.code.equals(code, ignoreCase = true) } ?: supportedCountries[0]
    }
}
