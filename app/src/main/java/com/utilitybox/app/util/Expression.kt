package com.utilitybox.app.util

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/** Outcome of evaluating a typed expression. */
sealed interface EvalResult {
    data class Ok(val value: Double) : EvalResult
    data class Error(val message: String) : EvalResult
}

/**
 * A small recursive-descent evaluator for the calculator.
 *
 * Grammar, tightest binding last:
 *
 *     expression := term (('+' | '-') term)*
 *     term       := unary (('*' | '/') unary)*
 *     unary      := ('-' | '+')* power
 *     power      := primary ('^' unary)?        // right associative
 *     primary    := number | '(' expression ')'
 *
 * Multiplication may be implied, so "2(3+4)" and "(1+1)(2+2)" both parse.
 * There is deliberately no '%': in a calculator it is ambiguous between modulo
 * and percent, and the Percentages tool covers the percent case properly.
 */
object Expressions {

    fun evaluate(input: String): EvalResult {
        val text = input.replace('×', '*').replace('÷', '/').replace('−', '-')
        if (text.isBlank()) return EvalResult.Error("")
        return try {
            val parser = Parser(text)
            val value = parser.parseExpression()
            parser.expectEnd()
            when {
                value.isNaN() -> EvalResult.Error("Not a number")
                value.isInfinite() -> EvalResult.Error("Division by zero")
                else -> EvalResult.Ok(value)
            }
        } catch (error: ParseException) {
            EvalResult.Error(error.message ?: "Invalid expression")
        }
    }

    private class ParseException(message: String) : Exception(message)

    private class Parser(private val text: String) {
        private var position = 0

        fun parseExpression(): Double {
            var value = parseTerm()
            while (true) {
                when (peek()) {
                    '+' -> { position++; value += parseTerm() }
                    '-' -> { position++; value -= parseTerm() }
                    else -> return value
                }
            }
        }

        private fun parseTerm(): Double {
            var value = parseUnary()
            while (true) {
                when {
                    peek() == '*' -> { position++; value *= parseUnary() }
                    peek() == '/' -> {
                        position++
                        val divisor = parseUnary()
                        if (divisor == 0.0) throw ParseException("Division by zero")
                        value /= divisor
                    }
                    // Implied multiplication: a number or '(' directly after a value.
                    peek() == '(' -> value *= parseUnary()
                    else -> return value
                }
            }
        }

        private fun parseUnary(): Double = when (peek()) {
            '-' -> { position++; -parseUnary() }
            '+' -> { position++; parseUnary() }
            else -> parsePower()
        }

        private fun parsePower(): Double {
            val base = parsePrimary()
            if (peek() == '^') {
                position++
                // Right associative, and the exponent may itself be signed.
                return Math.pow(base, parseUnary())
            }
            return base
        }

        private fun parsePrimary(): Double {
            skipSpaces()
            when (peek()) {
                '(' -> {
                    position++
                    val value = parseExpression()
                    skipSpaces()
                    if (peek() != ')') throw ParseException("Missing closing bracket")
                    position++
                    return value
                }

                null -> throw ParseException("Unfinished expression")
            }
            return parseNumber()
        }

        private fun parseNumber(): Double {
            skipSpaces()
            val start = position
            var seenDot = false
            while (true) {
                val character = peek() ?: break
                if (character.isDigit()) {
                    position++
                } else if (character == '.' && !seenDot) {
                    seenDot = true
                    position++
                } else {
                    break
                }
            }
            if (position == start) throw ParseException("Invalid expression")
            val literal = text.substring(start, position)
            return literal.toDoubleOrNull() ?: throw ParseException("Invalid number")
        }

        private fun peek(): Char? {
            skipSpaces()
            return text.getOrNull(position)
        }

        private fun skipSpaces() {
            while (position < text.length && text[position] == ' ') position++
        }

        fun expectEnd() {
            if (peek() != null) {
                throw ParseException(
                    if (peek() == ')') "Unmatched closing bracket" else "Invalid expression"
                )
            }
        }
    }
}

/**
 * Calculator-style formatting: enough precision to be useful, without the
 * floating point noise of a raw toString (0.1 + 0.2 should read as 0.3).
 */
fun formatCalculatorResult(value: Double): String {
    if (value.isNaN() || value.isInfinite()) return "—"
    if (value == 0.0) return "0"
    val magnitude = kotlin.math.abs(value)
    if (magnitude >= 1e12 || magnitude < 1e-9) {
        return String.format(java.util.Locale.US, "%.6g", value)
    }
    return BigDecimal(value)
        .round(MathContext(12))
        .setScale(9, RoundingMode.HALF_UP)
        .stripTrailingZeros()
        .toPlainString()
}
