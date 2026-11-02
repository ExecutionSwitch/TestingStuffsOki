package com.example.mathnotes

import org.junit.Assert.*
import org.junit.Test

class MathEngineTest {
 @Test fun multiplicationAndParentheses() {
  assertEquals("675", MathEngine.evaluate("3+(8x7)x12"))
  assertEquals("675", MathEngine.evaluate("3+(8×7)×12"))
  assertEquals("675", MathEngine.evaluate("3+(8*7)*12"))
 }
 @Test fun basicArithmetic() {
  assertEquals("30", MathEngine.evaluate("25+5"))
  assertEquals("2.5", MathEngine.evaluate("10/4"))
  assertEquals("14", MathEngine.evaluate("2+3*4"))
  assertEquals("0.5", MathEngine.evaluate("50%"))
 }
 @Test fun inlineCompletion() {
  val input = "Milk: 25 + 5 ="
  assertEquals("Milk: 25 + 5 = 30", MathEngine.complete(input, input.length)?.first)
  val x = "3+(8x7)x12="
  assertEquals("3+(8x7)x12= 675", MathEngine.complete(x, x.length)?.first)
 }
 @Test fun rejectsBrokenAndPartialExpressions() {
  assertNull(MathEngine.evaluate("3+"))
  assertNull(MathEngine.evaluate("(3+2"))
  val partial = "wrongx12="
  assertNull(MathEngine.complete(partial,partial.length))
 }
}
