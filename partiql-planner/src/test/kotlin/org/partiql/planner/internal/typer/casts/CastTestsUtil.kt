package org.partiql.planner.internal.typer.casts

import org.partiql.planner.internal.typer.toStaticType
import org.partiql.types.StaticType
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType

@OptIn(PartiQLValueExperimental::class)
object CastTestsUtil {
    // Simple nested when statement as this is just testing logic
    fun getCastKind(from: PartiQLValueType, to: PartiQLValueType): StaticType? =
        when (from) {
            PartiQLValueType.ANY -> when (to) {
                PartiQLValueType.ANY -> StaticType.ANY
                PartiQLValueType.NULL -> StaticType.unionOf(StaticType.NULL, StaticType.MISSING)
                PartiQLValueType.MISSING -> StaticType.unionOf(StaticType.NULL, StaticType.MISSING)
                else -> to.toStaticType().asOptional()
            }
            PartiQLValueType.BOOL -> when (to) {
                PartiQLValueType.ANY -> StaticType.ANY
                PartiQLValueType.NULL -> StaticType.unionOf(StaticType.NULL, StaticType.MISSING)
                // BOOL
                PartiQLValueType.BOOL,
                // Fix-Precision INT
                PartiQLValueType.INT8, PartiQLValueType.INT16, PartiQLValueType.INT32, PartiQLValueType.INT64,
                // Arbitrary Precision INT
                PartiQLValueType.INT,
                // DECIMAL
                PartiQLValueType.DECIMAL, PartiQLValueType.DECIMAL_ARBITRARY,
                // FLOAT
                PartiQLValueType.FLOAT32, PartiQLValueType.FLOAT64,
                // TEXT
                PartiQLValueType.CHAR, PartiQLValueType.STRING, PartiQLValueType.SYMBOL
                -> to.toStaticType()
                else -> null
            }
            PartiQLValueType.INT8 -> when (to) {
                PartiQLValueType.ANY -> StaticType.ANY
                PartiQLValueType.NULL -> StaticType.unionOf(StaticType.NULL, StaticType.MISSING)

                PartiQLValueType.BOOL,

                PartiQLValueType.INT8, PartiQLValueType.INT16, PartiQLValueType.INT32, PartiQLValueType.INT64,

                PartiQLValueType.INT,

                PartiQLValueType.DECIMAL, PartiQLValueType.DECIMAL_ARBITRARY,
                PartiQLValueType.FLOAT32, PartiQLValueType.FLOAT64,

                PartiQLValueType.CHAR, PartiQLValueType.STRING, PartiQLValueType.SYMBOL -> to.toStaticType()
                else -> null
            }
            PartiQLValueType.INT16 -> when (to) {
                PartiQLValueType.ANY -> StaticType.ANY
                PartiQLValueType.NULL -> StaticType.unionOf(StaticType.NULL, StaticType.MISSING)

                PartiQLValueType.INT8 -> to.toStaticType().asOptional()

                PartiQLValueType.BOOL,
                PartiQLValueType.INT16, PartiQLValueType.INT32, PartiQLValueType.INT64,
                PartiQLValueType.INT,
                PartiQLValueType.DECIMAL, PartiQLValueType.DECIMAL_ARBITRARY,
                PartiQLValueType.FLOAT32, PartiQLValueType.FLOAT64,
                PartiQLValueType.CHAR, PartiQLValueType.STRING, PartiQLValueType.SYMBOL -> to.toStaticType()
                else -> null
            }
            PartiQLValueType.INT32 -> when (to) {
                PartiQLValueType.ANY -> StaticType.ANY
                PartiQLValueType.NULL -> StaticType.unionOf(StaticType.NULL, StaticType.MISSING)

                PartiQLValueType.INT8, PartiQLValueType.INT16 -> to.toStaticType().asOptional()

                PartiQLValueType.BOOL,
                PartiQLValueType.INT32, PartiQLValueType.INT64,
                PartiQLValueType.INT,
                PartiQLValueType.DECIMAL, PartiQLValueType.DECIMAL_ARBITRARY,
                PartiQLValueType.FLOAT32, PartiQLValueType.FLOAT64,
                PartiQLValueType.CHAR, PartiQLValueType.STRING, PartiQLValueType.SYMBOL -> to.toStaticType()
                else -> null
            }
            PartiQLValueType.INT64 -> when (to) {
                PartiQLValueType.ANY -> StaticType.ANY
                PartiQLValueType.NULL -> StaticType.unionOf(StaticType.NULL, StaticType.MISSING)

                PartiQLValueType.INT8, PartiQLValueType.INT16, PartiQLValueType.INT32 -> to.toStaticType().asOptional()

                PartiQLValueType.BOOL,
                PartiQLValueType.INT64,
                PartiQLValueType.INT,
                PartiQLValueType.DECIMAL, PartiQLValueType.DECIMAL_ARBITRARY,
                PartiQLValueType.FLOAT32, PartiQLValueType.FLOAT64,
                PartiQLValueType.CHAR, PartiQLValueType.STRING, PartiQLValueType.SYMBOL -> to.toStaticType()
                else -> null
            }
            PartiQLValueType.INT -> when (to) {
                PartiQLValueType.ANY -> StaticType.ANY
                PartiQLValueType.NULL -> StaticType.unionOf(StaticType.NULL, StaticType.MISSING)

                PartiQLValueType.INT8, PartiQLValueType.INT16, PartiQLValueType.INT32, PartiQLValueType.INT64
                -> to.toStaticType().asOptional()
                PartiQLValueType.BOOL,
                PartiQLValueType.INT,
                PartiQLValueType.DECIMAL, PartiQLValueType.DECIMAL_ARBITRARY,
                PartiQLValueType.FLOAT32, PartiQLValueType.FLOAT64,
                PartiQLValueType.CHAR, PartiQLValueType.STRING, PartiQLValueType.SYMBOL -> to.toStaticType()
                else -> null
            }
            PartiQLValueType.DECIMAL -> when (to) {
                PartiQLValueType.ANY -> StaticType.ANY
                PartiQLValueType.NULL -> StaticType.unionOf(StaticType.NULL, StaticType.MISSING)

                PartiQLValueType.INT8, PartiQLValueType.INT16,
                PartiQLValueType.INT32, PartiQLValueType.INT64 -> to.toStaticType().asOptional()

                PartiQLValueType.BOOL,
                PartiQLValueType.INT,
                PartiQLValueType.DECIMAL, PartiQLValueType.DECIMAL_ARBITRARY,

                PartiQLValueType.FLOAT32, PartiQLValueType.FLOAT64,
                PartiQLValueType.CHAR, PartiQLValueType.STRING, PartiQLValueType.SYMBOL -> to.toStaticType()
                else -> null
            }
            PartiQLValueType.DECIMAL_ARBITRARY -> when (to) {
                PartiQLValueType.ANY -> StaticType.ANY
                PartiQLValueType.NULL -> StaticType.unionOf(StaticType.NULL, StaticType.MISSING)

                PartiQLValueType.INT8, PartiQLValueType.INT16, PartiQLValueType.INT32, PartiQLValueType.INT64,
                PartiQLValueType.DECIMAL, -> to.toStaticType().asOptional()

                PartiQLValueType.BOOL,
                PartiQLValueType.INT, PartiQLValueType.DECIMAL_ARBITRARY,
                PartiQLValueType.FLOAT32, PartiQLValueType.FLOAT64,
                PartiQLValueType.CHAR, PartiQLValueType.STRING, PartiQLValueType.SYMBOL -> to.toStaticType()
                else -> null
            }

            PartiQLValueType.FLOAT32 -> when (to) {
                PartiQLValueType.ANY -> StaticType.ANY
                PartiQLValueType.NULL -> StaticType.unionOf(StaticType.NULL, StaticType.MISSING)

                PartiQLValueType.INT8, PartiQLValueType.INT16, PartiQLValueType.INT32, PartiQLValueType.INT64,
                PartiQLValueType.DECIMAL, -> to.toStaticType().asOptional()

                PartiQLValueType.BOOL,
                PartiQLValueType.INT,
                PartiQLValueType.DECIMAL_ARBITRARY,
                PartiQLValueType.FLOAT32, PartiQLValueType.FLOAT64,
                PartiQLValueType.CHAR, PartiQLValueType.STRING, PartiQLValueType.SYMBOL -> to.toStaticType()
                else -> null
            }

            PartiQLValueType.FLOAT64 -> when (to) {
                PartiQLValueType.ANY -> StaticType.ANY
                PartiQLValueType.NULL -> StaticType.unionOf(StaticType.NULL, StaticType.MISSING)

                PartiQLValueType.INT8, PartiQLValueType.INT16, PartiQLValueType.INT32, PartiQLValueType.INT64,
                PartiQLValueType.DECIMAL,
                PartiQLValueType.FLOAT32, -> to.toStaticType().asOptional()

                PartiQLValueType.BOOL,
                PartiQLValueType.INT,
                PartiQLValueType.DECIMAL_ARBITRARY,
                PartiQLValueType.FLOAT64,
                PartiQLValueType.CHAR, PartiQLValueType.STRING, PartiQLValueType.SYMBOL -> to.toStaticType()
                else -> null
            }

            PartiQLValueType.CHAR -> when (to) {
                PartiQLValueType.ANY -> StaticType.ANY
                PartiQLValueType.NULL -> StaticType.unionOf(StaticType.NULL, StaticType.MISSING)

                PartiQLValueType.BOOL,
                PartiQLValueType.INT8, PartiQLValueType.INT16, PartiQLValueType.INT32, PartiQLValueType.INT64,
                PartiQLValueType.INT,
                PartiQLValueType.DECIMAL, PartiQLValueType.DECIMAL_ARBITRARY,
                PartiQLValueType.FLOAT32, PartiQLValueType.FLOAT64 -> to.toStaticType().asOptional()

                PartiQLValueType.CHAR, PartiQLValueType.STRING, PartiQLValueType.SYMBOL -> to.toStaticType()
                else -> null
            }

            PartiQLValueType.STRING -> when (to) {
                PartiQLValueType.ANY -> StaticType.ANY
                PartiQLValueType.NULL -> StaticType.unionOf(StaticType.NULL, StaticType.MISSING)

                PartiQLValueType.BOOL,
                PartiQLValueType.INT8, PartiQLValueType.INT16,
                PartiQLValueType.INT32, PartiQLValueType.INT64, PartiQLValueType.DECIMAL,
                PartiQLValueType.INT,
                PartiQLValueType.DECIMAL_ARBITRARY, PartiQLValueType.FLOAT32,
                PartiQLValueType.FLOAT64, -> to.toStaticType().asOptional()

                PartiQLValueType.CHAR, PartiQLValueType.STRING, PartiQLValueType.SYMBOL -> to.toStaticType()
                else -> null
            }

            PartiQLValueType.SYMBOL -> when (to) {
                PartiQLValueType.ANY -> StaticType.ANY
                PartiQLValueType.NULL -> StaticType.unionOf(StaticType.NULL, StaticType.MISSING)

                PartiQLValueType.BOOL,
                PartiQLValueType.INT8, PartiQLValueType.INT16, PartiQLValueType.INT32, PartiQLValueType.INT64,
                PartiQLValueType.INT,
                PartiQLValueType.DECIMAL, PartiQLValueType.DECIMAL_ARBITRARY,
                PartiQLValueType.FLOAT32, PartiQLValueType.FLOAT64, -> to.toStaticType().asOptional()

                PartiQLValueType.CHAR, PartiQLValueType.STRING, PartiQLValueType.SYMBOL -> to.toStaticType()
                else -> null
            }

            PartiQLValueType.BINARY -> null
            PartiQLValueType.BYTE -> null
            PartiQLValueType.BLOB -> when (to) {
                PartiQLValueType.BLOB -> StaticType.BLOB.asNullable()
                else -> null
            }
            PartiQLValueType.CLOB -> when (to) {
                PartiQLValueType.CLOB -> StaticType.CLOB.asNullable()
                PartiQLValueType.CHAR, PartiQLValueType.STRING, PartiQLValueType.SYMBOL -> to.toStaticType().asOptional()
                else -> null
            }
            PartiQLValueType.DATE -> when (to) {
                PartiQLValueType.ANY -> StaticType.ANY
                PartiQLValueType.NULL -> StaticType.unionOf(StaticType.NULL, StaticType.MISSING)

                PartiQLValueType.CHAR, PartiQLValueType.STRING, PartiQLValueType.SYMBOL,

                PartiQLValueType.DATE, PartiQLValueType.TIMESTAMP -> to.toStaticType()

                else -> null
            }
            PartiQLValueType.TIME -> when (to) {
                PartiQLValueType.ANY -> StaticType.ANY
                PartiQLValueType.NULL -> StaticType.unionOf(StaticType.NULL, StaticType.MISSING)

                PartiQLValueType.CHAR, PartiQLValueType.STRING, PartiQLValueType.SYMBOL,
                PartiQLValueType.TIME, PartiQLValueType.TIMESTAMP -> to.toStaticType()
                else -> null
            }

            PartiQLValueType.TIMESTAMP -> when (to) {
                PartiQLValueType.ANY -> StaticType.ANY
                PartiQLValueType.NULL -> StaticType.unionOf(StaticType.NULL, StaticType.MISSING)

                PartiQLValueType.CHAR, PartiQLValueType.STRING, PartiQLValueType.SYMBOL,
                PartiQLValueType.DATE, PartiQLValueType.TIME, PartiQLValueType.TIMESTAMP -> to.toStaticType()

                else -> null
            }
            PartiQLValueType.INTERVAL -> null
            PartiQLValueType.BAG -> when (to) {
                PartiQLValueType.ANY -> StaticType.ANY
                PartiQLValueType.NULL -> StaticType.unionOf(StaticType.NULL, StaticType.MISSING)

                PartiQLValueType.LIST, PartiQLValueType.SEXP, PartiQLValueType.BAG, -> to.toStaticType()
                else -> null
            }

            PartiQLValueType.LIST -> when (to) {
                PartiQLValueType.ANY -> StaticType.ANY
                PartiQLValueType.NULL -> StaticType.unionOf(StaticType.NULL, StaticType.MISSING)

                PartiQLValueType.LIST, PartiQLValueType.SEXP, PartiQLValueType.BAG, -> to.toStaticType()
                else -> null
            }
            PartiQLValueType.SEXP -> when (to) {
                PartiQLValueType.ANY -> StaticType.ANY
                PartiQLValueType.NULL -> StaticType.unionOf(StaticType.NULL, StaticType.MISSING)

                PartiQLValueType.LIST, PartiQLValueType.SEXP, PartiQLValueType.BAG, -> to.toStaticType()
                else -> null
            }
            PartiQLValueType.STRUCT -> when (to) {
                PartiQLValueType.ANY -> StaticType.ANY
                PartiQLValueType.NULL -> StaticType.unionOf(StaticType.NULL, StaticType.MISSING)

                PartiQLValueType.STRUCT -> to.toStaticType()

                else -> null
            }
            PartiQLValueType.NULL -> to.toStaticType()
            PartiQLValueType.MISSING -> when (to) {
                PartiQLValueType.ANY -> null // TODO: SHOULD WE PERMIT THIS?
                PartiQLValueType.NULL -> null
                PartiQLValueType.MISSING -> StaticType.MISSING
                else -> null
            }
        }
}
