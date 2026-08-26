package com.utilitybox.app

import com.utilitybox.app.tools.convert.morseToText
import com.utilitybox.app.tools.convert.textToMorse
import org.junit.Assert.assertEquals
import org.junit.Test

class MorseTest {

    @Test
    fun `sos encodes to the distress signal`() {
        assertEquals("... --- ...", textToMorse("SOS"))
    }

    @Test
    fun `encoding is case insensitive`() {
        assertEquals(textToMorse("hello"), textToMorse("HELLO"))
    }

    @Test
    fun `words are separated by a slash`() {
        assertEquals(".... .. / - .... . .-. .", textToMorse("hi there"))
    }

    @Test
    fun `decoding reverses encoding`() {
        val original = "THE QUICK BROWN FOX 123"
        assertEquals(original, morseToText(textToMorse(original)))
    }

    @Test
    fun `unknown morse sequences become a question mark`() {
        assertEquals("?", morseToText("........----"))
    }

    @Test
    fun `blank input produces blank output`() {
        assertEquals("", textToMorse("   "))
        assertEquals("", morseToText("   "))
    }

    @Test
    fun `punctuation survives a round trip`() {
        assertEquals("HELLO, WORLD!", morseToText(textToMorse("Hello, world!")))
    }
}
