package org.partiql.lang.eval.builtins

import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.eval.ErrorDetails
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.ExprFunction
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.eval.TypingMode
import org.partiql.lang.eval.createErrorSignaler
import org.partiql.lang.types.FunctionSignature
import org.partiql.types.StaticType

/** Built-in functions specific to the PartiQL language definition.
 *
 *  These functions are defined in the language specification in order to explain some of the PartiQL semantics,
 *  but are also made available to the user.
 *  Implementations of these built-ins can depend on compilation options and specific compilation options
 *  must be chosen in order to obtain an [ExprFunction] instance of these built-ins.
 *  It is intended that the compilation options used for instantiating a compiler are the same ones
 *  as used for instantiating these built-ins when adding them as functions to the compiler's environment.
 *
 */
// TODO: Currently, the only compilation option in use is [TypingMode], so it is passed here by itself.
// Ideally, something like [CompileOptions] would be the argument instead, but [CompileOptions] is only used
// by the "evaluating compiler" and not by the "planning compiler", while this parameterization
// of the built-ins needs to be applicable in both.
internal fun definitionalBuiltins(typingMode: TypingMode): List<ExprFunction> =
    listOf(
        ExprFunctionCollToScalar(typingMode),
    )

/** `coll_to_scalar` extracts the scalar value contained in a "singleton table",
 *   as performed in most cases of subquery coercion.
 *   If the input is a collection consisting of a single element,
 *   which in turn is a struct with exactly one attribute,
 *   `coll_to_scalar` returns the value of the attribute.
 *   For all other inputs, the result is either `MISSING` or an error,
 *   depending on the typing mode.
 */
internal class ExprFunctionCollToScalar(typingMode: TypingMode) : ExprFunction {
    override val signature = FunctionSignature(
        name = "coll_to_scalar",
        requiredParameters = listOf(StaticType.ANY),
        returnType = StaticType.ANY
    )

    // TODO: Is ErrorSignaler most appropriate to use here?
    // By its external structure, ErrorSignaler is exactly what is needed:
    // based on TypingMode, it either produces MISSING or an error.
    // However, it is not used much, the existing usage is for a different setting
    // (defined on top of the basic setting needed here),
    // and the final error message is not best formatted.
    // The latter appears to be a symptom of general accumulated cruft in error-handling.
    private val signaler = typingMode.createErrorSignaler()

    /** Handler for situations when extraction cannot succeed.
     *  Produces either the MISSING or an error.  */
    private fun hiccup(reason: String): ExprValue {
        return signaler.error(
            ErrorCode.EVALUATOR_NON_SINGLETON_COLLECTION
        ) { ErrorDetails(metas = emptyMap(), message = reason) }
    }

    override fun callWithRequired(session: EvaluationSession, required: List<ExprValue>): ExprValue {
        val coll = required[0]
        if (!coll.type.isSequence) {
            return hiccup("because it is not a collection.")
        } else { // coll is a LIST, BAG, or SEXP
            val seq = coll.asSequence()
            if (seq.count() != 1) {
                return hiccup("because the collection does not contain exactly one member.")
            } else { // we have a singleton collection
                val struct = seq.first()
                if (struct.type != ExprValueType.STRUCT) {
                    return hiccup("because the only member of the collection is not a struct.")
                } else { // the single element is a struct
                    val vals = struct.asSequence()
                    if (vals.count() != 1) {
                        return hiccup("because the only struct member of the collection does not contain exactly one attribute.")
                    } else { // the single struct has exactly one attribute
                        return vals.first()
                    }
                }
            }
        }
    }
}
