package org.partiql.planner

import com.amazon.ionelement.api.ionSymbol
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.fail
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.lang.ION
import org.partiql.lang.eval.CompileOptions
import org.partiql.lang.eval.EVALUATOR_TEST_SUITE
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.ExprValueFactory
import org.partiql.lang.eval.PartiqlPhysicalCompiler
import org.partiql.lang.eval.TypingMode
import org.partiql.lang.eval.builtins.createBuiltinFunctions
import org.partiql.lang.mockdb.MockDb
import org.partiql.lang.syntax.SqlParser
import org.partiql.lang.util.assertIonEquals
import org.partiql.lang.util.testdsl.IonResultTestCase

class PhysicalPlanEvaluatorTests {
    private val valueFactory = ExprValueFactory.standard(ION)
    private val mockDb = EVALUATOR_TEST_SUITE.mockDb(valueFactory)

    companion object {
        val SKIP_LIST = hashSetOf(
            // https://github.com/partiql/partiql-lang-kotlin/issues/169
            "selectDistinctStarLists", "selectDistinctStarBags", "selectDistinctStarMixed",

            // https://github.com/partiql/partiql-lang-kotlin/issues/336
            "projectionIterationBehaviorUnfiltered_select_list",
            "projectionIterationBehaviorUnfiltered_select_star",

            // below this line use features not supported by the current physical algebra compiler.q
            // most of fail due to not supporting foundational nodes like id and scan yet.
            // PartiQL's test cases are not all that cleanly separated.
            "identifier",
            "identifierCaseMismatch",
            "quotedIdentifier",
            "lexicalScope",
            "functionCall",
            "grouping",
            "listLiteral",
            "rowValueConstructor",
            "bagLiteral",
            "tableValueConstructor",
            "structLiteral",
            "unaryPlus",
            "unaryMinus",
            "addIntFloat",
            "subIntFloatDecimal",
            "divDecimalInt",
            "mulFloatIntInt",
            "comparisonsConjuctTrue",
            "comparisonsDisjunctFalse",
            "pathSimpleDotOnly",
            "pathDotOnly",
            "pathDotMissingAttribute",
            "pathIndexing",
            "pathUnpivotWildcard",
            "pathUnpivotWildcardFieldsAfter",
            "pathSimpleWildcard",
            "selectValuePath",
            "pathWildcardPath",
            "pathWildcard",
            "pathDoubleWildCard",
            "pathDoubleUnpivotWildCard",
            "pathWildCardOverScalar",
            "pathUnpivotWildCardOverScalar",
            "pathWildCardOverStructMultiple",
            "pathUnpivotWildCardOverStructMultiple",
            "undefinedUnqualifiedVariableWithUndefinedVariableBehaviorMissing",
            "undefinedUnqualifiedVariableIsNullExprWithUndefinedVariableBehaviorMissing",
            "undefinedUnqualifiedVariableIsMissingExprWithUndefinedVariableBehaviorMissing",
            "undefinedUnqualifiedVariableInSelectWithUndefinedVariableBehaviorMissing",
            "selectFromScalarAndAtUnpivotWildCardOverScalar",
            "selectFromListAndAtUnpivotWildCardOverScalar",
            "selectFromBagAndAtUnpivotWildCardOverScalar",
            "selectPathUnpivotWildCardOverStructMultiple",
            "selectStarSingleSourceHoisted",
            "ordinalAccessWithNegativeIndex",
            "ordinalAccessWithNegativeIndexAndBindings",
            "rangeOverScalar",
            "rangeTwiceOverScalar",
            "rangeOverSexp",
            "rangeOverStruct",
            "rangeOverList",
            "rangeOverListWithAt",
            "rangeOverListWithAsAndAt",
            "rangeOverListConstructorWithAt",
            "rangeOverListConstructorWithAsAndAt",
            "rangeOverBagWithAt",
            "rangeOverNestedWithAt",
            "explicitAliasSelectSingleSource",
            "selectImplicitAndExplicitAliasSingleSourceHoisted",
            "syntheticColumnNameInSelect",
            "properAliasFromPathInSelect",
            "selectListWithMissing",
            "selectCrossProduct",
            "selectWhereStringEqualsSameCase",
            "selectWhereStrinEqualsDifferentCase",
            "selectJoin",
            "selectCorrelatedJoin",
            "selectCorrelatedLeftJoin",
            "selectCorrelatedLeftJoinOnClause",
            "selectJoinOnClauseScoping",
            "selectNonCorrelatedJoin",
            "selectCorrelatedUnpivot",
            "nestedSelectJoinWithUnpivot",
            "nestedSelectJoinLimit",
            "correlatedJoinWithShadowedAttributes",
            "correlatedJoinWithoutLexicalScope",
            "joinWithShadowedGlobal",
            "pivotFrom",
            "pivotLiteralFieldNameFrom",
            "pivotBadFieldType",
            "pivotUnpivotWithWhereLimit",
            "inPredicate",
            "inPredicateSingleItem",
            "inPredicateSingleExpr",
            "inPredicateSingleItemListVar",
            "inPredicateSingleListVar",
            "inPredicateSubQuerySelectValue",
            "notInPredicate",
            "notInPredicateSingleItem",
            "notInPredicateSingleExpr",
            "notInPredicateSingleItemListVar",
            "notInPredicateSingleListVar",
            "notInPredicateSubQuerySelectValue",
            "inPredicateWithTableConstructor",
            "notInPredicateWithTableConstructor",
            "inPredicateWithExpressionOnRightSide",
            "notInPredicateWithExpressionOnRightSide",
            "simpleCase",
            "simpleCaseNoElse",
            "searchedCase",
            "searchedCaseNoElse",
            "betweenPredicate",
            "notBetweenPredicate",
            "betweenStringsPredicate",
            "notBetweenStringsPredicate",
            "topLevelCountDistinct",
            "topLevelCount",
            "topLevelAllCount",
            "topLevelSum",
            "topLevelAllSum",
            "topLevelDistinctSum",
            "topLevelMin",
            "topLevelDistinctMin",
            "topLevelAllMin",
            "topLevelMax",
            "topLevelDistinctMax",
            "topLevelAllMax",
            "topLevelAvg",
            "topLevelDistinctAvg",
            "topLevelAvgOnlyInt",
            "selectValueAggregate",
            "selectListCountStar",
            "selectListCountVariable",
            "selectListMultipleAggregates",
            "selectListMultipleAggregatesNestedQuery",
            "aggregateInSubqueryOfSelect",
            "aggregateInSubqueryOfSelectValue",
            "aggregateWithAliasingInSubqueryOfSelectValue",
            "undefinedUnqualifiedVariable_inSelect_withProjectionOption",
            "wildcardOrderedNames",
            "aliasWildcardOrderedNames",
            "aliasWildcardOrderedNamesSelectList",
            "aliasOrderedNamesSelectList",
            "selectDistinct",
            "selectDistinctWithAggregate",
            "selectDistinctSubQuery",
            "selectDistinctWithSubQuery",
            "selectDistinctAggregationWithGroupBy",
            "selectDistinctWithGroupBy",
            "selectDistinctWithJoin",
            "selectDistinctStarScalars",
            "selectDistinctStarStructs",
            "selectDistinctStarUnknowns",
            "selectDistinctStarIntegers",
            "selectDistinctValue",
            "selectDistinctExpressionAndWhere",
            "selectDistinctExpression",
            "projectOfListOfList",
            "projectOfBagOfBag",
            "projectOfListOfBag",
            "projectOfBagOfList",
            "projectOfSexp",
            "projectOfUnpivotPath",
            "parameters",
            "unpivotMissing",
            "unpivotEmptyStruct",
            "unpivotMissingWithAsAndAt",
            "unpivotMissingCrossJoinWithAsAndAt",
            "variableShadow",
            "selectValueStructConstructorWithMissing",
            "selectIndexStruct",
            "selectStarSingleSource",
            "implicitAliasSelectSingleSource",
            "selectValues",
            "explicitAliasSelectSingleSourceWithWhere",
            "undefinedQualifiedVariableWithUndefinedVariableBehaviorError",
            "emptySymbol",
            "emptySymbolInGlobals",
            "semicolonAtEndOfExpression",
            "dateTimePartsAsVariableNames",
            "dateTimePartsAsStructFieldNames",
            "identifier",
            "identifierCaseMismatch",
            "quotedIdentifier",
            "lexicalScope",
            "functionCall",
            "grouping",
            "listLiteral",
            "rowValueConstructor",
            "bagLiteral",
            "tableValueConstructor",
            "structLiteral",
            "unaryPlus",
            "unaryMinus",
            "addIntFloat",
            "subIntFloatDecimal",
            "divDecimalInt",
            "mulFloatIntInt",
            "comparisonsConjuctTrue",
            "comparisonsDisjunctFalse",
            "pathSimpleDotOnly",
            "pathDotOnly",
            "pathDotMissingAttribute",
            "pathIndexing",
            "pathUnpivotWildcard",
            "pathUnpivotWildcardFieldsAfter",
            "pathSimpleWildcard",
            "selectValuePath",
            "pathWildcardPath",
            "pathWildcard",
            "pathDoubleWildCard",
            "pathDoubleUnpivotWildCard",
            "pathWildCardOverScalar",
            "pathUnpivotWildCardOverScalar",
            "pathWildCardOverStructMultiple",
            "pathUnpivotWildCardOverStructMultiple",
            "undefinedUnqualifiedVariableWithUndefinedVariableBehaviorMissing",
            "undefinedUnqualifiedVariableIsNullExprWithUndefinedVariableBehaviorMissing",
            "undefinedUnqualifiedVariableIsMissingExprWithUndefinedVariableBehaviorMissing",
            "undefinedUnqualifiedVariableInSelectWithUndefinedVariableBehaviorMissing",
            "selectFromScalarAndAtUnpivotWildCardOverScalar",
            "selectFromListAndAtUnpivotWildCardOverScalar",
            "selectFromBagAndAtUnpivotWildCardOverScalar",
            "selectPathUnpivotWildCardOverStructMultiple",
            "selectStarSingleSourceHoisted",
            "ordinalAccessWithNegativeIndex",
            "ordinalAccessWithNegativeIndexAndBindings",
            "rangeOverScalar",
            "rangeTwiceOverScalar",
            "rangeOverSexp",
            "rangeOverStruct",
            "rangeOverList",
            "rangeOverListWithAt",
            "rangeOverListWithAsAndAt",
            "rangeOverListConstructorWithAt",
            "rangeOverListConstructorWithAsAndAt",
            "rangeOverBagWithAt",
            "rangeOverNestedWithAt",
            "explicitAliasSelectSingleSource",
            "selectImplicitAndExplicitAliasSingleSourceHoisted",
            "syntheticColumnNameInSelect",
            "properAliasFromPathInSelect",
            "selectListWithMissing",
            "selectCrossProduct",
            "selectWhereStringEqualsSameCase",
            "selectWhereStrinEqualsDifferentCase",
            "selectJoin",
            "selectCorrelatedJoin",
            "selectCorrelatedLeftJoin",
            "selectCorrelatedLeftJoinOnClause",
            "selectJoinOnClauseScoping",
            "selectNonCorrelatedJoin",
            "selectCorrelatedUnpivot",
            "nestedSelectJoinWithUnpivot",
            "nestedSelectJoinLimit",
            "correlatedJoinWithShadowedAttributes",
            "correlatedJoinWithoutLexicalScope",
            "joinWithShadowedGlobal",
            "pivotFrom",
            "pivotLiteralFieldNameFrom",
            "pivotBadFieldType",
            "pivotUnpivotWithWhereLimit",
            "inPredicate",
            "inPredicateSingleItem",
            "inPredicateSingleExpr",
            "inPredicateSingleItemListVar",
            "inPredicateSingleListVar",
            "inPredicateSubQuerySelectValue",
            "notInPredicate",
            "notInPredicateSingleItem",
            "notInPredicateSingleExpr",
            "notInPredicateSingleItemListVar",
            "notInPredicateSingleListVar",
            "notInPredicateSubQuerySelectValue",
            "inPredicateWithTableConstructor",
            "notInPredicateWithTableConstructor",
            "inPredicateWithExpressionOnRightSide",
            "notInPredicateWithExpressionOnRightSide",
            "simpleCase",
            "simpleCaseNoElse",
            "searchedCase",
            "searchedCaseNoElse",
            "betweenPredicate",
            "notBetweenPredicate",
            "betweenStringsPredicate",
            "notBetweenStringsPredicate",
            "topLevelCountDistinct",
            "topLevelCount",
            "topLevelAllCount",
            "topLevelSum",
            "topLevelAllSum",
            "topLevelDistinctSum",
            "topLevelMin",
            "topLevelDistinctMin",
            "topLevelAllMin",
            "topLevelMax",
            "topLevelDistinctMax",
            "topLevelAllMax",
            "topLevelAvg",
            "topLevelDistinctAvg",
            "topLevelAvgOnlyInt",
            "selectValueAggregate",
            "selectListCountStar",
            "selectListCountVariable",
            "selectListMultipleAggregates",
            "selectListMultipleAggregatesNestedQuery",
            "aggregateInSubqueryOfSelect",
            "aggregateInSubqueryOfSelectValue",
            "aggregateWithAliasingInSubqueryOfSelectValue",
            "undefinedUnqualifiedVariable_inSelect_withProjectionOption",
            "wildcardOrderedNames",
            "aliasWildcardOrderedNames",
            "aliasWildcardOrderedNamesSelectList",
            "aliasOrderedNamesSelectList",
            "selectDistinct",
            "selectDistinctWithAggregate",
            "selectDistinctSubQuery",
            "selectDistinctWithSubQuery",
            "selectDistinctAggregationWithGroupBy",
            "selectDistinctWithGroupBy",
            "selectDistinctWithJoin",
            "selectDistinctStarScalars",
            "selectDistinctStarStructs",
            "selectDistinctStarUnknowns",
            "selectDistinctStarIntegers",
            "selectDistinctValue",
            "selectDistinctExpressionAndWhere",
            "selectDistinctExpression",
            "projectOfListOfList",
            "projectOfBagOfBag",
            "projectOfListOfBag",
            "projectOfBagOfList",
            "projectOfSexp",
            "projectOfUnpivotPath",
            "parameters",
            "unpivotMissing",
            "unpivotEmptyStruct",
            "unpivotMissingWithAsAndAt",
            "unpivotMissingCrossJoinWithAsAndAt",
            "variableShadow",
            "selectValueStructConstructorWithMissing",
            "selectIndexStruct",
            "selectStarSingleSource",
            "implicitAliasSelectSingleSource",
            "selectValues",
            "explicitAliasSelectSingleSourceWithWhere",
            "undefinedQualifiedVariableWithUndefinedVariableBehaviorError",
            "emptySymbol",
            "emptySymbolInGlobals",
            "semicolonAtEndOfExpression",
            "dateTimePartsAsVariableNames",
            "dateTimePartsAsStructFieldNames",
        )

        @JvmStatic
        @Suppress("UNUSED")
        fun evaluatorTests(): List<IonResultTestCase> {
            val unskippedTests = EVALUATOR_TEST_SUITE.getAllTests(SKIP_LIST)

            return unskippedTests.map {
                it.copy(
                    note = "legacy typing",
                    compileOptions = CompileOptions.build(it.compileOptions) { typingMode(TypingMode.LEGACY) })
            } +
                unskippedTests.map {
                    it.copy(
                        note = "permissive typing",
                        compileOptions = CompileOptions.build(it.compileOptions) { typingMode(TypingMode.PERMISSIVE) })
                }
        }
    }

    @ParameterizedTest
    @MethodSource("evaluatorTests")
    fun allTests(tc: IonResultTestCase) = tc.runTestCaseWithPlanner(valueFactory, mockDb)
}

// TODO: move to a better file
internal fun IonResultTestCase.runTestCaseWithPlanner(
    valueFactory: ExprValueFactory,
    db: MockDb,
    compileOptionsBlock: (CompileOptions.Builder.() -> Unit)? = null
) {
    fun runTheTest() {
        val parser = SqlParser(ION)

        val astStatement = assertDoesNotThrow("Parsing the sql under test should not throw for test \"${this.name}\"") {
            parser.parseAstStatement(sqlUnderTest)
        }

        val expectedResult = assertDoesNotThrow(
            "Parsing the expected ion result should not throw for test \"${this.name}\""
        ) {
            expectedIonResult?.let { ION.singleValue(it) }
        }

        val globalBindings = GlobalBindings { bindingName ->
            val result = db.globals.entries.firstOrNull { bindingName.isEquivalentTo(it.key) }
            if(result != null) {
                // Note that the unique id is set to result.key (which is the actual name of the variable)
                // which *might* have different letter case than the [bindingName].
                ResolutionResult.GlobalVariable(ionSymbol(result.key))
            } else {
                ResolutionResult.Undefined
            }
        }

        val plannerResult = assertDoesNotThrow("Planning the query should not throw for test \"${this.name}\"") {
            val qp = createQueryPlanner(ION, globalBindings)
            qp.plan(astStatement)
        }

        val plannedQuery = when(plannerResult) {
            // DL TODO: what to do about warnings?
            is PlanningResult.Success -> plannerResult.physicalPlan
            is PlanningResult.Error -> fail("Failed to plan query for tests \"${this.name}\"")
        }

        val modifiedCompileOptions = when(compileOptionsBlock) {
            null -> compileOptions
            else -> CompileOptions.build { compileOptionsBlock() }
        }

        val expression = assertDoesNotThrow("Compiling the query should not throw for test \"${this.name}\"") {
            PartiqlPhysicalCompiler(
                valueFactory = valueFactory,
                functions = createBuiltinFunctions(valueFactory).associateBy { it.signature.name },
                customTypedOpParameters = emptyMap(),
                procedures = emptyMap(),
                compileOptions = modifiedCompileOptions
            ).compile(plannedQuery)
        }

        val session = EvaluationSession.build {
            globals(db.valueBindings)
            parameters(EVALUATOR_TEST_SUITE.createParameters(valueFactory))
        }

        val (exprValueResult, ionValueResult) = assertDoesNotThrow(
            "evaluating the expression should not throw for test \"${this.name}\""
        ) {
            val result = expression.eval(session)
            result to result.ionValue
        }

        expectedResult?.let { assertIonEquals(it, ionValueResult, "for test \"${this.name}\", ") }

        assertDoesNotThrow("extraAssertions should not throw for test \"${this.name}\"") {
            extraAssertions?.invoke(exprValueResult, compileOptions)
        }
    }

    when {
        !expectFailure -> runTheTest()
        else -> {
            val message = "We expect test \"${this.name}\" to fail, but it did not. This check exists to ensure the " +
                "failing list is up to date."

            assertThrows<Throwable>(message) {
                runTheTest()
            }
        }
    }
}