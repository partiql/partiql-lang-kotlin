package org.partiql.eval.internal.operator.rex

import org.partiql.errors.TypeCheckException
import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.helpers.ArithmeticTyper
import org.partiql.eval.internal.operator.Operator
import org.partiql.eval.value.Datum
import org.partiql.plan.Ref
import org.partiql.types.PType
import java.math.BigDecimal
import java.math.BigInteger

internal class ExprArithmeticUnary<T>(
    val arg: Operator.Expr,
    val extract: (Datum) -> T,
    val toReturn: (T) -> Datum,
    val op: (T) -> T,
    val returnType: PType
) : Operator.Expr {

    override fun eval(env: Environment): Datum {
        val argEval = arg.eval(env)
        if (argEval.isNull) return Datum.nullValue(returnType)
        if (argEval.isMissing) throw TypeCheckException()
        return toReturn(op(extract(argEval)))
    }

    internal interface Factory {

        fun positive(arg: Operator.Expr): Operator.Expr

        fun negative(arg: Operator.Expr): Operator.Expr

        abstract class Simple<T> : Factory {

            abstract val extract: (Datum) -> T
            abstract val toReturn: (T) -> Datum
            abstract val positive: (T) -> T
            abstract val negative: (T) -> T
            abstract val returnType: PType

            override fun positive(arg: Operator.Expr): Operator.Expr {
                return ExprArithmeticUnary(arg, extract, toReturn, positive, returnType)
            }

            override fun negative(arg: Operator.Expr): Operator.Expr {
                return ExprArithmeticUnary(arg, extract, toReturn, negative, returnType)
            }
        }

        object Int : Simple<kotlin.Int>() {
            override val extract: (Datum) -> kotlin.Int = Datum::getInt
            override val toReturn: (kotlin.Int) -> Datum = Datum::int32Value
            override val positive: (kotlin.Int) -> kotlin.Int = kotlin.Int::unaryPlus
            override val negative: (kotlin.Int) -> kotlin.Int = kotlin.Int::unaryMinus
            override val returnType: PType = PType.typeInt()
        }

        object BigInt : Simple<kotlin.Long>() {
            override val extract: (Datum) -> kotlin.Long = Datum::getLong
            override val toReturn: (kotlin.Long) -> Datum = Datum::int64Value
            override val positive: (kotlin.Long) -> kotlin.Long = kotlin.Long::unaryPlus
            override val negative: (kotlin.Long) -> kotlin.Long = kotlin.Long::unaryMinus
            override val returnType: PType = PType.typeBigInt()
        }

        object Short : Simple<kotlin.Short>() {
            override val extract: (Datum) -> kotlin.Short = Datum::getShort
            override val toReturn: (kotlin.Short) -> Datum = Datum::smallInt
            override val positive: (kotlin.Short) -> kotlin.Short = { x -> x }
            override val negative: (kotlin.Short) -> kotlin.Short = { x -> (x * -1).toShort() }
            override val returnType: PType = PType.typeSmallInt()
        }

        object Float : Simple<kotlin.Float>() {
            override val extract: (Datum) -> kotlin.Float = Datum::getFloat
            override val toReturn: (kotlin.Float) -> Datum = Datum::real
            override val positive: (kotlin.Float) -> kotlin.Float = kotlin.Float::unaryPlus
            override val negative: (kotlin.Float) -> kotlin.Float = kotlin.Float::unaryMinus
            override val returnType: PType = PType.typeReal()
        }

        object Double : Simple<kotlin.Double>() {
            override val extract: (Datum) -> kotlin.Double = Datum::getDouble
            override val toReturn: (kotlin.Double) -> Datum = Datum::doublePrecision
            override val positive: (kotlin.Double) -> kotlin.Double = kotlin.Double::unaryPlus
            override val negative: (kotlin.Double) -> kotlin.Double = kotlin.Double::unaryMinus
            override val returnType: PType = PType.typeDoublePrecision()
        }

        object IntArbitrary : Simple<BigInteger>() {
            override val extract: (Datum) -> BigInteger = Datum::getBigInteger
            override val toReturn: (BigInteger) -> Datum = Datum::intArbitrary
            override val positive: (BigInteger) -> BigInteger = { x -> x }
            override val negative: (BigInteger) -> BigInteger = BigInteger::unaryMinus
            override val returnType: PType = PType.typeIntArbitrary()
        }

        object DecimalArbitrary : Simple<BigDecimal>() {
            override val extract: (Datum) -> BigDecimal = Datum::getBigDecimal
            override val toReturn: (BigDecimal) -> Datum = Datum::decimalArbitrary
            override val positive: (BigDecimal) -> BigDecimal = { x -> x }
            override val negative: (BigDecimal) -> BigDecimal = BigDecimal::unaryMinus
            override val returnType: PType = PType.typeDecimalArbitrary()
        }

        object Byte : Simple<kotlin.Byte>() {
            override val extract: (Datum) -> kotlin.Byte = Datum::getByte
            override val toReturn: (kotlin.Byte) -> Datum = Datum::tinyInt
            override val positive: (kotlin.Byte) -> kotlin.Byte = { x -> x }
            override val negative: (kotlin.Byte) -> kotlin.Byte = { x -> (x * -1).toByte() }
            override val returnType: PType = PType.typeTinyInt()
        }

        class Decimal(private val returnType: PType) : Factory {
            override fun positive(arg: Operator.Expr): Operator.Expr {
                return Decimal(arg, returnType) { x -> x }
            }

            override fun negative(arg: Operator.Expr): Operator.Expr {
                return Decimal(arg, returnType, BigDecimal::unaryMinus)
            }
        }

        object Dynamic : Factory {
            override fun positive(arg: Operator.Expr): Operator.Expr {
                return Dynamic(arg, Factory::positive, ArithmeticTyper::positive)
            }

            override fun negative(arg: Operator.Expr): Operator.Expr {
                return Dynamic(arg, Factory::negative, ArithmeticTyper::negative)
            }
        }
    }

    internal class Decimal(
        private val arg: Operator.Expr,
        private val returnType: PType,
        private val op: (BigDecimal) -> BigDecimal
    ) : Operator.Expr {

        override fun eval(env: Environment): Datum {
            val lhsEval = arg.eval(env)
            if (lhsEval.isNull) return Datum.nullValue(returnType)
            if (lhsEval.isMissing) throw TypeCheckException()
            return Datum.decimal(op(lhsEval.bigDecimal), returnType.precision, returnType.scale)
        }
    }

    internal class Dynamic(
        val arg: Operator.Expr,
        val op: (Factory, Operator.Expr) -> Operator.Expr,
        val type: (PType) -> PType?
    ) : Operator.Expr {
        override fun eval(env: Environment): Datum {
            val lhsEval = arg.eval(env)
            val returns = type(lhsEval.type) ?: throw TypeCheckException()
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
            return op(factory, lhsEval.coerce(lhsEval.type, returns)).eval(env)
        }

        private fun Datum.coerce(input: PType, target: PType): Operator.Expr {
            if (input == target) return ExprLiteral(this)
            return ExprCast(ExprLiteral(this), Ref.Cast(input, target, isNullable = true))
        }
    }
}
