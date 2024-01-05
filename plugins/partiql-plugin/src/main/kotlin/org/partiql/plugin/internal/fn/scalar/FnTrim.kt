// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.plugin.internal.fn.scalar

import org.partiql.plugin.internal.extensions.codepointTrim
import org.partiql.spi.function.PartiQLFunction
import org.partiql.spi.function.PartiQLFunctionExperimental
import org.partiql.types.function.FunctionParameter
import org.partiql.types.function.FunctionSignature
import org.partiql.value.ClobValue
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType.CLOB
import org.partiql.value.PartiQLValueType.STRING
import org.partiql.value.PartiQLValueType.SYMBOL
import org.partiql.value.StringValue
import org.partiql.value.SymbolValue
import org.partiql.value.check
import org.partiql.value.stringValue

/**
 * From section 6.7 of SQL 92 spec:
 * ```
 * 6) If <trim function> is specified, then
 *   a) If FROM is specified, then either <trim specification> or <trim character> or both shall be specified.
 *
 *   b) If <trim specification> is not specified, then BOTH is implicit.
 *
 *   c) If <trim character> is not specified, then ' ' is implicit.
 *
 *   d) If TRIM ( SRC ) is specified, then TRIM ( BOTH ' ' FROM SRC ) is implicit.
 *
 *   e) The data type of the <trim function> is variable-length character string with maximum length equal to the
 *   fixed length or maximum variable length of the <trim source>.
 *
 *   f) If a <trim character> is specified, then <trim character> and <trim source> shall be comparable.
 *
 *   g) The character repertoire and form-of-use of the <trim function> are the same as those of the <trim source>.
 *
 *   h) The collating sequence and the coercibility attribute are determined as specified for monadic operators in
 *      Subclause 4.2.3, "Rules determining collating sequence usage", where the <trim source> of TRIM plays the
 *      role of the monadic operand.
 *  ```
 *
 *  Where:
 *  * `<trim specification> ::= LEADING | TRAILING | BOTH`
 *  * `<trim character> ::= <character value expression>`
 *  * `<trim source> ::= <character value expression>`
 */

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_TRIM__STRING__STRING : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "trim",
        returns = STRING,
        parameters = listOf(FunctionParameter("value", STRING)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val value = args[0].check<StringValue>().string
        if (value == null) {
            return stringValue(null)
        }
        val result = value.codepointTrim()
        return stringValue(result)
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_TRIM__SYMBOL__SYMBOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "trim",
        returns = SYMBOL,
        parameters = listOf(FunctionParameter("value", SYMBOL)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val value = args[0].check<SymbolValue>().string
        if (value == null) {
            return stringValue(null)
        }
        val result = value.codepointTrim()
        return stringValue(result)
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_TRIM__CLOB__CLOB : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "trim",
        returns = CLOB,
        parameters = listOf(FunctionParameter("value", CLOB)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val value = args[0].check<ClobValue>().string
        if (value == null) {
            return stringValue(null)
        }
        val result = value.codepointTrim()
        return stringValue(result)
    }
}
