package com.stealth.calcbox.calculator

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.util.ArrayDeque

/**
 * Production-grade mathematical calculation engine implementing Dijkstra's Shunting-Yard
 * algorithm for infix-to-RPN conversion and RPN evaluation with high precision [BigDecimal].
 *
 * Supports:
 * - Basic operators: +, -, *, /, %
 * - Unicode aliases: ×, ÷, −
 * - Parentheses (including auto-closing unclosed parens)
 * - Negative numbers and unary minus
 * - Contextual percentage calculations (e.g. 100+10% = 110, 50*10% = 5)
 * - Precise decimal arithmetic without floating point artifacts
 * - Division by zero and error handling returning "Error"
 */
object MathEngine {

    private val MATH_CONTEXT = MathContext(16, RoundingMode.HALF_UP)

    private sealed class RawToken {
        data class Number(val text: String) : RawToken()
        data class Op(val char: Char) : RawToken()
        object OpenParen : RawToken()
        object CloseParen : RawToken()
        object Percent : RawToken()
    }

    private sealed class Token {
        data class Number(val value: BigDecimal) : Token()
        data class Operator(val symbol: Char, val isUnary: Boolean = false) : Token()
        object LeftParen : Token()
        object RightParen : Token()
    }

    /**
     * Evaluates a mathematical expression string and returns the formatted result.
     * Returns "Error" for invalid syntax, division by zero, or calculation errors.
     */
    fun evaluate(expression: String): String {
        val trimmed = expression.trim()
        if (trimmed.isEmpty()) return "0"

        return try {
            val normalized = normalize(trimmed)
            if (normalized.isEmpty()) return "0"

            val rawTokens = tokenize(normalized)
            if (rawTokens.isEmpty()) return "0"

            validateRawTokens(rawTokens)
            val percentExpandedTokens = expandPercentages(rawTokens)
            val tokens = resolveUnaryOperators(percentExpandedTokens)
            val rpn = toRpn(tokens)
            val result = evaluateRpn(rpn)
            formatResult(result)
        } catch (e: Exception) {
            "Error"
        }
    }

    private fun normalize(expr: String): String {
        return expr
            .replace('×', '*')
            .replace('÷', '/')
            .replace('−', '-') // Unicode minus \u2212
            .replace(" ", "")
    }

    private fun tokenize(expr: String): List<RawToken> {
        val tokens = mutableListOf<RawToken>()
        var i = 0
        while (i < expr.length) {
            val c = expr[i]
            when {
                c.isDigit() || c == '.' -> {
                    val sb = StringBuilder()
                    var dotCount = 0
                    while (i < expr.length && (expr[i].isDigit() || expr[i] == '.')) {
                        if (expr[i] == '.') dotCount++
                        sb.append(expr[i])
                        i++
                    }
                    if (dotCount > 1) throw IllegalArgumentException("Multiple decimal points")
                    val str = sb.toString()
                    if (str == ".") throw IllegalArgumentException("Lone decimal point")
                    tokens.add(RawToken.Number(str))
                    continue
                }
                c == '+' || c == '-' || c == '*' || c == '/' -> {
                    tokens.add(RawToken.Op(c))
                }
                c == '%' -> {
                    tokens.add(RawToken.Percent)
                }
                c == '(' -> {
                    tokens.add(RawToken.OpenParen)
                }
                c == ')' -> {
                    tokens.add(RawToken.CloseParen)
                }
                else -> {
                    throw IllegalArgumentException("Invalid character: $c")
                }
            }
            i++
        }
        return tokens
    }

    private fun validateRawTokens(tokens: List<RawToken>) {
        if (tokens.isEmpty()) return

        // First token validation: only '-', '(', or Number are permitted at start
        val first = tokens.first()
        if (first is RawToken.Op && first.char != '-') {
            throw IllegalArgumentException("Invalid starting operator: ${first.char}")
        }
        if (first is RawToken.Percent || first is RawToken.CloseParen) {
            throw IllegalArgumentException("Invalid starting token: $first")
        }

        // Last token validation: cannot end with binary/unary op or OpenParen
        val last = tokens.last()
        if (last is RawToken.Op || last is RawToken.OpenParen) {
            throw IllegalArgumentException("Incomplete expression ending with: $last")
        }

        // Consecutive operator validation
        for (i in 0 until tokens.size - 1) {
            val curr = tokens[i]
            val next = tokens[i + 1]

            if (curr is RawToken.Op) {
                if (next is RawToken.Op) {
                    // Only '-' is allowed after an operator (acting as unary minus)
                    if (next.char != '-') {
                        throw IllegalArgumentException("Invalid consecutive operators: ${curr.char}${next.char}")
                    }
                    // Cannot have three consecutive operators
                    if (i + 2 < tokens.size && tokens[i + 2] is RawToken.Op) {
                        throw IllegalArgumentException("Triple consecutive operators")
                    }
                } else if (next is RawToken.Percent || next is RawToken.CloseParen) {
                    throw IllegalArgumentException("Operator followed by invalid token: ${curr.char} followed by $next")
                }
            } else if (curr is RawToken.OpenParen) {
                if (next is RawToken.Op && next.char != '-') {
                    throw IllegalArgumentException("Open paren followed by invalid operator: ${next.char}")
                }
                if (next is RawToken.Percent || next is RawToken.CloseParen) {
                    throw IllegalArgumentException("Open paren followed by invalid token: $next")
                }
            }
        }
    }

    private fun expandPercentages(tokens: List<RawToken>): List<RawToken> {
        var current = tokens.toMutableList()
        var idx = current.indexOfFirst { it is RawToken.Percent }

        while (idx != -1) {
            if (idx == 0) throw IllegalArgumentException("Percent at start")

            val bEnd = idx - 1
            val bStart: Int
            if (current[bEnd] is RawToken.CloseParen) {
                var depth = 1
                var j = bEnd - 1
                while (j >= 0 && depth > 0) {
                    if (current[j] is RawToken.CloseParen) depth++
                    if (current[j] is RawToken.OpenParen) depth--
                    if (depth == 0) break
                    j--
                }
                if (depth != 0) throw IllegalArgumentException("Mismatched parens in percent operand")
                bStart = j
            } else if (current[bEnd] is RawToken.Number) {
                bStart = bEnd
            } else {
                throw IllegalArgumentException("Invalid percent operand before %")
            }
            val bTokens = current.subList(bStart, bEnd + 1)

            val opBeforeBIndex = bStart - 1
            var isAddOrSub = false
            var aTokens: List<RawToken>? = null

            if (opBeforeBIndex >= 0) {
                val opToken = current[opBeforeBIndex]
                if (opToken is RawToken.Op && (opToken.char == '+' || opToken.char == '-')) {
                    if (opBeforeBIndex > 0 && (current[opBeforeBIndex - 1] is RawToken.Number || current[opBeforeBIndex - 1] is RawToken.CloseParen)) {
                        isAddOrSub = true
                        var depth = 0
                        var aStart = 0
                        for (k in (opBeforeBIndex - 1) downTo 0) {
                            val tok = current[k]
                            if (tok is RawToken.CloseParen) depth++
                            else if (tok is RawToken.OpenParen) depth--
                            else if (depth == 0 && tok is RawToken.Op && (tok.char == '+' || tok.char == '-')) {
                                aStart = k + 1
                                break
                            }
                        }
                        aTokens = current.subList(aStart, opBeforeBIndex)
                    }
                }
            }

            val replacement = mutableListOf<RawToken>()
            replacement.add(RawToken.OpenParen)
            if (isAddOrSub && aTokens != null && aTokens.isNotEmpty()) {
                replacement.add(RawToken.OpenParen)
                replacement.addAll(aTokens)
                replacement.add(RawToken.CloseParen)
                replacement.add(RawToken.Op('*'))
            }
            replacement.add(RawToken.OpenParen)
            replacement.addAll(bTokens)
            replacement.add(RawToken.CloseParen)
            replacement.add(RawToken.Op('/'))
            replacement.add(RawToken.Number("100"))
            replacement.add(RawToken.CloseParen)

            val nextList = mutableListOf<RawToken>()
            nextList.addAll(current.subList(0, bStart))
            nextList.addAll(replacement)
            nextList.addAll(current.subList(idx + 1, current.size))
            current = nextList

            idx = current.indexOfFirst { it is RawToken.Percent }
        }
        return current
    }

    private fun resolveUnaryOperators(tokens: List<RawToken>): List<Token> {
        val result = mutableListOf<Token>()
        for (i in tokens.indices) {
            when (val raw = tokens[i]) {
                is RawToken.Number -> {
                    result.add(Token.Number(BigDecimal(raw.text)))
                }
                is RawToken.OpenParen -> result.add(Token.LeftParen)
                is RawToken.CloseParen -> result.add(Token.RightParen)
                is RawToken.Op -> {
                    val isUnary = (raw.char == '-') && (i == 0 || tokens[i - 1] is RawToken.Op || tokens[i - 1] is RawToken.OpenParen)
                    result.add(Token.Operator(raw.char, isUnary = isUnary))
                }
                is RawToken.Percent -> throw IllegalArgumentException("Unexpanded percent token")
            }
        }
        return result
    }

    private fun toRpn(tokens: List<Token>): List<Token> {
        val output = mutableListOf<Token>()
        val opStack = ArrayDeque<Token>()

        fun precedence(token: Token.Operator): Int {
            if (token.isUnary) return 3
            return when (token.symbol) {
                '*', '/' -> 2
                '+', '-' -> 1
                else -> 0
            }
        }

        for (token in tokens) {
            when (token) {
                is Token.Number -> output.add(token)
                is Token.LeftParen -> opStack.addFirst(token)
                is Token.RightParen -> {
                    var foundLeftParen = false
                    while (opStack.isNotEmpty()) {
                        val top = opStack.removeFirst()
                        if (top is Token.LeftParen) {
                            foundLeftParen = true
                            break
                        } else {
                            output.add(top)
                        }
                    }
                    if (!foundLeftParen) throw IllegalArgumentException("Mismatched parentheses")
                }
                is Token.Operator -> {
                    val pCurr = precedence(token)
                    val isRightAssoc = token.isUnary
                    while (opStack.isNotEmpty()) {
                        val top = opStack.first()
                        if (top is Token.Operator) {
                            val pTop = precedence(top)
                            if ((!isRightAssoc && pCurr <= pTop) || (isRightAssoc && pCurr < pTop)) {
                                output.add(opStack.removeFirst())
                            } else {
                                break
                            }
                        } else {
                            break
                        }
                    }
                    opStack.addFirst(token)
                }
            }
        }

        // Flush remaining operators (auto-close any unclosed parens)
        while (opStack.isNotEmpty()) {
            val top = opStack.removeFirst()
            if (top !is Token.LeftParen) {
                output.add(top)
            }
        }

        return output
    }

    private fun evaluateRpn(rpn: List<Token>): BigDecimal {
        val stack = ArrayDeque<BigDecimal>()
        for (token in rpn) {
            when (token) {
                is Token.Number -> stack.addFirst(token.value)
                is Token.Operator -> {
                    if (token.isUnary) {
                        if (stack.isEmpty()) throw IllegalArgumentException("Missing operand for unary minus")
                        val a = stack.removeFirst()
                        stack.addFirst(a.negate())
                    } else {
                        if (stack.size < 2) throw IllegalArgumentException("Missing operands for ${token.symbol}")
                        val b = stack.removeFirst()
                        val a = stack.removeFirst()
                        val res = when (token.symbol) {
                            '+' -> a.add(b, MATH_CONTEXT)
                            '-' -> a.subtract(b, MATH_CONTEXT)
                            '*' -> a.multiply(b, MATH_CONTEXT)
                            '/' -> {
                                if (b.compareTo(BigDecimal.ZERO) == 0) {
                                    throw ArithmeticException("Division by zero")
                                }
                                a.divide(b, MATH_CONTEXT)
                            }
                            else -> throw IllegalArgumentException("Unknown operator: ${token.symbol}")
                        }
                        stack.addFirst(res)
                    }
                }
                else -> throw IllegalArgumentException("Unexpected token in RPN: $token")
            }
        }
        if (stack.size != 1) throw IllegalArgumentException("Invalid expression: final stack size = ${stack.size}")
        return stack.first()
    }

    private fun formatResult(result: BigDecimal): String {
        if (result.compareTo(BigDecimal.ZERO) == 0) return "0"
        val stripped = result.stripTrailingZeros()
        return stripped.toPlainString()
    }
}
