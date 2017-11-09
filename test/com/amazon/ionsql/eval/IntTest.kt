package com.amazon.ionsql.eval

import junitparams.*
import org.junit.*
import java.math.*
import java.util.*

class IntTest : EvaluatorBase() {
    companion object {
        private val RANDOM = Random()

        @JvmStatic
        @BeforeClass
        fun setupRandomSeed() {
            val seed = System.nanoTime()

            println("IntTest seed = $seed, use it to reproduce failures")

            RANDOM.setSeed(seed)
        }
    }

    private val closeToMaxLong = (Long.MAX_VALUE - 1)
    private val closeToMinLong = (Long.MIN_VALUE + 1)

    private val bigInt = BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE)
    private val negativeBigInt = BigInteger.valueOf(Long.MIN_VALUE).minus(BigInteger.ONE)

    @Test
    @Parameters
    fun values(pair: Pair<String, String>) = assertPair(pair)

    fun parametersForValues(): List<Pair<String, String>> {
        val transform: (Number) -> Pair<String, String> = { i -> "$i" to "$i" }

        val parameters = mutableListOf<Pair<String, String>>()

        (1..20).map { RANDOM.nextInt() }.mapTo(parameters, transform)
        (1..20).map { RANDOM.nextLong() }.mapTo(parameters, transform)
        (1..20).map { i ->
            when (i % 2 == 0) {
                true  -> bigInt.add(BigInteger.valueOf(RANDOM.nextLong()))
                false -> negativeBigInt.minus(BigInteger.valueOf(RANDOM.nextLong()))
            }
        }.mapTo(parameters, transform)

        // defined manually
        parameters.add("$closeToMaxLong" to "$closeToMaxLong")
        parameters.add("$closeToMinLong" to "$closeToMinLong")
        parameters.add("$bigInt" to "$bigInt")
        parameters.add("$negativeBigInt" to "$negativeBigInt")
        parameters.add("`0x00ffFFffFFffFFff`" to "72057594037927935")

        return parameters
    }

    @Test
    @Parameters
    fun plus(pair: Pair<String, String>) = assertPair(pair)

    fun parametersForPlus(): List<Pair<String, String>> {
        val transform: (Triple<BigInteger, BigInteger, BigInteger>) -> Pair<String, String> =
            { (left, right, result) -> "$left + $right" to "$result" }

        val parameters = mutableListOf<Pair<String, String>>()

        (1..20).map {
            val left = BigInteger.valueOf(RANDOM.nextLong())
            val right = BigInteger.valueOf(RANDOM.nextLong())

            Triple(left, right, left.add(right))
        }.mapTo(parameters, transform)

        (1..20).map {
            val left = BigInteger.valueOf(RANDOM.nextLong()).add(bigInt)
            val right = BigInteger.valueOf(RANDOM.nextLong()).add(bigInt)

            Triple(left, right, left.add(right))
        }.mapTo(parameters, transform)

        // defined manually

        parameters.add("$closeToMaxLong + $closeToMaxLong" to "18446744073709551612")
        parameters.add("$closeToMaxLong + 123" to "9223372036854775929")
        parameters.add("$bigInt + 555" to "9223372036854776363")
        parameters.add("`0x00ffFFffFFffFFff` + `0x00ffFFffFFffFFff`" to "144115188075855870")

        return parameters
    }

    @Test
    @Parameters
    fun minus(pair: Pair<String, String>) = assertPair(pair)

    fun parametersForMinus(): List<Pair<String, String>> {
        val transform: (Triple<BigInteger, BigInteger, BigInteger>) -> Pair<String, String> =
            { (left, right, result) -> "$left - $right" to "$result" }

        val parameters = mutableListOf<Pair<String, String>>()

        (1..20).map {
            val left = BigInteger.valueOf(RANDOM.nextLong())
            val right = BigInteger.valueOf(RANDOM.nextLong())

            Triple(left, right, left.subtract(right))
        }.mapTo(parameters, transform)

        (1..20).map {
            val left = BigInteger.valueOf(RANDOM.nextLong()).add(negativeBigInt)
            val right = BigInteger.valueOf(RANDOM.nextLong()).add(bigInt)

            Triple(left, right, left.subtract(right))
        }.mapTo(parameters, transform)

        // defined manually

        parameters.add("-$closeToMaxLong - $closeToMaxLong" to "-18446744073709551612")
        parameters.add("$closeToMinLong - 123" to "-9223372036854775930")
        parameters.add("$negativeBigInt - 555" to "-9223372036854776364")
        parameters.add("-`0x00ffFFffFFffFFff` - `0x00ffFFffFFffFFff`" to "-144115188075855870")

        return parameters
    }

    @Test
    @Parameters
    fun times(pair: Pair<String, String>) = assertPair(pair)

    fun parametersForTimes(): List<Pair<String, String>> {
        val transform: (Triple<BigInteger, BigInteger, BigInteger>) -> Pair<String, String> =
            { (left, right, result) -> "$left * $right" to "$result" }

        val parameters = mutableListOf<Pair<String, String>>()

        (1..20).map {
            val left = BigInteger.valueOf(RANDOM.nextLong())
            val right = BigInteger.valueOf(RANDOM.nextLong())

            Triple(left, right, left.multiply(right))
        }.mapTo(parameters, transform)

        (1..20).map {
            val left = BigInteger.valueOf(RANDOM.nextLong()).add(negativeBigInt)
            val right = BigInteger.valueOf(RANDOM.nextLong()).add(bigInt)

            Triple(left, right, left.multiply(right))
        }.mapTo(parameters, transform)

        // defined manually

        parameters.add("$closeToMaxLong * $closeToMaxLong" to "85070591730234615828950163710522949636")
        parameters.add("$closeToMinLong * $closeToMaxLong" to "-85070591730234615838173535747377725442")
        parameters.add("$negativeBigInt * 555" to "-5118971480454400573995")
        parameters.add("`0x00ffFFffFFffFFff` * `0x00ffFFffFFffFFff`" to "5192296858534827484415308253364225")

        return parameters
    }

    @Test
    @Parameters
    fun cast(pair: Pair<String, String>) = assertPair(pair)

    private val oneFollowedBy300Zeros = "1${(1..300).fold("") { acc, _ -> acc + "0" }}"

    fun parametersForCast() = listOf("cast($bigInt as int)" to "$bigInt",
                                     "cast('$bigInt' as int)" to "$bigInt",
                                     "cast($negativeBigInt as int)" to "$negativeBigInt",
                                     "cast('$negativeBigInt' as int)" to "$negativeBigInt",
                                     "cast(1e300 as int)" to oneFollowedBy300Zeros)

    private fun assertPair(pair: Pair<String, String>) {
        val (query, expected) = pair
        assertEval(query, expected)
    }
}
