package com.example.mathnotes
import java.math.BigDecimal
import java.math.MathContext
object MathEngine {
 private val mc = MathContext.DECIMAL64
 fun evaluate(expression:String):String? = try { Parser(expression.replace('×','*').replace('x','*').replace('X','*').replace('÷','/')).parse().stripTrailingZeros().toPlainString() } catch (_:Exception) { null }
 fun complete(text:String,cursor:Int):Pair<String,Int>? {
  if(cursor<=0 || cursor>text.length || text[cursor-1]!='=') return null
  val start = text.lastIndexOf('\n',cursor-1).let { if(it<0) 0 else it+1 }
  val prefix = text.substring(start,cursor-1)
  val match = Regex("[0-9(][0-9\\s.+\\-*/×÷()%]*$").find(prefix) ?: return null
  if (match.range.first > 0 && (prefix[match.range.first - 1].isLetterOrDigit() || prefix[match.range.first - 1] in "+-*/×÷(")) return null
  val result = evaluate(match.value.trim()) ?: return null
  val insertion = " $result"
  return (text.substring(0,cursor)+insertion+text.substring(cursor)) to (cursor+insertion.length)
 }
 private class Parser(val input:String) {
  var pos=0
  fun spaces(){while(pos<input.length && input[pos].isWhitespace())pos++}
  fun eat(c:Char):Boolean {spaces();if(pos<input.length && input[pos]==c){pos++;return true};return false}
  fun parse():BigDecimal {val x=sum();spaces();require(pos==input.length);return x}
  fun sum():BigDecimal {var x=product();while(true){x=when{eat('+')->x.add(product(),mc);eat('-')->x.subtract(product(),mc);else->return x}}}
  fun product():BigDecimal {var x=unary();while(true){x=when{eat('*')->x.multiply(unary(),mc);eat('/')->x.divide(unary(),mc);else->return x}}}
  fun unary():BigDecimal {if(eat('+'))return unary();if(eat('-'))return unary().negate();var x=atom();while(eat('%'))x=x.divide(BigDecimal(100),mc);return x}
  fun atom():BigDecimal {if(eat('(')){val x=sum();require(eat(')'));return x};spaces();val start=pos;while(pos<input.length && (input[pos].isDigit()||input[pos]=='.'))pos++;require(pos>start);return input.substring(start,pos).toBigDecimal()}
 }
}
