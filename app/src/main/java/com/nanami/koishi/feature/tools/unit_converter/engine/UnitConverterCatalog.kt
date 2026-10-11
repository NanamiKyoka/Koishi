package com.nanami.koishi.feature.tools.unit_converter.engine

import androidx.annotation.StringRes
import com.nanami.koishi.R
import java.math.BigDecimal
import java.math.MathContext

enum class UnitCategory(
    val id: String,
    @StringRes val titleRes: Int,
    val allowNegative: Boolean = false
) {
    LENGTH("length", R.string.unit_category_length),
    AREA("area", R.string.unit_category_area),
    VOLUME("volume", R.string.unit_category_volume),
    MASS("mass", R.string.unit_category_mass),
    TEMPERATURE("temperature", R.string.unit_category_temperature, allowNegative = true),
    SPEED("speed", R.string.unit_category_speed),
    TIME("time", R.string.unit_category_time),
    DATA("data", R.string.unit_category_data),
    PRESSURE("pressure", R.string.unit_category_pressure),
    ENERGY("energy", R.string.unit_category_energy, allowNegative = true),
    POWER("power", R.string.unit_category_power, allowNegative = true),
    ANGLE("angle", R.string.unit_category_angle, allowNegative = true),
    FREQUENCY("frequency", R.string.unit_category_frequency);

    companion object {
        fun fromId(id: String): UnitCategory =
            entries.firstOrNull { it.id == id } ?: LENGTH
    }
}

data class UnitDefinition(
    val id: String,
    @StringRes val nameRes: Int,
    val symbol: String,
    val scale: BigDecimal,
    val offset: BigDecimal = BigDecimal.ZERO
)

object UnitConverterCatalog {

    private val mathContext = MathContext.DECIMAL128

    fun units(category: UnitCategory): List<UnitDefinition> = unitsByCategory.getValue(category)

    fun find(category: UnitCategory, unitId: String): UnitDefinition =
        units(category).firstOrNull { it.id == unitId } ?: units(category).first()

    fun defaults(category: UnitCategory): Pair<UnitDefinition, UnitDefinition> {
        val units = units(category)
        return units.first() to units.getOrElse(1) { units.first() }
    }

    private fun decimal(value: String) = BigDecimal(value)

    private fun fraction(numerator: String, denominator: String) =
        decimal(numerator).divide(decimal(denominator), mathContext)

    private fun temperatureOffset(scale: BigDecimal, valueAtZero: String) =
        decimal(valueAtZero).subtract(decimal("32").multiply(scale))

    private val fahrenheitScale = fraction("5", "9")
    private val cupUs = decimal("0.0002365882365")
    private val fluidOunceUs = decimal("0.0000295735295625")
    private val usGallon = decimal("0.003785411784")
    private val imperialGallon = decimal("0.00454609")
    private val usQuart = usGallon.divide(decimal("4"), mathContext)
    private val usPint = usGallon.divide(decimal("8"), mathContext)
    private val acre = decimal("4046.8564224")
    private val btu = decimal("1055.05585262")
    private val metricHorsepower = decimal("735.49875")
    private val mechanicalHorsepower = decimal("745.69987158227022")
    private val pi = decimal(Math.PI.toString())

    private val unitsByCategory: Map<UnitCategory, List<UnitDefinition>> = mapOf(
        UnitCategory.LENGTH to listOf(
            unit("meter", R.string.unit_meter, "m", "1"),
            unit("kilometer", R.string.unit_kilometer, "km", "1000"),
            unit("centimeter", R.string.unit_centimeter, "cm", "0.01"),
            unit("millimeter", R.string.unit_millimeter, "mm", "0.001"),
            unit("micrometer", R.string.unit_micrometer, "μm", "0.000001"),
            unit("nanometer", R.string.unit_nanometer, "nm", "0.000000001"),
            unit("mile", R.string.unit_mile, "mi", "1609.344"),
            unit("yard", R.string.unit_yard, "yd", "0.9144"),
            unit("foot", R.string.unit_foot, "ft", "0.3048"),
            unit("inch", R.string.unit_inch, "in", "0.0254"),
            unit("nautical_mile", R.string.unit_nautical_mile, "nmi", "1852"),
            unit("li", R.string.unit_li, "里", "500")
        ),
        UnitCategory.AREA to listOf(
            unit("square_meter", R.string.unit_square_meter, "m²", "1"),
            unit("square_kilometer", R.string.unit_square_kilometer, "km²", "1000000"),
            unit("square_centimeter", R.string.unit_square_centimeter, "cm²", "0.0001"),
            unit("hectare", R.string.unit_hectare, "ha", "10000"),
            unit("mu", R.string.unit_mu, "亩", fraction("2000", "3")),
            unit("acre", R.string.unit_acre, "ac", acre.toPlainString()),
            unit("square_foot", R.string.unit_square_foot, "ft²", "0.09290304"),
            unit("square_inch", R.string.unit_square_inch, "in²", "0.00064516"),
            unit("square_yard", R.string.unit_square_yard, "yd²", "0.83612736")
        ),
        UnitCategory.VOLUME to listOf(
            unit("cubic_meter", R.string.unit_cubic_meter, "m³", "1"),
            unit("liter", R.string.unit_liter, "L", "0.001"),
            unit("milliliter", R.string.unit_milliliter, "mL", "0.000001"),
            unit("cubic_centimeter", R.string.unit_cubic_centimeter, "cm³", "0.000001"),
            unit("us_gallon", R.string.unit_us_gallon, "gal", usGallon.toPlainString()),
            unit("imperial_gallon", R.string.unit_imperial_gallon, "gal UK", imperialGallon.toPlainString()),
            unit("us_quart", R.string.unit_us_quart, "qt", usQuart.toPlainString()),
            unit("us_pint", R.string.unit_us_pint, "pt", usPint.toPlainString()),
            unit("us_fluid_ounce", R.string.unit_us_fluid_ounce, "fl oz", fluidOunceUs.toPlainString()),
            unit("us_cup", R.string.unit_us_cup, "cup", cupUs.toPlainString()),
            unit("cubic_foot", R.string.unit_cubic_foot, "ft³", "0.028316846592"),
            unit("cubic_inch", R.string.unit_cubic_inch, "in³", "0.000016387064")
        ),
        UnitCategory.MASS to listOf(
            unit("kilogram", R.string.unit_kilogram, "kg", "1"),
            unit("gram", R.string.unit_gram, "g", "0.001"),
            unit("milligram", R.string.unit_milligram, "mg", "0.000001"),
            unit("metric_ton", R.string.unit_metric_ton, "t", "1000"),
            unit("pound", R.string.unit_pound, "lb", "0.45359237"),
            unit("ounce", R.string.unit_ounce, "oz", "0.028349523125"),
            unit("stone", R.string.unit_stone, "st", "6.35029318"),
            unit("jin", R.string.unit_jin, "斤", "0.5"),
            unit("liang", R.string.unit_liang, "两", "0.05")
        ),
        UnitCategory.TEMPERATURE to listOf(
            unit("celsius", R.string.unit_celsius, "°C", "1", "273.15"),
            unit("fahrenheit", R.string.unit_fahrenheit, "°F", fahrenheitScale, temperatureOffset(fahrenheitScale, "273.15")),
            unit("kelvin", R.string.unit_kelvin, "K", "1")
        ),
        UnitCategory.SPEED to listOf(
            unit("meter_per_second", R.string.unit_meter_per_second, "m/s", "1"),
            unit("kilometer_per_hour", R.string.unit_kilometer_per_hour, "km/h", fraction("1", "3.6")),
            unit("mile_per_hour", R.string.unit_mile_per_hour, "mph", "0.44704"),
            unit("knot", R.string.unit_knot, "kn", fraction("1852", "3600")),
            unit("foot_per_second", R.string.unit_foot_per_second, "ft/s", "0.3048")
        ),
        UnitCategory.TIME to listOf(
            unit("second", R.string.unit_second, "s", "1"),
            unit("minute", R.string.unit_minute, "min", "60"),
            unit("hour", R.string.unit_hour, "h", "3600"),
            unit("day", R.string.unit_day, "d", "86400"),
            unit("week", R.string.unit_week, "wk", "604800"),
            unit("millisecond", R.string.unit_millisecond, "ms", "0.001"),
            unit("microsecond", R.string.unit_microsecond, "μs", "0.000001"),
            unit("nanosecond", R.string.unit_nanosecond, "ns", "0.000000001")
        ),
        UnitCategory.DATA to listOf(
            unit("byte", R.string.unit_byte, "B", "1"),
            unit("bit", R.string.unit_bit, "bit", "0.125"),
            unit("kilobyte", R.string.unit_kilobyte, "KB", "1000"),
            unit("megabyte", R.string.unit_megabyte, "MB", "1000000"),
            unit("gigabyte", R.string.unit_gigabyte, "GB", "1000000000"),
            unit("terabyte", R.string.unit_terabyte, "TB", "1000000000000"),
            unit("kibibyte", R.string.unit_kibibyte, "KiB", "1024"),
            unit("mebibyte", R.string.unit_mebibyte, "MiB", "1048576"),
            unit("gibibyte", R.string.unit_gibibyte, "GiB", "1073741824"),
            unit("tebibyte", R.string.unit_tebibyte, "TiB", "1099511627776")
        ),
        UnitCategory.PRESSURE to listOf(
            unit("pascal", R.string.unit_pascal, "Pa", "1"),
            unit("hectopascal", R.string.unit_hectopascal, "hPa", "100"),
            unit("kilopascal", R.string.unit_kilopascal, "kPa", "1000"),
            unit("megapascal", R.string.unit_megapascal, "MPa", "1000000"),
            unit("bar", R.string.unit_bar, "bar", "100000"),
            unit("millibar", R.string.unit_millibar, "mbar", "100"),
            unit("atmosphere", R.string.unit_atmosphere, "atm", "101325"),
            unit("millimeter_of_mercury", R.string.unit_millimeter_of_mercury, "mmHg", "133.322387415"),
            unit("psi", R.string.unit_psi, "psi", "6894.757293168361")
        ),
        UnitCategory.ENERGY to listOf(
            unit("joule", R.string.unit_joule, "J", "1"),
            unit("kilojoule", R.string.unit_kilojoule, "kJ", "1000"),
            unit("calorie", R.string.unit_calorie, "cal", "4.184"),
            unit("kilocalorie", R.string.unit_kilocalorie, "kcal", "4184"),
            unit("watt_hour", R.string.unit_watt_hour, "Wh", "3600"),
            unit("kilowatt_hour", R.string.unit_kilowatt_hour, "kWh", "3600000"),
            unit("btu", R.string.unit_btu, "BTU", btu.toPlainString()),
            unit("electronvolt", R.string.unit_electronvolt, "eV", "0.0000000000000000001602176634")
        ),
        UnitCategory.POWER to listOf(
            unit("watt", R.string.unit_watt, "W", "1"),
            unit("kilowatt", R.string.unit_kilowatt, "kW", "1000"),
            unit("megawatt", R.string.unit_megawatt, "MW", "1000000"),
            unit("metric_horsepower", R.string.unit_metric_horsepower, "PS", metricHorsepower.toPlainString()),
            unit("mechanical_horsepower", R.string.unit_mechanical_horsepower, "hp", mechanicalHorsepower.toPlainString())
        ),
        UnitCategory.ANGLE to listOf(
            unit("degree", R.string.unit_degree, "°", pi.divide(decimal("180"), mathContext)),
            unit("radian", R.string.unit_radian, "rad", "1"),
            unit("gradian", R.string.unit_gradian, "grad", pi.divide(decimal("200"), mathContext)),
            unit("arcminute", R.string.unit_arcminute, "′", pi.divide(decimal("10800"), mathContext)),
            unit("arcsecond", R.string.unit_arcsecond, "″", pi.divide(decimal("648000"), mathContext))
        ),
        UnitCategory.FREQUENCY to listOf(
            unit("hertz", R.string.unit_hertz, "Hz", "1"),
            unit("kilohertz", R.string.unit_kilohertz, "kHz", "1000"),
            unit("megahertz", R.string.unit_megahertz, "MHz", "1000000"),
            unit("gigahertz", R.string.unit_gigahertz, "GHz", "1000000000"),
            unit("rpm", R.string.unit_rpm, "rpm", fraction("1", "60"))
        )
    )

    private fun unit(
        id: String,
        @StringRes nameRes: Int,
        symbol: String,
        scale: String,
        offset: String = "0"
    ) = UnitDefinition(id, nameRes, symbol, decimal(scale), decimal(offset))

    private fun unit(
        id: String,
        @StringRes nameRes: Int,
        symbol: String,
        scale: BigDecimal,
        offset: BigDecimal = BigDecimal.ZERO
    ) = UnitDefinition(id, nameRes, symbol, scale, offset)
}