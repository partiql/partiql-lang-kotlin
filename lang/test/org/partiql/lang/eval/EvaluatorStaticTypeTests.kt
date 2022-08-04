package org.partiql.lang.eval

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.lang.ION
import org.partiql.lang.eval.evaluatortestframework.EvaluatorTestTarget
import org.partiql.lang.util.testdsl.IonResultTestCase
import org.partiql.lang.util.testdsl.runTestCase

/**
 * This test class is effectively the same as [EvaluatorTests] however it:
 *
 * - enables the static type inferencer ([org.partiql.lang.eval.visitors.StaticTypeInferenceVisitorTransform])
 * - sets the [org.partiql.lang.eval.CompileOptions.thunkReturnTypeAssertions] compile option
 * - only runs in permissive mode.
 *
 * The intent here is to test if the inferencer agrees with the runtime behavior of the evaluator.
 */
class EvaluatorStaticTypeTests {

    companion object {
        val valueFactory = ExprValueFactory.standard(ION)
        private val mockDb = EVALUATOR_TEST_SUITE.mockDb(valueFactory)

        // These tests are known to be failing.  If they are fixed but not removed from this list
        // the test will fail.  (This forces us to keep this list up-to-date.)
        private val FAILING_TESTS = hashSetOf(
            // https://github.com/partiql/partiql-lang-kotlin/issues/497
            "selectIndexStruct",

            // https://github.com/partiql/partiql-lang-kotlin/issues/498
            "ordinalAccessWithNegativeIndex",
            "ordinalAccessWithNegativeIndexAndBindings",

            // https://github.com/partiql/partiql-lang-kotlin/issues/499
            "selectJoinOnClauseScoping",

            // Unimplemented features:

            // Support non-struct types for wildcard projections in STIVT
            // https://github.com/partiql/partiql-lang-kotlin/issues/500
            "functionCall",

            "selectCorrelatedLeftJoin",
            "selectCorrelatedLeftJoinOnClause",
            "simpleCase",
            "simpleCaseNoElse",
            "searchedCase",
            "searchedCaseNoElse",

            // STIR's should support non-sequence types in UNPIVOT expression
            // https://github.com/partiql/partiql-lang-kotlin/issues/501
            "selectCorrelatedUnpivot",
            "nestedSelectJoinWithUnpivot",
            "unpivotMissingWithAsAndAt",
            "unpivotMissingCrossJoinWithAsAndAt",

            // TODO: why are these failing due to unexpected (empty) result?
            "selectDistinct",
            "selectDistinctStarBags",
            "selectDistinctStarLists",
            "selectDistinctStarMixed",
            "selectDistinctSubQuery",
            "selectDistinctWithSubQuery",
            "selectDistinctStarStructs",
            "selectDistinctValue",
            "selectDistinctExpressionAndWhere",
            "selectDistinctExpression",
            "nestedSelectJoinLimit",

            // STIR does not support path wildcards -i.e. `foo[*]` yet
            "pathFieldStructLiteral",
            "pathIndexStructLiteral",
            "pathIndexStructOutOfBoundsLowLiteral",
            "pathIndexStructOutOfBoundsHighLiteral",
            "pathUnpivotWildcard",
            "pathUnpivotWildcardFieldsAfter",
            "pathSimpleWildcard",
            "pathWildcardPath",
            "pathWildcard",
            "pathDoubleWildCard",
            "pathDoubleUnpivotWildCard",
            "pathWildCardOverScalar",
            "pathUnpivotWildCardOverScalar",
            "pathWildCardOverScalarMultiple",
            "pathUnpivotWildCardOverScalarMultiple",
            "pathWildCardOverStructMultiple",
            "pathUnpivotWildCardOverStructMultiple",
            "selectFromScalarAndAtUnpivotWildCardOverScalar",
            "selectFromListAndAtUnpivotWildCardOverScalar",
            "selectFromBagAndAtUnpivotWildCardOverScalar",
            "selectPathUnpivotWildCardOverStructMultiple",
            "selectStarSingleSourceHoisted",
            "selectImplicitAndExplicitAliasSingleSourceHoisted",

            // these are intended to test the `IN` operator but also use path wildcards which aren't supported
            // by STIR
            "inPredicate",
            "inPredicateSingleItem",
            "inPredicateSingleExpr",
            "inPredicateSingleItemListVar",
            "notInPredicate",
            "notInPredicateSingleItem",
            "notInPredicateSingleExpr",
            "notInPredicateSingleItemListVar",
            "notInPredicateSingleListVar",
            "notInPredicateSubQuerySelectValue",
            "inPredicateWithTableConstructor",
            "notInPredicateWithTableConstructor",
            "inPredicateSingleListVar",
            "inPredicateSubQuerySelectValue",
            "inPredicateWithExpressionOnRightSide",
            "notInPredicateWithExpressionOnRightSide",

            "parameters",
            // STIR does not support aggregates
            "selectDistinctWithAggregate",
            "selectDistinctAggregationWithGroupBy",
            "selectDistinctWithGroupBy",
            "selectDistinctWithJoin",
            "selectDistinctStarScalars",
            "selectDistinctStarUnknowns",
            "selectDistinctStarIntegers",
            // STIR does support `FROM` sources that aren't a collection of structs.
            "projectOfListOfList",
            "projectOfBagOfBag",
            "projectOfListOfBag",
            "projectOfBagOfList",
            "projectOfSexp",
            "emptySymbolInGlobals",
            "semicolonAtEndOfExpression",
            // STIR does not support unpivot in path i.e. `<expr>.*`
            "unpivotMissing",
            "unpivotEmptyStruct",
            "pathUnpivotEmptyStruct1",
            "pathUnpivotEmptyStruct2",
            "pathUnpivotEmptyStruct3",
            "projectOfUnpivotPath",

            // PIVOT not supported by STIR
            "pivotFrom",
            "pivotBadFieldType",
            "pivotLiteralFieldNameFrom",
            "pivotUnpivotWithWhereLimit",
            "unpivotStructWithMissingField",

            // STIR does not support `CompilePipeline.undefinedVariableBehavior`
            // (these are likely to be a permanent entries to this list since STR/STIR will probably
            // never support undefined variables).
            "undefinedUnqualifiedVariableWithUndefinedVariableBehaviorMissing",
            "undefinedUnqualifiedVariableIsNullExprWithUndefinedVariableBehaviorMissing",
            "undefinedUnqualifiedVariableIsMissingExprWithUndefinedVariableBehaviorMissing",

        )

        @JvmStatic
        @Suppress("unused")
        fun evaluatorStaticTypeTests() = EVALUATOR_TEST_SUITE.getAllTests(
            EvaluatorTests.AST_EVALUATOR_SKIP_LIST.union(FAILING_TESTS)
        ).map {
            it.copy(
                compileOptionsBuilderBlock = {
                    it.compileOptionsBuilderBlock(this)

                    // set permissive mode
                    typingMode(TypingMode.PERMISSIVE)
                    thunkOptions {
                        // enable evaluation time type checking
                        evaluationTimeTypeChecks(ThunkReturnTypeAssertions.ENABLED)
                    }
                }
            )
        }
    }

    @ParameterizedTest
    @MethodSource("evaluatorStaticTypeTests")
    fun allTests(tc: IonResultTestCase) =
        tc.runTestCase(
            valueFactory = valueFactory,
            db = mockDb,
            // the planner doesn't yet support type inferencing pass needed to make this work
            EvaluatorTestTarget.COMPILER_PIPELINE,
            // Enable the static type inferencer for this
            compilerPipelineBuilderBlock = { this.globalTypeBindings(mockDb.typeBindings) },
        )
}
