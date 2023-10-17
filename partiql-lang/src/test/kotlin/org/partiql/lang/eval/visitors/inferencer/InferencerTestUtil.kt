package org.partiql.lang.eval.visitors.inferencer

import com.amazon.ion.system.IonSystemBuilder
import org.partiql.errors.Problem
import org.partiql.errors.ProblemLocation
import org.partiql.errors.ProblemSeverity
import org.partiql.lang.ast.SourceLocationMeta
import org.partiql.lang.ast.passes.SemanticProblemDetails
import org.partiql.lang.ast.passes.inference.StaticTypeInferencer
import org.partiql.lang.ast.passes.inference.isLob
import org.partiql.lang.ast.passes.inference.isNumeric
import org.partiql.lang.ast.passes.inference.isText
import org.partiql.lang.ast.passes.inference.isUnknown
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.eval.Bindings
import org.partiql.lang.eval.ExprFunction
import org.partiql.lang.eval.numberValue
import org.partiql.lang.eval.visitors.StaticTypeVisitorTransform
import org.partiql.lang.eval.visitors.VisitorTransformTestBase
import org.partiql.lang.eval.visitors.basicVisitorTransforms
import org.partiql.lang.types.FunctionSignature
import org.partiql.lang.types.TypedOpParameter
import org.partiql.lang.util.cartesianProduct
import org.partiql.lang.util.compareTo
import org.partiql.lang.util.countMatchingSubstrings
import org.partiql.types.BagType
import org.partiql.types.BoolType
import org.partiql.types.CollectionType
import org.partiql.types.IntType
import org.partiql.types.ListType
import org.partiql.types.StaticType
import org.partiql.types.StructType

object InferencerTestUtil : VisitorTransformTestBase() {

    const val TOKEN = "{op}"

    val ALL_UNKNOWN_TYPES = listOf(StaticType.NULL, StaticType.MISSING, StaticType.NULL_OR_MISSING)
    val ALL_NON_UNKNOWN_TYPES = StaticType.ALL_TYPES.filter { !it.isUnknown() }
    val ALL_NUMERIC_TYPES = ALL_NON_UNKNOWN_TYPES.filter { it.isNumeric() }
    val ALL_NON_NUMERIC_NON_UNKNOWN_TYPES = ALL_NON_UNKNOWN_TYPES.filter { !it.isNumeric() }
    val ALL_TEXT_TYPES = ALL_NON_UNKNOWN_TYPES.filter { it.isText() }
    val ALL_NON_TEXT_NON_UNKNOWN_TYPES = ALL_NON_UNKNOWN_TYPES.filter { !it.isText() }
    val ALL_NON_BOOL_NON_UNKNOWN_TYPES = ALL_NON_UNKNOWN_TYPES.filter { it !is BoolType }
    val ALL_LOB_TYPES = ALL_NON_UNKNOWN_TYPES.filter { it.isLob() }
    val ALL_NON_LOB_NON_UNKNOWN_TYPES = ALL_NON_UNKNOWN_TYPES.filter { !it.isLob() }
    val ALL_NON_COLLECTION_NON_UNKNOWN_TYPES = ALL_NON_UNKNOWN_TYPES.filter { it !is CollectionType }

    // non-unknown [StaticType]s from ALL_TYPES that aren't numeric, text, or lobs
    // This will include all the container types, BOOL, and TIMESTAMP. These are only comparable to unknowns and
    // itself.
    val ALL_TYPES_ONLY_COMPARABLE_TO_SELF = ALL_NON_UNKNOWN_TYPES.filter { !it.isNumeric() && !it.isText() && !it.isLob() }

    enum class OpType(vararg ops: String) {
        ARITHMETIC("+", "-", "*", "/", "%"),
        COMPARISON("<", "<=", ">", ">="),
        EQUALITY("!=", "="),
        LOGICAL("AND", "OR"),
        CONCAT("||");

        val operators = ops.toList()
    }

    // A shared type that is used across multiple tests
    val customerType = BagType(
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

    val customTypedOpParameters = mapOf(
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

    val formatFunc = object : ExprFunction {

        override val signature = FunctionSignature(
            name = "format",
            requiredParameters = listOf(StaticType.STRING, StaticType.LIST),
            returnType = StaticType.STRING
        )
    }

    /**
     * From the passed lists, [l1] and [l2], returns a list of all unique pairs of permutations taking one element
     * from [l1] and one element from [l2]. This is used when generating NAry op [TestCase]s in which each operand
     * could take on many values.
     *
     * E.g. [l1] = [a, b], [l2] = [b, c]
     *   => [<a, b>, <b, a>, <b, b>, <b, c>, <c, b>]
     */
    fun generateAllUniquePairs(l1: List<StaticType>, l2: List<StaticType>): List<Pair<StaticType, StaticType>> =
        listOf(l1, l2).cartesianProduct()
            .flatMap { listOf(Pair(it[0], it[1]), Pair(it[1], it[0])) }
            .distinct()

    fun crossExpand(template: String, operators: List<String>): List<String> =
        when (template.countMatchingSubstrings(TOKEN)) {
            0 -> listOf(template)
            else -> {
                operators.flatMap {
                    val newTemplate = template.replaceFirst(TOKEN, it)
                    crossExpand(newTemplate, operators)
                }
            }
        }

    private fun expectedSemanticErrors(expectedErrors: List<Problem>): (ResolveTestResult) -> Unit = { result: ResolveTestResult ->
        val actualErrors = result.problems.filter { it.details.severity == ProblemSeverity.ERROR }
        assertEquals(
            "Expected ${expectedErrors.size} errors but received ${actualErrors.size} errors",
            expectedErrors.size, actualErrors.size
        )
        assertEquals(expectedErrors.toSet(), actualErrors.toSet())
    }

    private fun expectedSemanticWarnings(expectedWarnings: List<Problem>): (ResolveTestResult) -> Unit = { result: ResolveTestResult ->
        val actualProblems = result.problems.filter { it.details.severity == ProblemSeverity.WARNING }
        assertEquals(
            "Expected ${expectedWarnings.size} problems but received ${actualProblems.size} problems",
            expectedWarnings.size, actualProblems.size
        )
        assertEquals(expectedWarnings.toSet(), actualProblems.toSet())
    }

    fun expectSemanticProblems(expectedWarnings: List<Problem> = emptyList(), expectedErrors: List<Problem> = emptyList()): (ResolveTestResult) -> Unit = { result: ResolveTestResult ->
        when (result) {
            is ResolveTestResult.Value -> if (expectedErrors.isNotEmpty()) {
                fail("Expected failure for \"${result.testCase.name}\" but got $result")
            } else {
                expectedSemanticWarnings(expectedWarnings).invoke(result)
            }
            is ResolveTestResult.Failure -> {
                expectedSemanticErrors(expectedErrors).invoke(result)
                expectedSemanticWarnings(expectedWarnings).invoke(result)
            }
        }
    }

    fun expectQueryOutputType(expectedType: StaticType, expectedWarnings: List<Problem> = emptyList(), expectedErrors: List<Problem> = emptyList()): (ResolveTestResult) -> Unit = { result: ResolveTestResult ->
        when (result) {
            is ResolveTestResult.Value -> if (expectedErrors.isNotEmpty()) {
                fail("Expected success for \"${result.testCase.name}\" but got $result")
            } else {
                assertEquals("Failed assertion for \"${result.testCase.name}\"", expectedType, result.staticType)
                expectedSemanticWarnings(expectedWarnings).invoke(result)
            }
            is ResolveTestResult.Failure -> {
                assertEquals("Failed assertion for \"${result.testCase.name}\"", expectedType, result.staticType)
                expectedSemanticErrors(expectedErrors).invoke(result)
                expectedSemanticWarnings(expectedWarnings).invoke(result)
            }
        }.let { }
    }

    fun expectProblemsAndAssert(
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

    fun runTest(tc: TestCase) = forEachTarget {
        val globalBindings = Bindings.ofMap(tc.globals)
        val ion = IonSystemBuilder.standard().build()
        val inferencer = StaticTypeInferencer(
            globalBindings = globalBindings,
            customFunctionSignatures = tc.customFunctionSignatures,
            customTypedOpParameters = customTypedOpParameters
        )

        val defaultVisitorTransforms = basicVisitorTransforms()
        val staticTypeVisitorTransform = StaticTypeVisitorTransform(ion, globalBindings)
        val originalStatement = parser.parseAstStatement(tc.originalSql).let {
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
                is StaticTypeInferencer.InferenceResult.Success -> ResolveTestResult.Value(tc, inferenceResult.staticType, inferenceResult.problems)
                is StaticTypeInferencer.InferenceResult.Failure -> ResolveTestResult.Failure(tc, inferenceResult.staticType, inferenceResult.partiqlAst, inferenceResult.problems)
            }
        )
    }

    /**
     * Creates ternary op cases with different operators of [opType]
     */
    fun createDoubleNAryOpCases(
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

    fun createNAryOpCases(
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
    fun createSingleNAryOpCasesWithSwappedArgs(
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
     * Creates a [TestCase] with the query "x [op] y" with x bound to [leftType] and y to [rightType]. This
     * [TestCase] expects [expectedErrors] during inference.
     */
    fun singleNAryOpErrorTestCase(
        name: String,
        op: String,
        leftType: StaticType,
        rightType: StaticType,
        expectedErrors: List<Problem>,
        expectedWarnings: List<Problem> = emptyList()
    ) =
        TestCase(
            name = "x $op y : $name",
            originalSql = "x $op y",
            globals = mapOf(
                "x" to leftType,
                "y" to rightType
            ),
            handler = expectSemanticProblems(expectedErrors = expectedErrors, expectedWarnings = expectedWarnings)
        )

    fun singleNAryTestCase(
        name: String,
        op: String,
        leftType: StaticType,
        rightType: StaticType,
        expectedType: StaticType,
        expectedWarnings: List<Problem>
    ) = TestCase(
        name = "x $op y : $name",
        originalSql = "x $op y",
        globals = mapOf(
            "x" to leftType,
            "y" to rightType
        ),
        handler = expectQueryOutputType(expectedType = expectedType, expectedWarnings = expectedWarnings)
    )

    fun doubleOpTestCases(
        name: String,
        op: String,
        leftType: StaticType,
        middleType: StaticType,
        rightType: StaticType,
        expectedType: StaticType,
        expectedWarnings: List<Problem> = emptyList()
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
            handler = expectQueryOutputType(expectedType = expectedType, expectedWarnings = expectedWarnings)
        )
    }

    /**
     * Creates one [TestCase] with the specified binary [op] in the query "x [op] y [op] z" with `x` corresponding
     * to [leftType], `y` corresponding to [middleType], and `z` corresponding to [rightType]. The created
     * [TestCase] expects [expectedErrors] through inference.
     */
    fun doubleOpErrorCases(
        name: String,
        op: String,
        leftType: StaticType,
        middleType: StaticType,
        rightType: StaticType,
        expectedErrors: List<Problem> = emptyList(),
        expectedWarnings: List<Problem> = emptyList()
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
            handler = expectSemanticProblems(expectedWarnings, expectedErrors)
        )
    }

    /**
     * Creates [TestCase]s with the query "x [op] y" with x bound to [leftType] and [rightType]. Each [TestCase]
     * expects to find [SemanticProblemDetails.IncompatibleDatatypesForOp].
     *
     * If [leftType] != [rightType], then new [TestCase] "y [op] x" is created.
     */
    fun singleNAryOpMismatchWithSwappedCases(
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
                expectedErrors = listOf(createDataTypeMismatchError(col = 3, argTypes = listOf(leftType, rightType), nAryOp = op))
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
                        expectedErrors = listOf(createDataTypeMismatchError(col = 3, argTypes = listOf(rightType, leftType), nAryOp = op))
                    )
                )
            )
        }
    }

    fun createReturnsMissingError(line: Long = 1, col: Long, nAryOp: String): Problem =
        Problem(
            ProblemLocation(line, col, nAryOp.length.toLong()),
            SemanticProblemDetails.ExpressionAlwaysReturnsMissing
        )

    fun createReturnsMissingError(sourceLocation: SourceLocationMeta): Problem =
        Problem(
            sourceLocation.toProblemLocation(),
            SemanticProblemDetails.ExpressionAlwaysReturnsMissing
        )

    fun createReturnsNullOrMissingWarning(line: Long = 1, col: Long, nAryOp: String): Problem =
        Problem(
            ProblemLocation(line, col, nAryOp.length.toLong()),
            SemanticProblemDetails.ExpressionAlwaysReturnsMissingOrNull
        )

    fun createReturnsNullOrMissingWarning(sourceLocation: SourceLocationMeta): Problem =
        Problem(
            sourceLocation.toProblemLocation(),
            SemanticProblemDetails.ExpressionAlwaysReturnsMissingOrNull
        )

    fun createDataTypeMismatchError(line: Long = 1, col: Long, argTypes: List<StaticType>, nAryOp: String): Problem =
        Problem(
            ProblemLocation(line, col, nAryOp.length.toLong()),
            SemanticProblemDetails.IncompatibleDatatypesForOp(actualArgumentTypes = argTypes, nAryOp = nAryOp)
        )

    fun createDataTypeMismatchError(sourceLocation: SourceLocationMeta, argTypes: List<StaticType>, nAryOp: String): Problem =
        Problem(
            sourceLocation.toProblemLocation(),
            SemanticProblemDetails.IncompatibleDatatypesForOp(actualArgumentTypes = argTypes, nAryOp = nAryOp)
        )

    fun createIncompatibleTypesForExprError(sourceLocation: SourceLocationMeta, expectedType: StaticType, actualType: StaticType): Problem =
        Problem(
            sourceLocation.toProblemLocation(),
            SemanticProblemDetails.IncompatibleDataTypeForExpr(expectedType, actualType)
        )

    fun createInvalidArgumentTypeForFunctionError(
        sourceLocation: SourceLocationMeta,
        functionName: String,
        expectedArgType: StaticType,
        actualType: StaticType
    ): Problem =
        Problem(
            sourceLocation.toProblemLocation(),
            SemanticProblemDetails.InvalidArgumentTypeForFunction(functionName, expectedArgType, actualType)
        )

    fun createNullOrMissingFunctionArgumentError(sourceLocation: SourceLocationMeta, functionName: String): Problem =
        Problem(
            sourceLocation.toProblemLocation(),
            SemanticProblemDetails.NullOrMissingFunctionArgument(functionName)
        )
}
