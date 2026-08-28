package com.utilitybox.app

import com.utilitybox.app.tools.calculate.Counter
import com.utilitybox.app.tools.calculate.decodeCounters
import com.utilitybox.app.tools.calculate.encodeCounters
import com.utilitybox.app.tools.convert.offsetLabel
import com.utilitybox.app.tools.convert.zoneLabel
import com.utilitybox.app.tools.measure.exposureValue
import com.utilitybox.app.tools.measure.logScale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class TallyStorageTest {

    @Test
    fun `counters round trip`() {
        val counters = listOf(Counter("Boxes", 12), Counter("People", 0), Counter("Laps", -3))
        assertEquals(counters, decodeCounters(encodeCounters(counters)))
    }

    @Test
    fun `an empty set round trips to nothing`() {
        assertEquals(emptyList<Counter>(), decodeCounters(encodeCounters(emptyList())))
        assertEquals(emptyList<Counter>(), decodeCounters(""))
    }

    @Test
    fun `a name containing the separators cannot corrupt the record`() {
        val counters = listOf(Counter("Odd\tname\nhere", 5), Counter("Second", 1))
        val decoded = decodeCounters(encodeCounters(counters))
        assertEquals(2, decoded.size)
        assertEquals(5, decoded[0].value)
        assertEquals(1, decoded[1].value)
        assertTrue(decoded[0].name.none { it == '\t' || it == '\n' })
    }

    @Test
    fun `malformed lines are skipped rather than crashing`() {
        assertEquals(
            listOf(Counter("Good", 3)),
            decodeCounters("garbage\n\nnotanumber\tNope\n3\tGood\n\t"),
        )
    }
}

class WorldClockTest {

    @Test
    fun `zone ids read as place names`() {
        assertEquals("London — Europe", zoneLabel("Europe/London"))
        assertEquals("New York — America", zoneLabel("America/New_York"))
        assertEquals("UTC", zoneLabel("UTC"))
    }

    @Test
    fun `offsets are described relative to the reference`() {
        val instant = ZonedDateTime.of(2026, 1, 15, 12, 0, 0, 0, ZoneId.of("UTC"))
        val london = instant.withZoneSameInstant(ZoneId.of("Europe/London"))
        val tokyo = instant.withZoneSameInstant(ZoneId.of("Asia/Tokyo"))
        val newYork = instant.withZoneSameInstant(ZoneId.of("America/New_York"))

        assertEquals("same time", offsetLabel(london, london))
        assertEquals("+9h", offsetLabel(london, tokyo))
        assertEquals("−5h", offsetLabel(london, newYork))
    }

    @Test
    fun `half hour zones are described with their minutes`() {
        val instant = ZonedDateTime.of(2026, 1, 15, 12, 0, 0, 0, ZoneId.of("UTC"))
        val kolkata = instant.withZoneSameInstant(ZoneId.of("Asia/Kolkata"))
        assertEquals("+5h30", offsetLabel(instant, kolkata))
    }

    @Test
    fun `daylight saving is reflected in the offset`() {
        val winter = ZonedDateTime.of(2026, 1, 15, 12, 0, 0, 0, ZoneId.of("UTC"))
        val summer = ZonedDateTime.of(2026, 7, 15, 12, 0, 0, 0, ZoneId.of("UTC"))
        assertEquals("same time", offsetLabel(winter, winter.withZoneSameInstant(ZoneId.of("Europe/London"))))
        assertEquals("+1h", offsetLabel(summer, summer.withZoneSameInstant(ZoneId.of("Europe/London"))))
    }
}

class LightMeterTest {

    @Test
    fun `the brightness bar is logarithmic and bounded`() {
        assertEquals(0f, logScale(0f), 1e-6f)
        assertEquals(0f, logScale(-10f), 1e-6f)
        assertEquals(1f, logScale(100_000f), 1e-3f)
        assertTrue(logScale(500_000f) <= 1f)
    }

    @Test
    fun `a tenfold brightness change moves the bar by a similar step each time`() {
        val first = logScale(100f) - logScale(10f)
        val second = logScale(1_000f) - logScale(100f)
        assertEquals(first, second, 0.02f)
    }

    @Test
    fun `exposure value rises by one stop per doubling of light`() {
        assertEquals(1.0, exposureValue(1_000f) - exposureValue(500f), 1e-6)
        assertEquals(1.0, exposureValue(20_000f) - exposureValue(10_000f), 1e-6)
    }

    @Test
    fun `exposure value is finite in the dark rather than negative infinity`() {
        assertTrue(exposureValue(0f).isFinite())
    }
}
