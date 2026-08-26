package com.utilitybox.app

import com.utilitybox.app.tools.calculate.PassphraseWords
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PassphraseWordsTest {

    @Test
    fun `word count is a power of two so the entropy estimate is exact`() {
        val size = PassphraseWords.words.size
        assertEquals(0, size and (size - 1))
        assertEquals(size, 1 shl PassphraseWords.BITS_PER_WORD)
    }

    @Test
    fun `there are no duplicate words`() {
        assertEquals(
            PassphraseWords.words.size,
            PassphraseWords.words.distinct().size,
        )
    }

    @Test
    fun `words are lowercase letters only and easy to type`() {
        PassphraseWords.words.forEach { word ->
            assertTrue("'$word' is not plain lowercase", word.all { it in 'a'..'z' })
            assertTrue("'$word' is too short", word.length >= 3)
            assertTrue("'$word' is too long", word.length <= 9)
        }
    }

    @Test
    fun `a five word passphrase clears fifty bits of entropy`() {
        assertTrue(5 * PassphraseWords.BITS_PER_WORD >= 50)
    }
}
