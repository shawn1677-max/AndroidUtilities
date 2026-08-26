package com.utilitybox.app

import com.utilitybox.app.tools.convert.UnitCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UnitConversionTest {

    private fun category(name: String) =
        UnitCatalog.categories.first { it.name == name }

    private fun convert(categoryName: String, from: String, to: String, value: Double): Double {
        val category = category(categoryName)
        val source = category.units.first { it.symbol == from }
        val target = category.units.first { it.symbol == to }
        return target.fromBase(source.toBase(value))
    }

    @Test
    fun `one mile is 1609_344 metres`() {
        assertEquals(1609.344, convert("Length", "mi", "m", 1.0), 1e-9)
    }

    @Test
    fun `one inch is exactly 2_54 centimetres`() {
        assertEquals(2.54, convert("Length", "in", "cm", 1.0), 1e-9)
    }

    @Test
    fun `freezing point converts between all three temperature scales`() {
        assertEquals(32.0, convert("Temperature", "°C", "°F", 0.0), 1e-6)
        assertEquals(273.15, convert("Temperature", "K", "K", 273.15), 1e-6)
        assertEquals(0.0, convert("Temperature", "K", "°C", 273.15), 1e-6)
    }

    @Test
    fun `body temperature converts to fahrenheit`() {
        assertEquals(98.6, convert("Temperature", "°C", "°F", 37.0), 1e-6)
    }

    @Test
    fun `minus forty is the same in celsius and fahrenheit`() {
        assertEquals(-40.0, convert("Temperature", "°C", "°F", -40.0), 1e-6)
    }

    @Test
    fun `a gibibyte is 1073741824 bytes`() {
        assertEquals(1_073_741_824.0, convert("Digital storage", "GiB", "B", 1.0), 1e-3)
    }

    @Test
    fun `a gigabyte is smaller than a gibibyte`() {
        val decimal = convert("Digital storage", "GB", "B", 1.0)
        val binary = convert("Digital storage", "GiB", "B", 1.0)
        assertTrue(decimal < binary)
    }

    @Test
    fun `a pound is 453_59237 grams`() {
        assertEquals(453.59237, convert("Mass", "lb", "g", 1.0), 1e-6)
    }

    @Test
    fun `sixty miles per hour is 96_56064 kilometres per hour`() {
        assertEquals(96.56064, convert("Speed", "mph", "km/h", 60.0), 1e-6)
    }

    @Test
    fun `standard atmosphere is 1013_25 hectopascals`() {
        assertEquals(1013.25, convert("Pressure", "atm", "hPa", 1.0), 1e-6)
    }

    @Test
    fun `half a turn is 180 degrees`() {
        assertEquals(180.0, convert("Angle", "turn", "°", 0.5), 1e-9)
    }

    @Test
    fun `every unit round trips through its base unit`() {
        UnitCatalog.categories.forEach { category ->
            category.units.forEach { unit ->
                val value = 12.5
                val roundTripped = unit.fromBase(unit.toBase(value))
                assertEquals(
                    "${category.name}/${unit.name} did not round trip",
                    value,
                    roundTripped,
                    1e-6,
                )
            }
        }
    }

    @Test
    fun `unit symbols are unique within a category`() {
        UnitCatalog.categories.forEach { category ->
            val symbols = category.units.map { it.symbol }
            assertEquals(
                "${category.name} has duplicate symbols",
                symbols.size,
                symbols.distinct().size,
            )
        }
    }

    @Test
    fun `every category offers at least two units to convert between`() {
        UnitCatalog.categories.forEach { category ->
            assertTrue("${category.name} needs at least two units", category.units.size >= 2)
        }
    }
}
