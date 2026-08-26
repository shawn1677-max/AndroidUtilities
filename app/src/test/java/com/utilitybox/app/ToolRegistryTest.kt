package com.utilitybox.app

import com.utilitybox.app.tools.ToolIds
import com.utilitybox.app.tools.ToolRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolRegistryTest {

    @Test
    fun `tool ids are unique`() {
        val ids = ToolRegistry.all.map { it.id }
        assertEquals(ids.size, ids.distinct().size)
    }

    @Test
    fun `every declared tool id appears in the registry exactly once`() {
        val declared = ToolIds::class.java.declaredFields
            .filter { it.type == String::class.java }
            .map {
                it.isAccessible = true
                it.get(ToolIds) as String
            }
        val registered = ToolRegistry.all.map { it.id }.toSet()
        assertEquals(
            "Every ToolIds constant needs a registry entry",
            declared.toSet(),
            registered,
        )
        assertEquals(declared.size, ToolRegistry.all.size)
    }

    @Test
    fun `every tool has a title and a subtitle`() {
        ToolRegistry.all.forEach { tool ->
            assertTrue("${tool.id} needs a title", tool.title.isNotBlank())
            assertTrue("${tool.id} needs a subtitle", tool.subtitle.isNotBlank())
        }
    }

    @Test
    fun `an empty query matches every tool`() {
        assertTrue(ToolRegistry.all.all { it.matches("") })
    }

    @Test
    fun `search finds tools by keyword rather than title alone`() {
        val torch = ToolRegistry.all.filter { it.matches("torch") }
        assertTrue(torch.any { it.id == ToolIds.FLASHLIGHT })

        val ip = ToolRegistry.all.filter { it.matches("ip") }
        assertTrue(ip.any { it.id == ToolIds.NETWORK })
    }

    @Test
    fun `search is case insensitive`() {
        assertEquals(
            ToolRegistry.all.filter { it.matches("BATTERY") },
            ToolRegistry.all.filter { it.matches("battery") },
        )
    }

    @Test
    fun `categories together cover every tool`() {
        val fromCategories = ToolRegistry.byCategory().flatMap { it.second }
        assertEquals(ToolRegistry.all.size, fromCategories.size)
        assertEquals(ToolRegistry.all.toSet(), fromCategories.toSet())
    }

    @Test
    fun `lookup by id returns the matching tool`() {
        ToolRegistry.all.forEach { tool ->
            assertEquals(tool, ToolRegistry.find(tool.id))
        }
    }
}
