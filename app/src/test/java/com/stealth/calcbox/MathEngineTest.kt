package com.stealth.calcbox

import com.stealth.calcbox.calculator.MathEngine
import org.junit.Assert.assertEquals
import org.junit.Test

class MathEngineTest {

    @Test
    fun testBasicAdditionAndSubtraction() {
        assertEquals("5", MathEngine.evaluate("2+3"))
        assertEquals("6", MathEngine.evaluate("10-4"))
        assertEquals("50", MathEngine.evaluate("100-35-15"))
    }

    @Test
    fun testOperatorPrecedence() {
        assertEquals("14", MathEngine.evaluate("2+3*4"))
        assertEquals("4", MathEngine.evaluate("10-2*3"))
        assertEquals("11", MathEngine.evaluate("20/4+2*3"))
        assertEquals("7", MathEngine.evaluate("10+6/2-3*2"))
    }

    @Test
    fun testParentheses() {
        assertEquals("20", MathEngine.evaluate("(2+3)*4"))
        assertEquals("70", MathEngine.evaluate("2*(3+4)*5"))
        assertEquals("11", MathEngine.evaluate("((2+3)*2)+1"))
        assertEquals("16", MathEngine.evaluate("(10-2)*(8/(2+2))"))
        assertEquals("5", MathEngine.evaluate("(2+3"))
    }

    @Test
    fun testDecimalAndFloatingPoint() {
        assertEquals("0.3", MathEngine.evaluate("0.1+0.2"))
        assertEquals("11", MathEngine.evaluate("5.5*2"))
        assertEquals("7.3", MathEngine.evaluate("10.5-3.2"))
        assertEquals("0.625", MathEngine.evaluate("1.25*0.5"))
        assertEquals("3.5", MathEngine.evaluate("7/2"))
    }

    @Test
    fun testPercentageCalculations() {
        assertEquals("5", MathEngine.evaluate("50*10%"))
        assertEquals("110", MathEngine.evaluate("100+10%"))
        assertEquals("80", MathEngine.evaluate("100-20%"))
        assertEquals("2000", MathEngine.evaluate("200/10%"))
        assertEquals("0.25", MathEngine.evaluate("25%"))
    }

    @Test
    fun testNegativeNumbersAndUnaryMinus() {
        assertEquals("-2", MathEngine.evaluate("-5+3"))
        assertEquals("-10", MathEngine.evaluate("5*-2"))
        assertEquals("-5", MathEngine.evaluate("-(3+2)"))
        assertEquals("2", MathEngine.evaluate("5+(-3)"))
        assertEquals("5", MathEngine.evaluate("-10/-2"))
        assertEquals("-2", MathEngine.evaluate("-0.5*4"))
    }

    @Test
    fun testDivisionByZero() {
        assertEquals("Error", MathEngine.evaluate("5/0"))
        assertEquals("Error", MathEngine.evaluate("0/0"))
        assertEquals("Error", MathEngine.evaluate("10/(5-5)"))
    }

    @Test
    fun testUnicodeOperators() {
        assertEquals("42", MathEngine.evaluate("6×7"))
        assertEquals("5", MathEngine.evaluate("15÷3"))
        assertEquals("6", MathEngine.evaluate("10−4"))
    }

    @Test
    fun testInvalidAndIncompleteExpressions() {
        assertEquals("0", MathEngine.evaluate(""))
        assertEquals("Error", MathEngine.evaluate("++"))
        assertEquals("Error", MathEngine.evaluate("abc"))
        assertEquals("Error", MathEngine.evaluate("5..2"))
        assertEquals("Error", MathEngine.evaluate(")"))
    }

    @Test
    fun testNumberFormatting() {
        assertEquals("49", MathEngine.evaluate("49.0"))
        assertEquals("1000000000000", MathEngine.evaluate("1000000*1000000"))
        assertEquals("0", MathEngine.evaluate("0"))
        assertEquals("0", MathEngine.evaluate("0.0"))
    }
}
