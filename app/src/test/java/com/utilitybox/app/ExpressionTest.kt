package com.utilitybox.app

import com.utilitybox.app.util.EvalResult
import com.utilitybox.app.util.Expressions
import com.utilitybox.app.util.formatCalculatorResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpressionTest {

    private fun eval(input: String): Double {
        val result = Expressions.evaluate(input)
        assertTrue("Expected $input to evaluate, got $result", result is EvalResult.Ok)
        return (result as EvalResult.Ok).value
    }

    private fun error(input: String): String {
        val result = Expressions.evaluate(input)
        assertTrue("Expected $input to fail, got $result", result is EvalResult.Error)
        return (result as EvalResult.Error).message
    }

    @Test
    fun `basic arithmetic`() {
        assertEquals(7.0, eval("3+4"), 1e-9)
        assertEquals(-1.0, eval("3-4"), 1e-9)
        assertEquals(12.0, eval("3*4"), 1e-9)
        assertEquals(0.75, eval("3/4"), 1e-9)
    }

    @Test
    fun `multiplication binds tighter than addition`() {
        assertEquals(14.0, eval("2+3*4"), 1e-9)
        assertEquals(20.0, eval("(2+3)*4"), 1e-9)
    }

    @Test
    fun `subtraction is left associative`() {
        assertEquals(0.0, eval("10-5-5"), 1e-9)
    }

    @Test
    fun `division is left associative`() {
        assertEquals(2.0, eval("100/5/10"), 1e-9)
    }

    @Test
    fun `exponentiation is right associative`() {
        // 2^(3^2) = 512, not (2^3)^2 = 64
        assertEquals(512.0, eval("2^3^2"), 1e-9)
    }

    @Test
    fun `exponentiation binds tighter than multiplication`() {
        assertEquals(18.0, eval("2*3^2"), 1e-9)
    }

    @Test
    fun `unary minus works everywhere it should`() {
        assertEquals(-5.0, eval("-5"), 1e-9)
        assertEquals(-1.0, eval("2*-0.5"), 1e-9)
        assertEquals(5.0, eval("--5"), 1e-9)
        assertEquals(0.25, eval("2^-2"), 1e-9)
    }

    @Test
    fun `nested brackets`() {
        assertEquals(30.0, eval("((2+3)*(4+2))"), 1e-9)
    }

    @Test
    fun `implied multiplication`() {
        assertEquals(14.0, eval("2(3+4)"), 1e-9)
        assertEquals(8.0, eval("(1+1)(2+2)"), 1e-9)
    }

    @Test
    fun `decimals and spaces`() {
        assertEquals(3.5, eval("1.25 + 2.25"), 1e-9)
        assertEquals(10.0, eval("  2  *  5  "), 1e-9)
    }

    @Test
    fun `display operator glyphs are accepted`() {
        assertEquals(12.0, eval("3×4"), 1e-9)
        assertEquals(4.0, eval("8÷2"), 1e-9)
        assertEquals(1.0, eval("3−2"), 1e-9)
    }

    @Test
    fun `division by zero is reported rather than returning infinity`() {
        assertEquals("Division by zero", error("1/0"))
        assertEquals("Division by zero", error("5/(3-3)"))
    }

    @Test
    fun `malformed input is rejected`() {
        assertTrue(error("2+").isNotEmpty())
        assertTrue(error("(2+3").isNotEmpty())
        assertTrue(error("2+3)").isNotEmpty())
        assertTrue(error("abc").isNotEmpty())
        assertTrue(error("*5").isNotEmpty())
    }

    @Test
    fun `blank input is not an error message worth showing`() {
        assertEquals("", error(""))
        assertEquals("", error("   "))
    }

    @Test
    fun `result formatting hides floating point noise`() {
        assertEquals("0.3", formatCalculatorResult(0.1 + 0.2))
        assertEquals("3", formatCalculatorResult(3.0))
        assertEquals("0.5", formatCalculatorResult(0.5))
        assertEquals("-2.25", formatCalculatorResult(-2.25))
        assertEquals("0", formatCalculatorResult(0.0))
    }

    @Test
    fun `a long division renders readably`() {
        assertEquals("0.333333333", formatCalculatorResult(eval("1/3")))
    }
}
