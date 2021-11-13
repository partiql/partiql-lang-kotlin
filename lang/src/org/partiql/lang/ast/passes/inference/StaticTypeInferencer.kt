package org.partiql.lang.ast.passes.inference

import org.partiql.lang.ast.passes.SemanticException
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.domains.staticType
import org.partiql.lang.errors.Problem
import org.partiql.lang.errors.ProblemCollector
import org.partiql.lang.errors.ProblemSeverity
import org.partiql.lang.eval.Bindings
import org.partiql.lang.eval.visitors.StaticTypeInferenceVisitorTransform
import org.partiql.lang.eval.visitors.StaticTypeVisitorTransform
import org.partiql.lang.types.CustomTypeFunction
import org.partiql.lang.types.FunctionSignature
import org.partiql.lang.types.StaticType

/**
 * Infers the [StaticType] of a [PartiqlAst.Statement]. Assumes [StaticTypeVisitorTransform] was run before on this
 * [PartiqlAst.Statement] and all implicit variables have been resolved.
 *
 * @param globalBindings The global bindings to the static environment.  This is a data catalog purely from a lookup
 * perspective.
 * @param customFunctionSignatures Custom user-defined function signatures that can be called by the query.
 * @param customTypeFunctions Mapping of custom type name to [CustomTypeFunction] to be used for typed operators
 * (i.e CAST/IS) inference.
 */
class StaticTypeInferencer(
    private val globalBindings: Bindings<StaticType>,
    private val customFunctionSignatures: List<FunctionSignature>,
    private val customTypeFunctions: Map<String, CustomTypeFunction>,
) {
    /**
     * Infers the [StaticType] of [node] and returns an [InferenceResult]. Currently does not support inference for
     * [PartiqlAst.Statement.Dml] and [PartiqlAst.Statement.Ddl] statements.
     *
     * Returns [InferenceResult.Success] if no encountered [Problem] has severity of [ProblemSeverity.ERROR]. This
     * result will contain the [node]'s [StaticType] and encountered problems (all of which will have
     * [ProblemSeverity.WARNING]).
     *
     * Otherwise, returns [InferenceResult.Failure] with all of the [Problem]s encountered regardless of severity.
     *
     * @param node [PartiqlAst.Statement] to perform static type inference on
     */
    fun inferStaticType(node: PartiqlAst.Statement): InferenceResult {
        val problemCollector = ProblemCollector()
        val inferencer = StaticTypeInferenceVisitorTransform(globalBindings, customFunctionSignatures, customTypeFunctions, problemCollector)
        val transformedPartiqlAst = inferencer.transformStatement(node)
        val inferredStaticType = when (transformedPartiqlAst) {
            is PartiqlAst.Statement.Query -> transformedPartiqlAst.expr.metas.staticType?.type
                ?: error("Expected query's inferred StaticType to not be null")
            is PartiqlAst.Statement.Dml,
            is PartiqlAst.Statement.Ddl,
            is PartiqlAst.Statement.Exec -> error("Type inference for DML, DDL, EXEC statements is not currently supported")
        }
        return when {
            problemCollector.hasErrors -> InferenceResult.Failure(inferredStaticType, transformedPartiqlAst, problemCollector.problems)
            else -> InferenceResult.Success(inferredStaticType, problemCollector.problems)
        }
    }

    /**
     * Result of static type inference on a query. Can be one of:
     * - [InferenceResult.Success] or
     * - [InferenceResult.Failure]
     */
    sealed class InferenceResult {
        /**
         * List of all the [Problem]s encountered during static type inference.
         */
        abstract val problems: List<Problem>

        /**
         * Successful static type inference result (i.e. no problems encountered with [ProblemSeverity.ERROR]).
         *
         * @param staticType overall query's [StaticType]
         * @param problems all of the [Problem]s encountered through static type inference, which will all have
         * [ProblemSeverity.WARNING]
         */
        data class Success(val staticType: StaticType, override val problems: List<Problem>): InferenceResult()

        /**
         * Unsuccessful static type inference result due to at least one [Problem] encountered with
         * [ProblemSeverity.ERROR]
         *
         * @param staticType overall query's [StaticType]. This is internal because the static type cannot be relied
         * upon when a static inference problem is encountered with severity [ProblemSeverity.ERROR]. It is used
         * internally for testing the query's type inference behavior after an error is encountered.
         * @param partiqlAst query's [StaticType]-annotated [PartiqlAst]. This is internal because the static type
         * cannot be relied upon when a static inference problem is encountered with severity [ProblemSeverity.ERROR].
         * It is used internally for testing the query's type inference behavior after an error is encountered.
         * @param problems all of the [Problem]s encountered through static type inference.
         */
        data class Failure(internal val staticType: StaticType, internal val partiqlAst: PartiqlAst.Statement, override val problems: List<Problem>): InferenceResult()
    }
}

/**
 * From [this] [StaticTypeInferencer.InferenceResult], returns the [StaticType] if and only if there are no [Problem]s
 * encountered with [ProblemSeverity.ERROR]. Otherwise, throws the first [Problem] with [ProblemSeverity.ERROR] as
 * a [SemanticException].
 */
fun StaticTypeInferencer.InferenceResult.staticTypeOrError(): StaticType =
    when (this) {
        is StaticTypeInferencer.InferenceResult.Success -> staticType
        is StaticTypeInferencer.InferenceResult.Failure -> {
            val firstError = problems.first { it.details.severity == ProblemSeverity.ERROR }
            throw SemanticException(firstError)
        }
    }
