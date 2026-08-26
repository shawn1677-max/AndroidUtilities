package com.utilitybox.app.tools.convert

/**
 * A unit expressed as a linear transform to a category's base unit:
 * base = value * factor + offset. Only temperature needs a non-zero offset.
 */
data class UnitDef(
    val name: String,
    val symbol: String,
    val factor: Double,
    val offset: Double = 0.0,
) {
    fun toBase(value: Double): Double = value * factor + offset
    fun fromBase(base: Double): Double = (base - offset) / factor
}

data class UnitCategory(val name: String, val units: List<UnitDef>)

object UnitCatalog {

    val categories: List<UnitCategory> = listOf(
        UnitCategory(
            "Length",
            listOf(
                UnitDef("Millimetre", "mm", 0.001),
                UnitDef("Centimetre", "cm", 0.01),
                UnitDef("Metre", "m", 1.0),
                UnitDef("Kilometre", "km", 1000.0),
                UnitDef("Inch", "in", 0.0254),
                UnitDef("Foot", "ft", 0.3048),
                UnitDef("Yard", "yd", 0.9144),
                UnitDef("Mile", "mi", 1609.344),
                UnitDef("Nautical mile", "nmi", 1852.0),
                UnitDef("Micrometre", "µm", 1e-6),
            ),
        ),
        UnitCategory(
            "Mass",
            listOf(
                UnitDef("Milligram", "mg", 1e-6),
                UnitDef("Gram", "g", 0.001),
                UnitDef("Kilogram", "kg", 1.0),
                UnitDef("Tonne", "t", 1000.0),
                UnitDef("Ounce", "oz", 0.028349523125),
                UnitDef("Pound", "lb", 0.45359237),
                UnitDef("Stone", "st", 6.35029318),
                UnitDef("US ton", "ton", 907.18474),
            ),
        ),
        UnitCategory(
            "Temperature",
            listOf(
                UnitDef("Celsius", "°C", 1.0, 273.15),
                UnitDef("Fahrenheit", "°F", 5.0 / 9.0, 255.372222222222),
                UnitDef("Kelvin", "K", 1.0, 0.0),
            ),
        ),
        UnitCategory(
            "Area",
            listOf(
                UnitDef("Square metre", "m²", 1.0),
                UnitDef("Square centimetre", "cm²", 0.0001),
                UnitDef("Square kilometre", "km²", 1e6),
                UnitDef("Hectare", "ha", 10_000.0),
                UnitDef("Square foot", "ft²", 0.09290304),
                UnitDef("Square yard", "yd²", 0.83612736),
                UnitDef("Acre", "ac", 4046.8564224),
                UnitDef("Square mile", "mi²", 2_589_988.110336),
            ),
        ),
        UnitCategory(
            "Volume",
            listOf(
                UnitDef("Millilitre", "ml", 0.001),
                UnitDef("Litre", "L", 1.0),
                UnitDef("Cubic metre", "m³", 1000.0),
                UnitDef("US teaspoon", "tsp", 0.00492892159375),
                UnitDef("US tablespoon", "tbsp", 0.01478676478125),
                UnitDef("US fluid ounce", "fl oz", 0.0295735295625),
                UnitDef("US cup", "cup", 0.2365882365),
                UnitDef("US pint", "pt", 0.473176473),
                UnitDef("US gallon", "gal", 3.785411784),
                UnitDef("Imperial pint", "imp pt", 0.56826125),
                UnitDef("Imperial gallon", "imp gal", 4.54609),
            ),
        ),
        UnitCategory(
            "Speed",
            listOf(
                UnitDef("Metres per second", "m/s", 1.0),
                UnitDef("Kilometres per hour", "km/h", 1.0 / 3.6),
                UnitDef("Miles per hour", "mph", 0.44704),
                UnitDef("Knot", "kn", 0.514444444444444),
                UnitDef("Feet per second", "ft/s", 0.3048),
            ),
        ),
        UnitCategory(
            "Time",
            listOf(
                UnitDef("Millisecond", "ms", 0.001),
                UnitDef("Second", "s", 1.0),
                UnitDef("Minute", "min", 60.0),
                UnitDef("Hour", "h", 3600.0),
                UnitDef("Day", "d", 86_400.0),
                UnitDef("Week", "wk", 604_800.0),
                UnitDef("Year (365 d)", "yr", 31_536_000.0),
            ),
        ),
        UnitCategory(
            "Digital storage",
            listOf(
                UnitDef("Bit", "b", 0.125),
                UnitDef("Byte", "B", 1.0),
                UnitDef("Kilobyte (1000)", "kB", 1e3),
                UnitDef("Megabyte (1000)", "MB", 1e6),
                UnitDef("Gigabyte (1000)", "GB", 1e9),
                UnitDef("Terabyte (1000)", "TB", 1e12),
                UnitDef("Kibibyte (1024)", "KiB", 1024.0),
                UnitDef("Mebibyte (1024)", "MiB", 1_048_576.0),
                UnitDef("Gibibyte (1024)", "GiB", 1_073_741_824.0),
                UnitDef("Tebibyte (1024)", "TiB", 1_099_511_627_776.0),
            ),
        ),
        UnitCategory(
            "Pressure",
            listOf(
                UnitDef("Pascal", "Pa", 1.0),
                UnitDef("Hectopascal", "hPa", 100.0),
                UnitDef("Kilopascal", "kPa", 1000.0),
                UnitDef("Bar", "bar", 100_000.0),
                UnitDef("Millibar", "mbar", 100.0),
                UnitDef("PSI", "psi", 6894.757293168),
                UnitDef("Atmosphere", "atm", 101_325.0),
                UnitDef("Millimetre of mercury", "mmHg", 133.322387415),
            ),
        ),
        UnitCategory(
            "Energy",
            listOf(
                UnitDef("Joule", "J", 1.0),
                UnitDef("Kilojoule", "kJ", 1000.0),
                UnitDef("Calorie", "cal", 4.184),
                UnitDef("Kilocalorie", "kcal", 4184.0),
                UnitDef("Watt hour", "Wh", 3600.0),
                UnitDef("Kilowatt hour", "kWh", 3_600_000.0),
                UnitDef("Foot-pound", "ft·lb", 1.3558179483314),
                UnitDef("BTU", "BTU", 1055.05585262),
            ),
        ),
        UnitCategory(
            "Angle",
            listOf(
                UnitDef("Degree", "°", 1.0),
                UnitDef("Radian", "rad", 57.29577951308232),
                UnitDef("Gradian", "grad", 0.9),
                UnitDef("Arcminute", "′", 1.0 / 60.0),
                UnitDef("Arcsecond", "″", 1.0 / 3600.0),
                UnitDef("Turn", "turn", 360.0),
            ),
        ),
    )
}
