package org.partiql.lang.eval.visitors

import com.amazon.ion.system.IonSystemBuilder
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.lang.ast.SourceLocationMeta
import org.partiql.lang.ast.passes.SemanticProblemDetails
import org.partiql.lang.ast.passes.inference.StaticTypeInferencer
import org.partiql.lang.ast.passes.inference.StaticTypeInferencer.InferenceResult
import org.partiql.lang.ast.passes.inference.isLob
import org.partiql.lang.ast.passes.inference.isNumeric
import org.partiql.lang.ast.passes.inference.isText
import org.partiql.lang.ast.passes.inference.isUnknown
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.domains.staticType
import org.partiql.lang.errors.Problem
import org.partiql.lang.errors.ProblemSeverity
import org.partiql.lang.eval.Bindings
import org.partiql.lang.eval.ExprFunction
import org.partiql.lang.eval.numberValue
import org.partiql.lang.types.AnyOfType
import org.partiql.lang.types.BagType
import org.partiql.lang.types.BoolType
import org.partiql.lang.types.CollectionType
import org.partiql.lang.types.DecimalType
import org.partiql.lang.types.FunctionSignature
import org.partiql.lang.types.IntType
import org.partiql.lang.types.ListType
import org.partiql.lang.types.NumberConstraint
import org.partiql.lang.types.SexpType
import org.partiql.lang.types.StaticType
import org.partiql.lang.types.StaticType.Companion.ALL_TYPES
import org.partiql.lang.types.StaticType.Companion.ANY
import org.partiql.lang.types.StaticType.Companion.BAG
import org.partiql.lang.types.StaticType.Companion.BOOL
import org.partiql.lang.types.StaticType.Companion.CLOB
import org.partiql.lang.types.StaticType.Companion.DECIMAL
import org.partiql.lang.types.StaticType.Companion.FLOAT
import org.partiql.lang.types.StaticType.Companion.INT
import org.partiql.lang.types.StaticType.Companion.INT2
import org.partiql.lang.types.StaticType.Companion.INT4
import org.partiql.lang.types.StaticType.Companion.INT8
import org.partiql.lang.types.StaticType.Companion.LIST
import org.partiql.lang.types.StaticType.Companion.MISSING
import org.partiql.lang.types.StaticType.Companion.NULL
import org.partiql.lang.types.StaticType.Companion.NULL_OR_MISSING
import org.partiql.lang.types.StaticType.Companion.NUMERIC
import org.partiql.lang.types.StaticType.Companion.SEXP
import org.partiql.lang.types.StaticType.Companion.STRING
import org.partiql.lang.types.StaticType.Companion.STRUCT
import org.partiql.lang.types.StaticType.Companion.SYMBOL
import org.partiql.lang.types.StaticType.Companion.TIMESTAMP
import org.partiql.lang.types.StaticType.Companion.unionOf
import org.partiql.lang.types.StringType
import org.partiql.lang.types.StructType
import org.partiql.lang.types.TypedOpParameter
import org.partiql.lang.types.VarargFormalParameter
import org.partiql.lang.util.cartesianProduct
import org.partiql.lang.util.compareTo
import org.partiql.lang.util.countMatchingSubstrings

class StaticTypeInferenceVisitorTransformTest : VisitorTransformTestBase() {

    @ParameterizedTest
    @MethodSource("parametersForTests")
    fun tests(tc: TestCase) = runTest(tc)

    @ParameterizedTest
    @MethodSource("parametersForNAryArithmeticTests")
    fun naryArithmeticInferenceTests(tc: TestCase) = runTest(tc)

    @ParameterizedTest
    @MethodSource("parametersForUnaryArithmeticOpTests")
    fun unaryArithmeticOpTests(tc: TestCase) = runTest(tc)

    @ParameterizedTest
    @MethodSource("parametersForSimplePathsOnStructs")
    fun simplePathsOnStructs(tc: TestCase) = runTest(tc)

    @ParameterizedTest
    @MethodSource("parametersForSimplePathsOnSequences")
    fun simplePathsOnSequences(tc: TestCase) = runTest(tc)

    @ParameterizedTest
    @MethodSource("parametersForNAryComparisonAndEqualityTests")
    fun naryComparisonAndEqualityInferenceTests(tc: TestCase) = runTest(tc)

    @ParameterizedTest
    @MethodSource("parametersForNAryLogicalTests")
    fun naryLogicalInferenceTests(tc: TestCase) = runTest(tc)

    @ParameterizedTest
    @MethodSource("parametersForNAryConcatTests")
    fun naryConcatInferenceTests(tc: TestCase) = runTest(tc)

    @ParameterizedTest
    @MethodSource("parametersForNAryLikeTests")
    fun naryLikeInferenceTests(tc: TestCase) = runTest(tc)

    @ParameterizedTest
    @MethodSource("parametersForNAryBetweenTests")
    fun naryBetweenInferenceTests(tc: TestCase) = runTest(tc)

    @ParameterizedTest
    @MethodSource("parametersForTrimFunctionTests")
    fun trimFunctionTests(tc: TestCase) = runTest(tc)

    @ParameterizedTest
    @MethodSource("parametersForPathingIntoUnion")
    fun pathingIntoUnion(tc: TestCase) = runTest(tc)

    @ParameterizedTest
    @MethodSource("parametersForSimpleCaseWhen")
    fun simpleCaseWhenTests(tc: TestCase) = runTest(tc)

    @ParameterizedTest
    @MethodSource("parametersForSearchedCaseWhen")
    fun searchedCaseWhenTests(tc: TestCase) = runTest(tc)

    @ParameterizedTest
    @MethodSource("parametersForSimpleAndSearchedCaseWhen")
    fun simpleAndSearchedCaseWhenTests(tc: TestCase) = runTest(tc)

    @ParameterizedTest
    @MethodSource("parametersForNAryOpInTests")
    fun nAryOpInTests(tc: TestCase) = runTest(tc)

    @ParameterizedTest
    @MethodSource("parametersForNullIfTests")
    fun nullIfTests(tc: TestCase) = runTest(tc)

    @ParameterizedTest
    @MethodSource("parametersForStructTests")
    fun structTests(tc: TestCase) = runTest(tc)

    @ParameterizedTest
    @MethodSource("parametersForMultipleInferenceProblemsTests")
    fun multipleInferenceProblemsTests(tc: TestCase) = runTest(tc)

    @ParameterizedTest
    @MethodSource("parametersForContinuationTypeTests")
    fun continuationTypeTests(tc: TestCase) = runTest(tc)

    @ParameterizedTest
    @MethodSource("parametersForJoinPredicateTests")
    fun joinPredicateTests(tc: TestCase) = runTest(tc)

    @ParameterizedTest
    @MethodSource("parametersForSelectWhereTests")
    fun selectWhereTests(tc: TestCase) = runTest(tc)

    @ParameterizedTest
    @MethodSource("parametersForErrorInExpressionSourceLocationTests")
    fun errorInExpressionSourceLocationTests(tc: TestCase) = runTest(tc)

    @ParameterizedTest
    @MethodSource("parametersForErrorInFunctionCallArgumentTests")
    fun errorInFunctionCallArgumentTests(tc: TestCase) = runTest(tc)

    @ParameterizedTest
    @MethodSource("parametersForTypedExpressionTests")
    fun typedExpressionTests(tc: TestCase) = runTest(tc)

    @ParameterizedTest
    @MethodSource("parametersForAggFunctionTests")
    fun aggFunctionTests(tc: TestCase) = runTest(tc)

    private fun runTest(tc: TestCase) {
        val globalBindings = Bindings.ofMap(tc.globals)
        val ion = IonSystemBuilder.standard().build()
        val inferencer = StaticTypeInferencer(
            globalBindings = globalBindings,
            customFunctionSignatures = tc.customFunctionSignatures,
            customTypedOpParameters = customTypedOpParameters
        )

        val defaultVisitorTransforms = basicVisitorTransforms()
        val staticTypeVisitorTransform = StaticTypeVisitorTransform(ion, globalBindings)
        val originalStatement = parse(tc.originalSql).let {
            // We always pass the query under test through all of the basic VisitorTransforms primarily because we need
            // FromSourceAliasVisitorTransform to execute first but also to help ensure the queries we're testing
            // make sense when they're all run.
            defaultVisitorTransforms.transformStatement(it)
        }.let {
            // We then run it through static type VisitorTransform to make sure all the implicit variables are resolved.
            staticTypeVisitorTransform.transformStatement(it)
        }

        // for these tests, we collect all the semantic problems that occur
        val inferenceResult = inferencer.inferStaticType(originalStatement)

        tc.handler(
            when (inferenceResult) {
                is InferenceResult.Success -> ResolveTestResult.Value(tc, inferenceResult.staticType, inferenceResult.problems)
                is InferenceResult.Failure -> ResolveTestResult.Failure(tc, inferenceResult.staticType, inferenceResult.partiqlAst, inferenceResult.problems)
            }
        )
    }

    companion object {
        private const val TOKEN = "{op}"

        private val ALL_UNKNOWN_TYPES = listOf(NULL, MISSING, NULL_OR_MISSING)
        private val ALL_NON_UNKNOWN_TYPES = ALL_TYPES.filter { !it.isUnknown() }
        private val ALL_NUMERIC_TYPES = ALL_NON_UNKNOWN_TYPES.filter { it.isNumeric() }
        private val ALL_NON_NUMERIC_NON_UNKNOWN_TYPES = ALL_NON_UNKNOWN_TYPES.filter { !it.isNumeric() }
        private val ALL_TEXT_TYPES = ALL_NON_UNKNOWN_TYPES.filter { it.isText() }
        private val ALL_NON_TEXT_NON_UNKNOWN_TYPES = ALL_NON_UNKNOWN_TYPES.filter { !it.isText() }
        private val ALL_NON_BOOL_NON_UNKNOWN_TYPES = ALL_NON_UNKNOWN_TYPES.filter { it !is BoolType }
        private val ALL_LOB_TYPES = ALL_NON_UNKNOWN_TYPES.filter { it.isLob() }
        private val ALL_NON_LOB_NON_UNKNOWN_TYPES = ALL_NON_UNKNOWN_TYPES.filter { !it.isLob() }
        private val ALL_NON_COLLECTION_NON_UNKNOWN_TYPES = ALL_NON_UNKNOWN_TYPES.filter { it !is CollectionType }

        // non-unknown [StaticType]s from ALL_TYPES that aren't numeric, text, or lobs
        // This will include all the container types, BOOL, and TIMESTAMP. These are only comparable to unknowns and
        // itself.
        private val ALL_TYPES_ONLY_COMPARABLE_TO_SELF = ALL_NON_UNKNOWN_TYPES.filter { !it.isNumeric() && !it.isText() && !it.isLob() }

        enum class OpType(vararg ops: String) {
            ARITHMETIC("+", "-", "*", "/", "%"),
            COMPARISON("<", "<=", ">", ">="),
            EQUALITY("!=", "="),
            LOGICAL("AND", "OR"),
            CONCAT("||");

            val operators = ops.toList()
        }

        private fun crossExpand(template: String, operators: List<String>): List<String> =
            when (template.countMatchingSubstrings(TOKEN)) {
                0 -> listOf(template)
                else -> {
                    operators.flatMap {
                        val newTemplate = template.replaceFirst(TOKEN, it)
                        crossExpand(newTemplate, operators)
                    }
                }
            }

        /**
         * From the passed lists, [l1] and [l2], returns a list of all unique pairs of permutations taking one element
         * from [l1] and one element from [l2]. This is used when generating NAry op [TestCase]s in which each operand
         * could take on many values.
         *
         * E.g. [l1] = [a, b], [l2] = [b, c]
         *   => [<a, b>, <b, a>, <b, b>, <b, c>, <c, b>]
         */
        private fun generateAllUniquePairs(l1: List<StaticType>, l2: List<StaticType>): List<Pair<StaticType, StaticType>> =
            listOf(l1, l2).cartesianProduct()
                .flatMap { listOf(Pair(it[0], it[1]), Pair(it[1], it[0])) }
                .distinct()

        private fun createReturnsNullOrMissingError(line: Long = 1, col: Long, nAryOp: String): Problem =
            Problem(
                SourceLocationMeta(line, col, nAryOp.length.toLong()),
                SemanticProblemDetails.ExpressionAlwaysReturnsNullOrMissing
            )

        private fun createReturnsNullOrMissingError(sourceLocation: SourceLocationMeta): Problem =
            Problem(
                sourceLocation,
                SemanticProblemDetails.ExpressionAlwaysReturnsNullOrMissing
            )

        private fun createDataTypeMismatchError(line: Long = 1, col: Long, argTypes: List<StaticType>, nAryOp: String): Problem =
            Problem(
                SourceLocationMeta(line, col, nAryOp.length.toLong()),
                SemanticProblemDetails.IncompatibleDatatypesForOp(actualArgumentTypes = argTypes, nAryOp = nAryOp)
            )

        private fun createDataTypeMismatchError(sourceLocation: SourceLocationMeta, argTypes: List<StaticType>, nAryOp: String): Problem =
            Problem(
                sourceLocation,
                SemanticProblemDetails.IncompatibleDatatypesForOp(actualArgumentTypes = argTypes, nAryOp = nAryOp)
            )

        private fun createIncompatibleTypesForExprError(sourceLocation: SourceLocationMeta, expectedType: StaticType, actualType: StaticType): Problem =
            Problem(
                sourceLocation,
                SemanticProblemDetails.IncompatibleDataTypeForExpr(expectedType, actualType)
            )

        private fun createInvalidArgumentTypeForFunctionError(
            sourceLocation: SourceLocationMeta,
            functionName: String,
            expectedArgType: StaticType,
            actualType: StaticType
        ): Problem =
            Problem(
                sourceLocation,
                SemanticProblemDetails.InvalidArgumentTypeForFunction(functionName, expectedArgType, actualType)
            )

        private fun createNullOrMissingFunctionArgumentError(sourceLocation: SourceLocationMeta, functionName: String): Problem =
            Problem(
                sourceLocation,
                SemanticProblemDetails.NullOrMissingFunctionArgument(functionName)
            )

        private fun createNAryOpCases(
            opType: OpType,
            name: String,
            originalSql: String,
            globals: Map<String, StaticType> = mapOf(),
            customFunctionSignatures: List<FunctionSignature> = listOf(),
            handler: (ResolveTestResult) -> Unit
        ) =
            crossExpand(originalSql, opType.operators)
                .map {
                    TestCase(
                        "$it : $name ",
                        it,
                        globals,
                        customFunctionSignatures,
                        handler
                    )
                }

        /**
         * Creates binary op cases (x {op} y) and (y {op} x) with different operators of [opType]
         */
        private fun createSingleNAryOpCasesWithSwappedArgs(
            opType: OpType,
            name: String,
            leftType: StaticType,
            rightType: StaticType,
            handler: (ResolveTestResult) -> Unit
        ) =
            createNAryOpCases(
                opType,
                name,
                "x {op} y",
                mapOf(
                    "x" to leftType,
                    "y" to rightType
                ),
                handler = handler
            ).let {
                if (leftType != rightType) {
                    it +
                        createNAryOpCases(
                            opType,
                            name,
                            "y {op} x",
                            mapOf(
                                "x" to leftType,
                                "y" to rightType
                            ),
                            handler = handler
                        )
                } else {
                    it
                }
            }

        /**
         * Creates ternary op cases with different operators of [opType]
         */
        private fun createDoubleNAryOpCases(
            opType: OpType,
            name: String,
            leftType: StaticType,
            middleType: StaticType,
            rightType: StaticType,
            expectedType: StaticType,
            expectedWarnings: List<Problem>
        ) =
            createNAryOpCases(
                opType,
                name,
                "x {op} y {op} z",
                mapOf(
                    "x" to leftType,
                    "y" to middleType,
                    "z" to rightType
                ),
                handler = expectQueryOutputType(expectedType, expectedWarnings)
            )

        /**
         * Creates two test cases with the specified operand and expected types and warnings for every arithmetic
         * operator (creates x {op} y and y {op} x), about `ARITHMETIC_OPERATORS.size * 2` test cases in total.
         * If leftType != rightType, then new testCase "y {op} x" is created.
         */
        private fun singleArithmeticOpCases(
            name: String,
            leftType: StaticType,
            rightType: StaticType,
            expectedType: StaticType,
            expectedWarnings: List<Problem> = emptyList()
        ) =
            createSingleNAryOpCasesWithSwappedArgs(OpType.ARITHMETIC, name, leftType, rightType, expectQueryOutputType(expectedType, expectedWarnings))

        /**
         * Creates a [TestCase] with the query "x [op] y" with x bound to [leftType] and y to [rightType]. This
         * [TestCase] expects [expectedProblems] during inference.
         */
        private fun singleNAryOpErrorTestCase(
            name: String,
            op: String,
            leftType: StaticType,
            rightType: StaticType,
            expectedProblems: List<Problem>
        ) =
            TestCase(
                name = "x $op y : $name",
                originalSql = "x $op y",
                globals = mapOf(
                    "x" to leftType,
                    "y" to rightType
                ),
                handler = expectSemanticProblems(expectedProblems)
            )

        /**
         * Creates [TestCase]s with the query "x [op] y" with x bound to [leftType] and [rightType]. Each [TestCase]
         * expects to find [SemanticProblemDetails.IncompatibleDatatypesForOp].
         *
         * If [leftType] != [rightType], then new [TestCase] "y [op] x" is created.
         */
        private fun singleNAryOpMismatchWithSwappedCases(
            name: String,
            op: String,
            leftType: StaticType,
            rightType: StaticType,
        ): List<TestCase> {
            val originalTestCase = TestCase(
                name = "x $op y : $name",
                originalSql = "x $op y",
                globals = mapOf(
                    "x" to leftType,
                    "y" to rightType
                ),
                handler = expectSemanticProblems(
                    listOf(createDataTypeMismatchError(col = 3, argTypes = listOf(leftType, rightType), nAryOp = op))
                )
            )

            return if (leftType == rightType) {
                listOf(originalTestCase)
            } else {
                listOf(
                    originalTestCase,
                    TestCase(
                        name = "x $op y : $name",
                        originalSql = "x $op y",
                        globals = mapOf(
                            "x" to rightType,
                            "y" to leftType
                        ),
                        handler = expectSemanticProblems(
                            listOf(createDataTypeMismatchError(col = 3, argTypes = listOf(rightType, leftType), nAryOp = op))
                        )
                    )
                )
            }
        }

        /**
         * Creates one test case with the specified operand and expected types for every arithmetic
         * operator, combined with every other arithmetic operator, `ARITHMETIC_OPERATORS.size^2` test
         * cases in total.
         */
        private fun doubleArithmeticOpCases(
            name: String,
            leftType: StaticType,
            middleType: StaticType,
            rightType: StaticType,
            expectedType: StaticType,
            expectedWarnings: List<Problem> = emptyList()
        ) =
            createDoubleNAryOpCases(OpType.ARITHMETIC, name, leftType, middleType, rightType, expectedType, expectedWarnings)

        @JvmStatic
        @Suppress("UNUSED")
        fun parametersForNAryArithmeticTests() =
            // Same numeric operand types, single binary operator
            ALL_NUMERIC_TYPES.flatMap { numericType ->
                singleArithmeticOpCases(
                    name = "$numericType",
                    leftType = numericType,
                    rightType = numericType,
                    expectedType = numericType
                )
            } +
                // Same numeric operand types, double binary operators
                ALL_NUMERIC_TYPES.flatMap { numericType ->
                    doubleArithmeticOpCases(
                        name = "$numericType",
                        leftType = numericType,
                        middleType = numericType,
                        rightType = numericType,
                        expectedType = numericType
                    )
                } +
                listOf(
                    // mixed operand types, single binary operators
                    singleArithmeticOpCases(
                        name = "int2 and int4 operands",
                        leftType = INT2,
                        rightType = INT4,
                        expectedType = INT4
                    ),
                    singleArithmeticOpCases(
                        name = "int2 and int8 operands",
                        leftType = INT2,
                        rightType = INT8,
                        expectedType = INT8
                    ),
                    singleArithmeticOpCases(
                        name = "int2 and int operands",
                        leftType = INT2,
                        rightType = INT,
                        expectedType = INT
                    ),
                    singleArithmeticOpCases(
                        name = "int4 and int8 operands",
                        leftType = INT4,
                        rightType = INT8,
                        expectedType = INT8
                    ),
                    singleArithmeticOpCases(
                        name = "int4 and int operands",
                        leftType = INT4,
                        rightType = INT,
                        expectedType = INT
                    ),
                    singleArithmeticOpCases(
                        name = "int4 and any_of(int2, int4) operands",
                        leftType = INT4,
                        rightType = unionOf(INT2, INT4),
                        expectedType = INT4
                    ),
                    singleArithmeticOpCases(
                        name = "int8 and any_of(int2, int4)",
                        leftType = INT8,
                        rightType = unionOf(INT2, INT4),
                        expectedType = INT8
                    ),
                    singleArithmeticOpCases(
                        name = "int8 and any_of(int2, int4, float)",
                        leftType = INT8,
                        rightType = unionOf(INT2, INT4, FLOAT),
                        expectedType = unionOf(INT8, FLOAT)
                    ),
                    singleArithmeticOpCases(
                        name = "int8 and any_of(int8, int2, int4, float, decimal)",
                        leftType = INT8,
                        rightType = unionOf(INT2, INT4, FLOAT, DECIMAL),
                        expectedType = unionOf(INT8, FLOAT, DECIMAL)
                    ),
                    singleArithmeticOpCases(
                        name = "any_of(int8, decimal) and any_of(int2, int4, float)",
                        leftType = unionOf(INT8, DECIMAL),
                        rightType = unionOf(INT2, INT4, FLOAT),
                        expectedType = unionOf(INT8, FLOAT, DECIMAL)
                    ),
                    doubleArithmeticOpCases(
                        name = "int2, int4 and int8",
                        leftType = INT2,
                        middleType = INT4,
                        rightType = INT8,
                        expectedType = INT8
                    ),

                    // mixed operand types, double binary operators
                    doubleArithmeticOpCases(
                        name = "int8, int4 and int2",
                        leftType = INT8,
                        middleType = INT4,
                        rightType = INT2,
                        expectedType = INT8
                    ),
                    doubleArithmeticOpCases(
                        name = "any_of(int8, decimal) and int4",
                        leftType = unionOf(INT8, DECIMAL),
                        middleType = INT4,
                        rightType = INT2,
                        expectedType = unionOf(INT8, DECIMAL)
                    ),
                    doubleArithmeticOpCases(
                        name = "any_of(int8, decimal), any_of(int4, float, missing) and any_of(int2, decimal)",
                        leftType = unionOf(INT8, DECIMAL),
                        middleType = unionOf(INT4, FLOAT, MISSING),
                        rightType = unionOf(INT2, DECIMAL),
                        expectedType = unionOf(
                            MISSING,
                            INT8,
                            FLOAT,
                            DECIMAL
                        )
                    ),

                    // NULL propagation, single binary operators
                    singleArithmeticOpCases(
                        name = "one nullable operand",
                        leftType = INT4.asNullable(),
                        rightType = INT4,
                        expectedType = INT4.asNullable()
                    ),
                    singleArithmeticOpCases(
                        name = "two nullable operands",
                        leftType = INT4.asNullable(),
                        rightType = INT4.asNullable(),
                        expectedType = INT4.asNullable()
                    ),
                    singleArithmeticOpCases(
                        name = "int4, union(int4, float)",
                        leftType = INT4,
                        rightType = AnyOfType(setOf(INT4, FLOAT)),
                        expectedType = AnyOfType(setOf(INT4, FLOAT))
                    ),
                    singleArithmeticOpCases(
                        name = "int4, union(int4, float)",
                        leftType = DECIMAL,
                        rightType = AnyOfType(setOf(INT4, FLOAT)),
                        expectedType = DECIMAL
                    ),
                    singleArithmeticOpCases(
                        name = "any, int",
                        leftType = ANY,
                        rightType = INT,
                        expectedType = unionOf(
                            MISSING,
                            NULL,
                            INT,
                            FLOAT,
                            DECIMAL
                        )
                    ),
                    singleArithmeticOpCases(
                        name = "any, float",
                        leftType = ANY,
                        rightType = FLOAT,
                        expectedType = unionOf(
                            MISSING,
                            NULL,
                            FLOAT,
                            DECIMAL
                        )
                    ),
                    singleArithmeticOpCases(
                        name = "any, decimal",
                        leftType = ANY,
                        rightType = DECIMAL,
                        expectedType = unionOf(
                            MISSING,
                            NULL,
                            DECIMAL
                        )
                    ),
                    singleArithmeticOpCases(
                        name = "any, any",
                        leftType = ANY,
                        rightType = ANY,
                        expectedType = unionOf(
                            MISSING,
                            NULL,
                            INT,
                            INT2,
                            INT4,
                            INT8,
                            FLOAT,
                            DECIMAL
                        )
                    ),

                    // NULL propagation, single binary operator
                    singleArithmeticOpCases(
                        name = "int4, union(null, float)",
                        leftType = INT4,
                        rightType = FLOAT.asNullable(),
                        expectedType = FLOAT.asNullable()
                    ),

                    // NULL propagation, double binary operators
                    doubleArithmeticOpCases(
                        name = "one nullable operand, 1 of 3",
                        leftType = INT4.asNullable(),
                        middleType = INT4,
                        rightType = INT4,
                        expectedType = INT4.asNullable()
                    ),
                    doubleArithmeticOpCases(
                        name = "one nullable operand, 2 of 3",
                        leftType = INT4,
                        middleType = INT4.asNullable(),
                        rightType = INT4,
                        expectedType = INT4.asNullable()
                    ),
                    doubleArithmeticOpCases(
                        name = "one nullable operand, 3 of 3",
                        leftType = INT4,
                        middleType = INT4,
                        rightType = INT4.asNullable(),
                        expectedType = INT4.asNullable()
                    ),
                    doubleArithmeticOpCases(
                        name = "three nullable operands",
                        leftType = INT4.asNullable(),
                        middleType = INT4.asNullable(),
                        rightType = INT4.asNullable(),
                        expectedType = INT4.asNullable()
                    ),

                    // MISSING propagation, single binary operators
                    singleArithmeticOpCases(
                        name = "one optional operand, 1 of 2",
                        leftType = INT4.asOptional(),
                        rightType = INT4,
                        expectedType = INT4.asOptional()
                    ),
                    singleArithmeticOpCases(
                        name = "one optional operand, 2 of 2",
                        leftType = INT4,
                        rightType = INT4.asOptional(),
                        expectedType = INT4.asOptional()
                    ),
                    singleArithmeticOpCases(
                        name = "two optional operands",
                        leftType = INT4.asOptional(),
                        rightType = INT4.asOptional(),
                        expectedType = INT4.asOptional()
                    ),

                    // NULL propagation, double binary operators
                    doubleArithmeticOpCases(
                        name = "one optional operand, 1 of 3",
                        leftType = INT4.asOptional(),
                        middleType = INT4,
                        rightType = INT4,
                        expectedType = INT4.asOptional()
                    ),
                    doubleArithmeticOpCases(
                        name = "one optional operand, 2 of 3",
                        leftType = INT4,
                        middleType = INT4.asOptional(),
                        rightType = INT4,
                        expectedType = INT4.asOptional()
                    ),
                    doubleArithmeticOpCases(
                        name = "one optional operand, 3 of 3",
                        leftType = INT4,
                        middleType = INT4,
                        rightType = INT4.asOptional(),
                        expectedType = INT4.asOptional()
                    ),
                    doubleArithmeticOpCases(
                        name = "three optional operands",
                        leftType = INT4.asOptional(),
                        middleType = INT4.asOptional(),
                        rightType = INT4.asOptional(),
                        expectedType = INT4.asOptional()
                    ),
                    doubleArithmeticOpCases(
                        name = "int4, float, int4",
                        leftType = INT4,
                        middleType = FLOAT,
                        rightType = INT4,
                        expectedType = FLOAT
                    ),
                    doubleArithmeticOpCases(
                        name = "float, decimal, int4",
                        leftType = FLOAT,
                        middleType = DECIMAL,
                        rightType = INT4,
                        expectedType = DECIMAL
                    ),
                    doubleArithmeticOpCases(
                        name = "nullable and optional",
                        leftType = INT4,
                        middleType = INT4.asNullable(),
                        rightType = INT4.asOptional(),
                        expectedType = INT4.asOptional().asNullable()
                    ),

                    //
                    // data type mismatch cases for arithmetic ops below
                    //
                    OpType.ARITHMETIC.operators.flatMap { op ->
                        // non-numeric, non-unknown with non-unknown -> data type mismatch error
                        generateAllUniquePairs(ALL_NON_NUMERIC_NON_UNKNOWN_TYPES, ALL_NON_UNKNOWN_TYPES).map {
                            singleNAryOpErrorTestCase(
                                name = "data type mismatch - ${it.first}, ${it.second}",
                                op = op,
                                leftType = it.first,
                                rightType = it.second,
                                expectedProblems = listOf(
                                    createDataTypeMismatchError(col = 3, argTypes = listOf(it.first, it.second), nAryOp = op)
                                )
                            )
                        } +
                            // non-numeric, non-unknown with an unknown -> data type mismatch and null or missing error
                            generateAllUniquePairs(ALL_NON_NUMERIC_NON_UNKNOWN_TYPES, ALL_UNKNOWN_TYPES).map {
                                singleNAryOpErrorTestCase(
                                    name = "data type mismatch, null or missing error - ${it.first}, ${it.second}",
                                    op = op,
                                    leftType = it.first,
                                    rightType = it.second,
                                    expectedProblems = listOf(
                                        createDataTypeMismatchError(col = 3, argTypes = listOf(it.first, it.second), nAryOp = op),
                                        createReturnsNullOrMissingError(col = 3, nAryOp = op)
                                    )
                                )
                            } +
                            // numeric with an unknown -> null or missing error
                            generateAllUniquePairs(ALL_NUMERIC_TYPES, ALL_UNKNOWN_TYPES).map {
                                singleNAryOpErrorTestCase(
                                    name = "null or missing error - ${it.first}, ${it.second}",
                                    op = op,
                                    leftType = it.first,
                                    rightType = it.second,
                                    expectedProblems = listOf(createReturnsNullOrMissingError(col = 3, nAryOp = op))
                                )
                            } +
                            // unknown with an unknown -> null or missing error
                            generateAllUniquePairs(ALL_UNKNOWN_TYPES, ALL_UNKNOWN_TYPES).map {
                                singleNAryOpErrorTestCase(
                                    name = "null or missing error - ${it.first}, ${it.second}",
                                    op = op,
                                    leftType = it.first,
                                    rightType = it.second,
                                    expectedProblems = listOf(createReturnsNullOrMissingError(col = 3, nAryOp = op))
                                )
                            } +
                            listOf(
                                // double arithmetic ops with unknowns -> null or missing errors
                                doubleOpErrorCases(
                                    name = "null, null, null",
                                    op = op,
                                    leftType = NULL,
                                    middleType = NULL,
                                    rightType = NULL,
                                    expectedProblems = listOf(
                                        createReturnsNullOrMissingError(col = 3, nAryOp = op),
                                        createReturnsNullOrMissingError(col = 7, nAryOp = op)
                                    )
                                ),
                                doubleOpErrorCases(
                                    name = "null, null, missing",
                                    op = op,
                                    leftType = NULL,
                                    middleType = NULL,
                                    rightType = MISSING,
                                    expectedProblems = listOf(
                                        createReturnsNullOrMissingError(col = 3, nAryOp = op),
                                        createReturnsNullOrMissingError(col = 7, nAryOp = op)
                                    )
                                ),
                                doubleOpErrorCases(
                                    name = "missing, null, null",
                                    op = op,
                                    leftType = MISSING,
                                    middleType = NULL,
                                    rightType = NULL,
                                    expectedProblems = listOf(
                                        createReturnsNullOrMissingError(col = 3, nAryOp = op),
                                        createReturnsNullOrMissingError(col = 7, nAryOp = op)
                                    )
                                ),
                                doubleOpErrorCases(
                                    name = "null, missing, null",
                                    op = op,
                                    leftType = NULL,
                                    middleType = MISSING,
                                    rightType = NULL,
                                    expectedProblems = listOf(
                                        createReturnsNullOrMissingError(col = 3, nAryOp = op),
                                        createReturnsNullOrMissingError(col = 7, nAryOp = op)
                                    )
                                ),
                                doubleOpErrorCases(
                                    name = "missing, missing, null",
                                    op = op,
                                    leftType = MISSING,
                                    middleType = MISSING,
                                    rightType = NULL,
                                    expectedProblems = listOf(
                                        createReturnsNullOrMissingError(col = 3, nAryOp = op),
                                        createReturnsNullOrMissingError(col = 7, nAryOp = op)
                                    )
                                ),
                                doubleOpErrorCases(
                                    name = "null, missing, missing",
                                    op = op,
                                    leftType = NULL,
                                    middleType = MISSING,
                                    rightType = MISSING,
                                    expectedProblems = listOf(
                                        createReturnsNullOrMissingError(col = 3, nAryOp = op),
                                        createReturnsNullOrMissingError(col = 7, nAryOp = op)
                                    )
                                ),
                                doubleOpErrorCases(
                                    name = "missing, null, missing",
                                    op = op,
                                    leftType = MISSING,
                                    middleType = NULL,
                                    rightType = MISSING,
                                    expectedProblems = listOf(
                                        createReturnsNullOrMissingError(col = 3, nAryOp = op),
                                        createReturnsNullOrMissingError(col = 7, nAryOp = op)
                                    )
                                ),
                                doubleOpErrorCases(
                                    name = "missing, missing, missing",
                                    op = op,
                                    leftType = MISSING,
                                    middleType = MISSING,
                                    rightType = MISSING,
                                    expectedProblems = listOf(
                                        createReturnsNullOrMissingError(col = 3, nAryOp = op),
                                        createReturnsNullOrMissingError(col = 7, nAryOp = op)
                                    )
                                )
                            ) +
                            // other test cases resulting in a data type mismatch
                            listOf(
                                Pair(unionOf(STRING, SYMBOL), SYMBOL),
                                Pair(unionOf(STRING, SYMBOL), unionOf(STRING, SYMBOL)),
                                Pair(STRING.asNullable(), INT4),
                                Pair(STRING.asOptional(), INT4),
                                Pair(STRING.asNullable().asOptional(), INT4),
                                Pair(ANY, STRING)
                            ).flatMap {
                                singleNAryOpMismatchWithSwappedCases(
                                    name = "data type mismatch - ${it.first}, ${it.second}",
                                    op = op,
                                    leftType = it.first,
                                    rightType = it.second
                                )
                            }
                    }
                ).flatten()

        /**
         * Creates a test case for each unary arithmetic operand (+, -) of the form `{unary op} x` with [argType]
         * corresponding to `x`'s static type. Test cases are expected to result in [expectedOutputType] as the output
         * static type and have [expectedWarnings] from inference.
         */
        private fun createUnaryArithmeticOpCases(
            name: String,
            argType: StaticType,
            expectedOutputType: StaticType,
            expectedWarnings: List<Problem> = emptyList()
        ) =
            listOf("+", "-").map { op ->
                val query = "$op x"
                TestCase(
                    name = "$query : $name",
                    originalSql = query,
                    globals = mapOf("x" to argType),
                    handler = expectQueryOutputType(expectedOutputType, expectedWarnings)
                )
            }

        /**
         * Creates a test case with the specified unary [op] of the form `[op] x` with [argType] corresponding to
         * `x`'s static type. Each test case is expected to have [expectedProblems] through inference.
         */
        private fun createUnaryOpErrorCase(
            name: String,
            op: String,
            argType: StaticType,
            expectedProblems: List<Problem>
        ) =
            TestCase(
                name = "$op x : $name",
                originalSql = "$op x",
                globals = mapOf("x" to argType),
                handler = expectSemanticProblems(expectedProblems)
            )

        @JvmStatic
        @Suppress("UNUSED")
        private fun parametersForUnaryArithmeticOpTests() = listOf(
            // numeric operand type
            ALL_NUMERIC_TYPES.flatMap { numericType ->
                createUnaryArithmeticOpCases(
                    name = "$numericType",
                    argType = numericType,
                    expectedOutputType = numericType
                )
            } +
                createUnaryArithmeticOpCases(
                    name = "unary op - ANY",
                    argType = ANY,
                    expectedOutputType = unionOf(
                        NULL,
                        MISSING,
                        FLOAT,
                        INT2,
                        INT4,
                        INT8,
                        INT,
                        DECIMAL,
                        FLOAT
                    )
                ),
            createUnaryArithmeticOpCases(
                name = "unary op - union(INT, STRING)",
                argType = unionOf(INT, STRING),
                expectedOutputType = unionOf(INT, MISSING)
            )
        ).flatten() +
            //
            // data type mismatch cases below this line
            //
            listOf("+", "-").flatMap { op ->
                // unknown -> expression always returns null or missing error
                ALL_UNKNOWN_TYPES.map { unknownType ->
                    createUnaryOpErrorCase(
                        name = "unary op with unknown op error - $unknownType",
                        op = op,
                        argType = unknownType,
                        expectedProblems = listOf(
                            createReturnsNullOrMissingError(col = 1, nAryOp = op)
                        )
                    )
                } +
                    // incompatible types for unary arithmetic -> data type mismatch
                    ALL_NON_NUMERIC_NON_UNKNOWN_TYPES.map { nonNumericType ->
                        createUnaryOpErrorCase(
                            name = "unary op with data type mismatch - $nonNumericType",
                            op = op,
                            argType = nonNumericType,
                            expectedProblems = listOf(
                                createDataTypeMismatchError(col = 1, argTypes = listOf(nonNumericType), nAryOp = op)
                            )
                        )
                    } +
                    listOf(
                        // other unary arithmetic tests
                        createUnaryOpErrorCase(
                            name = "unary op with data type mismatch - union(STRING, SYMBOL)",
                            op = op,
                            argType = unionOf(STRING, SYMBOL),
                            expectedProblems = listOf(
                                createDataTypeMismatchError(col = 1, argTypes = listOf(unionOf(STRING, SYMBOL)), nAryOp = op)
                            )
                        ),
                        createUnaryOpErrorCase(
                            name = "unary op with data type mismatch - nullable string",
                            op = op,
                            argType = STRING.asNullable(),
                            expectedProblems = listOf(
                                createDataTypeMismatchError(col = 1, argTypes = listOf(STRING.asNullable()), nAryOp = op)
                            )
                        ),
                        createUnaryOpErrorCase(
                            name = "unary op with data type mismatch - optional string",
                            op = op,
                            argType = STRING.asOptional(),
                            expectedProblems = listOf(
                                createDataTypeMismatchError(col = 1, argTypes = listOf(STRING.asOptional()), nAryOp = op)
                            )
                        ),
                        createUnaryOpErrorCase(
                            name = "unary op with data type mismatch - nullable, optional string",
                            op = op,
                            argType = STRING.asNullable().asOptional(),
                            expectedProblems = listOf(
                                createDataTypeMismatchError(col = 1, argTypes = listOf(STRING.asNullable().asOptional()), nAryOp = op)
                            )
                        )
                    )
            }

        /**
         * Creates two test cases with the specified operand and expected types for every NAry comparison and equality
         * operators, about `(COMPARISON_OPERATORS.size + EQUALITY_OPERATORS.size) X 2` test cases in total.
         * If [leftType] != [rightType], then new [TestCase] "y {op} x" is created.
         *
         * The expected output type will be [expectedComparisonType] for every created comparison op test case. If
         * [expectedEqualityType] is not specified, the created equality op test case will default to use
         * [expectedComparisonType]. Otherwise, the created equality op test case will use [expectedEqualityType] as the
         * expected output type.
         */
        private fun singleNAryComparisonAndEqualityCases(
            name: String,
            leftType: StaticType,
            rightType: StaticType,
            expectedComparisonType: StaticType,
            expectedEqualityType: StaticType = expectedComparisonType,
            expectedWarnings: List<Problem> = emptyList()
        ) =
            createSingleNAryOpCasesWithSwappedArgs(
                OpType.COMPARISON,
                name,
                leftType,
                rightType,
                expectQueryOutputType(expectedComparisonType, expectedWarnings)
            ) +
                createSingleNAryOpCasesWithSwappedArgs(
                    OpType.EQUALITY,
                    name,
                    leftType,
                    rightType,
                    expectQueryOutputType(expectedEqualityType, expectedWarnings)
                )

        @JvmStatic
        @Suppress("unused")
        fun parametersForNAryComparisonAndEqualityTests() =
            // number {comparison/equality op} number -> bool
            generateAllUniquePairs(ALL_NUMERIC_TYPES, ALL_NUMERIC_TYPES).flatMap {
                singleNAryComparisonAndEqualityCases(
                    name = "${it.first}, ${it.second}",
                    leftType = it.first,
                    rightType = it.second,
                    expectedComparisonType = BOOL
                )
            } +
                // text {comparison/equality op} text -> bool
                generateAllUniquePairs(ALL_TEXT_TYPES, ALL_TEXT_TYPES).flatMap {
                    singleNAryComparisonAndEqualityCases(
                        name = "${it.first}, ${it.second}",
                        leftType = it.first,
                        rightType = it.second,
                        expectedComparisonType = BOOL
                    )
                } +
                // lob {comparison/equality op} lob -> bool
                generateAllUniquePairs(ALL_LOB_TYPES, ALL_LOB_TYPES).flatMap {
                    singleNAryComparisonAndEqualityCases(
                        name = "${it.first}, ${it.second}",
                        leftType = it.first,
                        rightType = it.second,
                        expectedComparisonType = BOOL
                    )
                } +
                listOf(
                    singleNAryComparisonAndEqualityCases(
                        name = "bool, bool",
                        leftType = BOOL,
                        rightType = BOOL,
                        expectedComparisonType = BOOL
                    ),
                    singleNAryComparisonAndEqualityCases(
                        name = "timestamp, timestamp",
                        leftType = TIMESTAMP,
                        rightType = TIMESTAMP,
                        expectedComparisonType = BOOL
                    ),
                    singleNAryComparisonAndEqualityCases(
                        name = "list, list",
                        leftType = LIST,
                        rightType = LIST,
                        expectedComparisonType = BOOL
                    ),
                    singleNAryComparisonAndEqualityCases(
                        name = "sexp, sexp",
                        leftType = SEXP,
                        rightType = SEXP,
                        expectedComparisonType = BOOL
                    ),
                    singleNAryComparisonAndEqualityCases(
                        name = "bag, bag",
                        leftType = BAG,
                        rightType = BAG,
                        expectedComparisonType = BOOL
                    ),
                    singleNAryComparisonAndEqualityCases(
                        name = "struct, struct",
                        leftType = STRUCT,
                        rightType = STRUCT,
                        expectedComparisonType = BOOL
                    ),
                    singleNAryComparisonAndEqualityCases(
                        name = "int4, union(string, float); equality gives bool",
                        leftType = INT4,
                        rightType = unionOf(STRING, FLOAT),
                        expectedComparisonType = unionOf(MISSING, BOOL),
                        expectedEqualityType = BOOL
                    ),
                    singleNAryComparisonAndEqualityCases(
                        name = "int4, union(null, float)",
                        leftType = INT4,
                        rightType = unionOf(NULL, FLOAT),
                        expectedComparisonType = unionOf(NULL, BOOL)
                    ),
                    singleNAryComparisonAndEqualityCases(
                        name = "int4, union(missing, float)",
                        leftType = INT4,
                        rightType = unionOf(MISSING, FLOAT),
                        expectedComparisonType = unionOf(MISSING, BOOL)
                    ),
                    singleNAryComparisonAndEqualityCases(
                        name = "int4, any",
                        leftType = INT4,
                        rightType = ANY,
                        expectedComparisonType = unionOf(MISSING, NULL, BOOL)
                    ),
                    singleNAryComparisonAndEqualityCases(
                        name = "union(int4, float), union(int4, string); equality gives bool",
                        leftType = unionOf(INT4, FLOAT),
                        rightType = unionOf(INT4, STRING),
                        expectedComparisonType = unionOf(MISSING, BOOL),
                        expectedEqualityType = BOOL
                    ),
                    singleNAryComparisonAndEqualityCases(
                        name = "union(int4, decimal), union(int4, float)",
                        leftType = unionOf(INT4, DECIMAL),
                        rightType = unionOf(INT4, FLOAT),
                        expectedComparisonType = BOOL
                    ),
                    singleNAryComparisonAndEqualityCases(
                        name = "union(int4, string), union(int4, string); equality gives bool",
                        leftType = unionOf(INT4, STRING),
                        rightType = unionOf(INT4, STRING),
                        expectedComparisonType = unionOf(MISSING, BOOL),
                        expectedEqualityType = BOOL
                    ),
                    // Collections with different, comparable element types
                    singleNAryComparisonAndEqualityCases(
                        name = "list(int), list(decimal)",
                        leftType = ListType(INT),
                        rightType = ListType(DECIMAL),
                        expectedComparisonType = BOOL
                    ),
                    singleNAryComparisonAndEqualityCases(
                        name = "list(int), list(null)",
                        leftType = ListType(INT),
                        rightType = ListType(NULL),
                        expectedComparisonType = BOOL
                    ),
                    singleNAryComparisonAndEqualityCases(
                        name = "list(int), list(missing)",
                        leftType = ListType(INT),
                        rightType = ListType(MISSING),
                        expectedComparisonType = BOOL
                    ),
                    singleNAryComparisonAndEqualityCases(
                        name = "list(int), list(unionOf(int, decimal))",
                        leftType = ListType(INT),
                        rightType = ListType(unionOf(INT, DECIMAL)),
                        expectedComparisonType = BOOL
                    ),
                    singleNAryComparisonAndEqualityCases(
                        name = "list(int), list(unionOf(int, timestamp))",
                        leftType = ListType(INT),
                        rightType = ListType(unionOf(INT, TIMESTAMP)),
                        expectedComparisonType = BOOL
                    ),
                    // Collections with different, incomparable element types doesn't give any error/warning. Further
                    // container comparability checks deferred to later https://github.com/partiql/partiql-lang-kotlin/issues/505
                    singleNAryComparisonAndEqualityCases(
                        name = "list(int), list(timestamp)",
                        leftType = ListType(INT),
                        rightType = ListType(TIMESTAMP),
                        expectedComparisonType = BOOL
                    ),
                    singleNAryComparisonAndEqualityCases(
                        name = "sexp(int), sexp(timestamp)",
                        leftType = SexpType(INT),
                        rightType = SexpType(TIMESTAMP),
                        expectedComparisonType = BOOL
                    ),
                    singleNAryComparisonAndEqualityCases(
                        name = "bag(int), bag(timestamp)",
                        leftType = BagType(INT),
                        rightType = BagType(TIMESTAMP),
                        expectedComparisonType = BOOL
                    ),
                    singleNAryComparisonAndEqualityCases(
                        name = "list(int), list(unionOf(timestamp, bool))",
                        leftType = ListType(INT),
                        rightType = ListType(unionOf(TIMESTAMP, BOOL)),
                        expectedComparisonType = BOOL
                    ),
                    singleNAryComparisonAndEqualityCases(
                        name = "list(list(int)), list(list(timestamp)) - nested incompatible lists",
                        leftType = ListType(ListType(INT)),
                        rightType = ListType(ListType(TIMESTAMP)),
                        expectedComparisonType = BOOL
                    ),
                    // structs with comparable fields
                    singleNAryComparisonAndEqualityCases(
                        name = "struct(a to int), struct(a to decimal)",
                        leftType = StructType(mapOf("a" to INT)),
                        rightType = StructType(mapOf("a" to DECIMAL)),
                        expectedComparisonType = BOOL
                    ),
                    singleNAryComparisonAndEqualityCases(
                        name = "struct(a to int, b to string), struct(a to decimal, b to symbol) - multiple, comparable fields",
                        leftType = StructType(mapOf("a" to INT, "b" to STRING)),
                        rightType = StructType(mapOf("a" to DECIMAL, "b" to SYMBOL)),
                        expectedComparisonType = BOOL
                    ),
                    singleNAryComparisonAndEqualityCases(
                        name = "struct(a to int), struct(a to missing)",
                        leftType = StructType(mapOf("a" to INT)),
                        rightType = StructType(mapOf("a" to MISSING)),
                        expectedComparisonType = BOOL
                    ),
                    // structs with different numbers of fields. Further container comparability checks deferred to later
                    // https://github.com/partiql/partiql-lang-kotlin/issues/505
                    singleNAryComparisonAndEqualityCases(
                        name = "struct(a to int), struct()",
                        leftType = StructType(mapOf("a" to INT)),
                        rightType = STRUCT,
                        expectedComparisonType = BOOL
                    ),
                    singleNAryComparisonAndEqualityCases(
                        name = "struct(a to int), struct(a to decimal, b to float)",
                        leftType = StructType(mapOf("a" to INT)),
                        rightType = StructType(mapOf("a" to DECIMAL, "b" to FLOAT)),
                        expectedComparisonType = BOOL
                    ),
                    // structs with incomparable fields. Further container comparability checks deferred to later
                    // https://github.com/partiql/partiql-lang-kotlin/issues/505
                    singleNAryComparisonAndEqualityCases(
                        name = "struct(a to int), struct(a to timestamp)",
                        leftType = StructType(mapOf("a" to INT)),
                        rightType = StructType(mapOf("a" to TIMESTAMP)),
                        expectedComparisonType = BOOL
                    ),
                    singleNAryComparisonAndEqualityCases(
                        name = "struct(a to int, b to symbol), struct(a to timestamp, b to timestamp) - multiple incomparable",
                        leftType = StructType(mapOf("a" to INT, "b" to SYMBOL)),
                        rightType = StructType(mapOf("a" to TIMESTAMP, "b" to TIMESTAMP)),
                        expectedComparisonType = BOOL
                    ),
                    // struct with different number of fields an incomparable field
                    singleNAryComparisonAndEqualityCases(
                        name = "struct(a to int), struct(a to timestamp, b to timestamp)",
                        leftType = StructType(mapOf("a" to INT)),
                        rightType = StructType(mapOf("a" to TIMESTAMP, "b" to TIMESTAMP)),
                        expectedComparisonType = BOOL
                    ),
                ).flatten() +
                (OpType.COMPARISON.operators + OpType.EQUALITY.operators).flatMap { op ->
                    // comparing numeric type with non-numeric, non-unknown type -> data type mismatch
                    ALL_NUMERIC_TYPES.flatMap { numericType ->
                        ALL_NON_NUMERIC_NON_UNKNOWN_TYPES.map { nonNumericType ->
                            singleNAryOpErrorTestCase(
                                name = "data type mismatch - $numericType, $nonNumericType",
                                op = op,
                                leftType = numericType,
                                rightType = nonNumericType,
                                expectedProblems = listOf(
                                    createDataTypeMismatchError(col = 3, argTypes = listOf(numericType, nonNumericType), nAryOp = op)
                                )
                            )
                        }
                    } +
                        // comparing text type with non-text, non-unknown type -> data type mismatch
                        ALL_TEXT_TYPES.flatMap { textType ->
                            ALL_NON_TEXT_NON_UNKNOWN_TYPES.map { nonTextType ->
                                singleNAryOpErrorTestCase(
                                    name = "data type mismatch - $textType, $nonTextType",
                                    op = op,
                                    leftType = textType,
                                    rightType = nonTextType,
                                    expectedProblems = listOf(
                                        createDataTypeMismatchError(col = 3, argTypes = listOf(textType, nonTextType), nAryOp = op)
                                    )
                                )
                            }
                        } +
                        // comparing lob type with non-lob, non-unknown type -> data type mismatch
                        ALL_LOB_TYPES.flatMap { lobType ->
                            ALL_NON_LOB_NON_UNKNOWN_TYPES.map { nonLobType ->
                                singleNAryOpErrorTestCase(
                                    name = "data type mismatch - $lobType, $nonLobType",
                                    op = op,
                                    leftType = lobType,
                                    rightType = nonLobType,
                                    expectedProblems = listOf(
                                        createDataTypeMismatchError(col = 3, argTypes = listOf(lobType, nonLobType), nAryOp = op)
                                    )
                                )
                            }
                        } +
                        // comparing non-categorized types with non-unknown other type -> data type mismatch
                        ALL_TYPES_ONLY_COMPARABLE_TO_SELF.flatMap { otherType ->
                            ALL_NON_UNKNOWN_TYPES.filter { it != otherType }.map { nonCompatibleType ->
                                singleNAryOpErrorTestCase(
                                    name = "data type mismatch - $otherType, $nonCompatibleType",
                                    op = op,
                                    leftType = otherType,
                                    rightType = nonCompatibleType,
                                    expectedProblems = listOf(
                                        createDataTypeMismatchError(col = 3, argTypes = listOf(otherType, nonCompatibleType), nAryOp = op)
                                    )
                                )
                            }
                        } +
                        // any type compared with an unknown -> null or missing error
                        generateAllUniquePairs(ALL_TYPES, ALL_UNKNOWN_TYPES).map {
                            singleNAryOpErrorTestCase(
                                name = "null or missing error - ${it.first}, ${it.second}",
                                op = op,
                                leftType = it.first,
                                rightType = it.second,
                                expectedProblems = listOf(createReturnsNullOrMissingError(col = 3, nAryOp = op))
                            )
                        } +
                        // other unknown error tests
                        singleNAryOpErrorTestCase(
                            name = "missing, union(null, float)",
                            op = op,
                            leftType = MISSING,
                            rightType = unionOf(NULL, FLOAT),
                            expectedProblems = listOf(createReturnsNullOrMissingError(col = 3, nAryOp = op))
                        ) +
                        singleNAryOpErrorTestCase(
                            name = "union(null, missing), any",
                            op = op,
                            leftType = NULL_OR_MISSING,
                            rightType = ANY,
                            expectedProblems = listOf(createReturnsNullOrMissingError(col = 3, nAryOp = op))
                        ) +
                        // other miscellaneous tests
                        singleNAryOpMismatchWithSwappedCases(
                            name = "int, union(timestamp, null)",
                            op = op,
                            leftType = INT,
                            rightType = unionOf(TIMESTAMP, NULL)
                        ) +
                        singleNAryOpMismatchWithSwappedCases(
                            name = "int, union(timestamp, missing)",
                            op = op,
                            leftType = INT,
                            rightType = unionOf(TIMESTAMP, MISSING)
                        ) +
                        singleNAryOpMismatchWithSwappedCases(
                            name = "union(int missing), union(timestamp, missing)",
                            op = op,
                            leftType = unionOf(INT, MISSING),
                            rightType = unionOf(TIMESTAMP, MISSING)
                        ) +
                        singleNAryOpMismatchWithSwappedCases(
                            name = "union(int, decimal, float), union(string, symbol)",
                            op = op,
                            leftType = unionOf(INT, DECIMAL, FLOAT),
                            rightType = unionOf(STRING, SYMBOL)
                        )
                }

        /**
         * Creates two test cases with the specified operand and expected types for every NAry logical
         * operator, about `LOGICAL_OPERATORS.size X 2` test cases in total.
         * If leftType != rightType, then new testCase "y {op} x" is created
         */
        private fun singleNAryLogicalCases(
            name: String,
            leftType: StaticType,
            rightType: StaticType,
            expectedType: StaticType,
            expectedWarnings: List<Problem> = emptyList()
        ) =
            createSingleNAryOpCasesWithSwappedArgs(OpType.LOGICAL, name, leftType, rightType, expectQueryOutputType(expectedType, expectedWarnings))

        /**
         * Creates one test case with the specified operand and expected types for every arithmetic
         * operator, combined with every other logical operator, `LOGICAL_OPERATORS.size^2` test
         * cases in total.
         */
        private fun doubleLogicalOpCases(
            name: String,
            leftType: StaticType,
            middleType: StaticType,
            rightType: StaticType,
            expectedType: StaticType,
            expectedWarnings: List<Problem> = emptyList()
        ) =
            createDoubleNAryOpCases(OpType.LOGICAL, name, leftType, middleType, rightType, expectedType, expectedWarnings)

        /**
         * Creates one [TestCase] with the specified binary [op] in the query "x [op] y [op] z" with `x` corresponding
         * to [leftType], `y` corresponding to [middleType], and `z` corresponding to [rightType]. The created
         * [TestCase] expects [expectedProblems] through inference.
         */
        private fun doubleOpErrorCases(
            name: String,
            op: String,
            leftType: StaticType,
            middleType: StaticType,
            rightType: StaticType,
            expectedProblems: List<Problem>
        ): TestCase {
            val query = "x $op y $op z"
            return TestCase(
                name = "$query : $name",
                originalSql = query,
                globals = mapOf(
                    "x" to leftType,
                    "y" to middleType,
                    "z" to rightType
                ),
                handler = expectSemanticProblems(expectedProblems)
            )
        }

        private fun createNotTestCase(
            name: String,
            argType: StaticType,
            expectedType: StaticType,
            expectedWarnings: List<Problem> = emptyList()
        ) =
            TestCase(
                name = name,
                originalSql = "NOT x",
                globals = mapOf("x" to argType),
                handler = expectQueryOutputType(expectedType, expectedWarnings)
            )

        private fun createNotDataTypeMismatchTestCase(
            name: String,
            argType: StaticType,
            expectedProblems: List<Problem>
        ) =
            TestCase(
                name = name,
                originalSql = "NOT x",
                globals = mapOf("x" to argType),
                handler = expectSemanticProblems(expectedProblems)
            )

        @JvmStatic
        @Suppress("unused")
        fun parametersForNAryLogicalTests() = listOf(
            //
            // `NOT` successful cases below this line
            //
            createNotTestCase(
                name = "NAry op NOT with boolean type",
                argType = BOOL,
                expectedType = BOOL
            ),
            createNotTestCase(
                name = "NAry op NOT with Union types",
                argType = unionOf(INT, BOOL),
                expectedType = unionOf(MISSING, BOOL)
            ),
            createNotTestCase(
                name = "NAry op NOT with ANY type",
                argType = ANY,
                expectedType = unionOf(MISSING, NULL, BOOL)
            ),
            //
            // `NOT` data type mismatch cases below this line
            //
            createNotDataTypeMismatchTestCase(
                name = "NAry op NOT data type mismatch - nullable non-bool",
                argType = INT.asNullable(),
                expectedProblems = listOf(
                    createDataTypeMismatchError(col = 1, argTypes = listOf(INT.asNullable()), nAryOp = "NOT")
                )
            ),
            createNotDataTypeMismatchTestCase(
                name = "NAry op NOT data type mismatch - optional non-bool",
                argType = INT.asOptional(),
                expectedProblems = listOf(
                    createDataTypeMismatchError(col = 1, argTypes = listOf(INT.asOptional()), nAryOp = "NOT")
                )
            ),
            createNotDataTypeMismatchTestCase(
                name = "NAry op NOT data type mismatch - nullable, optional non-bool",
                argType = INT.asNullable().asOptional(),
                expectedProblems = listOf(
                    createDataTypeMismatchError(col = 1, argTypes = listOf(INT.asNullable().asOptional()), nAryOp = "NOT")
                )
            ),
            createNotDataTypeMismatchTestCase(
                name = "NAry op NOT data type mismatch - union of non-bool types",
                argType = unionOf(INT, STRING),
                expectedProblems = listOf(
                    createDataTypeMismatchError(col = 1, argTypes = listOf(unionOf(INT, STRING)), nAryOp = "NOT")
                )
            )
        ) +
            // `NOT` non-bool -> data type mismatch
            ALL_NON_BOOL_NON_UNKNOWN_TYPES.map { nonBoolType ->
                createNotDataTypeMismatchTestCase(
                    name = "NAry op NOT data type mismatch - $nonBoolType",
                    argType = nonBoolType,
                    expectedProblems = listOf(
                        createDataTypeMismatchError(col = 1, argTypes = listOf(nonBoolType), nAryOp = "NOT")
                    )
                )
            } +
            // `NOT` unknown -> , null or missing error
            ALL_UNKNOWN_TYPES.map { unknownType ->
                createNotDataTypeMismatchTestCase(
                    name = "NAry op NOT null or missing error - $unknownType",
                    argType = unknownType,
                    expectedProblems = listOf(createReturnsNullOrMissingError(col = 1, nAryOp = "NOT"))
                )
            } + listOf(
            //
            // `AND` + `OR` successful cases below this line
            //
            singleNAryLogicalCases(
                "bool, bool",
                leftType = BOOL,
                rightType = BOOL,
                expectedType = BOOL
            ),
            singleNAryLogicalCases(
                "bool, any",
                leftType = BOOL,
                rightType = ANY,
                expectedType = unionOf(MISSING, NULL, BOOL)
            ),
            singleNAryLogicalCases(
                "union(int, bool), bool",
                leftType = unionOf(INT, BOOL),
                rightType = BOOL,
                expectedType = unionOf(MISSING, BOOL)
            ),
            doubleLogicalOpCases(
                "bool, bool, bool",
                leftType = BOOL,
                middleType = BOOL,
                rightType = BOOL,
                expectedType = BOOL
            ),
            //
            // `AND` + `OR` data type mismatch cases below this line
            //
            OpType.LOGICAL.operators.flatMap { op ->
                // non-unknown, non-boolean with non-unknown -> data type mismatch
                generateAllUniquePairs(ALL_NON_BOOL_NON_UNKNOWN_TYPES, ALL_NON_UNKNOWN_TYPES).map {
                    singleNAryOpErrorTestCase(
                        name = "data type mismatch - ${it.first}, ${it.second}",
                        op = op,
                        leftType = it.first,
                        rightType = it.second,
                        expectedProblems = listOf(
                            createDataTypeMismatchError(col = 3, argTypes = listOf(it.first, it.second), nAryOp = op)
                        )
                    )
                } +
                    // non-unknown, non-boolean with unknown -> data type mismatch and null or missing error
                    generateAllUniquePairs(ALL_NON_BOOL_NON_UNKNOWN_TYPES, ALL_UNKNOWN_TYPES).map {
                        singleNAryOpErrorTestCase(
                            name = "data type mismatch, null or missing error - ${it.first}, ${it.second}",
                            op = op,
                            leftType = it.first,
                            rightType = it.second,
                            expectedProblems = listOf(
                                createDataTypeMismatchError(col = 3, argTypes = listOf(it.first, it.second), nAryOp = op),
                                createReturnsNullOrMissingError(col = 3, nAryOp = op)
                            )
                        )
                    } +
                    // bool with an unknown -> null or missing error
                    generateAllUniquePairs(listOf(BOOL), ALL_UNKNOWN_TYPES).map {
                        singleNAryOpErrorTestCase(
                            name = "null or missing error - ${it.first}, ${it.second}",
                            op = op,
                            leftType = it.first,
                            rightType = it.second,
                            expectedProblems = listOf(createReturnsNullOrMissingError(col = 3, nAryOp = op))
                        )
                    } +
                    // unknown with an unknown -> null or missing error
                    generateAllUniquePairs(ALL_UNKNOWN_TYPES, ALL_UNKNOWN_TYPES).map {
                        singleNAryOpErrorTestCase(
                            name = "null or missing error - ${it.first}, ${it.second}",
                            op = op,
                            leftType = it.first,
                            rightType = it.second,
                            expectedProblems = listOf(createReturnsNullOrMissingError(col = 3, nAryOp = op))
                        )
                    } + listOf(
                    singleNAryOpErrorTestCase(
                        "data type mismatch - union(int, string), bool",
                        op = op,
                        leftType = unionOf(INT, STRING),
                        rightType = BOOL,
                        expectedProblems = listOf(
                            createDataTypeMismatchError(col = 3, argTypes = listOf(unionOf(INT, STRING), BOOL), nAryOp = op)
                        )
                    ),
                    singleNAryOpErrorTestCase(
                        "data type mismatch - null_or_missing, int",
                        op = op,
                        leftType = NULL_OR_MISSING,
                        rightType = INT,
                        expectedProblems = listOf(
                            createDataTypeMismatchError(col = 3, argTypes = listOf(NULL_OR_MISSING, INT), nAryOp = op),
                            createReturnsNullOrMissingError(col = 3, nAryOp = op)
                        )
                    )
                )
            } +
                // double logical op with at least one non-unknown, non-boolean -> data type mismatch
                listOf("AND", "OR ").flatMap { op ->
                    ALL_NON_BOOL_NON_UNKNOWN_TYPES.flatMap { nonBoolType ->
                        listOf(
                            doubleOpErrorCases(
                                name = "data type mismatch - $nonBoolType, bool, bool",
                                op = op,
                                leftType = nonBoolType,
                                middleType = BOOL,
                                rightType = BOOL,
                                expectedProblems = listOf(
                                    createDataTypeMismatchError(col = 3, argTypes = listOf(nonBoolType, BOOL), nAryOp = op.trim())
                                )
                            ),
                            doubleOpErrorCases(
                                name = "data type mismatch - bool, $nonBoolType, bool",
                                op = op,
                                leftType = BOOL,
                                middleType = nonBoolType,
                                rightType = BOOL,
                                expectedProblems = listOf(
                                    createDataTypeMismatchError(col = 3, argTypes = listOf(BOOL, nonBoolType), nAryOp = op.trim())
                                )
                            ),
                            doubleOpErrorCases(
                                name = "data type mismatch - bool, bool, $nonBoolType",
                                op = op,
                                leftType = BOOL,
                                middleType = BOOL,
                                rightType = nonBoolType,
                                expectedProblems = listOf(
                                    createDataTypeMismatchError(col = 9, argTypes = listOf(BOOL, nonBoolType), nAryOp = op.trim())
                                )
                            ),
                            doubleOpErrorCases(
                                name = "data type mismatch - $nonBoolType, $nonBoolType, bool",
                                op = op,
                                leftType = nonBoolType,
                                middleType = nonBoolType,
                                rightType = BOOL,
                                expectedProblems = listOf(
                                    createDataTypeMismatchError(col = 3, argTypes = listOf(nonBoolType, nonBoolType), nAryOp = op.trim())
                                )
                            ),
                            doubleOpErrorCases(
                                name = "data type mismatch - bool, $nonBoolType, $nonBoolType",
                                op = op,
                                leftType = BOOL,
                                middleType = nonBoolType,
                                rightType = nonBoolType,
                                expectedProblems = listOf(
                                    createDataTypeMismatchError(col = 3, argTypes = listOf(BOOL, nonBoolType), nAryOp = op.trim()),
                                    createDataTypeMismatchError(col = 9, argTypes = listOf(BOOL, nonBoolType), nAryOp = op.trim())
                                )
                            ),
                            doubleOpErrorCases(
                                name = "data type mismatch - $nonBoolType, $nonBoolType, $nonBoolType",
                                op = op,
                                leftType = nonBoolType,
                                middleType = nonBoolType,
                                rightType = nonBoolType,
                                expectedProblems = listOf(
                                    createDataTypeMismatchError(col = 3, argTypes = listOf(nonBoolType, nonBoolType), nAryOp = op.trim()),
                                    createDataTypeMismatchError(col = 9, argTypes = listOf(BOOL, nonBoolType), nAryOp = op.trim())
                                )
                            )
                        )
                    }
                }
        ).flatten()

        private fun createTrimTestCases(
            toRemove: StaticType? = null,
            target: StaticType,
            result: StaticType
        ) =
            listOf("both", "leading", "trailing", "").map {
                TestCase(
                    when {
                        it.isBlank() && toRemove == null -> "trim($target)"
                        else -> "trim($it ${toRemove ?: ""} from $target)"
                    },
                    when {
                        it.isBlank() && toRemove == null -> "trim(y)"
                        else -> "trim($it ${if (toRemove == null) ' ' else 'x'} from y)"
                    },
                    when (toRemove) {
                        null -> mapOf("y" to target)
                        else -> mapOf("x" to toRemove, "y" to target)
                    },
                    handler = expectQueryOutputType(result)
                )
            }

        /**
         * Creates two test cases with the specified operand and expected types and warnings for the concat op
         * (creates x || y and y || x).
         *
         * If [leftType] != [rightType], then new [TestCase] "y {op} x" is created.
         */
        private fun createNAryConcatTest(
            name: String,
            leftType: StaticType,
            rightType: StaticType,
            expectedType: StaticType,
            expectedWarnings: List<Problem> = emptyList()
        ) =
            createSingleNAryOpCasesWithSwappedArgs(OpType.CONCAT, name, leftType, rightType, expectQueryOutputType(expectedType, expectedWarnings))

        /**
         * Creates a [TestCase] for the concat with the query "x || y" with `x` corresponding to [leftType] and `y`
         * corresponding to [rightType]. The created [TestCase] will expect [expectedProblems] through inference.
         */
        private fun createNAryConcatDataTypeMismatchTest(
            name: String,
            leftType: StaticType,
            rightType: StaticType,
            expectedProblems: List<Problem>
        ) =
            singleNAryOpErrorTestCase(name, "||", leftType, rightType, expectedProblems)

        @JvmStatic
        @Suppress("unused")
        fun parametersForNAryConcatTests() = listOf(
            createNAryConcatTest(
                name = "unconstrained string, unconstrained string",
                leftType = STRING,
                rightType = STRING,
                expectedType = STRING
            ),
            createNAryConcatTest(
                name = "unconstrained string, symbol",
                leftType = STRING,
                rightType = SYMBOL,
                expectedType = STRING
            ),
            createNAryConcatTest(
                name = "unconstrained symbol, unconstrained symbol",
                leftType = SYMBOL,
                rightType = SYMBOL,
                expectedType = STRING
            ),
            createNAryConcatTest(
                name = "constrained string equals, unconstrained string",
                leftType = StringType(NumberConstraint.Equals(4)),
                rightType = STRING,
                expectedType = STRING
            ),
            createNAryConcatTest(
                name = "constrained string up to, unconstrained string",
                leftType = StringType(NumberConstraint.UpTo(4)),
                rightType = STRING,
                expectedType = STRING
            ),
            createNAryConcatTest(
                name = "constrained string equals 4, constrained string equals 6",
                leftType = StringType(NumberConstraint.Equals(4)),
                rightType = StringType(NumberConstraint.Equals(6)),
                expectedType = StringType(NumberConstraint.Equals(10))
            ),
            createNAryConcatTest(
                name = "constrained string equals 4, constrained string up to 6",
                leftType = StringType(NumberConstraint.Equals(4)),
                rightType = StringType(NumberConstraint.UpTo(6)),
                expectedType = StringType(NumberConstraint.UpTo(10))
            ),
            createNAryConcatTest(
                name = "constrained string up to 4, constrained string equals 6",
                leftType = StringType(NumberConstraint.UpTo(4)),
                rightType = StringType(NumberConstraint.Equals(6)),
                expectedType = StringType(NumberConstraint.UpTo(10))
            ),
            createNAryConcatTest(
                name = "constrained string up to 4, constrained string up to 6",
                leftType = StringType(NumberConstraint.UpTo(4)),
                rightType = StringType(NumberConstraint.UpTo(6)),
                expectedType = StringType(NumberConstraint.UpTo(10))
            ),
            createNAryConcatTest(
                name = "ANY, ANY",
                leftType = ANY,
                rightType = ANY,
                expectedType = unionOf(MISSING, STRING, NULL)
            ),
            createNAryConcatTest(
                name = "compatible union type, symbol",
                leftType = unionOf(INT, STRING),
                rightType = SYMBOL,
                expectedType = unionOf(MISSING, STRING)
            ),
            createNAryConcatTest(
                name = "compatible union type, null",
                leftType = unionOf(INT, STRING, NULL),
                rightType = SYMBOL,
                expectedType = unionOf(MISSING, STRING, NULL)
            )
        ).flatten() +
            //
            // data type mismatch cases below this line
            //

            // non-text, non-unknown with non-unknown -> data type mismatch
            generateAllUniquePairs(ALL_NON_TEXT_NON_UNKNOWN_TYPES, ALL_NON_UNKNOWN_TYPES).map {
                createNAryConcatDataTypeMismatchTest(
                    name = "data type mismatch - ${it.first}, ${it.second}",
                    leftType = it.first,
                    rightType = it.second,
                    expectedProblems = listOf(
                        createDataTypeMismatchError(col = 3, argTypes = listOf(it.first, it.second), nAryOp = "||")
                    )
                )
            } +
            // non-text, non-unknown with an unknown -> data type mismatch and null or missing error
            generateAllUniquePairs(ALL_NON_TEXT_NON_UNKNOWN_TYPES, ALL_UNKNOWN_TYPES).map {
                createNAryConcatDataTypeMismatchTest(
                    name = "data type mismatch, null or missing error - ${it.first}, ${it.second}",
                    leftType = it.first,
                    rightType = it.second,
                    expectedProblems = listOf(
                        createDataTypeMismatchError(col = 3, argTypes = listOf(it.first, it.second), nAryOp = "||"),
                        createReturnsNullOrMissingError(col = 3, nAryOp = "||")
                    )
                )
            } +
            // text with an unknown -> null or missing error
            generateAllUniquePairs(ALL_TEXT_TYPES, ALL_UNKNOWN_TYPES).map {
                createNAryConcatDataTypeMismatchTest(
                    name = "null or missing error - ${it.first}, ${it.second}",
                    leftType = it.first,
                    rightType = it.second,
                    expectedProblems = listOf(createReturnsNullOrMissingError(col = 3, nAryOp = "||"))
                )
            } +
            // unknown with an unknown -> null or missing error
            generateAllUniquePairs(ALL_UNKNOWN_TYPES, ALL_UNKNOWN_TYPES).map {
                createNAryConcatDataTypeMismatchTest(
                    name = "null or missing error - ${it.first}, ${it.second}",
                    leftType = it.first,
                    rightType = it.second,
                    expectedProblems = listOf(createReturnsNullOrMissingError(col = 3, nAryOp = "||"))
                )
            } + listOf(
            createNAryConcatDataTypeMismatchTest(
                name = "null or missing error - constrained string, null",
                leftType = StringType(NumberConstraint.Equals(2)),
                rightType = NULL,
                expectedProblems = listOf(createReturnsNullOrMissingError(col = 3, nAryOp = "||"))
            ),
            createNAryConcatDataTypeMismatchTest(
                name = "null or missing error - constrained string, missing",
                leftType = StringType(NumberConstraint.Equals(2)),
                rightType = MISSING,
                expectedProblems = listOf(createReturnsNullOrMissingError(col = 3, nAryOp = "||"))
            ),
            createNAryConcatDataTypeMismatchTest(
                name = "null or missing error - compatible union type, missing",
                leftType = unionOf(INT, STRING, NULL),
                rightType = MISSING,
                expectedProblems = listOf(createReturnsNullOrMissingError(col = 3, nAryOp = "||"))
            )
        ) +
            singleNAryOpMismatchWithSwappedCases(
                name = "data type mismatch - constrained string, int",
                op = "||",
                leftType = StringType(NumberConstraint.Equals(2)),
                rightType = INT
            ) +
            singleNAryOpMismatchWithSwappedCases(
                name = "data type mismatch - union(int, string), bool",
                op = "||",
                leftType = unionOf(INT, STRING),
                rightType = BOOL
            ) +
            singleNAryOpMismatchWithSwappedCases(
                name = "data type mismatch - union(int, string, null), bool",
                op = "||",
                leftType = unionOf(INT, STRING, NULL),
                rightType = BOOL
            ) +
            singleNAryOpMismatchWithSwappedCases(
                name = "data type mismatch - nullable int, string",
                op = "||",
                leftType = INT.asNullable(),
                rightType = STRING
            ) +
            singleNAryOpMismatchWithSwappedCases(
                name = "data type mismatch - optional int, string",
                op = "||",
                leftType = INT.asOptional(),
                rightType = STRING
            ) +
            singleNAryOpMismatchWithSwappedCases(
                name = "data type mismatch - nullable + optional int, string",
                op = "||",
                leftType = INT.asNullable().asOptional(),
                rightType = STRING
            ) +
            singleNAryOpMismatchWithSwappedCases(
                name = "data type mismatch - any, int",
                op = "||",
                leftType = ANY,
                rightType = INT
            )

        private fun createNAryLikeTest(
            name: String,
            valueType: StaticType,
            patternType: StaticType,
            escapeType: StaticType?,
            handler: (ResolveTestResult) -> Unit
        ) =
            when (escapeType) {
                null -> TestCase(
                    name = name,
                    originalSql = "x LIKE y",
                    globals = mapOf(
                        "x" to valueType,
                        "y" to patternType
                    ),
                    handler = handler
                )
                else -> TestCase(
                    name = name,
                    originalSql = "x LIKE y ESCAPE z",
                    globals = mapOf(
                        "x" to valueType,
                        "y" to patternType,
                        "z" to escapeType
                    ),
                    handler = handler
                )
            }

        private fun createNAryLikeValidTest(
            name: String,
            valueType: StaticType,
            patternType: StaticType,
            escapeType: StaticType? = null,
            outputType: StaticType,
            expectedWarnings: List<Problem> = emptyList()
        ) =
            createNAryLikeTest(
                name,
                valueType,
                patternType,
                escapeType,
                handler = expectQueryOutputType(
                    expectedType = outputType,
                    expectedWarnings = expectedWarnings
                )
            )

        private fun createNAryLikeDataTypeMismatchTest(
            name: String,
            valueType: StaticType,
            patternType: StaticType,
            escapeType: StaticType? = null,
            expectedProblems: List<Problem>
        ) =
            createNAryLikeTest(
                name,
                valueType,
                patternType,
                escapeType,
                handler = expectSemanticProblems(expectedProblems)
            )

        @JvmStatic
        @Suppress("unused")
        fun parametersForNAryLikeTests() = listOf(
            createNAryLikeValidTest(
                name = "NAry op LIKE - string LIKE string",
                valueType = STRING,
                patternType = STRING,
                outputType = BOOL
            ),
            createNAryLikeValidTest(
                name = "NAry op LIKE - string LIKE symbol",
                valueType = STRING,
                patternType = SYMBOL,
                outputType = BOOL
            ),
            createNAryLikeValidTest(
                name = "NAry op LIKE - any LIKE any",
                valueType = ANY,
                patternType = ANY,
                outputType = unionOf(BOOL, MISSING, NULL)
            ),
            createNAryLikeValidTest(
                name = "NAry op LIKE - valid union LIKE string",
                valueType = unionOf(STRING, INT),
                patternType = STRING,
                outputType = unionOf(BOOL, MISSING)
            ),
            createNAryLikeValidTest(
                name = "NAry op LIKE - valid union with null LIKE symbol",
                valueType = unionOf(INT, STRING, NULL),
                patternType = SYMBOL,
                outputType = unionOf(MISSING, BOOL, NULL)
            ),
            // If the optional escape character is provided, it can result in failure even if the type is text (string,
            // in this case)
            // This is because the escape character needs to be a single character (string with length 1),
            // Even if the escape character is of length 1, escape sequence can be incorrect.
            // Check EvaluatingCompiler.checkPattern method for more details.
            createNAryLikeValidTest(
                name = "NAry op LIKE - string LIKE string ESCAPE string",
                valueType = STRING,
                patternType = STRING,
                escapeType = STRING,
                outputType = unionOf(MISSING, BOOL)
            ),
            createNAryLikeValidTest(
                name = "NAry op LIKE - symbol LIKE string ESCAPE symbol",
                valueType = SYMBOL,
                patternType = STRING,
                escapeType = SYMBOL,
                outputType = unionOf(MISSING, BOOL)
            ),
            createNAryLikeValidTest(
                name = "NAry op LIKE - symbol LIKE symbol ESCAPE string",
                valueType = SYMBOL,
                patternType = SYMBOL,
                escapeType = STRING,
                outputType = unionOf(MISSING, BOOL)
            ),
            createNAryLikeValidTest(
                name = "NAry op LIKE - symbol LIKE symbol ESCAPE symbol",
                valueType = SYMBOL,
                patternType = SYMBOL,
                escapeType = SYMBOL,
                outputType = unionOf(MISSING, BOOL)
            ),
            createNAryLikeValidTest(
                name = "NAry op LIKE - escape type of union(int, string)",
                valueType = STRING,
                patternType = STRING,
                escapeType = unionOf(INT, STRING),
                outputType = unionOf(BOOL, MISSING)
            ),
            createNAryLikeValidTest(
                name = "NAry op LIKE - value type of union(int, string)",
                valueType = unionOf(INT, STRING),
                patternType = STRING,
                escapeType = STRING,
                outputType = unionOf(BOOL, MISSING)
            ),
            createNAryLikeValidTest(
                name = "NAry op LIKE - value type of union(int, string, null)",
                valueType = unionOf(INT, STRING, NULL),
                patternType = STRING,
                escapeType = STRING,
                outputType = unionOf(BOOL, MISSING, NULL)
            ),
            createNAryLikeValidTest(
                name = "NAry op LIKE - pattern type of union(int, string)",
                valueType = STRING,
                patternType = unionOf(INT, STRING, NULL),
                escapeType = STRING,
                outputType = unionOf(BOOL, MISSING, NULL)
            )
        ) +
            //
            // data type mismatch cases below this line
            //

            // 2 args (value and pattern args only) - non-text, non-unknown with non-unknown -> data type mismatch
            generateAllUniquePairs(ALL_NON_TEXT_NON_UNKNOWN_TYPES, ALL_NON_UNKNOWN_TYPES).map {
                createNAryLikeDataTypeMismatchTest(
                    name = "data type mismatch - ${it.first}, ${it.second}",
                    valueType = it.first,
                    patternType = it.second,
                    expectedProblems = listOf(
                        createDataTypeMismatchError(col = 3, argTypes = listOf(it.first, it.second), nAryOp = "LIKE")
                    )
                )
            } +
            // non-text, non-unknown with unknown -> data type mismatch and null or missing error
            generateAllUniquePairs(ALL_NON_TEXT_NON_UNKNOWN_TYPES, ALL_UNKNOWN_TYPES).map {
                createNAryLikeDataTypeMismatchTest(
                    name = "data type mismatch, null or missing error - ${it.first}, ${it.second}",
                    valueType = it.first,
                    patternType = it.second,
                    expectedProblems = listOf(
                        createDataTypeMismatchError(col = 3, argTypes = listOf(it.first, it.second), nAryOp = "LIKE"),
                        createReturnsNullOrMissingError(col = 3, nAryOp = "LIKE")
                    )
                )
            } +
            // text with an unknown -> null or missing error
            generateAllUniquePairs(ALL_TEXT_TYPES, ALL_UNKNOWN_TYPES).map {
                createNAryLikeDataTypeMismatchTest(
                    name = "null or missing error - ${it.first}, ${it.second}",
                    valueType = it.first,
                    patternType = it.second,
                    expectedProblems = listOf(createReturnsNullOrMissingError(col = 3, nAryOp = "LIKE"))
                )
            } +
            // unknown with an unknown -> null or missing error
            generateAllUniquePairs(ALL_UNKNOWN_TYPES, ALL_UNKNOWN_TYPES).map {
                createNAryLikeDataTypeMismatchTest(
                    name = "null or missing error - ${it.first}, ${it.second}",
                    valueType = it.first,
                    patternType = it.second,
                    expectedProblems = listOf(createReturnsNullOrMissingError(col = 3, nAryOp = "LIKE"))
                )
            } +
            // 3 args - 1 invalid argument (non-text, non-unknown) -> data type mismatch
            generateAllUniquePairs(ALL_TEXT_TYPES, ALL_TEXT_TYPES).flatMap { textTypes ->
                val (textType1, textType2) = textTypes
                ALL_NON_TEXT_NON_UNKNOWN_TYPES.flatMap { nonTextType ->
                    listOf(
                        createNAryLikeDataTypeMismatchTest(
                            name = "NAry op LIKE data type mismatch - $nonTextType LIKE $textType1 ESCAPE $textType2",
                            valueType = nonTextType,
                            patternType = textType1,
                            escapeType = textType2,
                            expectedProblems = listOf(
                                createDataTypeMismatchError(col = 3, argTypes = listOf(nonTextType, textType1, textType2), nAryOp = "LIKE")
                            )
                        ),
                        createNAryLikeDataTypeMismatchTest(
                            name = "NAry op LIKE data type mismatch - $textType1 LIKE $nonTextType ESCAPE $textType2",
                            valueType = textType1,
                            patternType = nonTextType,
                            escapeType = textType2,
                            expectedProblems = listOf(
                                createDataTypeMismatchError(col = 3, argTypes = listOf(textType1, nonTextType, textType2), nAryOp = "LIKE")
                            )
                        ),
                        createNAryLikeDataTypeMismatchTest(
                            name = "NAry op LIKE data type mismatch - $textType1 LIKE $textType2 ESCAPE $nonTextType",
                            valueType = textType1,
                            patternType = textType2,
                            escapeType = nonTextType,
                            expectedProblems = listOf(
                                createDataTypeMismatchError(col = 3, argTypes = listOf(textType1, textType2, nonTextType), nAryOp = "LIKE")
                            )
                        )
                    )
                }
            } +
            listOf(
                createNAryLikeDataTypeMismatchTest(
                    name = "NAry op LIKE data type mismatch - union(string, int, null) LIKE bool",
                    valueType = unionOf(STRING, INT, NULL),
                    patternType = BOOL,
                    expectedProblems = listOf(
                        createDataTypeMismatchError(col = 3, argTypes = listOf(unionOf(STRING, INT, NULL), BOOL), nAryOp = "LIKE")
                    )
                ),
                createNAryLikeDataTypeMismatchTest(
                    name = "NAry op LIKE data type mismatch - 3 args, escape type of union of incompatible types",
                    valueType = STRING,
                    patternType = STRING,
                    escapeType = unionOf(INT, DECIMAL, BOOL),
                    expectedProblems = listOf(
                        createDataTypeMismatchError(col = 3, argTypes = listOf(STRING, STRING, unionOf(INT, DECIMAL, BOOL)), nAryOp = "LIKE")
                    )
                ),
                createNAryLikeDataTypeMismatchTest(
                    name = "NAry op LIKE data type mismatch - 3 args, escape type of union of incompatible types",
                    valueType = STRING,
                    patternType = unionOf(INT, DECIMAL, BOOL),
                    escapeType = SYMBOL,
                    expectedProblems = listOf(
                        createDataTypeMismatchError(col = 3, argTypes = listOf(STRING, unionOf(INT, DECIMAL, BOOL), SYMBOL), nAryOp = "LIKE")
                    )
                ),
                createNAryLikeDataTypeMismatchTest(
                    name = "NAry op LIKE data type mismatch - 3 args, escape type of union of incompatible types",
                    valueType = unionOf(INT, DECIMAL, BOOL),
                    patternType = STRING,
                    escapeType = STRING,
                    expectedProblems = listOf(
                        createDataTypeMismatchError(col = 3, argTypes = listOf(unionOf(INT, DECIMAL, BOOL), STRING, STRING), nAryOp = "LIKE")
                    )
                ),
                createNAryLikeDataTypeMismatchTest(
                    name = "NAry op LIKE with null or missing error - string LIKE string ESCAPE null",
                    valueType = STRING,
                    patternType = STRING,
                    escapeType = NULL,
                    expectedProblems = listOf(createReturnsNullOrMissingError(col = 3, nAryOp = "LIKE"))
                ),
                createNAryLikeDataTypeMismatchTest(
                    name = "NAry op LIKE with null or missing error - string LIKE null ESCAPE string",
                    valueType = STRING,
                    patternType = NULL,
                    escapeType = STRING,
                    expectedProblems = listOf(createReturnsNullOrMissingError(col = 3, nAryOp = "LIKE"))
                ),
                createNAryLikeDataTypeMismatchTest(
                    name = "NAry op LIKE with null or missing error - null LIKE string ESCAPE string",
                    valueType = NULL,
                    patternType = STRING,
                    escapeType = STRING,
                    expectedProblems = listOf(createReturnsNullOrMissingError(col = 3, nAryOp = "LIKE"))
                ),
                createNAryLikeDataTypeMismatchTest(
                    name = "NAry op LIKE with null or missing error - null LIKE null ESCAPE null",
                    valueType = NULL,
                    patternType = NULL,
                    escapeType = NULL,
                    expectedProblems = listOf(createReturnsNullOrMissingError(col = 3, nAryOp = "LIKE"))
                ),
                createNAryLikeDataTypeMismatchTest(
                    name = "NAry op LIKE with null or missing error - string LIKE missing ESCAPE string",
                    valueType = STRING,
                    patternType = MISSING,
                    escapeType = STRING,
                    expectedProblems = listOf(createReturnsNullOrMissingError(col = 3, nAryOp = "LIKE"))
                ),
                createNAryLikeDataTypeMismatchTest(
                    name = "NAry op LIKE with null or missing error - missing LIKE string ESCAPE string",
                    valueType = MISSING,
                    patternType = STRING,
                    escapeType = STRING,
                    expectedProblems = listOf(createReturnsNullOrMissingError(col = 3, nAryOp = "LIKE"))
                ),
                createNAryLikeDataTypeMismatchTest(
                    name = "NAry op LIKE with null or missing error - missing LIKE missing ESCAPE missing",
                    valueType = MISSING,
                    patternType = MISSING,
                    escapeType = MISSING,
                    expectedProblems = listOf(createReturnsNullOrMissingError(col = 3, nAryOp = "LIKE"))
                ),
                createNAryLikeDataTypeMismatchTest(
                    name = "NAry op LIKE with null or missing error - missing LIKE null ESCAPE null",
                    valueType = MISSING,
                    patternType = NULL,
                    escapeType = NULL,
                    expectedProblems = listOf(createReturnsNullOrMissingError(col = 3, nAryOp = "LIKE"))
                ),
                createNAryLikeDataTypeMismatchTest(
                    name = "NAry op LIKE with null or missing error - null LIKE missing ESCAPE null",
                    valueType = NULL,
                    patternType = MISSING,
                    escapeType = NULL,
                    expectedProblems = listOf(createReturnsNullOrMissingError(col = 3, nAryOp = "LIKE"))
                ),
                createNAryLikeDataTypeMismatchTest(
                    name = "NAry op LIKE with null or missing error - null LIKE null ESCAPE missing",
                    valueType = NULL,
                    patternType = NULL,
                    escapeType = MISSING,
                    expectedProblems = listOf(createReturnsNullOrMissingError(col = 3, nAryOp = "LIKE"))
                ),
                createNAryLikeDataTypeMismatchTest(
                    name = "NAry op LIKE data type mismatch, null or missing error - 3 args, incompatible escape type with unknown types",
                    valueType = NULL,
                    patternType = MISSING,
                    escapeType = INT,
                    expectedProblems = listOf(
                        createDataTypeMismatchError(col = 3, argTypes = listOf(NULL, MISSING, INT), nAryOp = "LIKE"),
                        createReturnsNullOrMissingError(col = 3, nAryOp = "LIKE")
                    )
                ),
                createNAryLikeDataTypeMismatchTest(
                    name = "NAry op LIKE data type mismatch, null or missing error - 3 args, incompatible pattern type with unknown types",
                    valueType = NULL,
                    patternType = INT,
                    escapeType = MISSING,
                    expectedProblems = listOf(
                        createDataTypeMismatchError(col = 3, argTypes = listOf(NULL, INT, MISSING), nAryOp = "LIKE"),
                        createReturnsNullOrMissingError(col = 3, nAryOp = "LIKE")
                    )
                ),
                createNAryLikeDataTypeMismatchTest(
                    name = "NAry op LIKE data type mismatch, null or missing error - 3 args, incompatible value type with unknown types",
                    valueType = STRUCT,
                    patternType = NULL,
                    escapeType = MISSING,
                    expectedProblems = listOf(
                        createDataTypeMismatchError(col = 3, argTypes = listOf(STRUCT, NULL, MISSING), nAryOp = "LIKE"),
                        createReturnsNullOrMissingError(col = 3, nAryOp = "LIKE")
                    )
                ),
            )

        /**
         * Creates a test expecting [outputType] and [expectedWarnings] with the query:
         * [valueType] BETWEEN [fromType] AND [toType].
         *
         * If [createSwapped] is true and [fromType] != [toType], then another test will be included with
         * [fromType] and [toType] swapped in the created [TestCase] query.
         */
        private fun createNAryBetweenValidTest(
            name: String,
            valueType: StaticType,
            fromType: StaticType,
            toType: StaticType,
            outputType: StaticType,
            expectedWarnings: List<Problem> = emptyList(),
            createSwapped: Boolean = true
        ): List<TestCase> {
            val originalTest = TestCase(
                name = "x BETWEEN y AND z : $name",
                originalSql = "x BETWEEN y AND z",
                globals = mapOf(
                    "x" to valueType,
                    "y" to fromType,
                    "z" to toType
                ),
                handler = expectQueryOutputType(outputType, expectedWarnings)
            )
            return when (createSwapped && fromType != toType) {
                true ->
                    listOf(
                        originalTest,
                        TestCase(
                            name = "x BETWEEN z AND y : $name",
                            originalSql = "x BETWEEN z AND y",
                            globals = mapOf(
                                "x" to valueType,
                                "y" to fromType,
                                "z" to toType
                            ),
                            handler = expectQueryOutputType(outputType, expectedWarnings)
                        )
                    )
                else -> listOf(originalTest)
            }
        }

        /**
         * Creates tests with compatible arguments for `BETWEEN`. Argument [comparableTypes] must have the [StaticType]s
         * be comparable to one another.
         *
         * More details of the tests provided in the included comments.
         */
        private fun createNAryBetweenComparableTypeTests(comparableTypes: List<StaticType>): List<TestCase> =
            generateAllUniquePairs(comparableTypes, comparableTypes).flatMap { comparable ->
                // test of the form: <compatibleValueType> BETWEEN <comparable1> AND <comparable2>
                // results in bool as all types are comparable
                comparableTypes.flatMap { comparableValueType ->
                    createNAryBetweenValidTest(
                        name = "x: $comparableValueType, y: ${comparable.first}, z: ${comparable.second}",
                        valueType = comparableValueType,
                        fromType = comparable.first,
                        toType = comparable.second,
                        outputType = BOOL,
                        createSwapped = false
                    )
                }
            }

        /**
         * Creates a test expecting a data type mismatch error with the query:
         * [valueType] BETWEEN [fromType] AND [toType].
         *
         * If [fromType] != [toType], then another test will be included in the output with [fromType] and [toType]
         * swapped in the created [TestCase] query.
         */
        private fun createNAryBetweenDataTypeMismatchTest(
            name: String,
            valueType: StaticType,
            fromType: StaticType,
            toType: StaticType
        ): List<TestCase> {
            val originalTest = TestCase(
                name = "x BETWEEN y AND z : $name",
                originalSql = "x BETWEEN y AND z",
                globals = mapOf(
                    "x" to valueType,
                    "y" to fromType,
                    "z" to toType
                ),
                handler = expectSemanticErrors(listOf(createDataTypeMismatchError(col = 3, argTypes = listOf(valueType, fromType, toType), nAryOp = "BETWEEN")))
            )
            return when (fromType != toType) {
                true -> listOf(
                    originalTest,
                    TestCase(
                        name = "x BETWEEN z AND y : $name",
                        originalSql = "x BETWEEN z AND y",
                        globals = mapOf(
                            "x" to valueType,
                            "y" to fromType,
                            "z" to toType
                        ),
                        handler = expectSemanticErrors(listOf(createDataTypeMismatchError(col = 3, argTypes = listOf(valueType, toType, fromType), nAryOp = "BETWEEN")))
                    )
                )
                else -> listOf(originalTest)
            }
        }

        /**
         * Creates a test expecting [expectedErrors] with the query: [valueType] BETWEEN [fromType] AND [toType].
         */
        private fun createNAryBetweenErrorTest(
            name: String,
            valueType: StaticType,
            fromType: StaticType,
            toType: StaticType,
            expectedErrors: List<Problem>
        ) =
            listOf(
                TestCase(
                    name = "x BETWEEN y AND z : $name",
                    originalSql = "x BETWEEN y AND z",
                    globals = mapOf(
                        "x" to valueType,
                        "y" to fromType,
                        "z" to toType
                    ),
                    handler = expectSemanticErrors(expectedErrors)
                )
            )

        /**
         * Creates multiple different tests using `BETWEEN` resulting in at least one error. Argument [comparableTypes]
         * are expected to be comparable with each other. [incomparableTypes] are expected to be incomparable with each
         * of the [comparableTypes] and not contain any unknown [StaticType]s.
         *
         * More details on the tests are provided in the included comments.
         */
        private fun createMultipleNAryBetweenErrorTests(
            comparableTypes: List<StaticType>,
            incomparableTypes: List<StaticType>
        ): List<TestCase> =
            // <comparable> BETWEEN <incomparable1> AND <incomparable2> -> data type mismatch
            // where <comparable> comes from [comparableTypes] and <incomparable1> <incomparable2> come from
            // `incomparableTypes`
            generateAllUniquePairs(incomparableTypes, incomparableTypes).flatMap { incomparable ->
                comparableTypes.flatMap { valueType ->
                    createNAryBetweenErrorTest(
                        name = "data type mismatch - x: $valueType, y: ${incomparable.first}, z: ${incomparable.second}",
                        valueType = valueType,
                        fromType = incomparable.first,
                        toType = incomparable.second,
                        expectedErrors = listOf(
                            createDataTypeMismatchError(col = 3, argTypes = listOf(valueType, incomparable.first, incomparable.second), nAryOp = "BETWEEN")
                        )
                    )
                }
            } +
                // tests with two comparable types
                generateAllUniquePairs(comparableTypes, comparableTypes).flatMap { comparable ->
                    // <comparable1> BETWEEN <incomparable> AND <comparable2> -> data type mismatch
                    // <comparable1>, <comparable2> come from [comparableTypes] and are comparable with each other.
                    // <incomparable> comes from [incomparableTypes] and is incomparable with <comparable1>.
                    incomparableTypes.flatMap { incomparable ->
                        createNAryBetweenErrorTest(
                            name = "data type mismatch - x: ${comparable.first}, y: $incomparable, z: ${comparable.second}",
                            valueType = comparable.first,
                            fromType = incomparable,
                            toType = comparable.second,
                            expectedErrors = listOf(
                                createDataTypeMismatchError(col = 3, argTypes = listOf(comparable.first, incomparable, comparable.second), nAryOp = "BETWEEN")
                            )
                        )
                    } +
                        ALL_UNKNOWN_TYPES.flatMap { unknownType ->
                            // <unknown> BETWEEN <comparable1> AND <comparable2> -> null or missing error
                            // <comparable1> and <comparable2> come from `comparableTypes` and are comparable with each
                            // other
                            createNAryBetweenErrorTest(
                                name = "null or missing error - x: $unknownType, y: ${comparable.first}, z: ${comparable.second}",
                                valueType = unknownType,
                                fromType = comparable.first,
                                toType = comparable.second,
                                expectedErrors = listOf(createReturnsNullOrMissingError(col = 3, nAryOp = "BETWEEN"))
                            ) +
                                // <comparable1> BETWEEN <unknown> AND <comparable2> -> null or missing error
                                // <comparable1> and <comparable2> come from `comparableTypes` and are comparable with each
                                // other
                                createNAryBetweenErrorTest(
                                    name = "null or missing error - x: ${comparable.first}, y: $unknownType, z: ${comparable.second}",
                                    valueType = comparable.first,
                                    fromType = unknownType,
                                    toType = comparable.second,
                                    expectedErrors = listOf(createReturnsNullOrMissingError(col = 3, nAryOp = "BETWEEN"))
                                )
                        }
                } +
                comparableTypes.flatMap { comparable ->
                    incomparableTypes.flatMap { incomparable ->
                        ALL_UNKNOWN_TYPES.flatMap { unknownType ->
                            // <comparable> BETWEEN <incomparable> AND unknown -> data type mismatch and null or missing
                            // error
                            // <comparable> comes from [comparableTypes] and <incomparable> comes from [incomparableTypes].
                            // Comparing <comparable> with <unknown> results in a null or missing error
                            createNAryBetweenErrorTest(
                                name = "data type mismatch, null or missing error - x: $comparable, y: $incomparable, z: $unknownType",
                                valueType = comparable,
                                fromType = incomparable,
                                toType = unknownType,
                                expectedErrors = listOf(
                                    createDataTypeMismatchError(col = 3, argTypes = listOf(comparable, incomparable, unknownType), nAryOp = "BETWEEN"),
                                    createReturnsNullOrMissingError(col = 3, nAryOp = "BETWEEN")
                                )
                            )
                        }
                    }
                }

        @JvmStatic
        @Suppress("unused")
        fun parametersForNAryBetweenTests() =
            createNAryBetweenComparableTypeTests(ALL_NUMERIC_TYPES) +
                createNAryBetweenComparableTypeTests(ALL_TEXT_TYPES) +
                createNAryBetweenComparableTypeTests(ALL_LOB_TYPES) +
                ALL_TYPES_ONLY_COMPARABLE_TO_SELF.flatMap { otherType ->
                    createNAryBetweenComparableTypeTests(listOf(otherType))
                } +
                createNAryBetweenValidTest(
                    name = "matching union types; x: union(int, string), y: int, z: decimal",
                    valueType = unionOf(INT, STRING),
                    fromType = INT,
                    toType = DECIMAL,
                    outputType = unionOf(BOOL, MISSING)
                ) +
                createNAryBetweenValidTest(
                    name = "matching union types containing null; x: union(int, string, null), y: int, z: decimal",
                    valueType = unionOf(INT, STRING, NULL),
                    fromType = INT,
                    toType = DECIMAL,
                    outputType = unionOf(BOOL, MISSING, NULL)
                ) +
                createNAryBetweenValidTest(
                    name = "x: ANY, y: INT, z: DECIMAL",
                    valueType = ANY,
                    fromType = INT,
                    toType = DECIMAL,
                    outputType = unionOf(BOOL, MISSING, NULL)
                ) +
                //
                // data type mismatch cases for arithmetic ops below
                //

                // numeric with non-numerics
                createMultipleNAryBetweenErrorTests(comparableTypes = ALL_NUMERIC_TYPES, incomparableTypes = ALL_NON_NUMERIC_NON_UNKNOWN_TYPES) +
                // text with non-text
                createMultipleNAryBetweenErrorTests(comparableTypes = ALL_TEXT_TYPES, incomparableTypes = ALL_NON_TEXT_NON_UNKNOWN_TYPES) +
                // lob with non-lobs
                createMultipleNAryBetweenErrorTests(comparableTypes = ALL_LOB_TYPES, incomparableTypes = ALL_NON_LOB_NON_UNKNOWN_TYPES) +
                // types only comparable to self with different types
                ALL_TYPES_ONLY_COMPARABLE_TO_SELF.flatMap { otherType ->
                    createMultipleNAryBetweenErrorTests(comparableTypes = listOf(otherType), incomparableTypes = ALL_NON_UNKNOWN_TYPES.filter { it != otherType })
                } +
                // unknowns with non-unknown types
                generateAllUniquePairs(ALL_UNKNOWN_TYPES, ALL_UNKNOWN_TYPES).flatMap { unknownTypes ->
                    ALL_NON_UNKNOWN_TYPES.flatMap { nonUnknownType ->
                        createNAryBetweenErrorTest(
                            name = "null or missing error - x: $nonUnknownType, y: ${unknownTypes.first}, z: ${unknownTypes.second}",
                            valueType = nonUnknownType,
                            fromType = unknownTypes.first,
                            toType = unknownTypes.second,
                            expectedErrors = listOf(createReturnsNullOrMissingError(col = 3, nAryOp = "BETWEEN"))
                        ) +
                            createNAryBetweenErrorTest(
                                name = "null or missing error - x: ${unknownTypes.first}, y: $nonUnknownType, z: ${unknownTypes.second}",
                                valueType = unknownTypes.first,
                                fromType = nonUnknownType,
                                toType = unknownTypes.second,
                                expectedErrors = listOf(createReturnsNullOrMissingError(col = 3, nAryOp = "BETWEEN"))
                            ) +
                            createNAryBetweenErrorTest(
                                name = "null or missing error - x: ${unknownTypes.first}, y: ${unknownTypes.second}, z: $nonUnknownType",
                                valueType = unknownTypes.first,
                                fromType = unknownTypes.second,
                                toType = nonUnknownType,
                                expectedErrors = listOf(createReturnsNullOrMissingError(col = 3, nAryOp = "BETWEEN"))
                            )
                    }
                } +
                createNAryBetweenDataTypeMismatchTest(
                    name = "incomparable nullable valueType; x: nullable int, y: nullable string, z: nullable symbol",
                    valueType = INT.asNullable(),
                    fromType = STRING.asNullable(),
                    toType = SYMBOL.asNullable()
                ) +
                createNAryBetweenDataTypeMismatchTest(
                    name = "incomparable nullable from/toType; x: nullable string, y: nullable int, z: nullable symbol",
                    valueType = STRING.asNullable(),
                    fromType = INT.asNullable(),
                    toType = SYMBOL.asNullable()
                ) +
                createNAryBetweenDataTypeMismatchTest(
                    name = "incomparable optional valueType; x: optional int, y: optional string, z: optional symbol",
                    valueType = INT.asOptional(),
                    fromType = STRING.asOptional(),
                    toType = SYMBOL.asOptional()
                ) +
                createNAryBetweenDataTypeMismatchTest(
                    name = "incomparable optional from/toType; x: optional string, y: optional int, z: optional symbol",
                    valueType = STRING.asOptional(),
                    fromType = INT.asOptional(),
                    toType = SYMBOL.asOptional()
                ) +
                createNAryBetweenDataTypeMismatchTest(
                    name = "union comparable to one union, not to other union; x: union(int, decimal), y: union(int, null), z: union(string, symbol)",
                    valueType = unionOf(INT, DECIMAL),
                    fromType = unionOf(INT, NULL),
                    toType = unionOf(STRING, SYMBOL)
                ) +
                createNAryBetweenDataTypeMismatchTest(
                    name = "union incomparable to other unions; x: union(bool, string), y: union(int, null), z: union(string, symbol)",
                    valueType = unionOf(BOOL, STRING),
                    fromType = unionOf(INT, FLOAT),
                    toType = unionOf(INT, DECIMAL)
                ) +
                // valueType is comparable to fromType and toType. but fromType is incomparable to toType
                createNAryBetweenDataTypeMismatchTest(
                    name = "fromType incomparable to toType; x: union(int, string), y: int, z: string",
                    valueType = unionOf(INT, STRING),
                    fromType = INT,
                    toType = STRING
                )

        @JvmStatic
        @Suppress("unused")
        fun parametersForTrimFunctionTests() = listOf(
            createTrimTestCases(
                target = STRING,
                result = STRING
            ),
            createTrimTestCases(
                toRemove = STRING,
                target = STRING,
                result = STRING
            ),
            createTrimTestCases(
                target = unionOf(STRING, INT),
                result = unionOf(STRING, MISSING)
            ),
            createTrimTestCases(
                target = ANY,
                result = unionOf(STRING, MISSING, NULL)
            ),
            createTrimTestCases(
                toRemove = unionOf(STRING, SYMBOL),
                target = unionOf(INT, STRING),
                result = unionOf(STRING, MISSING)
            ),
            createTrimTestCases(
                toRemove = unionOf(STRING, SYMBOL, NULL),
                target = unionOf(INT, STRING, BOOL, MISSING),
                result = unionOf(STRING, MISSING, NULL)
            ),
            createTrimTestCases(
                toRemove = ANY,
                target = ANY,
                result = unionOf(STRING, MISSING, NULL)
            )
        ).flatten() + listOf(
            TestCase(
                "Leading is treated as keyword",
                "trim(leading from 'target')",
                mapOf(
                    "leading" to BOOL
                ),
                handler = expectQueryOutputType(STRING)
            )
        )

        @JvmStatic
        @Suppress("unused")
        fun parametersForPathingIntoUnion() = listOf(
            TestCase(
                name = "path on list",
                originalSql = "a[1]",
                globals = mapOf("a" to ListType(elementType = StaticType.INT)),
                handler = expectQueryOutputType(StaticType.INT)
            ),
            TestCase(
                name = "path on nullable list",
                originalSql = "a[1]",
                globals = mapOf("a" to ListType(elementType = StaticType.INT).asNullable()),
                handler = expectQueryOutputType(unionOf(StaticType.INT, StaticType.MISSING))
            ),
            TestCase(
                name = "path on optional list",
                originalSql = "a[1]",
                globals = mapOf("a" to ListType(elementType = StaticType.INT).asOptional()),
                handler = expectQueryOutputType(unionOf(StaticType.INT, StaticType.MISSING))
            ),
            TestCase(
                name = "path on nullable, optional list",
                originalSql = "a[1]",
                globals = mapOf("a" to ListType(elementType = StaticType.INT).asNullable().asOptional()),
                handler = expectQueryOutputType(unionOf(StaticType.INT, StaticType.MISSING))
            ),
            TestCase(
                name = "path on union of different list types",
                originalSql = "a[1]",
                globals = mapOf(
                    "a" to unionOf(
                        ListType(elementType = StaticType.INT),
                        ListType(elementType = StaticType.BOOL),
                        ListType(elementType = StaticType.STRING)
                    )
                ),
                handler = expectQueryOutputType(unionOf(StaticType.INT, StaticType.BOOL, StaticType.STRING))
            ),
            TestCase(
                name = "path on list of union type",
                originalSql = "a[1]",
                globals = mapOf("a" to ListType(elementType = unionOf(StaticType.INT, StaticType.BOOL, StaticType.STRING))),
                handler = expectQueryOutputType(unionOf(StaticType.INT, StaticType.BOOL, StaticType.STRING))
            ),
            TestCase(
                name = "path on union of list and ANY",
                originalSql = "a[1]",
                globals = mapOf(
                    "a" to unionOf(
                        ListType(elementType = StaticType.INT),
                        StaticType.ANY
                    )
                ),
                handler = expectQueryOutputType(StaticType.ANY)
            ),
            TestCase(
                name = "path on single element union",
                originalSql = "a.id",
                globals = mapOf(
                    "a" to unionOf(
                        StructType(mapOf("id" to StaticType.INT))
                    )
                ),
                handler = expectQueryOutputType(StaticType.INT)
            ),
            TestCase(
                name = "path on union of struct and ANY",
                originalSql = "a.id",
                globals = mapOf(
                    "a" to unionOf(
                        StaticType.ANY,
                        StructType(mapOf("id" to StaticType.INT))
                    )
                ),
                handler = expectQueryOutputType(StaticType.ANY)
            ),
            TestCase(
                name = "path on nullable struct",
                originalSql = "a.id",
                globals = mapOf(
                    "a" to StructType(
                        mapOf("id" to StaticType.INT)
                    ).asNullable()
                ),
                handler = expectQueryOutputType(unionOf(StaticType.INT, StaticType.MISSING))
            ),
            TestCase(
                name = "path on union of structs with same field name",
                originalSql = "a.id",
                globals = mapOf(
                    "a" to unionOf(
                        StructType(mapOf("id" to StaticType.INT)),
                        StructType(mapOf("id" to StaticType.STRING))
                    )
                ),
                handler = expectQueryOutputType(unionOf(StaticType.INT, StaticType.STRING))
            ),
            TestCase(
                name = "path on union of struct and int",
                originalSql = "a.id",
                globals = mapOf(
                    "a" to unionOf(
                        StructType(mapOf("id" to StaticType.INT)),
                        StaticType.INT
                    )
                ),
                handler = expectQueryOutputType(unionOf(StaticType.INT, StaticType.MISSING))
            ),
            TestCase(
                name = "path on nullable union of struct and int",
                originalSql = "a.id",
                globals = mapOf(
                    "a" to unionOf(
                        StructType(mapOf("id" to StaticType.INT)),
                        StaticType.INT
                    ).asNullable()
                ),
                handler = expectQueryOutputType(unionOf(StaticType.INT, StaticType.MISSING))
            ),
            TestCase(
                name = "path on union of struct, int, timestamp",
                originalSql = "a.id",
                globals = mapOf(
                    "a" to unionOf(
                        StructType(mapOf("id" to StaticType.INT)),
                        StaticType.INT,
                        StaticType.TIMESTAMP
                    )
                ),
                handler = expectQueryOutputType(unionOf(StaticType.INT, StaticType.MISSING))
            ),
            TestCase(
                name = "path on struct with union",
                originalSql = "a.id",
                globals = mapOf(
                    "a" to StructType(
                        mapOf("id" to unionOf(StaticType.INT, StaticType.STRING))
                    )
                ),
                handler = expectQueryOutputType(unionOf(StaticType.INT, StaticType.STRING))
            ),
            TestCase(
                name = "path on nullable field",
                originalSql = "a.b.id",
                globals = mapOf(
                    "a" to StructType(
                        mapOf(
                            "b" to StructType(
                                mapOf("id" to StaticType.INT)
                            ).asNullable()
                        )
                    )
                ),
                handler = expectQueryOutputType(unionOf(StaticType.INT, StaticType.MISSING))
            ),
            TestCase(
                name = "path on optional struct",
                originalSql = "a.id",
                globals = mapOf(
                    "a" to StructType(
                        mapOf("id" to StaticType.INT)
                    ).asOptional()
                ),
                handler = expectQueryOutputType(unionOf(StaticType.INT, StaticType.MISSING))
            ),
            TestCase(
                name = "path on nullable, optional struct",
                originalSql = "a.id",
                globals = mapOf(
                    "a" to StructType(
                        mapOf("id" to StaticType.INT)
                    ).asNullable().asOptional()
                ),
                handler = expectQueryOutputType(unionOf(StaticType.INT, StaticType.MISSING))
            ),
            TestCase(
                name = "path on nullable struct with multiple path steps",
                originalSql = "a.b.c.id",
                globals = mapOf(
                    "a" to StructType(
                        mapOf(
                            "b" to StructType(
                                mapOf(
                                    "c" to StructType(
                                        mapOf("id" to StaticType.INT)
                                    )
                                )
                            )
                        )
                    ).asNullable()
                ),
                handler = expectQueryOutputType(unionOf(StaticType.INT, StaticType.MISSING))
            ),
            TestCase(
                name = "path on nullable struct within path steps",
                originalSql = "a.b.c.id",
                globals = mapOf(
                    "a" to StructType(
                        mapOf(
                            "b" to StructType(
                                mapOf(
                                    "c" to StructType(
                                        mapOf("id" to StaticType.INT)
                                    ).asNullable()
                                )
                            )
                        )
                    )
                ),
                handler = expectQueryOutputType(unionOf(StaticType.INT, StaticType.MISSING))
            ),
            TestCase(
                name = "path on union type for every path step",
                originalSql = "a.b.c.id",
                globals = mapOf(
                    "a" to StructType(
                        mapOf(
                            "b" to StructType(
                                mapOf(
                                    "c" to StructType(
                                        mapOf("id" to StaticType.INT)
                                    ).asNullable()
                                )
                            ).asNullable()
                        )
                    ).asNullable()
                ),
                handler = expectQueryOutputType(unionOf(StaticType.INT, StaticType.MISSING))
            ),
            TestCase(
                name = "path on struct, terminal is ANY",
                originalSql = "a.b.c.id",
                globals = mapOf(
                    "a" to StructType(
                        mapOf(
                            "b" to StructType(
                                mapOf(
                                    "c" to StructType(
                                        mapOf("id" to StaticType.ANY)
                                    )
                                )
                            )
                        )
                    )
                ),
                handler = expectQueryOutputType(StaticType.ANY)
            ),
            TestCase(
                name = "path on struct, terminal is nullable",
                originalSql = "a.b.c.id",
                globals = mapOf(
                    "a" to StructType(
                        mapOf(
                            "b" to StructType(
                                mapOf(
                                    "c" to StructType(
                                        mapOf("id" to StaticType.INT.asNullable())
                                    )
                                )
                            )
                        )
                    )
                ),
                handler = expectQueryOutputType(unionOf(StaticType.INT, StaticType.NULL))
            ),
            TestCase(
                name = "path on struct, terminal is empty struct",
                originalSql = "a.b.c.id",
                globals = mapOf(
                    "a" to StructType(
                        mapOf(
                            "b" to StructType(
                                mapOf(
                                    "c" to StructType(
                                        mapOf("id" to StructType(emptyMap()))
                                    )
                                )
                            )
                        )
                    )
                ),
                handler = expectQueryOutputType(StructType(emptyMap()))
            ),
            TestCase(
                name = "path on struct, terminal is a struct",
                originalSql = "a.b.c",
                globals = mapOf(
                    "a" to StructType(
                        mapOf(
                            "b" to StructType(
                                mapOf(
                                    "c" to StructType(
                                        mapOf("id" to StaticType.INT)
                                    )
                                )
                            )
                        )
                    )
                ),
                handler = expectQueryOutputType(StructType(mapOf("id" to StaticType.INT)))
            ),
            TestCase(
                name = "path on struct, terminal is a nullable struct",
                originalSql = "a.b.c",
                globals = mapOf(
                    "a" to StructType(
                        mapOf(
                            "b" to StructType(
                                mapOf(
                                    "c" to StructType(
                                        mapOf("id" to StaticType.INT)
                                    ).asNullable()
                                )
                            )
                        )
                    )
                ),
                handler = expectQueryOutputType(unionOf(StructType(mapOf("id" to StaticType.INT)), StaticType.NULL))
            ),
            TestCase(
                name = "path on struct, terminal is an optional struct",
                originalSql = "a.b.c",
                globals = mapOf(
                    "a" to StructType(
                        mapOf(
                            "b" to StructType(
                                mapOf(
                                    "c" to StructType(
                                        mapOf("id" to StaticType.INT)
                                    ).asOptional()
                                )
                            )
                        )
                    )
                ),
                handler = expectQueryOutputType(unionOf(StructType(mapOf("id" to StaticType.INT)), StaticType.MISSING))
            ),
            TestCase(
                name = "path on struct, terminal is a nullable, optional struct",
                originalSql = "a.b.c",
                globals = mapOf(
                    "a" to StructType(
                        mapOf(
                            "b" to StructType(
                                mapOf(
                                    "c" to StructType(
                                        mapOf("id" to StaticType.INT)
                                    ).asNullable().asOptional()
                                )
                            )
                        )
                    )
                ),
                handler = expectQueryOutputType(unionOf(StructType(mapOf("id" to StaticType.INT)), StaticType.NULL, StaticType.MISSING))
            ),
            TestCase(
                name = "path on nullable struct, terminal is a struct",
                originalSql = "a.b.c",
                globals = mapOf(
                    "a" to StructType(
                        mapOf(
                            "b" to StructType(
                                mapOf(
                                    "c" to StructType(
                                        mapOf("id" to StaticType.INT)
                                    )
                                )
                            )
                        )
                    ).asNullable()
                ),
                handler = expectQueryOutputType(unionOf(StructType(mapOf("id" to StaticType.INT)), StaticType.MISSING))
            ),
            TestCase(
                name = "path on nullable struct and list",
                originalSql = "a.b[1]",
                globals = mapOf(
                    "a" to StructType(
                        mapOf("b" to ListType(elementType = StaticType.INT))
                    ).asNullable()
                ),
                handler = expectQueryOutputType(unionOf(StaticType.INT, StaticType.MISSING))
            ),
            TestCase(
                name = "path on nullable struct and nullable list",
                originalSql = "a.b[1]",
                globals = mapOf(
                    "a" to StructType(
                        mapOf("b" to ListType(elementType = StaticType.INT).asNullable())
                    ).asNullable()
                ),
                handler = expectQueryOutputType(unionOf(StaticType.INT, StaticType.MISSING))
            ),
            TestCase(
                name = "path on nullable list and struct",
                originalSql = "a[1].id",
                globals = mapOf(
                    "a" to ListType(
                        elementType = StructType(mapOf("id" to StaticType.INT))
                    ).asNullable()
                ),
                handler = expectQueryOutputType(unionOf(StaticType.INT, StaticType.MISSING))
            ),
            TestCase(
                name = "path on nullable list and nullable struct",
                originalSql = "a[1].id",
                globals = mapOf(
                    "a" to ListType(
                        elementType = StructType(
                            mapOf("id" to StaticType.INT)
                        ).asNullable()
                    ).asNullable()
                ),
                handler = expectQueryOutputType(unionOf(StaticType.INT, StaticType.MISSING))
            )
        )

        @JvmStatic
        @Suppress("unused")
        fun parametersForSimpleCaseWhen() = listOf(
            TestCase(
                name = "CASE <val> WHEN with ELSE expression resulting single type",
                originalSql = "CASE 'a_string' WHEN 'a_string' THEN true ELSE false END",
                handler = expectQueryOutputType(BOOL)
            ),
            TestCase(
                name = "CASE <val> WHEN without ELSE expression",
                originalSql = "CASE 'a_string' WHEN 'a_string' THEN true END",
                handler = expectQueryOutputType(unionOf(BOOL, NULL))
            ),
            TestCase(
                name = "CASE <val> WHEN without ELSE expression resulting union type",
                originalSql = "CASE 'a_string' WHEN 'a_string' THEN 'another_string' END",
                handler = expectQueryOutputType(unionOf(STRING, NULL))
            ),
            TestCase(
                name = "CASE <val> WHEN with ELSE expression, INT compared to known types, THEN and ELSE of known types",
                originalSql = """
                    CASE 1 
                        WHEN t_int THEN t_int
                        WHEN t_float THEN t_decimal
                        ELSE t_string
                    END
                    """,
                globals = mapOf(
                    "t_int" to INT,
                    "t_float" to FLOAT,
                    "t_decimal" to DECIMAL,
                    "t_string" to STRING
                ),
                handler = expectQueryOutputType(unionOf(INT, DECIMAL, STRING))
            ),
            TestCase(
                name = "CASE <val> WHEN with ELSE expression, INT compared to ANY, THEN and ELSE of known types",
                originalSql = """
                    CASE 1 
                        WHEN t_any THEN t_int
                        ELSE t_string
                    END
                    """,
                globals = mapOf(
                    "t_any" to ANY,
                    "t_int" to INT,
                    "t_string" to STRING
                ),
                handler = expectQueryOutputType(unionOf(INT, STRING))
            ),
            TestCase(
                name = "CASE <val> WHEN with ELSE expression, INT compared to nullable type, THEN and ELSE of known types",
                originalSql = """
                    CASE 1 
                        WHEN t_nullable_int THEN t_int
                        ELSE t_string
                    END
                    """,
                globals = mapOf(
                    "t_nullable_int" to INT.asNullable(),
                    "t_int" to INT,
                    "t_string" to STRING
                ),
                handler = expectQueryOutputType(unionOf(INT, STRING))
            ),
            TestCase(
                name = "CASE <val> WHEN with ELSE expression, INT compared to optional type, THEN and ELSE of known types",
                originalSql = """
                    CASE 1 
                        WHEN t_optional_int THEN t_int
                        ELSE t_string
                    END
                    """,
                globals = mapOf(
                    "t_optional_int" to INT.asOptional(),
                    "t_int" to INT,
                    "t_string" to STRING
                ),
                handler = expectQueryOutputType(unionOf(INT, STRING))
            ),
            TestCase(
                name = "CASE <val> WHEN with ELSE expression, INT compared to nullable and optional, THEN and ELSE of known types",
                originalSql = """
                    CASE 1 
                        WHEN t_nullable_int THEN t_int
                        WHEN t_optional_int THEN t_decimal
                        ELSE t_string
                    END
                    """,
                globals = mapOf(
                    "t_nullable_int" to INT.asNullable(),
                    "t_optional_int" to INT.asOptional(),
                    "t_int" to INT,
                    "t_decimal" to DECIMAL,
                    "t_string" to STRING
                ),
                handler = expectQueryOutputType(unionOf(INT, DECIMAL, STRING))
            ),
            TestCase(
                name = "CASE <val> WHEN with ELSE expression, union of numerics compared with numerics, THEN and ELSE of known types",
                originalSql = """
                    CASE t_numerics
                        WHEN t_int THEN t_int
                        WHEN t_decimal THEN t_decimal
                        ELSE t_float
                    END
                    """,
                globals = mapOf(
                    "t_numerics" to unionOf(ALL_NUMERIC_TYPES.toSet()),
                    "t_int" to INT,
                    "t_decimal" to DECIMAL,
                    "t_float" to FLOAT
                ),
                handler = expectQueryOutputType(unionOf(INT, DECIMAL, FLOAT))
            ),
            TestCase(
                name = "CASE <val> WHEN with ELSE expression, int compared with unions containing int, THEN and ELSE of known types",
                originalSql = """
                    CASE t_int
                        WHEN t_int_string THEN t_int_string
                        WHEN t_int_bool THEN t_int_bool
                        ELSE t_float
                    END
                    """,
                globals = mapOf(
                    "t_int" to INT,
                    "t_int_string" to unionOf(INT, STRING),
                    "t_int_bool" to unionOf(INT, BOOL),
                    "t_float" to FLOAT
                ),
                handler = expectQueryOutputType(unionOf(INT, STRING, BOOL, FLOAT))
            ),
            //
            // SimpleCaseWhen error cases below
            //
            TestCase(
                name = "data type mismatch errors: CASE <int> WHEN <string> THEN <string> WHEN <symbol> THEN <symbol> END",
                originalSql = """
                    CASE t_int
                        WHEN t_string THEN t_string
                        WHEN t_symbol THEN t_symbol
                    END
                    """,
                globals = mapOf(
                    "t_int" to INT,
                    "t_string" to STRING,
                    "t_symbol" to SYMBOL
                ),
                handler = expectSemanticProblems(
                    listOf(
                        createDataTypeMismatchError(SourceLocationMeta(3L, 30L, 8L), argTypes = listOf(INT, STRING), nAryOp = "CASE"),
                        createDataTypeMismatchError(SourceLocationMeta(4L, 30L, 8L), argTypes = listOf(INT, SYMBOL), nAryOp = "CASE")
                    )
                )
            ),
            TestCase(
                name = "data type mismatch errors: CASE <nullable_int> WHEN <nullable_string> THEN <nullable_string> WHEN <optional_symbol> THEN <optional_symbol> END",
                originalSql = """
                    CASE t_nullable_int
                        WHEN t_nullable_string THEN t_nullable_string
                        WHEN t_optional_symbol THEN t_optional_symbol
                    END
                    """,
                globals = mapOf(
                    "t_nullable_int" to INT.asNullable(),
                    "t_nullable_string" to STRING.asNullable(),
                    "t_optional_symbol" to SYMBOL.asOptional()
                ),
                handler = expectSemanticProblems(
                    listOf(
                        createDataTypeMismatchError(SourceLocationMeta(3L, 30L, 17L), argTypes = listOf(INT.asNullable(), STRING.asNullable()), nAryOp = "CASE"),
                        createDataTypeMismatchError(SourceLocationMeta(4L, 30L, 17L), argTypes = listOf(INT.asNullable(), SYMBOL.asOptional()), nAryOp = "CASE")
                    )
                )
            ),
            TestCase(
                name = "null or missing error (caseValue = null): CASE <null> WHEN <string> THEN <string> WHEN <symbol> THEN <symbol> END",
                originalSql = """
                    CASE t_null
                        WHEN t_string THEN t_string
                        WHEN t_symbol THEN t_symbol
                    END
                    """,
                globals = mapOf(
                    "t_null" to NULL,
                    "t_string" to STRING,
                    "t_symbol" to SYMBOL
                ),
                handler = expectSemanticProblems(listOf(createReturnsNullOrMissingError(SourceLocationMeta(2L, 26L, 6L))))
            ),
            TestCase(
                name = "null or missing error (caseValue = missing): CASE <missing> WHEN <string> THEN <string> WHEN <symbol> THEN <symbol> END",
                originalSql = """
                    CASE t_missing
                        WHEN t_string THEN t_string
                        WHEN t_symbol THEN t_symbol
                    END
                    """,
                globals = mapOf(
                    "t_missing" to MISSING,
                    "t_string" to STRING,
                    "t_symbol" to SYMBOL
                ),
                handler = expectSemanticProblems(listOf(createReturnsNullOrMissingError(SourceLocationMeta(2L, 26L, 9L))))
            ),
            TestCase(
                name = "null or missing error (caseValue = null_or_missing): CASE <null_or_missing> WHEN <string> THEN <string> WHEN <symbol> THEN <symbol> END",
                originalSql = """
                    CASE t_null_or_missing
                        WHEN t_string THEN t_string
                        WHEN t_symbol THEN t_symbol
                    END
                    """,
                globals = mapOf(
                    "t_null_or_missing" to NULL_OR_MISSING,
                    "t_string" to STRING,
                    "t_symbol" to SYMBOL
                ),
                handler = expectSemanticProblems(listOf(createReturnsNullOrMissingError(SourceLocationMeta(2L, 26L, 17L))))
            ),
            TestCase(
                name = "data type mismatch and null or missing errors: caseValue = INT, whenExprs of unknowns and incompatible types",
                originalSql = """
                    CASE t_int
                        WHEN t_null THEN t_null
                        WHEN t_missing THEN t_missing
                        WHEN t_null_or_missing THEN t_null_or_missing
                        WHEN t_string THEN t_string
                        WHEN t_symbol THEN t_symbol
                    END
                    """,
                globals = mapOf(
                    "t_int" to INT,
                    "t_null" to NULL,
                    "t_missing" to MISSING,
                    "t_null_or_missing" to NULL_OR_MISSING,
                    "t_string" to STRING,
                    "t_symbol" to SYMBOL
                ),
                handler = expectSemanticProblems(
                    listOf(
                        createReturnsNullOrMissingError(SourceLocationMeta(3L, 30L, 6L)),
                        createReturnsNullOrMissingError(SourceLocationMeta(4L, 30L, 9L)),
                        createReturnsNullOrMissingError(SourceLocationMeta(5L, 30L, 17L)),
                        createDataTypeMismatchError(SourceLocationMeta(6L, 30L, 8L), argTypes = listOf(INT, STRING), nAryOp = "CASE"),
                        createDataTypeMismatchError(SourceLocationMeta(7L, 30L, 8L), argTypes = listOf(INT, SYMBOL), nAryOp = "CASE")
                    )
                )
            ),
            TestCase(
                name = "null or missing error at caseValue and whenExprs: caseValue = null, whenExprs of unknowns",
                originalSql = """
                    CASE t_null
                        WHEN t_null THEN t_null
                        WHEN t_missing THEN t_missing
                        WHEN t_string THEN t_string
                    END
                    """,
                globals = mapOf(
                    "t_null" to NULL,
                    "t_missing" to MISSING,
                    "t_string" to STRING
                ),
                handler = expectSemanticProblems(
                    listOf(
                        createReturnsNullOrMissingError(SourceLocationMeta(2L, 26L, 6L)),
                        createReturnsNullOrMissingError(SourceLocationMeta(3L, 30L, 6L)),
                        createReturnsNullOrMissingError(SourceLocationMeta(4L, 30L, 9L))
                    )
                )
            ),
            TestCase(
                name = "data type mismatch errors: caseValue = union of numeric types, whenExprs of non-numeric union types",
                originalSql = """
                    CASE t_numeric
                        WHEN t_text THEN t_text
                        WHEN t_lob THEN t_lob
                        WHEN t_other THEN t_other
                    END
                    """,
                globals = mapOf(
                    "t_numeric" to unionOf(ALL_NUMERIC_TYPES.toSet()),
                    "t_text" to unionOf(ALL_TEXT_TYPES.toSet()),
                    "t_lob" to unionOf(ALL_LOB_TYPES.toSet()),
                    "t_other" to unionOf(ALL_TYPES_ONLY_COMPARABLE_TO_SELF.toSet())
                ),
                handler = expectSemanticProblems(
                    listOf(
                        createDataTypeMismatchError(
                            SourceLocationMeta(3L, 30L, 6L),
                            argTypes = listOf(unionOf(ALL_NUMERIC_TYPES.toSet()), unionOf(ALL_TEXT_TYPES.toSet())),
                            nAryOp = "CASE"
                        ),
                        createDataTypeMismatchError(
                            SourceLocationMeta(4L, 30L, 5L),
                            argTypes = listOf(unionOf(ALL_NUMERIC_TYPES.toSet()), unionOf(ALL_LOB_TYPES.toSet())),
                            nAryOp = "CASE"
                        ),
                        createDataTypeMismatchError(
                            SourceLocationMeta(5L, 30L, 7L),
                            argTypes = listOf(unionOf(ALL_NUMERIC_TYPES.toSet()), unionOf(ALL_TYPES_ONLY_COMPARABLE_TO_SELF.toSet())),
                            nAryOp = "CASE"
                        )
                    )
                )
            )
        )

        @JvmStatic
        @Suppress("unused")
        fun parametersForSearchedCaseWhen() =
            listOf(
                TestCase(
                    "CASE WHEN with ELSE expression resulting single type",
                    "CASE WHEN true THEN true ELSE false END",
                    handler = expectQueryOutputType(BOOL)
                ),
                TestCase(
                    "CASE WHEN without ELSE expression",
                    "CASE WHEN true THEN true WHEN false THEN false END",
                    handler = expectQueryOutputType(unionOf(BOOL, NULL))
                ),
                TestCase(
                    "CASE WHEN without ELSE expression resulting union type",
                    "CASE WHEN true THEN 'true' WHEN false THEN false END",
                    handler = expectQueryOutputType(unionOf(STRING, BOOL, NULL))
                ),
                TestCase(
                    "CASE WHEN with ELSE expression, nullable type in THEN",
                    "CASE WHEN true THEN nullable_bool ELSE false END",
                    mapOf(
                        "nullable_bool" to unionOf(BOOL, NULL)
                    ),
                    handler = expectQueryOutputType(unionOf(BOOL, NULL))
                ),
                TestCase(
                    "CASE WHEN with ELSE expression, nullable type in ELSE",
                    "CASE WHEN true THEN true ELSE nullable_bool END",
                    mapOf(
                        "nullable_bool" to unionOf(BOOL, NULL)
                    ),
                    handler = expectQueryOutputType(unionOf(BOOL, NULL))
                ),
                TestCase(
                    "CASE WHEN with ELSE expression, different nullable types",
                    "CASE WHEN true THEN nullable_string ELSE nullable_bool END",
                    mapOf(
                        "nullable_bool" to unionOf(BOOL, NULL),
                        "nullable_string" to unionOf(STRING, NULL)
                    ),
                    handler = expectQueryOutputType(unionOf(BOOL, STRING, NULL))
                ),
                TestCase(
                    "CASE WHEN with ELSE expression, WHEN expr INT compared to nullable INT, THEN of known types",
                    """
                     CASE
                         WHEN t_int = t_nullable_int THEN t_int
                         ELSE t_string
                     END
                     """,
                    mapOf(
                        "t_int" to INT,
                        "t_nullable_int" to INT.asNullable(),
                        "t_string" to STRING
                    ),
                    handler = expectQueryOutputType(unionOf(INT, STRING))
                ),
                TestCase(
                    "CASE WHEN without ELSE expression, WHEN expr INT compared to nullable INT, THEN of known type",
                    """
                     CASE
                         WHEN t_int = t_nullable_int THEN t_int
                     END
                     """,
                    mapOf(
                        "t_int" to INT,
                        "t_nullable_int" to INT.asNullable()
                    ),
                    handler = expectQueryOutputType(unionOf(INT, NULL))
                ),
                TestCase(
                    "CASE WHEN with ELSE expression, WHEN expr INT compared to optional INT, THEN of known types",
                    """
                     CASE
                         WHEN t_int = t_optional_int THEN t_int
                         ELSE t_string
                     END
                     """,
                    mapOf(
                        "t_int" to INT,
                        "t_optional_int" to INT.asOptional(),
                        "t_string" to STRING
                    ),
                    handler = expectQueryOutputType(unionOf(INT, STRING))
                ),
                TestCase(
                    "CASE WHEN without ELSE expression, WHEN expr INT compared to optional INT, THEN of known types",
                    """
                     CASE
                         WHEN t_int = t_optional_int THEN t_int
                     END
                     """,
                    mapOf(
                        "t_int" to INT,
                        "t_optional_int" to INT.asOptional()
                    ),
                    handler = expectQueryOutputType(unionOf(INT, NULL))
                ),
                TestCase(
                    "CASE WHEN with ELSE expression, WHEN expr union with bool, THEN of known types",
                    """
                     CASE
                         WHEN u_bool_and_other_types THEN t_int
                         ELSE t_string
                     END
                     """,
                    mapOf(
                        "u_bool_and_other_types" to unionOf(BOOL, INT, NULL),
                        "t_int" to INT,
                        "t_string" to STRING
                    ),
                    handler = expectQueryOutputType(unionOf(INT, STRING))
                ),
                TestCase(
                    "CASE WHEN without ELSE expression, WHEN expr union with bool, THEN of known type",
                    """
                     CASE
                         WHEN u_bool_and_other_types THEN t_int
                     END
                     """,
                    mapOf(
                        "u_bool_and_other_types" to unionOf(BOOL, INT, NULL),
                        "t_int" to INT
                    ),
                    handler = expectQueryOutputType(unionOf(INT, NULL))
                ),
                TestCase(
                    "CASE WHEN with ELSE expression, WHEN expr ANY, THEN of known type",
                    """
                     CASE
                         WHEN t_any THEN t_int
                         ELSE t_string
                     END
                     """,
                    mapOf(
                        "t_any" to ANY,
                        "t_int" to INT,
                        "t_string" to STRING
                    ),
                    handler = expectQueryOutputType(unionOf(INT, STRING))
                ),
                TestCase(
                    "CASE WHEN without ELSE expression, WHEN expr ANY, THEN of known type",
                    """
                     CASE
                         WHEN t_any THEN t_int
                     END
                     """,
                    mapOf(
                        "t_any" to ANY,
                        "t_int" to INT
                    ),
                    handler = expectQueryOutputType(unionOf(INT, NULL))
                )
            ) +
                //
                // SearchedCaseWhen error cases below
                //

                // tests with non-bool, non-unknown whenExpr
                ALL_NON_BOOL_NON_UNKNOWN_TYPES.flatMap { nonBool ->
                    listOf(
                        TestCase(
                            name = "data type mismatch error - $nonBool whenExpr",
                            originalSql = """
                            CASE
                                WHEN t_non_bool THEN t_non_bool
                            END
                            """,
                            globals = mapOf("t_non_bool" to nonBool),
                            handler = expectSemanticProblems(
                                listOf(
                                    createIncompatibleTypesForExprError(SourceLocationMeta(3L, 38L, 10L), expectedType = BOOL, actualType = nonBool)
                                )
                            )
                        ),
                        TestCase(
                            name = "data type mismatch error - $nonBool whenExpr and elseExpr",
                            originalSql = """
                            CASE
                                WHEN t_non_bool THEN t_non_bool
                                ELSE t_non_bool
                            END
                            """,
                            globals = mapOf("t_non_bool" to nonBool),
                            handler = expectSemanticProblems(
                                listOf(
                                    createIncompatibleTypesForExprError(SourceLocationMeta(3L, 38L, 10L), expectedType = BOOL, actualType = nonBool)
                                )
                            )
                        )
                    )
                } +
                // tests with unknown whenExpr
                ALL_UNKNOWN_TYPES.flatMap { unknownType ->
                    listOf(
                        TestCase(
                            name = "null or missing error - $unknownType whenExpr",
                            originalSql = """
                            CASE
                                WHEN t_unknown THEN t_unknown
                            END
                            """,
                            globals = mapOf("t_unknown" to unknownType),
                            handler = expectSemanticProblems(
                                listOf(
                                    createReturnsNullOrMissingError(SourceLocationMeta(3L, 38L, 9L))
                                )
                            )
                        ),
                        TestCase(
                            name = "null or missing error - $unknownType whenExpr and elseExpr",
                            originalSql = """
                            CASE
                                WHEN t_unknown THEN t_unknown
                                ELSE t_unknown
                            END
                            """,
                            globals = mapOf("t_unknown" to unknownType),
                            handler = expectSemanticProblems(
                                listOf(
                                    createReturnsNullOrMissingError(SourceLocationMeta(3L, 38L, 9L))
                                )
                            )
                        )
                    )
                } +
                listOf(
                    TestCase(
                        name = "multiple errors - non-bool whenExprs and unknown whenExprs",
                        originalSql = """
                        CASE
                            WHEN t_int THEN t_int
                            WHEN t_string THEN t_string
                            WHEN t_any THEN t_any
                            WHEN t_null THEN t_null
                            WHEN t_missing THEN t_missing
                        END
                        """,
                        globals = mapOf(
                            "t_int" to INT,
                            "t_string" to STRING,
                            "t_any" to ANY,
                            "t_null" to NULL,
                            "t_missing" to MISSING
                        ),
                        handler = expectSemanticProblems(
                            listOf(
                                createIncompatibleTypesForExprError(SourceLocationMeta(3L, 34L, 5L), expectedType = BOOL, actualType = INT),
                                createIncompatibleTypesForExprError(SourceLocationMeta(4L, 34L, 8L), expectedType = BOOL, actualType = STRING),
                                createReturnsNullOrMissingError(SourceLocationMeta(6L, 34L, 6L)),
                                createReturnsNullOrMissingError(SourceLocationMeta(7L, 34L, 9L))
                            )
                        )
                    ),
                    TestCase(
                        name = "multiple errors - whenExprs of unions not containing bool",
                        originalSql = """
                        CASE
                            WHEN t_numeric THEN t_numeric
                            WHEN t_text THEN t_text
                            WHEN t_lob THEN t_lob
                            WHEN t_null_or_missing THEN t_null_or_missing
                        END
                        """,
                        globals = mapOf(
                            "t_numeric" to unionOf(ALL_NUMERIC_TYPES.toSet()),
                            "t_text" to unionOf(ALL_TEXT_TYPES.toSet()),
                            "t_lob" to unionOf(ALL_LOB_TYPES.toSet()),
                            "t_null_or_missing" to NULL_OR_MISSING
                        ),
                        handler = expectSemanticProblems(
                            listOf(
                                createIncompatibleTypesForExprError(
                                    SourceLocationMeta(3L, 34L, 9L),
                                    expectedType = BOOL,
                                    actualType = unionOf(ALL_NUMERIC_TYPES.toSet())
                                ),
                                createIncompatibleTypesForExprError(
                                    SourceLocationMeta(4L, 34L, 6L),
                                    expectedType = BOOL,
                                    actualType = unionOf(ALL_TEXT_TYPES.toSet())
                                ),
                                createIncompatibleTypesForExprError(
                                    SourceLocationMeta(5L, 34L, 5L),
                                    expectedType = BOOL,
                                    actualType = unionOf(ALL_LOB_TYPES.toSet())
                                ),
                                createReturnsNullOrMissingError(SourceLocationMeta(6L, 34L, 17L))
                            )
                        )
                    )
                )

        /**
         * Creates a SimpleCaseWhen and SearchedCaseWhen clause [TestCase] for testing the inferred static type of the
         * THEN and ELSE expression results.
         *
         * For each SimpleCaseWhen test, every `case-value` and `when-value` will be an integer.
         * For each SearchedCaseWhen test, every `when-predicate` will be `true`.
         *
         * The `then-result` types are specified by [thenTypes]. An `else-result` type can optionally be specified
         * by [elseType]. If no [elseType] is specified, the [TestCase] will not have an else clause. The resulting
         * SimpleCaseWhen and SearchedCaseWhen clause [TestCase] will have its output type checked with [expectedType].
         */
        private fun createSimpleAndSearchedCaseWhenTestCases(
            name: String,
            thenTypes: List<StaticType>,
            elseType: StaticType? = null,
            expectedType: StaticType
        ): List<TestCase> {
            val globals = mutableMapOf<String, StaticType>()
            var simpleCaseWhenQuery = "CASE 0\n"
            var searchedCaseWhenQuery = "CASE\n"

            thenTypes.mapIndexed { index, staticType ->
                simpleCaseWhenQuery += "WHEN $index THEN t_$index\n"
                searchedCaseWhenQuery += "WHEN true THEN t_$index\n"
                globals.put("t_$index", staticType)
            }

            if (elseType != null) {
                val elseClause = "ELSE t_${thenTypes.size}\n"
                simpleCaseWhenQuery += elseClause
                searchedCaseWhenQuery += elseClause
                globals["t_${thenTypes.size}"] = elseType
            }
            simpleCaseWhenQuery += "END"
            searchedCaseWhenQuery += "END"

            return listOf(
                TestCase(
                    name = "SimpleCaseWhen $name",
                    originalSql = simpleCaseWhenQuery,
                    globals = globals,
                    handler = expectQueryOutputType(expectedType)
                ),
                TestCase(
                    name = "SearchedCaseWhen $name",
                    originalSql = searchedCaseWhenQuery,
                    globals = globals,
                    handler = expectQueryOutputType(expectedType)
                )
            )
        }

        @JvmStatic
        @Suppress("unused")
        fun parametersForSimpleAndSearchedCaseWhen() = listOf(
            createSimpleAndSearchedCaseWhenTestCases(
                name = "with ELSE, THEN of INT",
                thenTypes = listOf(INT, INT, INT),
                elseType = INT,
                expectedType = INT
            ),
            createSimpleAndSearchedCaseWhenTestCases(
                name = "without ELSE, THEN of INT",
                thenTypes = listOf(INT, INT, INT),
                expectedType = unionOf(INT, NULL)
            ),
            createSimpleAndSearchedCaseWhenTestCases(
                name = "with ELSE, THEN of mixed known types",
                thenTypes = listOf(INT, STRING, TIMESTAMP),
                elseType = CLOB,
                expectedType = unionOf(INT, STRING, TIMESTAMP, CLOB)
            ),
            createSimpleAndSearchedCaseWhenTestCases(
                name = "without ELSE, THEN of mixed known types",
                thenTypes = listOf(INT, STRING, TIMESTAMP),
                expectedType = unionOf(INT, STRING, TIMESTAMP, NULL)
            ),
            createSimpleAndSearchedCaseWhenTestCases(
                name = "with ELSE, THEN of mixed known union types",
                thenTypes = listOf(unionOf(INT, DECIMAL), STRING, TIMESTAMP),
                elseType = CLOB,
                expectedType = unionOf(INT, DECIMAL, STRING, TIMESTAMP, CLOB)
            ),
            createSimpleAndSearchedCaseWhenTestCases(
                name = "without ELSE, THEN of mixed known union types",
                thenTypes = listOf(unionOf(INT, DECIMAL), STRING, TIMESTAMP),
                expectedType = unionOf(INT, DECIMAL, STRING, TIMESTAMP, NULL)
            ),
            createSimpleAndSearchedCaseWhenTestCases(
                name = "with ELSE, THEN of unknown types",
                thenTypes = listOf(NULL, MISSING),
                elseType = NULL_OR_MISSING,
                expectedType = unionOf(NULL, MISSING)
            ),
            createSimpleAndSearchedCaseWhenTestCases(
                name = "without ELSE, THEN of unknown types",
                thenTypes = listOf(NULL, MISSING),
                expectedType = unionOf(NULL, MISSING)
            ),
            createSimpleAndSearchedCaseWhenTestCases(
                name = "with ELSE, THEN of unknown types and ANY",
                thenTypes = listOf(NULL, MISSING, ANY),
                elseType = NULL_OR_MISSING,
                expectedType = ANY
            ),
            createSimpleAndSearchedCaseWhenTestCases(
                name = "with ELSE, THEN of unknown types and ELSE of ANY",
                thenTypes = listOf(NULL, MISSING, NULL_OR_MISSING),
                elseType = ANY,
                expectedType = ANY
            ),
            createSimpleAndSearchedCaseWhenTestCases(
                name = "without ELSE, THEN of unknown types and ANY",
                thenTypes = listOf(NULL, MISSING, ANY),
                expectedType = ANY
            ),
            createSimpleAndSearchedCaseWhenTestCases(
                name = "with ELSE, THEN of INT, MISSING, NULL, ELSE STRING",
                thenTypes = listOf(INT, MISSING, NULL),
                elseType = STRING,
                expectedType = unionOf(INT, MISSING, NULL, STRING)
            ),
            createSimpleAndSearchedCaseWhenTestCases(
                name = "without ELSE, THEN of INT, MISSING, NULL",
                thenTypes = listOf(INT, MISSING, NULL),
                expectedType = unionOf(INT, MISSING, NULL)
            ),
            createSimpleAndSearchedCaseWhenTestCases(
                name = "with ELSE, THEN of INT, MISSING, NULL, ELSE ANY",
                thenTypes = listOf(INT, MISSING, NULL),
                elseType = ANY,
                expectedType = ANY
            ),
            createSimpleAndSearchedCaseWhenTestCases(
                name = "without ELSE, THEN of INT, MISSING, NULL, ELSE ANY",
                thenTypes = listOf(INT, MISSING, NULL, ANY),
                expectedType = ANY
            )
        ).flatten()

        /**
         * Creates a test expecting [outputType] with the query: [leftType] IN [rightType]
         */
        private fun createNAryOpInTest(
            name: String,
            leftType: StaticType,
            rightType: StaticType,
            outputType: StaticType
        ): TestCase =
            TestCase(
                name = "NAry op IN - ($name)",
                originalSql = "lhs IN rhs",
                globals = mapOf(
                    "lhs" to leftType,
                    "rhs" to rightType
                ),
                handler = expectQueryOutputType(outputType)
            )

        /**
         * Creates a test for each [CollectionType] of the form: [leftType] IN collection([rightElementType]). Each test
         * expects [StaticType.BOOL] as the output query type.
         *
         * Also creates a test with the row-value constructor of the form: [leftType] IN ([rightElementType])
         */
        private fun createNAryOpInAllCollectionsTest(
            leftType: StaticType,
            rightElementType: StaticType
        ): List<TestCase> = listOf(
            TestCase(
                name = "NAry op IN - $leftType IN list($rightElementType)",
                originalSql = "lhs IN rhs",
                globals = mapOf(
                    "lhs" to leftType,
                    "rhs" to ListType(elementType = rightElementType)
                ),
                handler = expectQueryOutputType(BOOL)
            ),
            TestCase(
                name = "NAry op IN - $leftType IN bag($rightElementType)",
                originalSql = "lhs IN rhs",
                globals = mapOf(
                    "lhs" to leftType,
                    "rhs" to BagType(elementType = rightElementType)
                ),
                handler = expectQueryOutputType(BOOL)
            ),
            TestCase(
                name = "NAry op IN - $leftType IN sexp($rightElementType)",
                originalSql = "lhs IN rhs",
                globals = mapOf(
                    "lhs" to leftType,
                    "rhs" to SexpType(elementType = rightElementType)
                ),
                handler = expectQueryOutputType(BOOL)
            ),
            // row-value constructor test
            TestCase(
                name = "NAry op IN - $leftType IN ($rightElementType)",
                originalSql = "lhs IN (rhs)",
                globals = mapOf(
                    "lhs" to leftType,
                    "rhs" to rightElementType
                ),
                handler = expectQueryOutputType(BOOL)
            )
        )

        /**
         * Creates a test that expects [expectedProblems] when inferring the static type of the query:
         * [leftType] IN [rightType]
         */
        private fun createNAryOpInErrorTest(
            name: String,
            leftType: StaticType,
            rightType: StaticType,
            expectedProblems: List<Problem>
        ): TestCase =
            TestCase(
                name = "NAry op IN - $name",
                originalSql = "lhs IN rhs",
                globals = mapOf(
                    "lhs" to leftType,
                    "rhs" to rightType
                ),
                handler = expectSemanticProblems(expectedProblems)
            )

        /**
         * Creates a test for each [CollectionType] expecting a data type mismatch error due to an incomparable
         * element type. The created queries will take one type from [leftTypes] and one type from [incomparableTypes]
         * and be of the form: leftType IN collection(incomparableType)
         */
        private fun createNAryOpInErrorIncomparableElementTests(
            leftTypes: List<StaticType>,
            incomparableTypes: List<StaticType>
        ): List<TestCase> =
            leftTypes.flatMap { leftType ->
                incomparableTypes.flatMap { incomparableType ->
                    listOf(
                        createNAryOpInErrorTest(
                            name = "$leftType IN list($incomparableType) - data type mismatch",
                            leftType = leftType,
                            rightType = ListType(elementType = incomparableType),
                            expectedProblems = listOf(
                                createDataTypeMismatchError(col = 5, argTypes = listOf(leftType, ListType(incomparableType)), nAryOp = "IN"),
                            )
                        ),
                        createNAryOpInErrorTest(
                            name = "$leftType IN bag($incomparableType) - data type mismatch",
                            leftType = leftType,
                            rightType = BagType(elementType = incomparableType),
                            expectedProblems = listOf(
                                createDataTypeMismatchError(col = 5, argTypes = listOf(leftType, BagType(incomparableType)), nAryOp = "IN"),
                            )
                        ),
                        createNAryOpInErrorTest(
                            name = "$leftType IN sexp($incomparableType) - data type mismatch",
                            leftType = leftType,
                            rightType = SexpType(elementType = incomparableType),
                            expectedProblems = listOf(
                                createDataTypeMismatchError(col = 5, argTypes = listOf(leftType, SexpType(incomparableType)), nAryOp = "IN"),
                            )
                        )
                    )
                }
            }

        @JvmStatic
        @Suppress("unused")
        fun parametersForNAryOpInTests() =
            generateAllUniquePairs(ALL_NUMERIC_TYPES, ALL_NUMERIC_TYPES).flatMap {
                createNAryOpInAllCollectionsTest(
                    leftType = it.first,
                    rightElementType = it.second
                )
            } +
                generateAllUniquePairs(ALL_TEXT_TYPES, ALL_TEXT_TYPES).flatMap {
                    createNAryOpInAllCollectionsTest(
                        leftType = it.first,
                        rightElementType = it.second
                    )
                } +
                generateAllUniquePairs(ALL_LOB_TYPES, ALL_LOB_TYPES).flatMap {
                    createNAryOpInAllCollectionsTest(
                        leftType = it.first,
                        rightElementType = it.second
                    )
                } +
                ALL_TYPES_ONLY_COMPARABLE_TO_SELF.flatMap {
                    createNAryOpInAllCollectionsTest(
                        leftType = it,
                        rightElementType = it
                    )
                } +
                listOf(
                    createNAryOpInTest(
                        name = "STRING IN unionOf(STRING, INT) LIST",
                        leftType = STRING,
                        rightType = ListType(elementType = unionOf(STRING, INT)),
                        outputType = BOOL
                    ),
                    createNAryOpInTest(
                        name = "STRING IN unionOf(STRING, NULL) LIST",
                        leftType = STRING,
                        rightType = ListType(elementType = unionOf(STRING, NULL)),
                        outputType = unionOf(BOOL, NULL)
                    ),
                    createNAryOpInTest(
                        name = "STRING IN unionOf(STRING, MISSING) LIST",
                        leftType = STRING,
                        rightType = ListType(elementType = unionOf(STRING, MISSING)),
                        outputType = unionOf(BOOL, MISSING)
                    ),
                    createNAryOpInTest(
                        name = "STRING IN unionOf(STRING, MISSING, NULL) LIST",
                        leftType = STRING,
                        rightType = ListType(elementType = unionOf(STRING, MISSING, NULL)),
                        outputType = unionOf(BOOL, MISSING, NULL)
                    ),
                    createNAryOpInTest(
                        name = "STRING IN ANY LIST",
                        leftType = STRING,
                        rightType = LIST,
                        outputType = unionOf(BOOL, MISSING, NULL)
                    ),
                    createNAryOpInTest(
                        name = "STRING IN ANY SEXP",
                        leftType = STRING,
                        rightType = SEXP,
                        outputType = unionOf(BOOL, MISSING, NULL)
                    ),
                    createNAryOpInTest(
                        name = "STRING IN ANY BAG",
                        leftType = STRING,
                        rightType = BAG,
                        outputType = unionOf(BOOL, MISSING, NULL)
                    ),
                    createNAryOpInTest(
                        name = "ANY IN ANY BAG",
                        leftType = ANY,
                        rightType = BAG,
                        outputType = unionOf(BOOL, MISSING, NULL)
                    ),
                    createNAryOpInTest(
                        name = "ANY IN ANY",
                        leftType = ANY,
                        rightType = ANY,
                        outputType = unionOf(BOOL, MISSING, NULL)
                    ),
                    createNAryOpInTest(
                        name = "ANY IN unionOf(ANY BAG, empty STRUCT)",
                        leftType = ANY,
                        rightType = unionOf(BAG, STRUCT),
                        outputType = unionOf(BOOL, NULL, MISSING)
                    ),
                    createNAryOpInTest(
                        name = "ANY IN unionOf(ANY BAG, ANY LIST)",
                        leftType = ANY,
                        rightType = unionOf(BAG, LIST),
                        outputType = unionOf(BOOL, NULL, MISSING)
                    ),
                    createNAryOpInTest(
                        name = "STRING LIST IN STRING LIST LIST",
                        leftType = ListType(elementType = STRING),
                        rightType = ListType(elementType = ListType(elementType = STRING)),
                        outputType = BOOL
                    ),
                    createNAryOpInTest(
                        name = "STRING LIST IN unionOf(STRING LIST LIST, STRING BAG BAG)",
                        leftType = ListType(elementType = STRING),
                        rightType = unionOf(
                            ListType(elementType = ListType(elementType = STRING)),
                            BagType(elementType = BagType(elementType = STRING))
                        ),
                        outputType = BOOL
                    ),
                    createNAryOpInTest(
                        name = "STRING LIST IN unionOf(STRING LIST LIST, MISSING)",
                        leftType = ListType(elementType = STRING),
                        rightType = unionOf(ListType(elementType = ListType(elementType = STRING)), MISSING),
                        outputType = unionOf(BOOL, MISSING)
                    ),
                    createNAryOpInTest(
                        name = "STRING LIST IN unionOf(STRING LIST LIST, NULL)",
                        leftType = ListType(elementType = STRING),
                        rightType = unionOf(ListType(elementType = ListType(elementType = STRING)), NULL),
                        outputType = unionOf(BOOL, NULL)
                    ),
                    createNAryOpInTest(
                        name = "STRING LIST IN unionOf(STRING LIST LIST, MISSING, NULL)",
                        leftType = ListType(elementType = STRING),
                        rightType = unionOf(ListType(elementType = ListType(elementType = STRING)), MISSING, NULL),
                        outputType = unionOf(BOOL, MISSING, NULL)
                    ),
                    // row-value constructor tests
                    TestCase(
                        name = "NAry op IN - <int> IN (<int>, <null>, <int>)",
                        originalSql = "intT IN (intT, nullT, intT)",
                        globals = mapOf(
                            "intT" to INT,
                            "nullT" to NULL
                        ),
                        handler = expectQueryOutputType(unionOf(BOOL, NULL))
                    ),
                    TestCase(
                        name = "NAry op IN - <int> IN (<int>, <missing>, <int>)",
                        originalSql = "intT IN (intT, missingT, intT)",
                        globals = mapOf(
                            "intT" to INT,
                            "missingT" to MISSING
                        ),
                        handler = expectQueryOutputType(unionOf(BOOL, MISSING))
                    ),
                    TestCase(
                        name = "NAry op IN - <int> IN (<int>, <missing>, <null>)",
                        originalSql = "intT IN (intT, missingT, nullT)",
                        globals = mapOf(
                            "intT" to INT,
                            "missingT" to MISSING,
                            "nullT" to NULL
                        ),
                        handler = expectQueryOutputType(unionOf(BOOL, MISSING, NULL))
                    ),
                    TestCase(
                        name = "NAry op IN - <int> IN (<int>, <nullOrMissing>, <int>)",
                        originalSql = "intT IN (intT, nullOrMissingT, intT)",
                        globals = mapOf(
                            "intT" to INT,
                            "nullOrMissingT" to NULL_OR_MISSING
                        ),
                        handler = expectQueryOutputType(unionOf(BOOL, MISSING, NULL))
                    )
                ) +
                //
                // `IN` cases with an error
                //
                // non-unknown IN non-collection (non-unknown) -> data type mismatch
                ALL_NON_UNKNOWN_TYPES.flatMap { nonUnknown ->
                    ALL_NON_COLLECTION_NON_UNKNOWN_TYPES.map { nonCollection ->
                        createNAryOpInErrorTest(
                            name = "$nonUnknown IN $nonCollection - data type mismatch",
                            leftType = nonUnknown,
                            rightType = nonCollection,
                            expectedProblems = listOf(
                                createDataTypeMismatchError(col = 5, argTypes = listOf(nonUnknown, nonCollection), nAryOp = "IN")
                            )
                        )
                    }
                } +
                // unknown IN non-collection (non-unknown) -> data type mismatch and unknown operand error
                ALL_UNKNOWN_TYPES.flatMap { unknown ->
                    ALL_NON_COLLECTION_NON_UNKNOWN_TYPES.map { nonCollection ->
                        createNAryOpInErrorTest(
                            name = "$unknown IN $nonCollection - data type mismatch, unknown error",
                            leftType = unknown,
                            rightType = nonCollection,
                            expectedProblems = listOf(
                                createDataTypeMismatchError(col = 5, argTypes = listOf(unknown, nonCollection), nAryOp = "IN"),
                                createReturnsNullOrMissingError(col = 5, nAryOp = "IN")
                            )
                        )
                    }
                } +
                // numeric IN collection(non-numeric) -> data type mismatch
                createNAryOpInErrorIncomparableElementTests(ALL_NUMERIC_TYPES, ALL_NON_NUMERIC_NON_UNKNOWN_TYPES) +
                // text IN collection(non-text) -> data type mismatch
                createNAryOpInErrorIncomparableElementTests(ALL_TEXT_TYPES, ALL_NON_TEXT_NON_UNKNOWN_TYPES) +
                // lob IN collection(non-lob) -> data type mismatch
                createNAryOpInErrorIncomparableElementTests(ALL_LOB_TYPES, ALL_NON_LOB_NON_UNKNOWN_TYPES) +
                // type-only-comparable-to-self IN collection(other type) -> data type mismatch
                ALL_TYPES_ONLY_COMPARABLE_TO_SELF.flatMap { type ->
                    createNAryOpInErrorIncomparableElementTests(listOf(type), ALL_NON_UNKNOWN_TYPES.filter { it != type })
                } +
                // unknown IN collection(type) -> unknown operand error
                ALL_UNKNOWN_TYPES.flatMap { unknownType ->
                    ALL_TYPES.flatMap { type ->
                        listOf(
                            createNAryOpInErrorTest(
                                name = "$unknownType IN list($type) - unknown error",
                                leftType = unknownType,
                                rightType = ListType(elementType = type),
                                expectedProblems = listOf(
                                    createReturnsNullOrMissingError(col = 5, nAryOp = "IN")
                                )
                            ),
                            createNAryOpInErrorTest(
                                name = "$unknownType IN bag($type) - unknown error",
                                leftType = unknownType,
                                rightType = BagType(elementType = type),
                                expectedProblems = listOf(
                                    createReturnsNullOrMissingError(col = 5, nAryOp = "IN")
                                )
                            ),
                            createNAryOpInErrorTest(
                                name = "$unknownType IN sexp($type) - unknown error",
                                leftType = unknownType,
                                rightType = SexpType(elementType = type),
                                expectedProblems = listOf(
                                    createReturnsNullOrMissingError(col = 5, nAryOp = "IN")
                                )
                            )
                        )
                    }
                } +
                // type IN unknown -> unknown operand error
                ALL_TYPES.flatMap { type ->
                    ALL_UNKNOWN_TYPES.map { unknownType ->
                        createNAryOpInErrorTest(
                            name = "$type IN $unknownType - unknown error",
                            leftType = type,
                            rightType = unknownType,
                            expectedProblems = listOf(
                                createReturnsNullOrMissingError(col = 5, nAryOp = "IN")
                            )
                        )
                    }
                } +
                // other tests resulting in an error
                listOf(
                    createNAryOpInErrorTest(
                        name = "ANY IN INT - data type mismatch",
                        leftType = ANY,
                        rightType = INT,
                        expectedProblems = listOf(
                            createDataTypeMismatchError(col = 5, argTypes = listOf(ANY, INT), nAryOp = "IN")
                        )
                    ),
                    createNAryOpInErrorTest(
                        name = "ANY IN unionOf(INT, empty struct) - data type mismatch",
                        leftType = ANY,
                        rightType = unionOf(INT, STRUCT),
                        expectedProblems = listOf(
                            createDataTypeMismatchError(col = 5, argTypes = listOf(ANY, unionOf(INT, STRUCT)), nAryOp = "IN")
                        )
                    ),
                    createNAryOpInErrorTest(
                        name = "ANY IN NULL - unknown error",
                        leftType = ANY,
                        rightType = NULL,
                        expectedProblems = listOf(
                            createReturnsNullOrMissingError(col = 5, nAryOp = "IN")
                        )
                    )
                )

        /**
         * Creates a test of the form: NULLIF([leftType], [rightType]) and expects an output type of [leftType] with
         * [StaticType.NULL].
         */
        private fun createValidNullIfTest(
            leftType: StaticType,
            rightType: StaticType
        ) =
            TestCase(
                name = "NULLIF($leftType, $rightType)",
                originalSql = "NULLIF(lhs, rhs)",
                globals = mapOf(
                    "lhs" to leftType,
                    "rhs" to rightType
                ),
                handler = expectQueryOutputType(leftType.asNullable())
            )

        /**
         * Creates a test of the form: NULLIF([leftType], [rightType]) and expects [expectedProblems] during inference.
         */
        private fun createErrorNullIfTest(
            name: String,
            leftType: StaticType,
            rightType: StaticType,
            expectedProblems: List<Problem>
        ) =
            TestCase(
                name = name,
                originalSql = "NULLIF(lhs, rhs)",
                globals = mapOf(
                    "lhs" to leftType,
                    "rhs" to rightType
                ),
                handler = expectSemanticProblems(expectedProblems)
            )

        @JvmStatic
        @Suppress("unused")
        fun parametersForNullIfTests() =
            // NULLIF(<numeric>, <numeric>)
            generateAllUniquePairs(ALL_NUMERIC_TYPES, ALL_NUMERIC_TYPES).map {
                createValidNullIfTest(
                    leftType = it.first,
                    rightType = it.second
                )
            } +
                // NULLIF(<text>, <text>)
                generateAllUniquePairs(ALL_TEXT_TYPES, ALL_TEXT_TYPES).map {
                    createValidNullIfTest(
                        leftType = it.first,
                        rightType = it.second
                    )
                } +
                // NULLIF(<lob>, <lob>)
                generateAllUniquePairs(ALL_LOB_TYPES, ALL_LOB_TYPES).map {
                    createValidNullIfTest(
                        leftType = it.first,
                        rightType = it.second
                    )
                } +
                // `NULLIF` with types only comparable to self
                ALL_TYPES_ONLY_COMPARABLE_TO_SELF.map {
                    createValidNullIfTest(
                        leftType = it,
                        rightType = it
                    )
                } +
                // other valid `NULLIF` tests
                listOf(
                    createValidNullIfTest(
                        leftType = ANY,
                        rightType = STRING
                    ),
                    createValidNullIfTest(
                        leftType = unionOf(STRING, INT),
                        rightType = STRING
                    ),
                    createValidNullIfTest(
                        leftType = unionOf(STRING, INT),
                        rightType = unionOf(INT8, FLOAT, SYMBOL)
                    ),
                    createValidNullIfTest(
                        leftType = INT.asNullable(),
                        rightType = INT.asOptional()
                    ),
                    createValidNullIfTest(
                        leftType = INT.asNullable(),
                        rightType = FLOAT.asOptional()
                    )
                ) +
                //
                // `NULLIF` error cases below
                //

                // NULLIF with a numeric and non-numeric, non-unknown -> data type mismatch
                ALL_NUMERIC_TYPES.flatMap { numericType ->
                    ALL_NON_NUMERIC_NON_UNKNOWN_TYPES.map { nonNumericType ->
                        createErrorNullIfTest(
                            name = "data type mismatch - NULLIF($numericType, $nonNumericType)",
                            leftType = numericType,
                            rightType = nonNumericType,
                            expectedProblems = listOf(
                                createDataTypeMismatchError(col = 1, argTypes = listOf(numericType, nonNumericType), nAryOp = "NULLIF")
                            )
                        )
                    }
                } +
                // NULLIF with a text and non-text, non-unknown -> data type mismatch
                ALL_TEXT_TYPES.flatMap { textType ->
                    ALL_NON_TEXT_NON_UNKNOWN_TYPES.map { nonTextType ->
                        createErrorNullIfTest(
                            name = "data type mismatch - NULLIF($textType, $nonTextType)",
                            leftType = textType,
                            rightType = nonTextType,
                            expectedProblems = listOf(
                                createDataTypeMismatchError(col = 1, argTypes = listOf(textType, nonTextType), nAryOp = "NULLIF")
                            )
                        )
                    }
                } +
                // NULLIF with a lob and non-lob, non-unknown -> data type mismatch
                ALL_LOB_TYPES.flatMap { lobType ->
                    ALL_NON_LOB_NON_UNKNOWN_TYPES.map { nonLobType ->
                        createErrorNullIfTest(
                            name = "data type mismatch - NULLIF($lobType, $nonLobType)",
                            leftType = lobType,
                            rightType = nonLobType,
                            expectedProblems = listOf(
                                createDataTypeMismatchError(col = 1, argTypes = listOf(lobType, nonLobType), nAryOp = "NULLIF")
                            )
                        )
                    }
                } +
                // NULLIF with a type only comparable to itself and other non-unknown type -> data type mismatch
                ALL_TYPES_ONLY_COMPARABLE_TO_SELF.flatMap { type ->
                    ALL_NON_UNKNOWN_TYPES.filter { it != type }.map { incomparableToType ->
                        createErrorNullIfTest(
                            name = "data type mismatch - NULLIF($type, $incomparableToType)",
                            leftType = type,
                            rightType = incomparableToType,
                            expectedProblems = listOf(
                                createDataTypeMismatchError(col = 1, argTypes = listOf(type, incomparableToType), nAryOp = "NULLIF")
                            )
                        )
                    }
                } +
                // NULLIF with a type and unknown -> null or missing error
                generateAllUniquePairs(ALL_TYPES, ALL_UNKNOWN_TYPES).map {
                    createErrorNullIfTest(
                        name = "null or missing error - ${it.first}, ${it.second}",
                        leftType = it.first,
                        rightType = it.second,
                        expectedProblems = listOf(createReturnsNullOrMissingError(col = 1, nAryOp = "NULLIF"))
                    )
                } +
                // other miscellaneous error tests
                listOf(
                    createErrorNullIfTest(
                        name = "data type mismatch - NULLIF(nullable int, string)",
                        leftType = INT.asNullable(),
                        rightType = STRING,
                        expectedProblems = listOf(
                            createDataTypeMismatchError(col = 1, argTypes = listOf(INT.asNullable(), STRING), nAryOp = "NULLIF")
                        )
                    ),
                    createErrorNullIfTest(
                        name = "data type mismatch - NULLIF(optional int, string)",
                        leftType = INT.asOptional(),
                        rightType = STRING,
                        expectedProblems = listOf(
                            createDataTypeMismatchError(col = 1, argTypes = listOf(INT.asOptional(), STRING), nAryOp = "NULLIF")
                        )
                    ),
                    createErrorNullIfTest(
                        name = "data type mismatch - NULLIF(nullable int, nullable string)",
                        leftType = INT.asNullable(),
                        rightType = STRING.asNullable(),
                        expectedProblems = listOf(
                            createDataTypeMismatchError(col = 1, argTypes = listOf(INT.asNullable(), STRING.asNullable()), nAryOp = "NULLIF")
                        )
                    ),
                    createErrorNullIfTest(
                        name = "data type mismatch - NULLIF(union(string, int), bool)",
                        leftType = unionOf(STRING, INT),
                        rightType = BOOL,
                        expectedProblems = listOf(
                            createDataTypeMismatchError(col = 1, argTypes = listOf(unionOf(STRING, INT), BOOL), nAryOp = "NULLIF")
                        )
                    ),
                    createErrorNullIfTest(
                        name = "data type mismatch - NULLIF(union(string, int), union(bag, list))",
                        leftType = unionOf(STRING, INT),
                        rightType = unionOf(BAG, LIST),
                        expectedProblems = listOf(
                            createDataTypeMismatchError(col = 1, argTypes = listOf(unionOf(STRING, INT), unionOf(BAG, LIST)), nAryOp = "NULLIF")
                        )
                    ),
                    createErrorNullIfTest(
                        name = "null or missing error - NULLIF(missing, optional int)",
                        leftType = MISSING,
                        rightType = INT.asOptional(),
                        expectedProblems = listOf(
                            createReturnsNullOrMissingError(col = 1, nAryOp = "NULLIF")
                        )
                    ),
                    createErrorNullIfTest(
                        name = "null or missing error - NULLIF(any, null or missing)",
                        leftType = ANY,
                        rightType = NULL_OR_MISSING,
                        expectedProblems = listOf(
                            createReturnsNullOrMissingError(col = 1, nAryOp = "NULLIF")
                        )
                    )
                )

        @JvmStatic
        @Suppress("unused")
        fun parametersForStructTests() = listOf(
            TestCase(
                "struct -- no fields",
                "{ }",
                handler = expectQueryOutputType(StructType(emptyMap(), contentClosed = true))
            ),
            TestCase(
                "struct -- text literal keys",
                "{ 'a': 1, 'b': true, `c`: 'foo' }",
                handler = expectQueryOutputType(
                    StructType(
                        mapOf(
                            "a" to INT,
                            "b" to BOOL,
                            "c" to STRING
                        ),
                        contentClosed = true
                    )
                )
            ),
            // TODO: non-text keys should be an error condition, see: https://github.com/partiql/partiql-lang-kotlin/issues/496
            TestCase(
                "struct -- string and int literal keys",
                "{ 'a': 1, 2: 2 }",
                handler = expectQueryOutputType(StructType(mapOf("a" to INT), contentClosed = true))
            ),
            // TODO: non-text keys should be an error condition, see: https://github.com/partiql/partiql-lang-kotlin/issues/496
            TestCase(
                "struct -- symbol and int literal keys",
                "{ `a`: 1, 2: 2 }",
                handler = expectQueryOutputType(StructType(mapOf("a" to INT), contentClosed = true))
            ),
            // TODO: non-text keys should be an error condition, see: https://github.com/partiql/partiql-lang-kotlin/issues/496
            TestCase(
                "struct -- null literal key",
                "{ null: 1 }",
                handler = expectQueryOutputType(StructType(emptyMap(), contentClosed = true))
            ),
            // TODO: non-text keys should be an error condition, see: https://github.com/partiql/partiql-lang-kotlin/issues/496
            TestCase(
                "struct -- missing key",
                "{ missing: 1 }",
                handler = expectQueryOutputType(StructType(emptyMap(), contentClosed = true))
            ),
            // TODO: non-text keys should be an error condition, see: https://github.com/partiql/partiql-lang-kotlin/issues/496
            TestCase(
                "struct -- multiple non-text keys",
                "{ 1: 1, null: 2, missing: 3, true: 4, {}: 5 }",
                handler = expectQueryOutputType(StructType(emptyMap(), contentClosed = true))
            ),
            TestCase(
                "struct -- string, non-literal key",
                "{ foo: 1 }",
                mapOf("foo" to STRING),
                handler = expectQueryOutputType(StructType(emptyMap(), contentClosed = false))
            ),
            TestCase(
                "struct -- symbol, non-literal key",
                "{ foo: 1 }",
                mapOf("foo" to SYMBOL),
                handler = expectQueryOutputType(StructType(emptyMap(), contentClosed = false))
            ),
            TestCase(
                "struct -- symbol, non-literal key and text literal key",
                "{ foo: 1, 'b': 123 }",
                mapOf("foo" to SYMBOL),
                handler = expectQueryOutputType(StructType(mapOf("b" to INT), contentClosed = false))
            ),
            // TODO: non-text keys should be an error condition, see: https://github.com/partiql/partiql-lang-kotlin/issues/496
            TestCase(
                "struct -- non-text, non-literal key",
                "{ foo: 1 }",
                mapOf("foo" to BOOL),
                handler = expectQueryOutputType(StructType(emptyMap(), contentClosed = true))
            ),
            // TODO: non-text keys should be an error condition, see: https://github.com/partiql/partiql-lang-kotlin/issues/496
            TestCase(
                "struct -- non-text, non-literal key and text literal key",
                "{ foo: 1, 'b': 123 }",
                mapOf("foo" to BOOL),
                handler = expectQueryOutputType(StructType(mapOf("b" to INT), contentClosed = true))
            ),
            TestCase(
                "struct -- nullable string, non-literal key",
                "{ foo: 1 }",
                mapOf("foo" to STRING.asNullable()),
                handler = expectQueryOutputType(StructType(emptyMap(), contentClosed = false))
            ),
            // TODO: non-text keys should be an error condition, see: https://github.com/partiql/partiql-lang-kotlin/issues/496
            TestCase(
                "struct -- nullable non-text, non-literal key",
                "{ foo: 1 }",
                mapOf("foo" to BOOL.asNullable()),
                handler = expectQueryOutputType(StructType(emptyMap(), contentClosed = true))
            ),
            TestCase(
                "struct -- union of text types non-literal key",
                "{ foo: 1 }",
                mapOf("foo" to unionOf(STRING, SYMBOL)),
                handler = expectQueryOutputType(StructType(emptyMap(), contentClosed = false))
            ),
            // TODO: non-text keys should be an error condition, see: https://github.com/partiql/partiql-lang-kotlin/issues/496
            TestCase(
                "struct -- union of non-text types non-literal key",
                "{ foo: 1 }",
                mapOf("foo" to unionOf(BOOL, INT)),
                handler = expectQueryOutputType(StructType(emptyMap(), contentClosed = true))
            ),
            TestCase(
                "struct -- ANY type non-literal key",
                "{ foo: 1 }",
                mapOf("foo" to ANY),
                handler = expectQueryOutputType(StructType(emptyMap(), contentClosed = false))
            ),
            // TODO: non-text keys should be an error condition, see: https://github.com/partiql/partiql-lang-kotlin/issues/496
            TestCase(
                "nested struct -- literal, non-text inner key",
                "{ 'a': 1, 'nestedStruct': { 2: 2, 'validKey': 42 } }",
                handler = expectQueryOutputType(
                    StructType(
                        mapOf(
                            "a" to INT,
                            "nestedStruct" to StructType(mapOf("validKey" to INT), contentClosed = true)
                        ),
                        contentClosed = true
                    )
                )
            ),
            TestCase(
                "nested struct -- non-literal, text inner key",
                "{ 'a': 1, 'nestedStruct': { nonLiteralTextKey: 2, 'validKey': 42 } }",
                mapOf("nonLiteralTextKey" to STRING),
                handler = expectQueryOutputType(
                    StructType(
                        mapOf(
                            "a" to INT,
                            "nestedStruct" to StructType(mapOf("validKey" to INT), contentClosed = false)
                        ),
                        contentClosed = true
                    )
                )
            ),
            TestCase(
                "nested struct -- non-literal, text outer key",
                "{ 'a': 1, nonLiteralTextKey: { 'b': 2, 'validKey': 42 } }",
                mapOf("nonLiteralTextKey" to STRING),
                handler = expectQueryOutputType(StructType(mapOf("a" to INT), contentClosed = false))
            ),
            // TODO: non-text keys should be an error condition, see: https://github.com/partiql/partiql-lang-kotlin/issues/496
            TestCase(
                "nested struct -- non-literal, non-text outer key",
                "{ 'a': 1, nonLiteralNonTextKey: { 'b': 2, 'validKey': 42 } }",
                mapOf("nonLiteralNonTextKey" to BOOL),
                handler = expectQueryOutputType(StructType(mapOf("a" to INT), contentClosed = true))
            )
        )

        @JvmStatic
        @Suppress("unused")
        fun parametersForTypedExpressionTests() = listOf(
            TestCase(
                name = "CAST to a type that doesn't expect any parameters",
                originalSql = "CAST(an_int AS INT)",
                globals = mapOf("an_int" to IntType(IntType.IntRangeConstraint.LONG)),
                handler = expectQueryOutputType(IntType(IntType.IntRangeConstraint.LONG))
            ),
            TestCase(
                name = "CAST to SMALLINT",
                originalSql = "CAST(an_int AS SMALLINT)",
                globals = mapOf("an_int" to INT4),
                handler = expectQueryOutputType(
                    unionOf(
                        MISSING,
                        IntType(IntType.IntRangeConstraint.SHORT)
                    )
                )
            ),
            TestCase(
                name = "CAST to VARCHAR",
                originalSql = "CAST(a_string AS VARCHAR)",
                globals = mapOf("a_string" to STRING),
                handler = expectQueryOutputType(StringType(StringType.StringLengthConstraint.Unconstrained))
            ),
            TestCase(
                name = "CAST to VARCHAR(x)",
                originalSql = "CAST(a_string AS VARCHAR(10))",
                globals = mapOf(
                    "a_string" to StringType(
                        StringType.StringLengthConstraint.Constrained(
                            NumberConstraint.UpTo(10)
                        )
                    )
                ),
                handler = expectQueryOutputType(
                    StringType(
                        StringType.StringLengthConstraint.Constrained(
                            NumberConstraint.UpTo(10)
                        )
                    )
                )
            ),
            TestCase(
                name = "CAST to CHAR",
                originalSql = "CAST(a_string AS CHAR)",
                globals = mapOf(
                    "a_string" to StringType(
                        StringType.StringLengthConstraint.Constrained(
                            NumberConstraint.Equals(1)
                        )
                    )
                ),
                handler = expectQueryOutputType(
                    StringType(
                        StringType.StringLengthConstraint.Constrained(
                            NumberConstraint.Equals(1)
                        )
                    )
                )
            ),
            TestCase(
                name = "CAST to CHAR(x)",
                originalSql = "CAST(a_string AS CHAR(10))",
                globals = mapOf(
                    "a_string" to StringType(
                        StringType.StringLengthConstraint.Constrained(
                            NumberConstraint.Equals(10)
                        )
                    )
                ),
                handler = expectQueryOutputType(
                    StringType(
                        StringType.StringLengthConstraint.Constrained(
                            NumberConstraint.Equals(10)
                        )
                    )
                )
            ),
            TestCase(
                name = "CAST to DECIMAL",
                originalSql = "CAST(an_int AS DECIMAL)",
                globals = mapOf("an_int" to INT4),
                handler = expectQueryOutputType(DECIMAL)
            ),
            TestCase(
                name = "CAST to DECIMAL with precision",
                originalSql = "CAST(a_decimal AS DECIMAL(10))",
                globals = mapOf("a_decimal" to DECIMAL),
                handler = expectQueryOutputType(
                    unionOf(
                        MISSING,
                        DecimalType(DecimalType.PrecisionScaleConstraint.Constrained(10))
                    )
                )
            ),
            TestCase(
                name = "CAST to DECIMAL with precision and scale",
                originalSql = "CAST(a_decimal AS DECIMAL(10,2))",
                globals = mapOf("a_decimal" to DECIMAL),
                handler = expectQueryOutputType(
                    unionOf(
                        MISSING,
                        DecimalType(DecimalType.PrecisionScaleConstraint.Constrained(10, 2))
                    )
                )
            ),
            TestCase(
                name = "CAST to NUMERIC with precision and scale",
                originalSql = "CAST(a_decimal AS NUMERIC(10,2))",
                globals = mapOf("a_decimal" to DECIMAL),
                handler = expectQueryOutputType(
                    unionOf(
                        MISSING,
                        DecimalType(DecimalType.PrecisionScaleConstraint.Constrained(10, 2))
                    )
                )
            ),
            TestCase(
                name = "IS operator",
                originalSql = "true IS BOOL",
                handler = expectQueryOutputType(BOOL)
            ),
            TestCase(
                name = "CAST with a custom type without validation thunk",
                originalSql = "CAST(an_int AS ES_INTEGER)",
                globals = mapOf("an_int" to IntType(IntType.IntRangeConstraint.LONG)),
                handler = expectQueryOutputType(customTypedOpParameters["es_integer"]!!.staticType)
            ),
            TestCase(
                name = "CAST with a custom type with validation thunk",
                originalSql = "CAST(an_int AS ES_FLOAT)",
                globals = mapOf("an_int" to IntType(IntType.IntRangeConstraint.LONG)),
                handler = expectQueryOutputType(
                    unionOf(customTypedOpParameters["es_float"]!!.staticType, MISSING)
                )
            ),
            TestCase(
                name = "can_lossless_cast int as decimal",
                originalSql = "CAN_LOSSLESS_CAST(an_int AS DECIMAL)",
                globals = mapOf("an_int" to INT),
                handler = expectQueryOutputType(BOOL)
            ),
            TestCase(
                name = "can_lossless_cast int as decimal with precision",
                originalSql = "CAN_LOSSLESS_CAST(an_int AS DECIMAL(5))",
                globals = mapOf("an_int" to INT),
                handler = expectQueryOutputType(BOOL)
            ),
            TestCase(
                name = "can_lossless_cast int as decimal with precision and scale",
                originalSql = "CAN_LOSSLESS_CAST(an_int AS DECIMAL(5,2))",
                globals = mapOf("an_int" to INT),
                handler = expectQueryOutputType(BOOL)
            ),
            TestCase(
                name = "can_lossless_cast decimal as int",
                originalSql = "CAN_LOSSLESS_CAST(a_decimal AS INT)",
                globals = mapOf("a_decimal" to DECIMAL),
                handler = expectQueryOutputType(BOOL)
            ),
            TestCase(
                name = "can_lossless_cast int as custom type",
                originalSql = "CAN_LOSSLESS_CAST(an_int AS ES_integer)",
                globals = mapOf("an_int" to INT),
                handler = expectQueryOutputType(BOOL)
            ),
            TestCase(
                name = "can_lossless_cast union of types as non-unknown type",
                originalSql = "CAN_LOSSLESS_CAST(int_or_decimal AS INT)",
                globals = mapOf("int_or_decimal" to unionOf(INT, DECIMAL)),
                handler = expectQueryOutputType(BOOL)
            ),
            TestCase(
                name = "can_lossless_cast union of types as non-unknown custom type",
                originalSql = "CAN_LOSSLESS_CAST(int_or_decimal AS ES_integer)",
                globals = mapOf("int_or_decimal" to unionOf(INT, DECIMAL)),
                handler = expectQueryOutputType(BOOL)
            )
        )

        @JvmStatic
        @Suppress("unused")
        fun parametersForTests() = listOf(
            TestCase(
                name = "multiple errors - arithmetic datatype mismatches",
                originalSql = "(a + b) + (c - d)",
                globals = mapOf(
                    "a" to STRING,
                    "b" to INT,
                    "c" to SYMBOL,
                    "d" to FLOAT
                ),
                handler = expectSemanticProblems(
                    expectedProblems = listOf(
                        createDataTypeMismatchError(col = 4, argTypes = listOf(STRING, INT), nAryOp = "+"),
                        createDataTypeMismatchError(col = 14, argTypes = listOf(SYMBOL, FLOAT), nAryOp = "-")
                    )
                )
            ),
            TestCase(
                name = "inference error - multiple arithmetic ops always resulting in unknown",
                originalSql = "(a + b) + (c - d)",
                globals = mapOf(
                    "a" to NULL,
                    "b" to INT,
                    "c" to MISSING,
                    "d" to FLOAT
                ),
                handler = expectSemanticProblems(
                    expectedProblems = listOf(
                        createReturnsNullOrMissingError(col = 4, nAryOp = "+"),
                        createReturnsNullOrMissingError(col = 14, nAryOp = "-")
                    )
                )
            ),
            TestCase(
                name = "multiple errors - too few args in function call and arithmetic datatype mismatch",
                originalSql = "size() + a",
                globals = mapOf("a" to STRING),
                handler = expectSemanticProblems(
                    expectedProblems = listOf(
                        Problem(
                            SourceLocationMeta(1L, 1L, 4L),
                            SemanticProblemDetails.IncorrectNumberOfArgumentsToFunctionCall(
                                functionName = "size",
                                expectedArity = 1..1,
                                actualArity = 0
                            )
                        ),
                        createDataTypeMismatchError(col = 8, argTypes = listOf(INT, STRING), nAryOp = "+")
                    )
                )
            ),
            TestCase(
                name = "multiple errors - too many args in function call and arithmetic datatype mismatch",
                originalSql = "size(l, extra_arg) + a",
                globals = mapOf(
                    "a" to STRING,
                    "l" to LIST,
                    "extra_arg" to LIST
                ),
                handler = expectSemanticProblems(
                    expectedProblems = listOf(
                        Problem(
                            SourceLocationMeta(1L, 1L, 4L),
                            SemanticProblemDetails.IncorrectNumberOfArgumentsToFunctionCall(
                                functionName = "size",
                                expectedArity = 1..1,
                                actualArity = 2
                            )
                        ),
                        createDataTypeMismatchError(col = 20, argTypes = listOf(INT, STRING), nAryOp = "+")
                    )
                )
            )
        )

        /**
         * Creates four tests expecting an error and expecting [expectedContinuationType] as the output type. This is
         * useful for checking the continuation type after an operation errors.
         *
         * Created tests are of the form:
         *   - good [op] bad <- data type mismatch error
         *   - bad [op] good <- data type mismatch error
         *   - good [op] null <- null or missing error
         *   - null [op] good <- null or missing error
         */
        private fun createBinaryOpContinuationTypeTest(
            goodType: StaticType,
            badType: StaticType,
            expectedContinuationType: StaticType,
            op: String
        ) = listOf(
            TestCase(
                name = "data type mismatch error: $goodType $op $badType -> $expectedContinuationType",
                originalSql = "goodT $op badT",
                globals = mapOf(
                    "goodT" to goodType,
                    "badT" to badType
                ),
                handler = expectQueryOutputTypeAndProblems(
                    expectedType = expectedContinuationType,
                    expectedProblems = listOf(
                        createDataTypeMismatchError(col = 7, argTypes = listOf(goodType, badType), nAryOp = op)
                    )
                )
            ),
            TestCase(
                name = "data type mismatch error: $badType $op $goodType -> $expectedContinuationType",
                originalSql = "badT $op goodT",
                globals = mapOf(
                    "goodT" to goodType,
                    "badT" to badType
                ),
                handler = expectQueryOutputTypeAndProblems(
                    expectedType = expectedContinuationType,
                    expectedProblems = listOf(
                        createDataTypeMismatchError(col = 6, argTypes = listOf(badType, goodType), nAryOp = op)
                    )
                )
            ),
            TestCase(
                name = "null or missing error: $goodType $op null -> $expectedContinuationType",
                originalSql = "goodT $op badT",
                globals = mapOf(
                    "goodT" to goodType,
                    "badT" to NULL
                ),
                handler = expectQueryOutputTypeAndProblems(
                    expectedType = expectedContinuationType,
                    expectedProblems = listOf(
                        createReturnsNullOrMissingError(col = 7, nAryOp = op)
                    )
                )
            ),
            TestCase(
                name = "null or missing error: null $op $goodType -> $expectedContinuationType",
                originalSql = "badT $op goodT",
                globals = mapOf(
                    "goodT" to NULL,
                    "badT" to goodType
                ),
                handler = expectQueryOutputTypeAndProblems(
                    expectedType = expectedContinuationType,
                    expectedProblems = listOf(
                        createReturnsNullOrMissingError(col = 6, nAryOp = op)
                    )
                )
            )
        )

        /**
         * Creates four tests with a single data type mismatch error to check that the single error doesn't cause other
         * errors in the expression. [op] is required to be a left-associative, binary operation. Created expressions
         * are of the form:
         *   - bad [op] good [op] good [op] good <- error at 1st [op]
         *   - good [op] bad [op] good [op] good <- error at 1st [op]
         *   - good [op] good [op] bad [op] good <- error at 2nd [op]
         *   - good [op] good [op] good [op] bad <- error at 3rd [op]
         */
        private fun createChainedOpSingleErrorTests(
            goodType: StaticType,
            badType: StaticType,
            op: String
        ): List<TestCase> {
            val globals = mapOf(
                "goodT" to goodType,
                "badT" to badType
            )
            return listOf(
                TestCase(
                    name = "single data type mismatch error: $badType $op $goodType $op $goodType $op $goodType",
                    originalSql = "badT \n$op goodT \n$op goodT \n$op goodT",
                    globals = globals,
                    handler = expectSemanticProblems(
                        expectedProblems = listOf(
                            createDataTypeMismatchError(line = 2, col = 1, argTypes = listOf(badType, goodType), nAryOp = op)
                        )
                    )
                ),
                TestCase(
                    name = "single data type mismatch error: $goodType $op $badType $goodType $op $goodType $op",
                    originalSql = "goodT \n$op badT \n$op goodT \n$op goodT",
                    globals = globals,
                    handler = expectSemanticProblems(
                        expectedProblems = listOf(
                            createDataTypeMismatchError(line = 2, col = 1, argTypes = listOf(goodType, badType), nAryOp = op)
                        )
                    )
                ),
                TestCase(
                    name = "single data type mismatch error: $goodType $op $goodType $op $badType $op $goodType",
                    originalSql = "goodT \n$op goodT \n$op badT \n$op goodT",
                    globals = globals,
                    handler = expectSemanticProblems(
                        expectedProblems = listOf(
                            createDataTypeMismatchError(line = 3, col = 1, argTypes = listOf(goodType, badType), nAryOp = op)
                        )
                    )
                ),
                TestCase(
                    name = "single data type mismatch error: $goodType $op $goodType $op $goodType $op $badType",
                    originalSql = "goodT \n$op goodT \n$op goodT \n$op badT",
                    globals = globals,
                    handler = expectSemanticProblems(
                        expectedProblems = listOf(
                            createDataTypeMismatchError(line = 4, col = 1, argTypes = listOf(goodType, badType), nAryOp = op)
                        )
                    )
                )
            )
        }

        @JvmStatic
        @Suppress("unused")
        fun parametersForContinuationTypeTests() =
            // arithmetic ops will return a union of all numeric types in the event of an error
            OpType.ARITHMETIC.operators.flatMap { arithmeticOp ->
                createBinaryOpContinuationTypeTest(
                    goodType = INT,
                    badType = STRING,
                    expectedContinuationType = unionOf(ALL_NUMERIC_TYPES.toSet()),
                    op = arithmeticOp
                )
            } +
                // concat will return string in the event of an error
                createBinaryOpContinuationTypeTest(
                    goodType = STRING,
                    badType = INT,
                    expectedContinuationType = STRING,
                    op = "||"
                ) +
                // LIKE will return bool in the event of an error
                createBinaryOpContinuationTypeTest(
                    goodType = STRING,
                    badType = INT,
                    expectedContinuationType = BOOL,
                    op = "LIKE"
                ) +
                // logical ops will return bool in the event of an error
                OpType.LOGICAL.operators.flatMap { logicalOp ->
                    createBinaryOpContinuationTypeTest(
                        goodType = BOOL,
                        badType = STRING,
                        expectedContinuationType = BOOL,
                        op = logicalOp
                    )
                } +
                // comparison ops will return bool in the event of an error
                OpType.COMPARISON.operators.flatMap { logicalOp ->
                    createBinaryOpContinuationTypeTest(
                        goodType = INT,
                        badType = STRING,
                        expectedContinuationType = BOOL,
                        op = logicalOp
                    )
                } +
                // equality ops will return bool in the event of an error
                OpType.EQUALITY.operators.flatMap { logicalOp ->
                    createBinaryOpContinuationTypeTest(
                        goodType = INT,
                        badType = STRING,
                        expectedContinuationType = BOOL,
                        op = logicalOp
                    )
                } +
                // unary arithmetic op tests - continuation type of numeric
                listOf("+", "-").flatMap { op ->
                    listOf(
                        TestCase(
                            name = "data type mismatch error: $op string -> union of numerics",
                            originalSql = "$op badT",
                            globals = mapOf("badT" to STRING),
                            handler = expectQueryOutputTypeAndProblems(
                                expectedType = unionOf(ALL_NUMERIC_TYPES.toSet()),
                                expectedProblems = listOf(createDataTypeMismatchError(col = 1, argTypes = listOf(STRING), nAryOp = op))
                            )
                        ),
                        TestCase(
                            name = "null or missing error: $op string -> union of numerics",
                            originalSql = "$op nullT",
                            globals = mapOf("nullT" to NULL),
                            handler = expectQueryOutputTypeAndProblems(
                                expectedType = unionOf(ALL_NUMERIC_TYPES.toSet()),
                                expectedProblems = listOf(createReturnsNullOrMissingError(col = 1, nAryOp = op))
                            )
                        )
                    )
                } +
                // LIKE tests with bad ESCAPE type - continuation type of bool
                listOf(
                    TestCase(
                        name = "data type mismatch error: string LIKE string ESCAPE int -> bool",
                        originalSql = "goodT LIKE goodT ESCAPE badT",
                        globals = mapOf(
                            "goodT" to STRING,
                            "badT" to INT
                        ),
                        handler = expectQueryOutputTypeAndProblems(
                            expectedType = BOOL,
                            expectedProblems = listOf(createDataTypeMismatchError(col = 7, argTypes = listOf(STRING, STRING, INT), nAryOp = "LIKE"))
                        )
                    ),
                    TestCase(
                        name = "null or missing error: string LIKE string ESCAPE null -> bool",
                        originalSql = "goodT LIKE goodT ESCAPE badT",
                        globals = mapOf(
                            "goodT" to STRING,
                            "badT" to NULL
                        ),
                        handler = expectQueryOutputTypeAndProblems(
                            expectedType = BOOL,
                            expectedProblems = listOf(createReturnsNullOrMissingError(col = 7, nAryOp = "LIKE"))
                        )
                    ),
                ) +
                // logical `NOT` with non-bool - continuation type of bool
                listOf(
                    TestCase(
                        name = "data type mismatch error: NOT string -> bool",
                        originalSql = "NOT badT",
                        globals = mapOf("badT" to STRING),
                        handler = expectQueryOutputTypeAndProblems(
                            expectedType = BOOL,
                            expectedProblems = listOf(createDataTypeMismatchError(col = 1, argTypes = listOf(STRING), nAryOp = "NOT"))
                        )
                    ),
                    TestCase(
                        name = "null or missing error: NOT null -> bool",
                        originalSql = "NOT nullT",
                        globals = mapOf("nullT" to NULL),
                        handler = expectQueryOutputTypeAndProblems(
                            expectedType = BOOL,
                            expectedProblems = listOf(createReturnsNullOrMissingError(col = 1, nAryOp = "NOT"))
                        )
                    )
                ) +
                // `BETWEEN` op tests - continuation type of bool
                listOf(
                    TestCase(
                        name = "data type mismatch error: int BETWEEN string AND string",
                        originalSql = "goodT BETWEEN badT AND badT",
                        globals = mapOf(
                            "goodT" to INT,
                            "badT" to STRING
                        ),
                        handler = expectQueryOutputTypeAndProblems(
                            expectedType = BOOL,
                            expectedProblems = listOf(createDataTypeMismatchError(col = 7, argTypes = listOf(INT, STRING, STRING), nAryOp = "BETWEEN"))
                        )
                    ),
                    TestCase(
                        name = "null or missing error: null BETWEEN int AND int",
                        originalSql = "nullT BETWEEN goodT AND goodT",
                        globals = mapOf(
                            "nullT" to NULL,
                            "goodT" to INT
                        ),
                        handler = expectQueryOutputTypeAndProblems(
                            expectedType = BOOL,
                            expectedProblems = listOf(createReturnsNullOrMissingError(col = 7, nAryOp = "BETWEEN"))
                        )
                    )
                ) +
                // `IN` op tests - continuation type of bool
                listOf(
                    TestCase(
                        name = "data type mismatch error: int IN int",
                        originalSql = "lhs IN rhs",
                        globals = mapOf(
                            "lhs" to INT,
                            "rhs" to INT
                        ),
                        handler = expectQueryOutputTypeAndProblems(
                            expectedType = BOOL,
                            expectedProblems = listOf(createDataTypeMismatchError(col = 5, argTypes = listOf(INT, INT), nAryOp = "IN"))
                        )
                    ),
                    TestCase(
                        name = "data type mismatch error (incomparable rhs element type): int IN list(string)",
                        originalSql = "lhs IN rhs",
                        globals = mapOf(
                            "lhs" to INT,
                            "rhs" to ListType(elementType = STRING)
                        ),
                        handler = expectQueryOutputTypeAndProblems(
                            expectedType = BOOL,
                            expectedProblems = listOf(createDataTypeMismatchError(col = 5, argTypes = listOf(INT, ListType(STRING)), nAryOp = "IN"))
                        )
                    ),
                    TestCase(
                        name = "null or missing error: null IN list(string)",
                        originalSql = "nullT IN rhs",
                        globals = mapOf(
                            "nullT" to NULL,
                            "rhs" to ListType(elementType = STRING)
                        ),
                        handler = expectQueryOutputTypeAndProblems(
                            expectedType = BOOL,
                            expectedProblems = listOf(createReturnsNullOrMissingError(col = 7, nAryOp = "IN"))
                        )
                    ),
                    TestCase(
                        name = "null or missing error: int IN null",
                        originalSql = "lhs IN nullT",
                        globals = mapOf(
                            "lhs" to INT,
                            "nullT" to NULL
                        ),
                        handler = expectQueryOutputTypeAndProblems(
                            expectedType = BOOL,
                            expectedProblems = listOf(createReturnsNullOrMissingError(col = 5, nAryOp = "IN"))
                        )
                    )
                ) +
                // `NULLIF` op tests - continuation type of left argument types and null
                listOf(
                    TestCase(
                        name = "data type mismatch error: NULLIF(union(INT, FLOAT), STRING)",
                        originalSql = "NULLIF(lhs, rhs)",
                        globals = mapOf(
                            "lhs" to unionOf(INT, FLOAT),
                            "rhs" to STRING
                        ),
                        handler = expectQueryOutputTypeAndProblems(
                            expectedType = unionOf(INT, FLOAT, NULL),
                            expectedProblems = listOf(createDataTypeMismatchError(col = 1, argTypes = listOf(unionOf(INT, FLOAT), STRING), nAryOp = "NULLIF"))
                        )
                    ),
                    TestCase(
                        name = "null or missing error: NULLIF(union(INT, FLOAT), MISSING)",
                        originalSql = "NULLIF(lhs, rhs)",
                        globals = mapOf(
                            "lhs" to unionOf(INT, FLOAT),
                            "rhs" to MISSING
                        ),
                        handler = expectQueryOutputTypeAndProblems(
                            expectedType = unionOf(INT, FLOAT, NULL),
                            expectedProblems = listOf(createReturnsNullOrMissingError(col = 1, nAryOp = "NULLIF"))
                        )
                    ),
                    TestCase(
                        name = "null or missing error: NULLIF(MISSING, union(INT, FLOAT))",
                        originalSql = "NULLIF(lhs, rhs)",
                        globals = mapOf(
                            "lhs" to MISSING,
                            "rhs" to unionOf(INT, FLOAT)
                        ),
                        handler = expectQueryOutputTypeAndProblems(
                            expectedType = unionOf(MISSING, NULL),
                            expectedProblems = listOf(createReturnsNullOrMissingError(col = 1, nAryOp = "NULLIF"))
                        )
                    )
                ) +
                // SimpleCaseWhen should include all `THEN` expression types in the case of error. If no `ELSE` branch is
                // included, then will also include `NULL` in the output types
                listOf(
                    TestCase(
                        name = "data type mismatch error: CASE <int> WHEN <string> THEN <string> WHEN <symbol> THEN <symbol> END",
                        originalSql = "CASE t_int WHEN t_string THEN t_string WHEN t_symbol THEN t_symbol END",
                        globals = mapOf(
                            "t_int" to INT,
                            "t_string" to STRING,
                            "t_symbol" to SYMBOL
                        ),
                        handler = expectQueryOutputTypeAndProblems(
                            expectedType = unionOf(STRING, SYMBOL, NULL),
                            expectedProblems = listOf(
                                createDataTypeMismatchError(SourceLocationMeta(1L, 17L, 8L), argTypes = listOf(INT, STRING), nAryOp = "CASE"),
                                createDataTypeMismatchError(SourceLocationMeta(1L, 45L, 8L), argTypes = listOf(INT, SYMBOL), nAryOp = "CASE")
                            )
                        )
                    ),
                    TestCase(
                        name = "data type mismatch error with elseExpr: CASE <int> WHEN <string> THEN <string> WHEN <symbol> THEN <symbol> ELSE t_float END",
                        originalSql = "CASE t_int WHEN t_string THEN t_string WHEN t_symbol THEN t_symbol ELSE t_float END",
                        globals = mapOf(
                            "t_int" to INT,
                            "t_string" to STRING,
                            "t_symbol" to SYMBOL,
                            "t_float" to FLOAT
                        ),
                        handler = expectQueryOutputTypeAndProblems(
                            expectedType = unionOf(STRING, SYMBOL, FLOAT),
                            expectedProblems = listOf(
                                createDataTypeMismatchError(SourceLocationMeta(1L, 17L, 8L), argTypes = listOf(INT, STRING), nAryOp = "CASE"),
                                createDataTypeMismatchError(SourceLocationMeta(1L, 45L, 8L), argTypes = listOf(INT, SYMBOL), nAryOp = "CASE")
                            )
                        )
                    ),
                    TestCase(
                        name = "null or missing error (from caseValue): CASE <missing> WHEN <string> THEN <string> WHEN <symbol> THEN <symbol> END",
                        originalSql = "CASE t_missing WHEN t_string THEN t_string WHEN t_symbol THEN t_symbol END",
                        globals = mapOf(
                            "t_missing" to MISSING,
                            "t_string" to STRING,
                            "t_symbol" to SYMBOL
                        ),
                        handler = expectQueryOutputTypeAndProblems(
                            expectedType = unionOf(STRING, SYMBOL, NULL),
                            expectedProblems = listOf(
                                createReturnsNullOrMissingError(SourceLocationMeta(1L, 6L, 9L))
                            )
                        )
                    ),
                    TestCase(
                        name = "data type mismatch and null or missing errors: CASE <int> WHEN <missingT> THEN <string> WHEN <symbol> THEN <symbol> END",
                        originalSql = "CASE t_int WHEN t_missing THEN t_string WHEN t_symbol THEN t_symbol END",
                        globals = mapOf(
                            "t_int" to INT,
                            "t_missing" to MISSING,
                            "t_string" to STRING,
                            "t_symbol" to SYMBOL
                        ),
                        handler = expectQueryOutputTypeAndProblems(
                            expectedType = unionOf(STRING, SYMBOL, NULL),
                            expectedProblems = listOf(
                                createReturnsNullOrMissingError(SourceLocationMeta(1L, 17L, 9L)),
                                createDataTypeMismatchError(SourceLocationMeta(1L, 46L, 8L), argTypes = listOf(INT, SYMBOL), nAryOp = "CASE")
                            )
                        )
                    )
                ) +
                // SearchedCaseWhen should include all `THEN` expression types in the case of error. If no `ELSE` branch is
                // included, then will also include `NULL` in the output types
                listOf(
                    TestCase(
                        name = "data type mismatch error: CASE WHEN <int> THEN <int> WHEN <string> THEN <string> END",
                        originalSql = "CASE WHEN t_int THEN t_int WHEN t_string THEN t_string END",
                        globals = mapOf(
                            "t_int" to INT,
                            "t_string" to STRING
                        ),
                        handler = expectQueryOutputTypeAndProblems(
                            expectedType = unionOf(INT, STRING, NULL),
                            expectedProblems = listOf(
                                createIncompatibleTypesForExprError(SourceLocationMeta(1L, 11L, 5L), expectedType = BOOL, actualType = INT),
                                createIncompatibleTypesForExprError(SourceLocationMeta(1L, 33L, 8L), expectedType = BOOL, actualType = STRING)
                            )
                        )
                    ),
                    TestCase(
                        name = "data type mismatch error with elseExpr: CASE WHEN <int> THEN <int> WHEN <string> THEN <string> ELSE <symbol> END",
                        originalSql = "CASE WHEN t_int THEN t_int WHEN t_string THEN t_string ELSE t_symbol END",
                        globals = mapOf(
                            "t_int" to INT,
                            "t_string" to STRING,
                            "t_symbol" to SYMBOL
                        ),
                        handler = expectQueryOutputTypeAndProblems(
                            expectedType = unionOf(INT, STRING, SYMBOL),
                            expectedProblems = listOf(
                                createIncompatibleTypesForExprError(SourceLocationMeta(1L, 11L, 5L), expectedType = BOOL, actualType = INT),
                                createIncompatibleTypesForExprError(SourceLocationMeta(1L, 33L, 8L), expectedType = BOOL, actualType = STRING)
                            )
                        )
                    ),
                    TestCase(
                        name = "null or missing error: CASE WHEN <null> THEN <null> WHEN <missing> THEN <missing> END",
                        originalSql = "CASE WHEN t_null THEN t_null WHEN t_missing THEN t_missing END",
                        globals = mapOf(
                            "t_null" to NULL,
                            "t_missing" to MISSING
                        ),
                        handler = expectQueryOutputTypeAndProblems(
                            expectedType = unionOf(NULL, MISSING),
                            expectedProblems = listOf(
                                createReturnsNullOrMissingError(SourceLocationMeta(1L, 11L, 6L)),
                                createReturnsNullOrMissingError(SourceLocationMeta(1L, 35L, 9L))
                            )
                        )
                    ),
                    TestCase(
                        name = "data type mismatch and null or missing errors: whenExprs of non-bools and unknown",
                        originalSql = "CASE WHEN t_int THEN t_int WHEN t_string THEN t_string WHEN t_missing THEN t_missing END",
                        globals = mapOf(
                            "t_int" to INT,
                            "t_string" to STRING,
                            "t_missing" to MISSING
                        ),
                        handler = expectQueryOutputTypeAndProblems(
                            expectedType = unionOf(INT, MISSING, STRING, NULL),
                            expectedProblems = listOf(
                                createIncompatibleTypesForExprError(SourceLocationMeta(1L, 11L, 5L), expectedType = BOOL, actualType = INT),
                                createIncompatibleTypesForExprError(SourceLocationMeta(1L, 33L, 8L), expectedType = BOOL, actualType = STRING),
                                createReturnsNullOrMissingError(SourceLocationMeta(1L, 61L, 9L))
                            )
                        )
                    )
                ) +
                // function calls with invalid arguments leading to errors have a continuation type of the function
                // signature's return type
                listOf(
                    TestCase(
                        name = "invalid function call arg: UPPER(INT) -> STRING",
                        originalSql = "UPPER(x)",
                        globals = mapOf("x" to INT),
                        handler = expectQueryOutputTypeAndProblems(
                            expectedType = STRING,
                            expectedProblems = listOf(
                                createInvalidArgumentTypeForFunctionError(
                                    sourceLocation = SourceLocationMeta(1L, 7L, 1L),
                                    functionName = "upper",
                                    expectedArgType = unionOf(STRING, SYMBOL),
                                    actualType = INT
                                )
                            )
                        )
                    ),
                    TestCase(
                        name = "null function call arg: UPPER(NULL) -> STRING",
                        originalSql = "UPPER(x)",
                        globals = mapOf("x" to NULL),
                        handler = expectQueryOutputTypeAndProblems(
                            expectedType = STRING,
                            expectedProblems = listOf(
                                createNullOrMissingFunctionArgumentError(
                                    sourceLocation = SourceLocationMeta(1L, 7L, 1L),
                                    functionName = "upper"
                                )
                            )
                        )
                    ),
                    TestCase(
                        name = "invalid function call arg and null in optional: SUBSTRING(STRING, NULL, BOOL) -> STRING",
                        originalSql = "SUBSTRING('123456789', x, y)",
                        globals = mapOf("x" to BOOL, "y" to NULL),
                        handler = expectQueryOutputTypeAndProblems(
                            expectedType = STRING,
                            expectedProblems = listOf(
                                createInvalidArgumentTypeForFunctionError(
                                    sourceLocation = SourceLocationMeta(1L, 24L, 1L),
                                    functionName = "substring",
                                    expectedArgType = INT,
                                    actualType = BOOL
                                ),
                                createNullOrMissingFunctionArgumentError(
                                    sourceLocation = SourceLocationMeta(1L, 27L, 1L),
                                    functionName = "substring"
                                )
                            )
                        )
                    ),
                    TestCase(
                        name = "invalid function call arg in variadic arg and missing: TRIM(BOTH INT FROM MISSING)",
                        originalSql = "TRIM(BOTH x FROM y)",
                        globals = mapOf("x" to INT, "y" to MISSING),
                        handler = expectQueryOutputTypeAndProblems(
                            expectedType = STRING,
                            expectedProblems = listOf(
                                createInvalidArgumentTypeForFunctionError(
                                    sourceLocation = SourceLocationMeta(1L, 11L, 1L),
                                    functionName = "trim",
                                    expectedArgType = STRING,
                                    actualType = INT
                                ),
                                createNullOrMissingFunctionArgumentError(
                                    sourceLocation = SourceLocationMeta(1L, 18L, 1L),
                                    functionName = "trim"
                                )
                            )
                        )
                    )
                ) +
                // operations that can be chained (i.e. left-associative, binary operation) with a data type mismatch
                // should not lead to multiple errors
                OpType.ARITHMETIC.operators.flatMap { arithmeticOp ->
                    createChainedOpSingleErrorTests(
                        goodType = INT,
                        badType = STRING,
                        op = arithmeticOp
                    )
                } +
                createChainedOpSingleErrorTests(
                    goodType = STRING,
                    badType = INT,
                    op = "||"
                ) +
                OpType.LOGICAL.operators.flatMap { logicalOp ->
                    createChainedOpSingleErrorTests(
                        goodType = BOOL,
                        badType = STRING,
                        op = logicalOp
                    )
                }

        private val JOIN_WITH_PREDICATE = listOf("JOIN", "INNER JOIN", "LEFT JOIN", "RIGHT JOIN")

        /**
         * Creates a simple SFW query with a join predicate for each [JOIN_WITH_PREDICATE] that verifies that no errors
         * come up during inference.
         */
        private fun createJoinPredicateTypeValidTests(predicateType: StaticType): List<TestCase> =
            JOIN_WITH_PREDICATE.map { join ->
                TestCase(
                    name = "$join with predicate type $predicateType",
                    originalSql = "SELECT * FROM a $join b ON c",
                    globals = mapOf(
                        "a" to BagType(StructType(mapOf("foo" to INT))),
                        "b" to BagType(StructType(mapOf("bar" to STRING))),
                        "c" to predicateType
                    ),
                    handler = expectQueryOutputType(BagType(StructType(mapOf("foo" to INT, "bar" to STRING))))
                )
            }

        /**
         * Creates a simple SFW query with a join predicate for each [JOIN_WITH_PREDICATE] that expects
         * [expectedProblems] during inference and checks that the JOIN predicate has a static type of
         * [StaticType.BOOL].
         */
        private fun createJoinPredicateContinuationTypeTests(
            predicateType: StaticType,
            expectedProblems: List<Problem>
        ): List<TestCase> =
            JOIN_WITH_PREDICATE.map { join ->
                TestCase(
                    name = "$join with predicate $predicateType",
                    originalSql = """
                        SELECT * FROM a
                        $join b
                        ON c""",
                    globals = mapOf(
                        "a" to BagType(StructType(mapOf("foo" to INT))),
                        "b" to BagType(StructType(mapOf("bar" to STRING))),
                        "c" to predicateType
                    ),
                    handler = expectProblemsAndAssert(
                        expectedProblems = expectedProblems,
                        assertionBlock = { partiqlAst ->
                            val query = (partiqlAst as PartiqlAst.Statement.Query)
                            val selectExpr = query.expr as PartiqlAst.Expr.Select
                            val joinExpr = selectExpr.from as PartiqlAst.FromSource.Join
                            assertEquals(BOOL, joinExpr.predicate?.metas?.staticType?.type)
                        }
                    )
                )
            }

        @JvmStatic
        @Suppress("unused")
        fun parametersForJoinPredicateTests() =
            // `JOIN` predicates with valid types containing `BOOL`. These tests are meant to just test the `JOIN`
            // predicate inference behavior.
            createJoinPredicateTypeValidTests(predicateType = BOOL) +
                createJoinPredicateTypeValidTests(predicateType = BOOL.asNullable()) +
                createJoinPredicateTypeValidTests(predicateType = BOOL.asOptional()) +
                createJoinPredicateTypeValidTests(predicateType = BOOL.asNullable().asOptional()) +
                createJoinPredicateTypeValidTests(predicateType = unionOf(BOOL, INT, STRING)) +
                //
                // `JOIN` predicates with invalid types below
                //
                // incompatible types for predicate expression -> incompatible types for expression error
                ALL_NON_BOOL_NON_UNKNOWN_TYPES.flatMap { nonBoolType ->
                    createJoinPredicateContinuationTypeTests(
                        predicateType = nonBoolType,
                        expectedProblems = listOf(
                            createIncompatibleTypesForExprError(
                                SourceLocationMeta(4L, 28L, 1L),
                                expectedType = BOOL,
                                actualType = nonBoolType
                            )
                        )
                    )
                } +
                // unknown types for predicate expression -> null or missing error
                ALL_UNKNOWN_TYPES.flatMap { unknownType ->
                    createJoinPredicateContinuationTypeTests(
                        predicateType = unknownType,
                        expectedProblems = listOf(createReturnsNullOrMissingError(SourceLocationMeta(4L, 28L, 1L)))
                    )
                } +
                // other predicate types resulting in an error
                createJoinPredicateContinuationTypeTests(
                    predicateType = INT.asNullable(),
                    expectedProblems = listOf(
                        createIncompatibleTypesForExprError(
                            SourceLocationMeta(4L, 28L, 1L),
                            expectedType = BOOL,
                            actualType = INT.asNullable()
                        )
                    )
                ) +
                createJoinPredicateContinuationTypeTests(
                    predicateType = INT.asOptional(),
                    expectedProblems = listOf(
                        createIncompatibleTypesForExprError(
                            SourceLocationMeta(4L, 28L, 1L),
                            expectedType = BOOL,
                            actualType = INT.asOptional()
                        )
                    )
                ) +
                createJoinPredicateContinuationTypeTests(
                    predicateType = INT.asNullable().asOptional(),
                    expectedProblems = listOf(
                        createIncompatibleTypesForExprError(
                            SourceLocationMeta(4L, 28L, 1L),
                            expectedType = BOOL,
                            actualType = INT.asNullable().asOptional()
                        )
                    )
                ) +
                createJoinPredicateContinuationTypeTests(
                    predicateType = unionOf(INT, FLOAT, STRING),
                    expectedProblems = listOf(
                        createIncompatibleTypesForExprError(
                            SourceLocationMeta(4L, 28L, 1L),
                            expectedType = BOOL,
                            actualType = unionOf(INT, FLOAT, STRING)
                        )
                    )
                )

        /**
         * Creates a simple SFW query with a where expression of type [whereType]. Verifies that no errors are
         * encountered during inference.
         */
        private fun createSelectWhereTypeValidTests(whereType: StaticType): TestCase =
            TestCase(
                name = "SELECT * FROM t WHERE <$whereType>",
                originalSql = "SELECT * FROM t WHERE condition",
                globals = mapOf("t" to BagType(StructType(mapOf("condition" to whereType)))),
                handler = expectQueryOutputType(BagType(StructType(mapOf("condition" to whereType))))
            )

        /**
         * Creates a simple SFW query with a where expression of type [whereType]. Expects [expectedProblems] during
         * inference and checks that the where clause has a static type of [StaticType.BOOL].
         */
        private fun createSelectWhereContinuationTypeTests(
            whereType: StaticType,
            expectedProblems: List<Problem>
        ): TestCase =
            TestCase(
                name = "SELECT * FROM t WHERE <$whereType>",
                originalSql = "SELECT * FROM t WHERE condition",
                globals = mapOf("t" to BagType(StructType(mapOf("condition" to whereType)))),
                handler = expectProblemsAndAssert(
                    expectedProblems = expectedProblems,
                    assertionBlock = { partiqlAst ->
                        val query = partiqlAst as PartiqlAst.Statement.Query
                        val selectExpr = query.expr as PartiqlAst.Expr.Select
                        val whereExpr = selectExpr.where
                        assertEquals(BOOL, whereExpr?.metas?.staticType?.type)
                    }
                )
            )

        @JvmStatic
        @Suppress("unused")
        fun parametersForSelectWhereTests() =
            // `WHERE` expressions with valid types containing `BOOL`. These tests are meant to just test the
            // `WHERE` expression inference behavior.
            listOf(
                createSelectWhereTypeValidTests(whereType = BOOL),
                createSelectWhereTypeValidTests(whereType = BOOL.asNullable()),
                createSelectWhereTypeValidTests(whereType = BOOL.asOptional()),
                createSelectWhereTypeValidTests(whereType = BOOL.asNullable().asOptional()),
                createSelectWhereTypeValidTests(whereType = unionOf(BOOL, INT, STRING))
            ) +
                //
                // `WHERE` expressions with invalid types below
                //
                // incompatible types for where expression -> incompatible types for expression error
                ALL_NON_BOOL_NON_UNKNOWN_TYPES.map { nonBoolType ->
                    createSelectWhereContinuationTypeTests(
                        whereType = nonBoolType,
                        expectedProblems = listOf(
                            createIncompatibleTypesForExprError(
                                SourceLocationMeta(1L, 23L, 9L),
                                expectedType = BOOL,
                                actualType = nonBoolType
                            )
                        )
                    )
                } +
                // unknown types for where expression -> null or missing error
                ALL_UNKNOWN_TYPES.map { unknownType ->
                    createSelectWhereContinuationTypeTests(
                        whereType = unknownType,
                        expectedProblems = listOf(createReturnsNullOrMissingError(SourceLocationMeta(1L, 23L, 9L)))
                    )
                } +
                listOf(
                    // other where expression types resulting in an error
                    createSelectWhereContinuationTypeTests(
                        whereType = INT.asNullable(),
                        expectedProblems = listOf(
                            createIncompatibleTypesForExprError(
                                SourceLocationMeta(1L, 23L, 9L),
                                expectedType = BOOL,
                                actualType = INT.asNullable()
                            )
                        )
                    ),
                    createSelectWhereContinuationTypeTests(
                        whereType = INT.asOptional(),
                        expectedProblems = listOf(
                            createIncompatibleTypesForExprError(
                                SourceLocationMeta(1L, 23L, 9L),
                                expectedType = BOOL,
                                actualType = INT.asOptional()
                            )
                        )
                    ),
                    createSelectWhereContinuationTypeTests(
                        whereType = INT.asNullable().asOptional(),
                        expectedProblems = listOf(
                            createIncompatibleTypesForExprError(
                                SourceLocationMeta(1L, 23L, 9L),
                                expectedType = BOOL,
                                actualType = INT.asNullable().asOptional()
                            )
                        )
                    ),
                    createSelectWhereContinuationTypeTests(
                        whereType = unionOf(INT, FLOAT, STRING),
                        expectedProblems = listOf(
                            createIncompatibleTypesForExprError(
                                SourceLocationMeta(1L, 23L, 9L),
                                expectedType = BOOL,
                                actualType = unionOf(INT, FLOAT, STRING)
                            )
                        )
                    )
                )

        @JvmStatic
        @Suppress("unused")
        fun parametersForErrorInExpressionSourceLocationTests() = listOf(
            // tests with a data type mismatch where the error points to the start of the expression
            TestCase(
                name = "SimpleCaseWhen error in WHEN expression",
                originalSql =
                """
                    CASE t_int
                        WHEN t_string || t_string || t_string THEN t_string
                        WHEN t_symbol || t_symbol || t_symbol THEN t_symbol
                    END
                    """,
                globals = mapOf(
                    "t_int" to INT,
                    "t_string" to STRING,
                    "t_symbol" to SYMBOL
                ),
                handler = expectSemanticProblems(
                    listOf(
                        createDataTypeMismatchError(SourceLocationMeta(3L, 30L, 8L), argTypes = listOf(INT, STRING), nAryOp = "CASE"),
                        createDataTypeMismatchError(SourceLocationMeta(4L, 30L, 8L), argTypes = listOf(INT, STRING), nAryOp = "CASE")
                    )
                )
            ),
            TestCase(
                name = "SearchedCaseWhen error in WHEN expression",
                originalSql =
                """
                    CASE
                        WHEN t_string || t_string || t_string THEN t_string
                        WHEN t_symbol || t_symbol || t_symbol THEN t_symbol
                    END
                    """,
                globals = mapOf(
                    "t_string" to STRING,
                    "t_symbol" to SYMBOL
                ),
                handler = expectSemanticProblems(
                    listOf(
                        createIncompatibleTypesForExprError(SourceLocationMeta(3L, 30L, 8L), expectedType = BOOL, actualType = STRING),
                        createIncompatibleTypesForExprError(SourceLocationMeta(4L, 30L, 8L), expectedType = BOOL, actualType = STRING)
                    )
                )
            ),
            TestCase(
                name = "JOIN condition error in expression",
                originalSql = """
                    SELECT * FROM a
                    JOIN b
                    ON c + c + c""",
                globals = mapOf(
                    "a" to BagType(StructType(mapOf("foo" to INT))),
                    "b" to BagType(StructType(mapOf("bar" to STRING))),
                    "c" to INT
                ),
                handler = expectSemanticProblems(
                    listOf(
                        createIncompatibleTypesForExprError(SourceLocationMeta(4L, 24L, 1L), expectedType = BOOL, actualType = INT)
                    )
                )
            ),
            TestCase(
                name = "WHERE clause error in expression",
                originalSql = "SELECT * FROM t WHERE a + a + a",
                globals = mapOf("t" to BagType(StructType(mapOf("a" to INT)))),
                handler = expectSemanticProblems(
                    listOf(
                        createIncompatibleTypesForExprError(SourceLocationMeta(1L, 23L, 1L), expectedType = BOOL, actualType = INT)
                    )
                )
            )
        )

        @JvmStatic
        @Suppress("unused")
        fun parametersForErrorInFunctionCallArgumentTests() = listOf(
            TestCase(
                name = "function signature without optional/variadic arguments, null propagating given non matching type",
                originalSql = "UPPER(x)",
                globals = mapOf("x" to INT),
                handler = expectSemanticProblems(
                    expectedProblems = listOf(
                        createInvalidArgumentTypeForFunctionError(
                            sourceLocation = SourceLocationMeta(1L, 7L, 1L),
                            functionName = "upper",
                            expectedArgType = unionOf(STRING, SYMBOL),
                            actualType = INT
                        )
                    )
                )
            ),
            TestCase(
                name = "function signature without optional/variadic arguments, null propagating given non matching type for a path",
                originalSql = "UPPER(x.y)",
                globals = mapOf("x" to StructType(mapOf("y" to INT))),
                handler = expectSemanticProblems(
                    expectedProblems = listOf(
                        createInvalidArgumentTypeForFunctionError(
                            sourceLocation = SourceLocationMeta(1L, 7L, 1L),
                            functionName = "upper",
                            expectedArgType = unionOf(STRING, SYMBOL),
                            actualType = INT
                        )
                    )
                )
            ),
            TestCase(
                name = "function signature without optional/variadic arguments, null propagating given non matching type for list reference",
                originalSql = "UPPER(x[0])",
                globals = mapOf("x" to ListType(INT)),
                handler = expectSemanticProblems(
                    expectedProblems = listOf(
                        createInvalidArgumentTypeForFunctionError(
                            sourceLocation = SourceLocationMeta(1L, 7L, 1L),
                            functionName = "upper",
                            expectedArgType = unionOf(STRING, SYMBOL),
                            actualType = INT
                        )
                    )
                )
            ),
            TestCase(
                name = "function signature without optional/variadic arguments, null propagating given non matching type with missing",
                originalSql = "UPPER(x)",
                globals = mapOf("x" to INT.asOptional()),
                handler = expectSemanticProblems(
                    expectedProblems = listOf(
                        createInvalidArgumentTypeForFunctionError(
                            sourceLocation = SourceLocationMeta(1L, 7L, 1L),
                            functionName = "upper",
                            expectedArgType = unionOf(STRING, SYMBOL),
                            actualType = INT.asOptional()
                        )
                    )
                )
            ),
            TestCase(
                name = "function signature without optional/variadic arguments, null propagating given non matching type with no valid overlapping type",
                originalSql = "UPPER(x)",
                globals = mapOf("x" to unionOf(INT, BOOL)),
                handler = expectSemanticProblems(
                    expectedProblems = listOf(
                        createInvalidArgumentTypeForFunctionError(
                            sourceLocation = SourceLocationMeta(1L, 7L, 1L),
                            functionName = "upper",
                            expectedArgType = unionOf(STRING, SYMBOL),
                            actualType = unionOf(INT, BOOL)
                        )
                    )
                )
            ),
            TestCase(
                name = "function signature without optional/variadic arguments, null propagating given non matching type including null with no valid overlapping type",
                originalSql = "UPPER(x)",
                globals = mapOf("x" to unionOf(INT, BOOL, NULL)),
                handler = expectSemanticProblems(
                    expectedProblems = listOf(
                        createInvalidArgumentTypeForFunctionError(
                            sourceLocation = SourceLocationMeta(1L, 7L, 1L),
                            functionName = "upper",
                            expectedArgType = unionOf(STRING, SYMBOL),
                            actualType = unionOf(INT, BOOL, NULL)
                        )
                    )
                )
            ),
            TestCase(
                name = "function signature without optional/variadic arguments, null propagating given null",
                originalSql = "UPPER(x)",
                globals = mapOf("x" to NULL),
                handler = expectSemanticProblems(
                    expectedProblems = listOf(
                        createNullOrMissingFunctionArgumentError(
                            sourceLocation = SourceLocationMeta(1L, 7L, 1L),
                            functionName = "upper"
                        )
                    )
                )
            ),
            TestCase(
                name = "function signature without optional/variadic arguments, null propagating given missing",
                originalSql = "UPPER(x)",
                globals = mapOf("x" to MISSING),
                handler = expectSemanticProblems(
                    expectedProblems = listOf(
                        createNullOrMissingFunctionArgumentError(
                            sourceLocation = SourceLocationMeta(1L, 7L, 1L),
                            functionName = "upper"
                        )
                    )
                )
            ),
            TestCase(
                name = "function signature without optional/variadic arguments, null propagating given non matching type with null and missing",
                originalSql = "UPPER(x)",
                globals = mapOf("x" to NULL_OR_MISSING),
                handler = expectSemanticProblems(
                    expectedProblems = listOf(
                        createNullOrMissingFunctionArgumentError(
                            sourceLocation = SourceLocationMeta(1L, 7L, 1L),
                            functionName = "upper"
                        )
                    )
                )
            ),
            TestCase(
                name = "function with more than one argument - invalid arg type at 2nd arg",
                originalSql = "date_add(year, x, `2010-01-01T`)",
                globals = mapOf("x" to DECIMAL),
                handler = expectSemanticProblems(
                    expectedProblems = listOf(
                        createInvalidArgumentTypeForFunctionError(
                            sourceLocation = SourceLocationMeta(1L, 16L, 1L),
                            functionName = "date_add",
                            expectedArgType = INT,
                            actualType = DECIMAL
                        )
                    )
                )
            ),
            TestCase(
                name = "function with more than one argument - null 3rd arg",
                originalSql = "date_add(year, 5, x)",
                globals = mapOf("x" to NULL),
                handler = expectSemanticProblems(
                    expectedProblems = listOf(
                        createNullOrMissingFunctionArgumentError(
                            sourceLocation = SourceLocationMeta(1L, 19L, 1L),
                            functionName = "date_add"
                        )
                    )
                )
            ),
            TestCase(
                name = "function with more than one argument - missing 3rd arg",
                originalSql = "date_add(year, 5, x)",
                globals = mapOf("x" to MISSING),
                handler = expectSemanticProblems(
                    expectedProblems = listOf(
                        createNullOrMissingFunctionArgumentError(
                            sourceLocation = SourceLocationMeta(1L, 19L, 1L),
                            functionName = "date_add"
                        )
                    )
                )
            ),
            TestCase(
                name = "function with more than one argument having missing and one argument null",
                originalSql = "date_add(year, x, y)",
                globals = mapOf(
                    "x" to MISSING,
                    "y" to NULL
                ),
                handler = expectSemanticProblems(
                    expectedProblems = listOf(
                        createNullOrMissingFunctionArgumentError(
                            sourceLocation = SourceLocationMeta(1L, 16L, 1L),
                            functionName = "date_add"
                        ),
                        createNullOrMissingFunctionArgumentError(
                            sourceLocation = SourceLocationMeta(1L, 19L, 1L),
                            functionName = "date_add"
                        )
                    )
                )
            ),
            TestCase(
                name = "function with more than one argument with nulls and union types with nulls",
                originalSql = "date_add(year, x, y)",
                globals = mapOf(
                    "x" to unionOf(INT4, NULL),
                    "y" to NULL
                ),
                handler = expectSemanticProblems(
                    expectedProblems = listOf(
                        createNullOrMissingFunctionArgumentError(
                            sourceLocation = SourceLocationMeta(1L, 19L, 1L),
                            functionName = "date_add"
                        )
                    )
                )
            ),
            TestCase(
                name = "function with more than one argument with union types and no valid case (x should have a INT type)",
                originalSql = "date_add(year, x, y)",
                globals = mapOf(
                    "x" to unionOf(BOOL, STRING),
                    "y" to unionOf(INT, TIMESTAMP)
                ),
                handler = expectSemanticProblems(
                    expectedProblems = listOf(
                        createInvalidArgumentTypeForFunctionError(
                            sourceLocation = SourceLocationMeta(1L, 16L, 1L),
                            functionName = "date_add",
                            expectedArgType = INT,
                            actualType = unionOf(BOOL, STRING)
                        )
                    )
                )
            ),
            TestCase(
                name = "function with more than one argument with union types, nulls and with no valid case",
                originalSql = "date_add(year, x, y)",
                globals = mapOf(
                    "x" to unionOf(BOOL, STRING),
                    "y" to unionOf(INT, TIMESTAMP, NULL)
                ),
                handler = expectSemanticProblems(
                    expectedProblems = listOf(
                        createInvalidArgumentTypeForFunctionError(
                            sourceLocation = SourceLocationMeta(1L, 16L, 1L),
                            functionName = "date_add",
                            expectedArgType = INT,
                            actualType = unionOf(BOOL, STRING)
                        )
                    )
                )
            ),
            TestCase(
                name = "function that expects union type in signature but NULL provided",
                originalSql = "size(x)",
                globals = mapOf("x" to NULL),
                handler = expectSemanticProblems(
                    expectedProblems = listOf(
                        createNullOrMissingFunctionArgumentError(
                            sourceLocation = SourceLocationMeta(1L, 6L, 1L),
                            functionName = "size"
                        )
                    )
                )
            ),
            TestCase(
                name = "function that expects union type in signature but MISSING provided",
                originalSql = "size(x)",
                globals = mapOf("x" to MISSING),
                handler = expectSemanticProblems(
                    expectedProblems = listOf(
                        createNullOrMissingFunctionArgumentError(
                            sourceLocation = SourceLocationMeta(1L, 6L, 1L),
                            functionName = "size"
                        )
                    )
                )
            ),
            TestCase(
                name = "function that expects union type in signature but STRING provided",
                originalSql = "size(x)",
                globals = mapOf("x" to STRING),
                handler = expectSemanticProblems(
                    expectedProblems = listOf(
                        createInvalidArgumentTypeForFunctionError(
                            sourceLocation = SourceLocationMeta(1L, 6L, 1L),
                            functionName = "size",
                            expectedArgType = unionOf(BAG, LIST, STRUCT, SEXP),
                            actualType = STRING
                        )
                    )
                )
            ),
            TestCase(
                name = "function that that could have optional parameter with null",
                originalSql = "SUBSTRING('123456789', x)",
                globals = mapOf("x" to NULL),
                handler = expectSemanticProblems(
                    expectedProblems = listOf(
                        createNullOrMissingFunctionArgumentError(
                            sourceLocation = SourceLocationMeta(1L, 24L, 1L),
                            functionName = "substring"
                        )
                    )
                )
            ),
            TestCase(
                name = "function that has optional parameter with null",
                originalSql = "SUBSTRING('123456789', x, 0)",
                globals = mapOf("x" to NULL),
                handler = expectSemanticProblems(
                    expectedProblems = listOf(
                        createNullOrMissingFunctionArgumentError(
                            sourceLocation = SourceLocationMeta(1L, 24L, 1L),
                            functionName = "substring"
                        )
                    )
                )
            ),
            TestCase(
                name = "function that has optional parameter with missing",
                originalSql = "SUBSTRING('123456789', x, 0)",
                globals = mapOf("x" to MISSING),
                handler = expectSemanticProblems(
                    expectedProblems = listOf(
                        createNullOrMissingFunctionArgumentError(
                            sourceLocation = SourceLocationMeta(1L, 24L, 1L),
                            functionName = "substring"
                        )
                    )
                )
            ),
            TestCase(
                name = "function that has optional parameter with optional parameter as null",
                originalSql = "SUBSTRING('123456789', 0, x)",
                globals = mapOf("x" to NULL),
                handler = expectSemanticProblems(
                    expectedProblems = listOf(
                        createNullOrMissingFunctionArgumentError(
                            sourceLocation = SourceLocationMeta(1L, 27L, 1L),
                            functionName = "substring"
                        )
                    )
                )
            ),
            TestCase(
                name = "function that has optional parameter with optional parameter as missing",
                originalSql = "SUBSTRING('123456789', 0, x)",
                globals = mapOf("x" to MISSING),
                handler = expectSemanticProblems(
                    expectedProblems = listOf(
                        createNullOrMissingFunctionArgumentError(
                            sourceLocation = SourceLocationMeta(1L, 27L, 1L),
                            functionName = "substring"
                        )
                    )
                )
            ),
            TestCase(
                name = "function that has optional parameter with optional parameters as null and missing",
                originalSql = "SUBSTRING('123456789', x, y)",
                globals = mapOf(
                    "x" to NULL,
                    "y" to MISSING
                ),
                handler = expectSemanticProblems(
                    expectedProblems = listOf(
                        createNullOrMissingFunctionArgumentError(
                            sourceLocation = SourceLocationMeta(1L, 24L, 1L),
                            functionName = "substring"
                        ),
                        createNullOrMissingFunctionArgumentError(
                            sourceLocation = SourceLocationMeta(1L, 27L, 1L),
                            functionName = "substring"
                        )
                    )
                )
            ),
            TestCase(
                name = "function that has optional parameter with optional parameters as missing and null",
                originalSql = "SUBSTRING('123456789', x, y)",
                globals = mapOf(
                    "x" to MISSING,
                    "y" to NULL
                ),
                handler = expectSemanticProblems(
                    expectedProblems = listOf(
                        createNullOrMissingFunctionArgumentError(
                            sourceLocation = SourceLocationMeta(1L, 24L, 1L),
                            functionName = "substring"
                        ),
                        createNullOrMissingFunctionArgumentError(
                            sourceLocation = SourceLocationMeta(1L, 27L, 1L),
                            functionName = "substring"
                        )
                    )
                )
            ),
            TestCase(
                name = "function that has optional parameter with optional parameters as null and null",
                originalSql = "SUBSTRING('123456789', x, y)",
                globals = mapOf(
                    "x" to NULL,
                    "y" to NULL
                ),
                handler = expectSemanticProblems(
                    expectedProblems = listOf(
                        createNullOrMissingFunctionArgumentError(
                            sourceLocation = SourceLocationMeta(1L, 24L, 1L),
                            functionName = "substring"
                        ),
                        createNullOrMissingFunctionArgumentError(
                            sourceLocation = SourceLocationMeta(1L, 27L, 1L),
                            functionName = "substring"
                        )
                    )
                )
            ),
            TestCase(
                name = "function that has optional parameter with optional parameters as missing and missing",
                originalSql = "SUBSTRING('123456789', x, y)",
                globals = mapOf(
                    "x" to MISSING,
                    "y" to MISSING
                ),
                handler = expectSemanticProblems(
                    expectedProblems = listOf(
                        createNullOrMissingFunctionArgumentError(
                            sourceLocation = SourceLocationMeta(1L, 24L, 1L),
                            functionName = "substring"
                        ),
                        createNullOrMissingFunctionArgumentError(
                            sourceLocation = SourceLocationMeta(1L, 27L, 1L),
                            functionName = "substring"
                        )
                    )
                )
            ),
            TestCase(
                name = "function that has optional parameter with optional parameter with incorrect type",
                originalSql = "SUBSTRING('123456789', 0, x)",
                globals = mapOf("x" to STRING),
                handler = expectSemanticProblems(
                    expectedProblems = listOf(
                        createInvalidArgumentTypeForFunctionError(
                            sourceLocation = SourceLocationMeta(1L, 27L, 1L),
                            functionName = "substring",
                            expectedArgType = INT,
                            actualType = STRING
                        )
                    )
                )
            ),
            // tests with a variadic ExprFunction
            TestCase(
                name = "variadic function, missing in required with variadic params",
                originalSql = "TRIM(BOTH FROM x)",
                globals = mapOf("x" to MISSING),
                handler = expectSemanticProblems(
                    expectedProblems = listOf(
                        createNullOrMissingFunctionArgumentError(
                            sourceLocation = SourceLocationMeta(1L, 16L, 1L),
                            functionName = "trim"
                        )
                    )
                )
            ),
            TestCase(
                name = "variadic function, missing in required with no variadic params",
                originalSql = "TRIM(x)",
                globals = mapOf("x" to MISSING),
                handler = expectSemanticProblems(
                    expectedProblems = listOf(
                        createNullOrMissingFunctionArgumentError(
                            sourceLocation = SourceLocationMeta(1L, 6L, 1L),
                            functionName = "trim"
                        )
                    )
                )
            ),
            TestCase(
                name = "variadic function, string in required, missing in variadic with other variadic param",
                originalSql = "TRIM(BOTH x FROM y)",
                globals = mapOf("x" to MISSING, "y" to STRING),
                handler = expectSemanticProblems(
                    expectedProblems = listOf(
                        createNullOrMissingFunctionArgumentError(
                            sourceLocation = SourceLocationMeta(1L, 11L, 1L),
                            functionName = "trim"
                        )
                    )
                )
            ),
            TestCase(
                name = "variadic function, string in required, missing in variadic with no other variadic param",
                originalSql = "TRIM(x FROM y)",
                globals = mapOf("x" to MISSING, "y" to STRING),
                handler = expectSemanticProblems(
                    expectedProblems = listOf(
                        createNullOrMissingFunctionArgumentError(
                            sourceLocation = SourceLocationMeta(1L, 6L, 1L),
                            functionName = "trim"
                        )
                    )
                )
            ),
            TestCase(
                name = "variadic function, missing in required, string in variadic with other variadic param",
                originalSql = "TRIM(BOTH x FROM y)",
                globals = mapOf("x" to STRING, "y" to MISSING),
                handler = expectSemanticProblems(
                    expectedProblems = listOf(
                        createNullOrMissingFunctionArgumentError(
                            sourceLocation = SourceLocationMeta(1L, 18L, 1L),
                            functionName = "trim"
                        )
                    )
                )
            ),
            TestCase(
                name = "variadic function, missing in required, string in variadic with no other variadic param",
                originalSql = "TRIM(x FROM y)",
                globals = mapOf("x" to STRING, "y" to MISSING),
                handler = expectSemanticProblems(
                    expectedProblems = listOf(
                        createNullOrMissingFunctionArgumentError(
                            sourceLocation = SourceLocationMeta(1L, 13L, 1L),
                            functionName = "trim"
                        )
                    )
                )
            ),
            TestCase(
                name = "variadic function, bad type in required with other variadic param",
                originalSql = "TRIM(BOTH FROM x)",
                globals = mapOf("x" to unionOf(BOOL, INT, TIMESTAMP, LIST)),
                handler = expectSemanticProblems(
                    expectedProblems = listOf(
                        createInvalidArgumentTypeForFunctionError(
                            sourceLocation = SourceLocationMeta(1L, 16L, 1L),
                            functionName = "trim",
                            expectedArgType = STRING,
                            actualType = unionOf(BOOL, INT, TIMESTAMP, LIST)
                        )
                    )
                )
            ),
            TestCase(
                name = "variadic function, missing in required, bad type in variadic with other variadic param",
                originalSql = "TRIM(BOTH x FROM y)",
                globals = mapOf("x" to unionOf(BOOL, INT, TIMESTAMP, LIST), "y" to MISSING),
                handler = expectSemanticProblems(
                    expectedProblems = listOf(
                        createInvalidArgumentTypeForFunctionError(
                            sourceLocation = SourceLocationMeta(1L, 11L, 1L),
                            functionName = "trim",
                            expectedArgType = STRING,
                            actualType = unionOf(BOOL, INT, TIMESTAMP, LIST)
                        ),
                        createNullOrMissingFunctionArgumentError(
                            sourceLocation = SourceLocationMeta(1L, 18L, 1L),
                            functionName = "trim"
                        )
                    )
                )
            )
        )

        @JvmStatic
        @Suppress("unused")
        fun parametersForMultipleInferenceProblemsTests() = listOf(
            TestCase(
                "projections with paths",
                "SELECT c.firstName, c.age FROM customers AS c",
                mapOf(
                    "customers" to customerType
                ),
                handler = expectQueryOutputType(
                    BagType(
                        StructType(
                            mapOf(
                                "firstName" to StaticType.STRING,
                                "age" to StaticType.INT2
                            ),
                            true
                        )
                    )
                )
            ),
            TestCase(
                "projections with paths with missing attributes and closed content",
                "SELECT c.missing_attr FROM customers AS c",
                mapOf(
                    "customers" to customerType
                ),
                handler = expectQueryOutputType(
                    BagType(
                        StructType(
                            mapOf(
                                "missing_attr" to StaticType.MISSING
                            ),
                            true
                        )
                    )
                )
            ),
            TestCase(
                "projections with paths with missing attributes and open content",
                "SELECT c.missing_attr FROM customers AS c",
                mapOf(
                    "customers" to BagType(
                        StructType(
                            mapOf(
                                "firstName" to StaticType.STRING
                            ),
                            contentClosed = false
                        )
                    )
                ),
                handler = expectQueryOutputType(
                    BagType(
                        StructType(
                            mapOf(
                                "missing_attr" to StaticType.ANY
                            ),
                            true
                        )
                    )
                )
            ),
            TestCase(
                "projections with multi level paths",
                "SELECT c.address.city FROM customers AS c",
                mapOf(
                    "customers" to customerType
                ),
                handler = expectQueryOutputType(
                    BagType(
                        StructType(
                            mapOf(
                                "city" to StaticType.STRING
                            ),
                            true
                        )
                    )
                )
            ),
            TestCase(
                "projections with select *",
                "SELECT * FROM customers AS c",
                mapOf(
                    "customers" to customerType
                ),
                handler = expectQueryOutputType(
                    BagType(
                        StructType(
                            (customerType.elementType as StructType).fields,
                            (customerType.elementType as StructType).contentClosed
                        )
                    )
                )
            ),
            TestCase(
                "projections with duplicate items in select list query",
                "SELECT firstName as foo, age as foo FROM customers AS c",
                mapOf(
                    "customers" to customerType
                ),
                handler = expectSemanticErrors(
                    expectedErrors = listOf(
                        Problem(
                            SourceLocationMeta(1L, 26L),
                            SemanticProblemDetails.DuplicateAliasesInSelectListItem
                        )
                    )
                )
            ),
            TestCase(
                "projections with AT value for list",
                "SELECT elem, atVal FROM x AS elem AT atVal",
                mapOf(
                    "x" to ListType(StaticType.STRING)
                ),
                handler = expectQueryOutputType(
                    BagType(
                        StructType(
                            mapOf(
                                "elem" to StaticType.STRING,
                                "atVal" to StaticType.INT
                            ),
                            contentClosed = true
                        )
                    )
                )
            ),
            TestCase(
                "projections with AT value for bag",
                "SELECT elem, atVal FROM x AS elem AT atVal",
                mapOf(
                    "x" to BagType(StaticType.STRING)
                ),
                handler = expectQueryOutputType(
                    BagType(
                        StructType(
                            mapOf(
                                "elem" to StaticType.STRING,
                                "atVal" to StaticType.MISSING
                            ),
                            contentClosed = true
                        )
                    )
                )
            ),
            TestCase(
                "projections with from source as ANY",
                "SELECT elem, atVal FROM x AS elem AT atVal",
                mapOf(
                    "x" to StaticType.ANY
                ),
                handler = expectQueryOutputType(
                    BagType(
                        StructType(
                            mapOf(
                                "elem" to StaticType.ANY,
                                // INT possible because `any` includes list. MISSING for everything else.
                                "atVal" to StaticType.unionOf(StaticType.INT, StaticType.MISSING)
                            ),
                            contentClosed = true
                        )
                    )
                )
            ),
            TestCase(
                "projections with from source as a union of list and bag",
                "SELECT elem, atVal FROM x AS elem AT atVal",
                mapOf(
                    "x" to StaticType.unionOf(ListType(StaticType.INT), BagType(StaticType.STRING))
                ),
                handler = expectQueryOutputType(
                    BagType(
                        StructType(
                            mapOf(
                                "elem" to StaticType.unionOf(StaticType.INT, StaticType.STRING),
                                "atVal" to StaticType.unionOf(StaticType.INT, StaticType.MISSING)
                            ),
                            contentClosed = true
                        )
                    )
                )
            ),
            TestCase(
                "projections with from source as a union of list and a non-collection type",
                "SELECT elem, atVal FROM x AS elem AT atVal",
                mapOf(
                    "x" to StaticType.unionOf(ListType(StaticType.INT), StaticType.STRING)
                ),
                handler = expectQueryOutputType(
                    BagType(
                        StructType(
                            mapOf(
                                // StaticType.STRING coerces to BagType(StaticType.STRING) in FROM clause
                                "elem" to StaticType.unionOf(StaticType.INT, StaticType.STRING),
                                "atVal" to StaticType.unionOf(StaticType.INT, StaticType.MISSING)
                            ),
                            contentClosed = true
                        )
                    )
                )
            ),
            TestCase(
                "projections with from source as collection of collections",
                "SELECT elem, atVal FROM x AS elem AT atVal",
                mapOf(
                    "x" to BagType(ListType(StaticType.STRING))
                ),
                handler = expectQueryOutputType(
                    BagType(
                        StructType(
                            mapOf(
                                "elem" to ListType(StaticType.STRING),
                                "atVal" to StaticType.MISSING
                            ),
                            contentClosed = true
                        )
                    )
                )
            ),
            TestCase(
                "projections with from source as a union of null and missing",
                "SELECT elem, atVal FROM x AS elem AT atVal",
                mapOf(
                    "x" to StaticType.unionOf(StaticType.NULL, StaticType.MISSING)
                ),
                handler = expectQueryOutputType(
                    BagType(
                        StructType(
                            mapOf(
                                "elem" to StaticType.unionOf(StaticType.NULL, StaticType.MISSING),
                                "atVal" to StaticType.MISSING
                            ),
                            contentClosed = true
                        )
                    )
                )
            ),
            TestCase(
                "projections with from source as a sexp",
                "SELECT elem, atVal FROM x AS elem AT atVal",
                mapOf(
                    "x" to SexpType(StaticType.INT)
                ),
                handler = expectQueryOutputType(
                    BagType(
                        StructType(
                            mapOf(
                                // Evaluator behavior when this test case was written -
                                // PartiQL> select f, x from sexp(1,2,3) as f at x
                                //   |
                                // ==='
                                // <<
                                //  {
                                //    'f': `(1 2 3)`
                                //  }
                                // >>
                                // ---
                                "elem" to SexpType(StaticType.INT),
                                "atVal" to StaticType.MISSING
                            ),
                            contentClosed = true
                        )
                    )
                )
            ),
            TestCase(
                "UNPIVOT on closed content struct",
                "SELECT sym, price FROM UNPIVOT closingPrice AS price AT sym",
                mapOf(
                    "closingPrice" to StructType(
                        mapOf(
                            "msft" to StaticType.DECIMAL,
                            "amzn" to StaticType.DECIMAL
                        ),
                        contentClosed = true
                    )
                ),
                handler = expectQueryOutputType(
                    BagType(
                        StructType(
                            mapOf(
                                "sym" to StaticType.STRING,
                                "price" to StaticType.DECIMAL
                            ),
                            contentClosed = true
                        )
                    )
                )
            ),
            TestCase(
                "UNPIVOT with open content",
                "SELECT sym, price FROM UNPIVOT closingPrice AS price AT sym",
                mapOf(
                    "closingPrice" to StructType(
                        mapOf(
                            "msft" to StaticType.STRING,
                            "amzn" to StaticType.DECIMAL
                        ),
                        contentClosed = false
                    )
                ),
                handler = expectQueryOutputType(
                    BagType(
                        StructType(
                            mapOf(
                                "sym" to StaticType.STRING.asOptional(),
                                "price" to StaticType.ANY
                            ),
                            contentClosed = true
                        )
                    )
                )
            ),
            TestCase(
                "UNPIVOT on AnyType",
                "SELECT sym, price FROM UNPIVOT closingPrice AS price AT sym",
                mapOf(
                    "closingPrice" to StaticType.ANY
                ),
                handler = expectQueryOutputType(
                    BagType(
                        StructType(
                            mapOf(
                                "sym" to StaticType.STRING.asOptional(),
                                "price" to StaticType.ANY
                            ),
                            contentClosed = true
                        )
                    )
                )
            ),
            TestCase(
                "UNPIVOT on MISSING",
                "SELECT sym, price FROM UNPIVOT closingPrice AS price AT sym",
                mapOf(
                    "closingPrice" to StaticType.MISSING
                ),
                handler = expectQueryOutputType(
                    BagType(
                        StructType(
                            mapOf(
                                "sym" to StaticType.MISSING,
                                "price" to StaticType.MISSING
                            ),
                            contentClosed = true
                        )
                    )
                )
            ),
            TestCase(
                "UNPIVOT on IntType",
                "SELECT sym, price FROM UNPIVOT closingPrice AS price AT sym",
                mapOf(
                    "closingPrice" to StaticType.INT
                ),
                handler = expectQueryOutputType(
                    BagType(
                        StructType(
                            mapOf(
                                "sym" to StaticType.STRING,
                                "price" to StaticType.INT
                            ),
                            contentClosed = true
                        )
                    )
                )
            ),
            TestCase(
                "join with closed-content structs",
                "SELECT * FROM a, b",
                mapOf(
                    "a" to BagType(
                        StructType(
                            mapOf("x" to StaticType.INT4),
                            true
                        )
                    ),
                    "b" to BagType(
                        StructType(
                            mapOf("y" to StaticType.INT4),
                            true
                        )
                    )
                ),
                handler = expectQueryOutputType(
                    BagType(
                        StructType(
                            mapOf(
                                "x" to StaticType.INT4,
                                "y" to StaticType.INT4
                            ),
                            true
                        )
                    )
                )
            ),
            TestCase(
                "join with mixed open content-ness",
                "SELECT * FROM a, b",
                mapOf(
                    "a" to BagType(
                        StructType(
                            mapOf("x" to StaticType.INT4),
                            true
                        )
                    ),
                    "b" to BagType(
                        StructType(
                            mapOf("y" to StaticType.INT4),
                            false
                        )
                    )
                ),
                handler = expectQueryOutputType(
                    BagType(
                        StructType(
                            mapOf(
                                "x" to StaticType.INT4,
                                "y" to StaticType.INT4
                            ),
                            false
                        )
                    )
                )
            ),
            TestCase(
                "SELECT VALUE",
                "SELECT VALUE elem FROM a_list AS elem",
                mapOf("a_list" to ListType(StaticType.STRING)),
                handler = expectQueryOutputType(BagType(StaticType.STRING))
            ),
            TestCase(
                "un-nesting a list",
                "SELECT o.orderId, o.orderDate FROM customers AS c, @c.orders AS o",
                mapOf(
                    "customers" to customerType
                ),
                handler = expectQueryOutputType(
                    BagType(
                        StructType(
                            mapOf(
                                "orderId" to StaticType.STRING,
                                "orderDate" to StaticType.TIMESTAMP
                            ),
                            true
                        )
                    )
                )
            ),
            TestCase(
                "coalesce op with variadic arguments - int, null",
                "COALESCE(1, null)",
                handler = expectQueryOutputType(StaticType.INT)
            ),
            TestCase(
                "coalesce op with variadic arguments - int, missing",
                "COALESCE(1, missing)",
                handler = expectQueryOutputType(StaticType.INT)
            ),
            TestCase(
                "coalesce op with variadic arguments - null, missing",
                "COALESCE(null, missing)",
                handler = expectQueryOutputType(StaticType.NULL)
            ),
            TestCase(
                "coalesce op with variadic arguments - null, string, missing, int",
                "COALESCE(null, 'a', missing, 1)",
                handler = expectQueryOutputType(StaticType.STRING)
            ),
            TestCase(
                "coalesce op with variadic arguments - missing, missing",
                "COALESCE(missing, missing)",
                handler = expectQueryOutputType(StaticType.MISSING)
            ),
            TestCase(
                "coalesce op with variadic arguments - null, null",
                "COALESCE(null, null)",
                handler = expectQueryOutputType(StaticType.NULL)
            ),
            TestCase(
                "coalesce op with variadic arguments - union types",
                "COALESCE(x, y)",
                mapOf(
                    "x" to unionOf(NULL, STRING),
                    "y" to STRING
                ),
                handler = expectQueryOutputType(STRING)
            ),
            TestCase(
                "coalesce op with variadic arguments - union types",
                "COALESCE(x, y)",
                mapOf(
                    "x" to unionOf(NULL, STRING),
                    "y" to INT
                ),
                handler = expectQueryOutputType(unionOf(STRING, INT))
            ),
            TestCase(
                "coalesce op with variadic arguments - union types",
                "COALESCE(x, y)",
                mapOf(
                    "x" to unionOf(NULL, STRING),
                    "y" to unionOf(NULL, INT)
                ),
                handler = expectQueryOutputType(unionOf(NULL, STRING, INT))
            ),
            TestCase(
                "coalesce op with variadic arguments - union types",
                "COALESCE(x, y)",
                mapOf(
                    "x" to unionOf(NULL, STRING),
                    "y" to unionOf(NULL, INT, MISSING)
                ),
                handler = expectQueryOutputType(unionOf(NULL, STRING, INT))
            ),
            TestCase(
                "coalesce op with variadic arguments - union types",
                "COALESCE(x, y)",
                mapOf(
                    "x" to unionOf(MISSING, STRING),
                    "y" to unionOf(MISSING, INT)
                ),
                handler = expectQueryOutputType(unionOf(MISSING, STRING, INT))
            ),
            TestCase(
                "coalesce op with variadic arguments - union types",
                "COALESCE(x, y)",
                mapOf(
                    "x" to unionOf(MISSING, NULL, STRING),
                    "y" to unionOf(MISSING, INT)
                ),
                handler = expectQueryOutputType(unionOf(MISSING, NULL, STRING, INT))
            ),
            TestCase(
                "coalesce op with variadic arguments - union types",
                "COALESCE(x, y, z, w)",
                mapOf(
                    "x" to unionOf(MISSING, STRING),
                    "y" to unionOf(MISSING, INT),
                    "z" to unionOf(BOOL, BAG),
                    "w" to unionOf(NULL, LIST)
                ),
                handler = expectQueryOutputType(unionOf(BOOL, BAG, STRING, INT))
            ),
            TestCase(
                "function signature without optional/variadic arguments, null propagating given matching type",
                "UPPER('test')",
                handler = expectQueryOutputType(StaticType.STRING)
            ),
            TestCase(
                "function signature without optional/variadic arguments, null propagating given matching type with null",
                "UPPER(x)",
                mapOf(
                    "x" to AnyOfType(setOf(StaticType.NULL, StaticType.STRING))
                ),
                handler = expectQueryOutputType(AnyOfType(setOf(StaticType.NULL, StaticType.STRING)))
            ),
            TestCase(
                "function signature without optional/variadic arguments, null propagating given matching type with missing",
                "UPPER(x)",
                mapOf(
                    "x" to AnyOfType(setOf(StaticType.MISSING, StaticType.STRING))
                ),
                handler = expectQueryOutputType(AnyOfType(setOf(StaticType.MISSING, StaticType.STRING)))
            ),
            TestCase(
                "function signature without optional/variadic arguments, null propagating given non matching type but has a valid overlapping type",
                "UPPER(x)",
                mapOf(
                    "x" to AnyOfType(setOf(StaticType.INT, StaticType.STRING))
                ),
                handler = expectQueryOutputType(AnyOfType(setOf(StaticType.MISSING, StaticType.STRING)))
            ),
            TestCase(
                "function signature without optional/variadic arguments, null propagating given non matching type but has a valid overlapping type including null",
                "UPPER(x)",
                mapOf(
                    "x" to AnyOfType(setOf(StaticType.INT, StaticType.BOOL, StaticType.STRING, StaticType.NULL))
                ),
                handler = expectQueryOutputType(AnyOfType(setOf(StaticType.MISSING, StaticType.NULL, StaticType.STRING)))
            ),
            TestCase(
                "function signature without optional/variadic arguments, with ANY in arguments",
                "UPPER(x)",
                mapOf(
                    "x" to StaticType.ANY
                ),
                handler = expectQueryOutputType(AnyOfType(setOf(StaticType.MISSING, StaticType.NULL, StaticType.STRING)))
            ),
            TestCase(
                "function signature without optional/variadic arguments, null propagating given lesser than expected arguments",
                "UPPER()",
                handler = expectSemanticErrors(
                    expectedErrors = listOf(
                        Problem(
                            SourceLocationMeta(1L, 1L),
                            SemanticProblemDetails.IncorrectNumberOfArgumentsToFunctionCall(
                                functionName = "upper",
                                expectedArity = 1..1,
                                actualArity = 0
                            )
                        )
                    )
                )
            ),
            TestCase(
                "function signature without optional/variadic arguments, null propagating given more than expected arguments",
                "UPPER('test', 'test')",
                handler = expectSemanticErrors(
                    expectedErrors = listOf(
                        Problem(
                            SourceLocationMeta(1L, 1L),
                            SemanticProblemDetails.IncorrectNumberOfArgumentsToFunctionCall(
                                functionName = "upper",
                                expectedArity = 1..1,
                                actualArity = 2
                            )
                        )
                    )
                )
            ),
            TestCase(
                "null propagating function with union types with null",
                "UPPER(x)",
                mapOf(
                    "x" to AnyOfType(setOf(StaticType.STRING, StaticType.NULL))

                ),
                handler = expectQueryOutputType(AnyOfType(setOf(StaticType.STRING, StaticType.NULL)))
            ),
            TestCase(
                "function that expects no arguments",
                "utcnow()",
                handler = expectQueryOutputType(StaticType.TIMESTAMP)
            ),
            TestCase(
                "function that expects no arguments given arguments",
                "utcnow(null)",
                handler = expectSemanticErrors(
                    expectedErrors = listOf(
                        Problem(
                            SourceLocationMeta(1L, 1L),
                            SemanticProblemDetails.IncorrectNumberOfArgumentsToFunctionCall(
                                functionName = "utcnow",
                                expectedArity = 0..0,
                                actualArity = 1
                            )
                        )
                    )
                )
            ),
            TestCase(
                "function with more than one argument",
                "date_add(year, 5, `2010-01-01T`)",
                handler = expectQueryOutputType(StaticType.TIMESTAMP)
            ),
            TestCase(
                "function with more than one argument with union types having nulls",
                "date_add(year, x, y)",
                mapOf(
                    "x" to AnyOfType(setOf(StaticType.NULL, StaticType.INT)),
                    "y" to AnyOfType(setOf(StaticType.NULL, StaticType.TIMESTAMP))
                ),
                handler = expectQueryOutputType(AnyOfType(setOf(StaticType.TIMESTAMP, StaticType.NULL)))
            ),
            TestCase(
                "function with more than one argument with union types having missing",
                "date_add(year, x, y)",
                mapOf(
                    "x" to AnyOfType(setOf(StaticType.MISSING, StaticType.INT)),
                    "y" to AnyOfType(setOf(StaticType.MISSING, StaticType.TIMESTAMP))
                ),
                handler = expectQueryOutputType(AnyOfType(setOf(StaticType.TIMESTAMP, StaticType.MISSING)))
            ),
            TestCase(
                "function with more than one argument with union types having null and missing",
                "date_add(year, x, y)",
                mapOf(
                    "x" to AnyOfType(setOf(StaticType.MISSING, StaticType.INT)),
                    "y" to AnyOfType(setOf(StaticType.NULL, StaticType.TIMESTAMP))
                ),
                handler = expectQueryOutputType(AnyOfType(setOf(StaticType.TIMESTAMP, StaticType.MISSING, StaticType.NULL)))
            ),
            TestCase(
                "function with more than one argument with union types and at least one valid case",
                "date_add(year, x, y)",
                mapOf(
                    "x" to AnyOfType(setOf(StaticType.INT, StaticType.STRING)),
                    "y" to AnyOfType(setOf(StaticType.INT, StaticType.TIMESTAMP))
                ),
                handler = expectQueryOutputType(AnyOfType(setOf(StaticType.TIMESTAMP, StaticType.MISSING)))
            ),
            TestCase(
                "function with more than one argument with union types, nulls and at least one valid case",
                "date_add(year, x, y)",
                mapOf(
                    "x" to AnyOfType(setOf(StaticType.INT, StaticType.STRING)),
                    "y" to AnyOfType(setOf(StaticType.INT, StaticType.TIMESTAMP, StaticType.NULL))
                ),
                handler = expectQueryOutputType(AnyOfType(setOf(StaticType.TIMESTAMP, StaticType.MISSING, StaticType.NULL)))
            ),
            TestCase(
                "function that expects union type in signature",
                "size([1, 2])",
                handler = expectQueryOutputType(StaticType.INT)
            ),
            TestCase(
                "function that expects union type in signature",
                "size({'a': 1, 'b': 2})",
                mapOf(),
                handler = expectQueryOutputType(StaticType.INT)
            ),
            TestCase(
                "function that has optional parameter",
                "SUBSTRING('123456789', 0, 999)",
                handler = expectQueryOutputType(StaticType.STRING)
            ),
            TestCase(
                "function that has optional parameter with missing optional parameter",
                "SUBSTRING('123456789', 0)",
                handler = expectQueryOutputType(StaticType.STRING)
            ),
            TestCase(
                "function that has optional parameter with optional parameter as union type",
                "SUBSTRING('123456789', x, y)",
                mapOf(
                    "x" to StaticType.INT,
                    "y" to AnyOfType(setOf(StaticType.INT, StaticType.NULL))
                ),
                handler = expectQueryOutputType(AnyOfType(setOf(StaticType.STRING, StaticType.NULL)))
            ),
            TestCase(
                "function that has optional parameter with optional parameter as union type",
                "SUBSTRING('123456789', x, y)",
                mapOf(
                    "x" to StaticType.INT,
                    "y" to AnyOfType(setOf(StaticType.INT, StaticType.MISSING))
                ),
                handler = expectQueryOutputType(AnyOfType(setOf(StaticType.STRING, StaticType.MISSING)))
            ),
            TestCase(
                "function that has optional parameter with too many arguments",
                "TO_TIMESTAMP('February 2016', 'MMMM yyyy', 'extra arg')",
                handler = expectSemanticErrors(
                    expectedErrors = listOf(
                        Problem(
                            SourceLocationMeta(1L, 1L),
                            SemanticProblemDetails.IncorrectNumberOfArgumentsToFunctionCall(
                                functionName = "to_timestamp",
                                expectedArity = 1..2,
                                actualArity = 3
                            )
                        )
                    )
                )
            ),
            TestCase(
                "custom function",
                "format('test %d %s', 1, 'a')",
                customFunctionSignatures = listOf(formatFunc.signature),
                handler = expectQueryOutputType(StaticType.STRING)
            ),
            TestCase(
                "non-existent function",
                "non_existent(null)",
                handler = expectSemanticErrors(
                    expectedErrors = listOf(
                        Problem(
                            SourceLocationMeta(1L, 1L),
                            SemanticProblemDetails.NoSuchFunction(
                                functionName = "non_existent"
                            )
                        )
                    )
                )
            ),
            TestCase(
                "LET bindings",
                "SELECT nameLength FROM A LET char_length(A.name) AS nameLength",
                mapOf("A" to BagType(StructType(mapOf("name" to StaticType.STRING)))),
                handler = expectQueryOutputType(BagType(StructType(mapOf("nameLength" to StaticType.INT), contentClosed = true)))
            ),
            TestCase(
                "Multiple unique bindings with LET",
                "SELECT x, y FROM A LET char_length(A.name) AS x, x + 1 AS y",
                mapOf("A" to BagType(StructType(mapOf("name" to StaticType.STRING)))),
                handler = expectQueryOutputType(
                    BagType(StructType(mapOf("x" to StaticType.INT, "y" to StaticType.INT), contentClosed = true))
                )
            ),
            TestCase(
                "Array index with numeric literal ",
                "SELECT a.l[1] AS x FROM a",
                mapOf(
                    "a" to StructType(
                        mapOf("l" to ListType(elementType = StaticType.BOOL))
                    )
                ),
                handler = expectQueryOutputType(
                    BagType(StructType(mapOf("x" to StaticType.BOOL), contentClosed = true))
                )
            ),
            TestCase(
                "Array index with call to operator ",
                "SELECT a.l[1 + 1] AS x FROM a",
                mapOf(
                    "a" to StructType(
                        mapOf("l" to ListType(elementType = StaticType.BOOL))
                    )
                ),
                handler = expectQueryOutputType(
                    BagType(StructType(mapOf("x" to StaticType.BOOL), contentClosed = true))
                )
            ),
            TestCase(
                "Struct index with call to operator ",
                "SELECT a.l[1 + 1] AS x, a.l.y AS p FROM a",
                mapOf(
                    "a" to StructType(
                        mapOf("l" to StructType(mapOf("y" to StaticType.BOOL)))
                    )
                ),
                handler = expectQueryOutputType(
                    BagType(
                        StructType(
                            mapOf("x" to StaticType.MISSING, "p" to StaticType.BOOL),
                            contentClosed = true
                        )
                    )
                )
            ),
            TestCase(
                "Struct pathing ",
                "SELECT a.b.c.d AS x FROM a",
                mapOf(
                    "a" to StructType(
                        mapOf(
                            "b" to StructType(
                                mapOf(
                                    "c" to StructType(
                                        mapOf(
                                            "d" to StaticType.BOOL,
                                            "e" to StaticType.DECIMAL
                                        )
                                    ),
                                    "xx" to StaticType.BLOB
                                )
                            ),
                            "ww" to StaticType.CLOB
                        )
                    )
                ),
                handler = expectQueryOutputType(
                    BagType(StructType(mapOf("x" to StaticType.BOOL), contentClosed = true))
                )
            )
        )

        @JvmStatic
        @Suppress("unused")
        fun parametersForSimplePathsOnStructs(): List<TestCase> {
            val VALID_PATH_EXPR_SOURCES = setOf(StaticType.ANY, StaticType.LIST, StaticType.SEXP, StaticType.STRUCT)

            val incompatibleTypeForB =
                StaticType.ALL_TYPES.filter { it !in VALID_PATH_EXPR_SOURCES }
                    .flatMap { type ->
                        listOf(
                            TestCase(
                                "Simple path on struct: a.b.c",
                                "a.b.c",
                                mapOf(
                                    "a" to StructType(
                                        mapOf("b" to type, "c" to StaticType.INT)
                                    )
                                ),
                                handler = expectQueryOutputType(StaticType.MISSING)
                            ),
                            TestCase(
                                "Simple path on struct: a[b].c",
                                "a['b'].c",
                                mapOf(
                                    "a" to StructType(
                                        mapOf("b" to type, "c" to StaticType.INT)
                                    )
                                ),
                                handler = expectQueryOutputType(StaticType.MISSING)
                            )
                        )
                    }

            val bHasAnyType = StaticType.ALL_TYPES.flatMap {
                listOf(
                    TestCase(
                        "Simple path on struct: a.b.c",
                        "a.b.c",
                        mapOf(
                            "a" to StructType(
                                mapOf("b" to ANY, "c" to it)
                            )
                        ),
                        handler = expectQueryOutputType(StaticType.ANY)
                    ),
                    TestCase(
                        "Simple path on struct: a['b'].c",
                        "a['b'].c",
                        mapOf(
                            "a" to StructType(
                                mapOf("b" to ANY, "c" to it)
                            )
                        ),
                        handler = expectQueryOutputType(StaticType.ANY)
                    )
                )
            }

            val validTypeForB = StaticType.ALL_TYPES.flatMap {
                listOf(
                    TestCase(
                        "Simple path on struct: a.b",
                        "a.b",
                        mapOf(
                            "a" to StructType(
                                mapOf("b" to it)
                            )
                        ),
                        handler = expectQueryOutputType(it)
                    ),
                    TestCase(
                        "Simple path on struct: a['b']",
                        "a['b']",
                        mapOf(
                            "a" to StructType(
                                mapOf("b" to it)
                            )
                        ),
                        handler = expectQueryOutputType(it)
                    )
                )
            }
            return incompatibleTypeForB + bHasAnyType + validTypeForB
        }

        @JvmStatic
        @Suppress("unused")
        fun parametersForSimplePathsOnSequences(): List<TestCase> {
            val INT_TYPES = setOf(StaticType.INT, StaticType.INT2, StaticType.INT4, StaticType.INT8)
            val incompatibleTypeForIndex = StaticType.ALL_TYPES.filter { it !in INT_TYPES }.map {
                TestCase(
                    "simple path for lists a[b] -- b is not INT type",
                    "a[b]",
                    mapOf(
                        "a" to ListType(elementType = StaticType.STRING),
                        "b" to it
                    ),
                    handler = expectQueryOutputType(StaticType.MISSING)
                )
            }

            val validIndexType = StaticType.ALL_TYPES.map { elementType ->
                INT_TYPES.map { intType ->
                    TestCase(
                        "a[b] -- valid type for 'b' varies element type in a ($elementType)",
                        "a[b]",
                        mapOf(
                            "a" to ListType(elementType = elementType),
                            "b" to intType
                        ),
                        handler = expectQueryOutputType(elementType)
                    )
                }
            }.flatten()

            return incompatibleTypeForIndex + validIndexType
        }
        private fun createAggFunctionValidTests(
            functionName: String,
            inputTypes: StaticType,
            expectedType: StaticType
        ): TestCase =
            // testing toplevel aggregated function
            // testing sql function(t)
            // global environment here is a bag type
            TestCase(
                name = "top level $functionName($inputTypes) -> $expectedType",
                originalSql = "$functionName(t)",
                globals = mapOf("t" to BagType(inputTypes)),
                handler = expectQueryOutputType(expectedType)
            )

        @JvmStatic
        @Suppress("unused")
        fun parametersForAggFunctionTests() =
            // valid tests
            listOf(
                // count
                createAggFunctionValidTests("COUNT", NULL, INT8),
                createAggFunctionValidTests("COUNT", MISSING, INT8),
                createAggFunctionValidTests("COUNT", ANY, INT8),
                createAggFunctionValidTests("COUNT", unionOf(NULL, MISSING, INT), INT8),
                createAggFunctionValidTests("COUNT", unionOf(NULL, MISSING, INT), INT8),

                // min
                createAggFunctionValidTests("MIN", MISSING, NULL),
                createAggFunctionValidTests("MIN", NULL, NULL),
                createAggFunctionValidTests("MIN", unionOf(INT, DECIMAL, FLOAT, LIST), unionOf(INT, DECIMAL, FLOAT, LIST)),
                createAggFunctionValidTests("MIN", unionOf(INT, DECIMAL, FLOAT, LIST, NULL, MISSING), unionOf(INT, DECIMAL, FLOAT, LIST, NULL)),

                // max
                createAggFunctionValidTests("MAX", MISSING, NULL),
                createAggFunctionValidTests("MAX", NULL, NULL),
                createAggFunctionValidTests("MAX", unionOf(INT, DECIMAL, FLOAT, STRING), unionOf(INT, DECIMAL, FLOAT, STRING)),
                createAggFunctionValidTests("MAX", unionOf(INT, DECIMAL, FLOAT, STRING, NULL, MISSING), unionOf(INT, DECIMAL, FLOAT, STRING, NULL)),

                // avg
                createAggFunctionValidTests("AVG", MISSING, NULL),
                createAggFunctionValidTests("AVG", unionOf(MISSING, NULL), NULL),
                createAggFunctionValidTests("AVG", unionOf(MISSING, NULL, INT), DECIMAL),
                createAggFunctionValidTests("AVG", unionOf(INT, DECIMAL, FLOAT), DECIMAL),

                // SUM
                createAggFunctionValidTests("SUM", MISSING, NULL),
                createAggFunctionValidTests("SUM", unionOf(MISSING, NULL), NULL),
                createAggFunctionValidTests("SUM", unionOf(MISSING, NULL, INT2), INT2),
                createAggFunctionValidTests("SUM", unionOf(INT2, INT4), INT4),
                createAggFunctionValidTests("SUM", unionOf(INT2, INT4, INT8), INT8),
                createAggFunctionValidTests("SUM", unionOf(INT2, INT4, INT8, FLOAT), FLOAT),
                createAggFunctionValidTests("SUM", unionOf(INT2, INT4, INT8, FLOAT, DECIMAL), DECIMAL),
            ) +
                // sum input type not compatible
                TestCase(
                    name = "data type mismatch SUM(STRING)",
                    originalSql = "SUM(t)",
                    globals = mapOf("t" to BagType(STRING)),
                    handler = expectSemanticProblems(
                        expectedProblems = listOf(
                            createInvalidArgumentTypeForFunctionError(
                                sourceLocation = SourceLocationMeta(1L, 1L, 3L),
                                functionName = "sum",
                                expectedArgType = unionOf(MISSING, NULL, NUMERIC),
                                actualType = STRING
                            )
                        )
                    )
                ) +
                // avg input type not compatible
                TestCase(
                    name = "data type mismatch AVG(STRING)",
                    originalSql = "AVG(t)",
                    globals = mapOf("t" to BagType(STRING)),
                    handler = expectSemanticProblems(
                        expectedProblems = listOf(
                            createInvalidArgumentTypeForFunctionError(
                                sourceLocation = SourceLocationMeta(1L, 1L, 3L),
                                functionName = "avg",
                                expectedArgType = unionOf(MISSING, NULL, NUMERIC),
                                actualType = STRING
                            )
                        )
                    )
                )

        private fun expectQueryOutputType(expectedType: StaticType, expectedWarnings: List<Problem> = emptyList()): (ResolveTestResult) -> Unit = { result: ResolveTestResult ->
            when (result) {
                is ResolveTestResult.Failure -> fail("Expected value, not failure.  Exception(s): \n${result.problems}")
                is ResolveTestResult.Value -> {
                    assertEquals("Failed assertion for \"${result.testCase.name}\"", expectedType, result.staticType)
                    val actualWarnings = result.problems // these should all be warnings
                    assertEquals(
                        "Expected ${expectedWarnings.size} warnings but received ${actualWarnings.size} warnings",
                        expectedWarnings.size, actualWarnings.size
                    )
                    assertEquals(expectedWarnings.toSet(), actualWarnings.toSet())
                }
            }.let { }
        }

        private fun expectSemanticErrors(expectedErrors: List<Problem>): (ResolveTestResult) -> Unit = { result: ResolveTestResult ->
            when (result) {
                is ResolveTestResult.Value -> fail("Expected failure for \"${result.testCase.name}\" but got $result")
                is ResolveTestResult.Failure -> {
                    val actualErrors = result.problems.filter { it.details.severity == ProblemSeverity.ERROR }
                    assertEquals(
                        "Expected ${expectedErrors.size} errors but received ${actualErrors.size} errors",
                        expectedErrors.size, actualErrors.size
                    )
                    assertEquals(expectedErrors.toSet(), actualErrors.toSet())
                }
            }.let { }
        }

        private fun expectQueryOutputTypeAndProblems(expectedType: StaticType, expectedProblems: List<Problem>): (ResolveTestResult) -> Unit = { result: ResolveTestResult ->
            when (result) {
                is ResolveTestResult.Value -> fail("Expected failure for \"${result.testCase.name}\" but got $result")
                is ResolveTestResult.Failure -> {
                    assertEquals("Failed assertion for \"${result.testCase.name}\"", expectedType, result.staticType)
                    val actualProblems = result.problems
                    assertEquals(
                        "Expected ${expectedProblems.size} problems but received ${actualProblems.size} problems",
                        expectedProblems.size, actualProblems.size
                    )
                    assertEquals(expectedProblems.toSet(), actualProblems.toSet())
                }
            }.let { }
        }

        private fun expectProblemsAndAssert(
            expectedProblems: List<Problem>,
            assertionBlock: (PartiqlAst.Statement) -> Unit
        ): (ResolveTestResult) -> Unit = { result: ResolveTestResult ->
            when (result) {
                is ResolveTestResult.Value -> fail("Expected failure for \"${result.testCase.name}\" but got $result")
                is ResolveTestResult.Failure -> {
                    val actualProblems = result.problems
                    assertEquals(
                        "Expected ${expectedProblems.size} problems but received ${actualProblems.size} problems",
                        expectedProblems.size, actualProblems.size
                    )
                    assertEquals(expectedProblems.toSet(), actualProblems.toSet())

                    // additional assertions using the annotated [PartiqlAst.Statement]
                    assertionBlock(result.partiqlAst)
                }
            }
        }

        private fun expectSemanticProblems(expectedProblems: List<Problem>): (ResolveTestResult) -> Unit = { result: ResolveTestResult ->
            val actualProblems = result.problems
            assertEquals(
                "Expected ${expectedProblems.size} problems but received ${actualProblems.size} problems",
                expectedProblems.size, actualProblems.size
            )
            assertEquals(expectedProblems.toSet(), actualProblems.toSet())
        }

        // A shared type that is used across multiple tests
        private val customerType = BagType(
            StructType(
                mapOf(
                    "firstName" to StaticType.STRING,
                    "lastName" to StaticType.STRING,
                    "age" to StaticType.INT2,
                    "address" to StructType(
                        mapOf(
                            "streetName" to StaticType.STRING,
                            "city" to StaticType.STRING
                        )
                    ),
                    "orders" to ListType(
                        StructType(
                            mapOf(
                                "orderId" to StaticType.STRING,
                                "orderDate" to StaticType.TIMESTAMP
                            )
                        )
                    )
                ),
                true
            )
        )

        private val customTypedOpParameters = mapOf(
            "es_integer" to TypedOpParameter(IntType(IntType.IntRangeConstraint.LONG)),
            "es_float" to TypedOpParameter(StaticType.FLOAT) {
                // For the sake of this test, lets say ES_FLOAT only allows values in range (-100, 100)
                it.numberValue() < 100L && it.numberValue() > -100L
            }
        )

        data class TestCase(
            val name: String,
            val originalSql: String,
            val globals: Map<String, StaticType> = mapOf(),
            val customFunctionSignatures: List<FunctionSignature> = listOf(),
            val handler: (ResolveTestResult) -> Unit
        ) {
            override fun toString(): String = this.name
        }

        sealed class ResolveTestResult {
            abstract val problems: List<Problem>
            data class Value(val testCase: TestCase, val staticType: StaticType, override val problems: List<Problem>) : ResolveTestResult()
            data class Failure(val testCase: TestCase, val staticType: StaticType, val partiqlAst: PartiqlAst.Statement, override val problems: List<Problem>) : ResolveTestResult()
        }

        private val formatFunc = object : ExprFunction {

            override val signature = FunctionSignature(
                name = "format",
                requiredParameters = listOf(StaticType.STRING),
                variadicParameter = VarargFormalParameter(StaticType.ANY, 0),
                returnType = StaticType.STRING
            )
        }
    }
}
