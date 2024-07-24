package org.partiql.eval.internal.operator.rex

import org.partiql.errors.TypeCheckException
import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.helpers.ArithmeticTyper
import org.partiql.eval.internal.operator.Operator
import org.partiql.eval.value.Datum
import org.partiql.plan.Ref
import org.partiql.types.PType
import org.partiql.types.PType.Kind
import java.math.BigDecimal
import java.math.BigInteger

internal class ExprArithmeticBinary<T>(
    val lhs: Operator.Expr,
    val rhs: Operator.Expr,
    val extract: (Datum) -> T,
    val toReturn: (T) -> Datum,
    val op: (T, T) -> T,
    val returnType: PType
) : Operator.Expr {

    override fun eval(env: Environment): Datum {
        val lhsEval = lhs.eval(env)
        val rhsEval = rhs.eval(env)
        if (lhsEval.isNull || rhsEval.isNull) return Datum.nullValue(returnType)
        if (lhsEval.isMissing || rhsEval.isMissing) throw TypeCheckException()
        return toReturn(op(extract(lhsEval), extract(rhsEval)))
    }

    internal interface Factory {

        fun add(lhs: Operator.Expr, rhs: Operator.Expr): Operator.Expr

        fun subtract(lhs: Operator.Expr, rhs: Operator.Expr): Operator.Expr

        fun modulo(lhs: Operator.Expr, rhs: Operator.Expr): Operator.Expr

        fun divide(lhs: Operator.Expr, rhs: Operator.Expr): Operator.Expr

        fun multiply(lhs: Operator.Expr, rhs: Operator.Expr): Operator.Expr

        abstract class Simple<T> : Factory {

            abstract val extract: (Datum) -> T
            abstract val toReturn: (T) -> Datum
            abstract val add: (T, T) -> T
            abstract val subtract: (T, T) -> T
            abstract val divide: (T, T) -> T
            abstract val multiply: (T, T) -> T
            abstract val modulo: (T, T) -> T
            abstract val returnType: PType

            override fun add(lhs: Operator.Expr, rhs: Operator.Expr): Operator.Expr {
                return ExprArithmeticBinary(lhs, rhs, extract, toReturn, add, returnType)
            }

            override fun subtract(lhs: Operator.Expr, rhs: Operator.Expr): Operator.Expr {
                return ExprArithmeticBinary(lhs, rhs, extract, toReturn, subtract, returnType)
            }

            override fun divide(lhs: Operator.Expr, rhs: Operator.Expr): Operator.Expr {
                return ExprArithmeticBinary(lhs, rhs, extract, toReturn, divide, returnType)
            }

            override fun multiply(lhs: Operator.Expr, rhs: Operator.Expr): Operator.Expr {
                return ExprArithmeticBinary(lhs, rhs, extract, toReturn, multiply, returnType)
            }

            override fun modulo(lhs: Operator.Expr, rhs: Operator.Expr): Operator.Expr {
                return ExprArithmeticBinary(lhs, rhs, extract, toReturn, modulo, returnType)
            }
        }

        object Int : Simple<kotlin.Int>() {
            override val extract: (Datum) -> kotlin.Int = Datum::getInt
            override val toReturn: (kotlin.Int) -> Datum = Datum::int32Value
            override val add: (kotlin.Int, kotlin.Int) -> kotlin.Int = kotlin.Int::plus
            override val subtract: (kotlin.Int, kotlin.Int) -> kotlin.Int = kotlin.Int::minus
            override val divide: (kotlin.Int, kotlin.Int) -> kotlin.Int = kotlin.Int::div
            override val multiply: (kotlin.Int, kotlin.Int) -> kotlin.Int = kotlin.Int::times
            override val modulo: (kotlin.Int, kotlin.Int) -> kotlin.Int = kotlin.Int::rem
            override val returnType: PType = PType.typeInt()
        }

        object BigInt : Simple<kotlin.Long>() {
            override val extract: (Datum) -> kotlin.Long = Datum::getLong
            override val toReturn: (kotlin.Long) -> Datum = Datum::int64Value
            override val add: (kotlin.Long, kotlin.Long) -> kotlin.Long = kotlin.Long::plus
            override val subtract: (kotlin.Long, kotlin.Long) -> kotlin.Long = kotlin.Long::minus
            override val divide: (kotlin.Long, kotlin.Long) -> kotlin.Long = kotlin.Long::div
            override val multiply: (kotlin.Long, kotlin.Long) -> kotlin.Long = kotlin.Long::times
            override val modulo: (kotlin.Long, kotlin.Long) -> kotlin.Long = kotlin.Long::rem
            override val returnType: PType = PType.typeBigInt()
        }

        object Short : Simple<kotlin.Short>() {
            override val extract: (Datum) -> kotlin.Short = Datum::getShort
            override val toReturn: (kotlin.Short) -> Datum = Datum::smallInt
            override val add: (kotlin.Short, kotlin.Short) -> kotlin.Short = { x, y -> (x + y).toShort() }
            override val subtract: (kotlin.Short, kotlin.Short) -> kotlin.Short = { x, y -> (x - y).toShort() }
            override val divide: (kotlin.Short, kotlin.Short) -> kotlin.Short = { x, y -> (x / y).toShort() }
            override val multiply: (kotlin.Short, kotlin.Short) -> kotlin.Short = { x, y -> (x * y).toShort() }
            override val modulo: (kotlin.Short, kotlin.Short) -> kotlin.Short = { x, y -> (x % y).toShort() }
            override val returnType: PType = PType.typeSmallInt()
        }

        object Float : Simple<kotlin.Float>() {
            override val extract: (Datum) -> kotlin.Float = Datum::getFloat
            override val toReturn: (kotlin.Float) -> Datum = Datum::real
            override val add: (kotlin.Float, kotlin.Float) -> kotlin.Float = kotlin.Float::plus
            override val subtract: (kotlin.Float, kotlin.Float) -> kotlin.Float = kotlin.Float::minus
            override val divide: (kotlin.Float, kotlin.Float) -> kotlin.Float = kotlin.Float::div
            override val multiply: (kotlin.Float, kotlin.Float) -> kotlin.Float = kotlin.Float::times
            override val modulo: (kotlin.Float, kotlin.Float) -> kotlin.Float = kotlin.Float::rem
            override val returnType: PType = PType.typeReal()
        }

        object Double : Simple<kotlin.Double>() {
            override val extract: (Datum) -> kotlin.Double = Datum::getDouble
            override val toReturn: (kotlin.Double) -> Datum = Datum::doublePrecision
            override val add: (kotlin.Double, kotlin.Double) -> kotlin.Double = kotlin.Double::plus
            override val subtract: (kotlin.Double, kotlin.Double) -> kotlin.Double = kotlin.Double::minus
            override val divide: (kotlin.Double, kotlin.Double) -> kotlin.Double = kotlin.Double::div
            override val multiply: (kotlin.Double, kotlin.Double) -> kotlin.Double = kotlin.Double::times
            override val modulo: (kotlin.Double, kotlin.Double) -> kotlin.Double = kotlin.Double::rem
            override val returnType: PType = PType.typeDoublePrecision()
        }

        object IntArbitrary : Simple<BigInteger>() {
            override val extract: (Datum) -> BigInteger = Datum::getBigInteger
            override val toReturn: (BigInteger) -> Datum = Datum::intArbitrary
            override val add: (BigInteger, BigInteger) -> BigInteger = BigInteger::plus
            override val subtract: (BigInteger, BigInteger) -> BigInteger = BigInteger::minus
            override val divide: (BigInteger, BigInteger) -> BigInteger = BigInteger::divide
            override val multiply: (BigInteger, BigInteger) -> BigInteger = BigInteger::times
            override val modulo: (BigInteger, BigInteger) -> BigInteger = BigInteger::rem
            override val returnType: PType = PType.typeIntArbitrary()
        }

        object DecimalArbitrary : Simple<BigDecimal>() {
            override val extract: (Datum) -> BigDecimal = Datum::getBigDecimal
            override val toReturn: (BigDecimal) -> Datum = Datum::decimalArbitrary
            override val add: (BigDecimal, BigDecimal) -> BigDecimal = BigDecimal::plus
            override val subtract: (BigDecimal, BigDecimal) -> BigDecimal = BigDecimal::minus
            override val divide: (BigDecimal, BigDecimal) -> BigDecimal = BigDecimal::divide
            override val multiply: (BigDecimal, BigDecimal) -> BigDecimal = BigDecimal::times
            override val modulo: (BigDecimal, BigDecimal) -> BigDecimal = BigDecimal::remainder
            override val returnType: PType = PType.typeDecimalArbitrary()
        }

        object Byte : Simple<kotlin.Byte>() {
            override val extract: (Datum) -> kotlin.Byte = Datum::getByte
            override val toReturn: (kotlin.Byte) -> Datum = Datum::tinyInt
            override val add: (kotlin.Byte, kotlin.Byte) -> kotlin.Byte = { x, y -> (x + y).toByte() }
            override val subtract: (kotlin.Byte, kotlin.Byte) -> kotlin.Byte = { x, y -> (x - y).toByte() }
            override val divide: (kotlin.Byte, kotlin.Byte) -> kotlin.Byte = { x, y -> (x / y).toByte() }
            override val multiply: (kotlin.Byte, kotlin.Byte) -> kotlin.Byte = { x, y -> (x * y).toByte() }
            override val modulo: (kotlin.Byte, kotlin.Byte) -> kotlin.Byte = { x, y -> (x % y).toByte() }
            override val returnType: PType = PType.typeTinyInt()
        }

        class Decimal(private val returnType: PType) : Factory {
            override fun add(lhs: Operator.Expr, rhs: Operator.Expr): Operator.Expr {
                return Decimal(lhs, rhs, returnType, BigDecimal::plus)
            }

            override fun subtract(lhs: Operator.Expr, rhs: Operator.Expr): Operator.Expr {
                return Decimal(lhs, rhs, returnType, BigDecimal::minus)
            }

            override fun multiply(lhs: Operator.Expr, rhs: Operator.Expr): Operator.Expr {
                return Decimal(lhs, rhs, returnType, BigDecimal::times)
            }

            override fun divide(lhs: Operator.Expr, rhs: Operator.Expr): Operator.Expr {
                return Decimal(lhs, rhs, returnType, BigDecimal::divide)
            }
            override fun modulo(lhs: Operator.Expr, rhs: Operator.Expr): Operator.Expr {
                return Decimal(lhs, rhs, returnType, BigDecimal::remainder)
            }
        }

        object Dynamic : Factory {
            override fun add(lhs: Operator.Expr, rhs: Operator.Expr): Operator.Expr {
                return Dynamic(lhs, rhs, Factory::add, ArithmeticTyper::add)
            }

            override fun subtract(lhs: Operator.Expr, rhs: Operator.Expr): Operator.Expr {
                return Dynamic(lhs, rhs, Factory::subtract, ArithmeticTyper::subtract)
            }

            override fun divide(lhs: Operator.Expr, rhs: Operator.Expr): Operator.Expr {
                return Dynamic(lhs, rhs, Factory::divide, ArithmeticTyper::divide)
            }

            override fun multiply(lhs: Operator.Expr, rhs: Operator.Expr): Operator.Expr {
                return Dynamic(lhs, rhs, Factory::multiply, ArithmeticTyper::multiply)
            }

            override fun modulo(lhs: Operator.Expr, rhs: Operator.Expr): Operator.Expr {
                return Dynamic(lhs, rhs, Factory::modulo, ArithmeticTyper::modulo)
            }
        }
    }

    internal class Decimal(
        private val lhs: Operator.Expr,
        private val rhs: Operator.Expr,
        private val returnType: PType,
        private val op: (BigDecimal, BigDecimal) -> BigDecimal
    ) : Operator.Expr {

        override fun eval(env: Environment): Datum {
            val lhsEval = lhs.eval(env)
            val rhsEval = rhs.eval(env)
            if (lhsEval.isNull || rhsEval.isNull) return Datum.nullValue(returnType)
            if (lhsEval.isMissing || rhsEval.isMissing) throw TypeCheckException()
            return Datum.decimal(op(lhsEval.bigDecimal, rhsEval.bigDecimal), returnType.precision, returnType.scale)
        }
    }

    internal class Dynamic(
        val lhs: Operator.Expr,
        val rhs: Operator.Expr,
        val op: (Factory, Operator.Expr, Operator.Expr) -> Operator.Expr,
        val type: (PType, PType) -> PType?
    ) : Operator.Expr {
        override fun eval(env: Environment): Datum {
            val lhsEval = lhs.eval(env)
            val rhsEval = rhs.eval(env)
            val returns = type(lhsEval.type, rhsEval.type) ?: throw TypeCheckException()
            val factory = when (returns.kind) {
                PType.Kind.TINYINT -> Factory.Byte
                PType.Kind.SMALLINT -> Factory.Short
                PType.Kind.INT -> Factory.Int
                PType.Kind.BIGINT -> Factory.BigInt
                PType.Kind.INT_ARBITRARY -> Factory.IntArbitrary
                PType.Kind.REAL -> Factory.Float
                PType.Kind.DOUBLE_PRECISION -> Factory.Double
                PType.Kind.DECIMAL -> Factory.Decimal(returns)
                PType.Kind.DECIMAL_ARBITRARY -> Factory.DecimalArbitrary
                else -> throw TypeCheckException()
            }
            return op(factory, lhsEval.coerce(lhsEval.type, returns), rhsEval.coerce(rhsEval.type, returns)).eval(env)
        }

        private fun Datum.coerce(input: PType, target: PType): Operator.Expr {
            if (input == target) return ExprLiteral(this)
            return ExprCast(ExprLiteral(this), Ref.Cast(input, target, isNullable = true))
        }
    }
}
