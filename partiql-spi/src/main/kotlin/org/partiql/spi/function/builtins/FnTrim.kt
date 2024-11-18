// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.function.utils.StringUtils.codepointTrim
import org.partiql.spi.value.Datum
import org.partiql.types.PType

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
internal val Fn_TRIM__STRING__STRING = Function.static(

    name = "trim",
    returns = PType.string(),
    parameters = arrayOf(Parameter("value", PType.string())),

) { args ->
    val value = args[0].string
    val result = value.codepointTrim()
    Datum.string(result)
}

internal val Fn_TRIM__CLOB__CLOB = Function.static(

    name = "trim",
    returns = PType.clob(Int.MAX_VALUE),
    parameters = arrayOf(Parameter("value", PType.clob(Int.MAX_VALUE))),

) { args ->
    val string = args[0].bytes.toString(Charsets.UTF_8)
    val result = string.codepointTrim()
    Datum.clob(result.toByteArray())
}
