package org.partiql.planner.internal.typer.operator

import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.TestFactory
import org.partiql.planner.internal.typer.PartiQLTyperTestBase
import org.partiql.planner.internal.typer.accumulateSuccess
import org.partiql.planner.util.CastType
import org.partiql.planner.util.allDatePType
import org.partiql.planner.util.allIntervalDTType
import org.partiql.planner.util.allIntervalType
import org.partiql.planner.util.allIntervalYMType
import org.partiql.planner.util.allNumberPType
import org.partiql.planner.util.allSupportedPType
import org.partiql.planner.util.allTimePType
import org.partiql.planner.util.allTimeStampPType
import org.partiql.planner.util.cartesianProduct
import org.partiql.planner.util.castTablePType
import org.partiql.spi.types.PType
import java.util.stream.Stream

class OpArithmeticTest : PartiQLTyperTestBase() {
    @TestFactory
    fun mod(): Stream<DynamicContainer> {
        val tests = listOf(
            "expr-39"
        ).map { inputs.get("basics", it)!! }

        val argsMap: Map<TestResult, Set<List<PType>>> = buildMap {
            val successArgs = allNumberPType.let { cartesianProduct(it, it) }
            val failureArgs = cartesianProduct(
                allSupportedPType,
                allSupportedPType
            ).filterNot {
                successArgs.contains(it)
            }.toSet()

            successArgs.forEach { args: List<PType> ->
                val arg0 = args.first()
                val arg1 = args[1]
                val output = when {
                    arg0 == arg1 -> arg1
                    // TODO arg0 == StaticType.DECIMAL && arg1 == StaticType.FLOAT -> arg1 // TODO: The cast table is wrong. Honestly, it should be deleted.
                    // TODO arg1 == StaticType.DECIMAL && arg0 == StaticType.FLOAT -> arg0 // TODO: The cast table is wrong
                    castTablePType(arg1, arg0) == CastType.COERCION -> arg0
                    castTablePType(arg0, arg1) == CastType.COERCION -> arg1
                    else -> error("Arguments do not conform to parameters. Args: $args")
                }
                accumulateSuccess(output, args)
            }

            put(TestResult.Failure, failureArgs)
        }

        return super.testGenPType("arithmetic", tests, argsMap)
    }

    @TestFactory
    fun plus(): Stream<DynamicContainer> {
        val tests = listOf(
            "expr-37",
        ).map { inputs.get("basics", it)!! }

        val argsMap: Map<TestResult, Set<List<PType>>> = buildMap {
            val successArgs = (
                allNumberPType.let { cartesianProduct(it, it) } +
                    cartesianProduct(allDatePType, allIntervalType) +
                    cartesianProduct(allIntervalType, allDatePType) +
                    cartesianProduct(allTimePType, allIntervalDTType) +
                    cartesianProduct(allIntervalDTType, allTimePType) +
                    cartesianProduct(allTimeStampPType, allIntervalType) +
                    cartesianProduct(allIntervalType, allTimeStampPType) +
                    cartesianProduct(allIntervalYMType, allIntervalYMType) +
                    cartesianProduct(allIntervalDTType, allIntervalDTType)
                )
            val failureArgs = cartesianProduct(
                allSupportedPType,
                allSupportedPType
            ).filterNot {
                successArgs.contains(it)
            }.toSet()

            successArgs.forEach { args: List<PType> ->
                val arg0 = args.first()
                val arg1 = args[1]
                val output = when {
                    arg0 in allDatePType && arg1 in allDatePType -> PType.intervalDaySecond(9, 0)
                    arg0 in allDatePType && arg1 in allIntervalType -> arg0
                    arg0 in allIntervalType && arg1 in allDatePType -> arg1
                    arg0 in allTimePType && arg1 in allTimePType -> PType.intervalDaySecond(9, 0)
                    arg0 in allTimePType && arg1 in allIntervalType -> arg0
                    arg0 in allIntervalType && arg1 in allTimePType -> arg1
                    arg0 in allTimeStampPType && arg1 in allTimeStampPType -> PType.intervalDaySecond(9, 0)
                    arg0 in allTimeStampPType && arg1 in allIntervalType -> arg0
                    arg0 in allIntervalType && arg1 in allTimeStampPType -> arg1
                    arg0 in allIntervalYMType && arg1 in allIntervalYMType -> PType.intervalYearMonth(9)
                    arg0 in allIntervalDTType && arg1 in allIntervalDTType -> PType.intervalDaySecond(9, 0)
                    arg0 == arg1 -> arg1
                    // TODO arg0 == StaticType.DECIMAL && arg1 == StaticType.FLOAT -> arg1 // TODO: The cast table is wrong. Honestly, it should be deleted.
                    // TODO arg1 == StaticType.DECIMAL && arg0 == StaticType.FLOAT -> arg0 // TODO: The cast table is wrong
                    castTablePType(arg1, arg0) == CastType.COERCION -> arg0
                    castTablePType(arg0, arg1) == CastType.COERCION -> arg1
                    else -> error("Arguments do not conform to parameters. Args: $args")
                }
                accumulateSuccess(output, args)
            }

            put(TestResult.Failure, failureArgs)
        }
        return super.testGenPType("plus", tests, argsMap)
    }

    @TestFactory
    fun minus(): Stream<DynamicContainer> {
        val tests = listOf(
            "expr-38",
        ).map { inputs.get("basics", it)!! }

        val argsMap: Map<TestResult, Set<List<PType>>> = buildMap {
            val successArgs = (
                allNumberPType.let { cartesianProduct(it, it) } +
                    allDatePType.let { cartesianProduct(it, it) } +
                    allTimePType.let { cartesianProduct(it, it) } +
                    allTimeStampPType.let { cartesianProduct(it, it) } +
                    cartesianProduct(allDatePType, allIntervalType) +
                    cartesianProduct(allTimePType, allIntervalDTType) +
                    cartesianProduct(allTimeStampPType, allIntervalType) +
                    cartesianProduct(allIntervalYMType, allIntervalYMType) +
                    cartesianProduct(allIntervalDTType, allIntervalDTType) +
                    cartesianProduct(allDatePType, allTimeStampPType) +
                    cartesianProduct(allTimeStampPType, allDatePType)
                )
            val failureArgs = cartesianProduct(
                allSupportedPType,
                allSupportedPType
            ).filterNot {
                successArgs.contains(it)
            }.toSet()

            successArgs.forEach { args: List<PType> ->
                val arg0 = args.first()
                val arg1 = args[1]
                val output = when {
                    arg0 in allDatePType && arg1 in allDatePType -> PType.intervalDaySecond(9, 0)
                    arg0 in allDatePType && arg1 in allIntervalType -> arg0
                    arg0 in allTimePType && arg1 in allTimePType -> PType.intervalDaySecond(9, 0)
                    arg0 in allTimePType && arg1 in allIntervalType -> arg0
                    arg0 in allTimeStampPType && arg1 in allTimeStampPType -> PType.intervalDaySecond(9, 0)
                    arg0 in allTimeStampPType && arg1 in allIntervalType -> arg0
                    arg0 in allIntervalYMType && arg1 in allIntervalYMType -> PType.intervalYearMonth(9)
                    arg0 in allIntervalDTType && arg1 in allIntervalDTType -> PType.intervalDaySecond(9, 0)
                    arg0 == arg1 -> arg1
                    // TODO arg0 == StaticType.DECIMAL && arg1 == StaticType.FLOAT -> arg1 // TODO: The cast table is wrong. Honestly, it should be deleted.
                    // TODO arg1 == StaticType.DECIMAL && arg0 == StaticType.FLOAT -> arg0 // TODO: The cast table is wrong
                    castTablePType(arg1, arg0) == CastType.COERCION -> arg0
                    castTablePType(arg0, arg1) == CastType.COERCION -> arg1
                    arg0 in allDatePType && arg1 in allTimeStampPType -> PType.intervalDaySecond(9, 0)
                    arg0 in allTimeStampPType && arg1 in allDatePType -> PType.intervalDaySecond(9, 0)
                    else -> error("Arguments do not conform to parameters. Args: $args")
                }
                accumulateSuccess(output, args)
            }

            put(TestResult.Failure, failureArgs)
        }
        return super.testGenPType("minus", tests, argsMap)
    }

    @TestFactory
    fun times(): Stream<DynamicContainer> {
        val tests = listOf(
            "expr-40"
        ).map { inputs.get("basics", it)!! }

        val argsMap: Map<TestResult, Set<List<PType>>> = buildMap {
            val successArgs = (
                allNumberPType.let { cartesianProduct(it, it) } +
                    cartesianProduct(allNumberPType, allIntervalType) +
                    cartesianProduct(allIntervalType, allNumberPType)
                )
            val failureArgs = cartesianProduct(
                allSupportedPType,
                allSupportedPType
            ).filterNot {
                successArgs.contains(it)
            }.toSet()

            successArgs.forEach { args: List<PType> ->
                val arg0 = args.first()
                val arg1 = args[1]
                val output = when {
                    arg0 in allNumberPType && arg1 in allIntervalYMType -> PType.intervalYearMonth(9)
                    arg0 in allNumberPType && arg1 in allIntervalDTType -> PType.intervalDaySecond(9, 6)

                    arg0 in allIntervalYMType && arg1 in allNumberPType -> PType.intervalYearMonth(9)
                    arg0 in allIntervalDTType && arg1 in allNumberPType -> PType.intervalDaySecond(9, 6)
                    arg0 == arg1 -> arg1
                    castTablePType(arg1, arg0) == CastType.COERCION -> arg0
                    castTablePType(arg0, arg1) == CastType.COERCION -> arg1
                    else -> error("Arguments do not conform to parameters. Args: $args")
                }
                accumulateSuccess(output, args)
            }

            put(TestResult.Failure, failureArgs)
        }

        return super.testGenPType("arithmetic", tests, argsMap)
    }

    @TestFactory
    fun divide(): Stream<DynamicContainer> {
        val tests = listOf(
            "expr-41",
        ).map { inputs.get("basics", it)!! }

        val argsMap: Map<TestResult, Set<List<PType>>> = buildMap {
            val successArgs = (
                allNumberPType.let { cartesianProduct(it, it) } +
                    cartesianProduct(allIntervalType, allNumberPType)
                )
            val failureArgs = cartesianProduct(
                allSupportedPType,
                allSupportedPType
            ).filterNot {
                successArgs.contains(it)
            }.toSet()

            successArgs.forEach { args: List<PType> ->
                val arg0 = args.first()
                val arg1 = args[1]
                val output = when {
                    arg0 in allIntervalYMType && arg1 in allNumberPType -> PType.intervalYearMonth(9)
                    arg0 in allIntervalDTType && arg1 in allNumberPType -> PType.intervalDaySecond(9, 6)
                    arg0 == arg1 -> arg1
                    castTablePType(arg1, arg0) == CastType.COERCION -> arg0
                    castTablePType(arg0, arg1) == CastType.COERCION -> arg1
                    else -> error("Arguments do not conform to parameters. Args: $args")
                }
                accumulateSuccess(output, args)
            }

            put(TestResult.Failure, failureArgs)
        }
        return super.testGenPType("plus", tests, argsMap)
    }
}
