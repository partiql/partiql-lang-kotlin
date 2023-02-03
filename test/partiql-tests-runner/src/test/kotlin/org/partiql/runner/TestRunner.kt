/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *       http://aws.amazon.com/apache2.0/
 *
 *  or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 *  language governing permissions and limitations under the License.
 */

package org.partiql.runner

import com.amazon.ion.IonType
import com.amazon.ion.system.IonSystemBuilder
import com.amazon.ion.system.IonTextWriterBuilder
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.TestWatcher
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.CompilerPipeline
import org.partiql.lang.SqlException
import org.partiql.lang.eval.CompileOptions
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.TypingMode
import org.partiql.lang.eval.toIonValue
import java.io.File

private val PARTIQL_EVAL_TEST_DATA_DIR = System.getenv("PARTIQL_EVAL_TESTS_DATA")
private val PARTIQL_EVAL_EQUIV_TEST_DATA_DIR = System.getenv("PARTIQL_EVAL_EQUIV_TESTS_DATA")

private val ION = IonSystemBuilder.standard().build()

private val COERCE_EVAL_MODE_COMPILE_OPTIONS = CompileOptions.build { typingMode(TypingMode.PERMISSIVE) }
private val ERROR_EVAL_MODE_COMPILE_OPTIONS = CompileOptions.build { typingMode(TypingMode.LEGACY) }

class ConformanceTestReportGenerator : TestWatcher, AfterAllCallback {
    var failingTests = emptySet<String>()
    var passingTests = emptySet<String>()
    var ignoredTests = emptySet<String>()
    override fun testFailed(context: ExtensionContext?, cause: Throwable?) {
        failingTests += context?.displayName ?: ""
        super.testFailed(context, cause)
    }

    override fun testSuccessful(context: ExtensionContext?) {
        passingTests += context?.displayName ?: ""
        super.testSuccessful(context)
    }

    override fun afterAll(p0: ExtensionContext?) {
        val file = File("./conformance_test_results.ion")
        val outputStream = file.outputStream()
        val writer = IonTextWriterBuilder.pretty().build(outputStream)
        writer.stepIn(IonType.STRUCT) // in: outer struct

        // set struct field for passing
        writer.setFieldName("passing")
        writer.stepIn(IonType.LIST)
        passingTests.forEach { passingTest ->
            writer.writeString(passingTest)
        }
        writer.stepOut()
        // set struct field for failing
        writer.setFieldName("failing")
        writer.stepIn(IonType.LIST)
        failingTests.forEach { failingTest ->
            writer.writeString(failingTest)
        }
        writer.stepOut()

        // set struct field for ignored
        writer.setFieldName("ignored")
        writer.stepIn(IonType.LIST)
        ignoredTests.forEach { ignoredTest ->
            writer.writeString(ignoredTest)
        }
        writer.stepOut()

        writer.stepOut() // out: outer struct
    }
}

/*
The fail lists defined in this file show how the current Kotlin implementation diverges from the PartiQL spec. Most of
the divergent behavior is due to `partiql-lang-kotlin` not having a STRICT typing mode/ERROR eval mode.  The
[LEGACY typing mode](https://github.com/partiql/partiql-lang-kotlin/blob/main/lang/src/org/partiql/lang/eval/CompileOptions.kt#L53-L62)
(which is closer to STRICT typing mode/ERROR eval mode but not a complete match) was used for testing the STRICT typing
mode/ERROR eval mode.

A lot of the other behavior differences is due to not supporting some syntax mentioned in the spec (like `COLL_*`
aggregation functions) and due to not supporting coercions.

The remaining divergent behavior causing certain conformance tests to fail are likely bugs. Tracking issue:
https://github.com/partiql/partiql-lang-kotlin/issues/804.
 */
private val LANG_KOTLIN_EVAL_FAIL_LIST = listOf(
    // from the spec: no explicit CAST to string means the query is "treated as an array navigation with wrongly typed
    // data" and will return `MISSING`
    Pair("tuple navigation with array notation without explicit CAST to string", COERCE_EVAL_MODE_COMPILE_OPTIONS),
    // same as above, but since in error mode, should give an error
    Pair("tuple navigation with array notation without explicit CAST to string", ERROR_EVAL_MODE_COMPILE_OPTIONS),

    // for the following, partiql-lang-kotlin doesn't have a STRICT typing mode/ERROR eval mode. tested using
    // partiql-lang-kotlin's LEGACY typing mode, which has some semantic differences from STRICT typing mode/ERROR eval
    // mode.
    Pair("path on string", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("tuple navigation missing attribute dot notation", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("tuple navigation missing attribute array notation", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("single source FROM with bag and AT clause", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("single source FROM with scalar and AT clause", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("single source FROM with tuple and AT clause", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("single source FROM with absent value null and AT clause", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("single source FROM with absent value missing and AT clause", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("cast and operations with missing argument", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("missing value in arithmetic expression", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("equality of scalar missing", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("arithmetic with null/missing", ERROR_EVAL_MODE_COMPILE_OPTIONS),

    // TODO: clarify behavior. spec (section 8) says it should return NULL based on 3-value logic
    Pair("missing and true", COERCE_EVAL_MODE_COMPILE_OPTIONS),

    // partiql-lang-kotlin doesn't currently implement subquery coercion. The inner SFW query returns a bag of two
    // elements that when coerced to a scalar should return MISSING in COERCE mode. As a result, `customerName` should
    // be missing from the first tuple.
    Pair("inner select evaluating to collection with more than one element", COERCE_EVAL_MODE_COMPILE_OPTIONS),

    // coll_* aggregate functions not supported in partiql-lang-kotlin -- results in parser error. coll_* functions
    // will be supported in https://github.com/partiql/partiql-lang-kotlin/issues/222
    Pair("coll_count without group by", COERCE_EVAL_MODE_COMPILE_OPTIONS),
    Pair("coll_count without group by", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("coll_count with result of subquery", COERCE_EVAL_MODE_COMPILE_OPTIONS),
    Pair("coll_count with result of subquery", ERROR_EVAL_MODE_COMPILE_OPTIONS),

    // WITH keyword not supported resulting in parse error
    Pair("windowing simplified with grouping", COERCE_EVAL_MODE_COMPILE_OPTIONS),
    Pair("windowing simplified with grouping", ERROR_EVAL_MODE_COMPILE_OPTIONS),

    // partiql-lang-kotlin doesn't have STRICT typing mode/ERROR eval mode. LEGACY mode used which doesn't error when
    // RHS of `IN` expression is not a bag, list, or sexp
    Pair("notInPredicateSingleExpr", ERROR_EVAL_MODE_COMPILE_OPTIONS),

    // PartiQL Test Suite (PTS, https://github.com/partiql/partiql-lang-kotlin/tree/main/test/partiql-pts) tests:
    // partiql-lang-kotlin doesn't have STRICT typing mode/ERROR eval mode. LEGACY mode used which propagates NULL
    // rather than missing
    Pair("""char_length null and missing propagation{in:"missing",result:(success missing::null)}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""character_length null and missing propagation{in:"missing",result:(success missing::null)}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""trim null and missing propagation{sql:"trim(missing)"}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""trim null and missing propagation{sql:"trim(leading from missing)"}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""trim null and missing propagation{sql:"trim(trailing from missing)"}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""trim null and missing propagation{sql:"trim(both from missing)"}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""trim null and missing propagation{sql:"trim(leading '' from missing)"}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""trim null and missing propagation{sql:"trim(trailing '' from missing)"}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""trim null and missing propagation{sql:"trim(both '' from missing)"}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""trim null and missing propagation{sql:"trim(leading missing from '')"}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""trim null and missing propagation{sql:"trim(trailing missing from '')"}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""trim null and missing propagation{sql:"trim(both missing from '')"}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""trim null and missing propagation{sql:"trim(leading null from missing)"}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""trim null and missing propagation{sql:"trim(trailing null from missing)"}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""trim null and missing propagation{sql:"trim(both null from missing)"}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""trim null and missing propagation{sql:"trim(leading missing from null)"}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""trim null and missing propagation{sql:"trim(trailing missing from null)"}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""trim null and missing propagation{sql:"trim(both missing from null)"}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""trim null and missing propagation{sql:"trim(leading missing from missing)"}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""trim null and missing propagation{sql:"trim(trailing missing from missing)"}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""trim null and missing propagation{sql:"trim(both missing from missing)"}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""substring null and missing propagation 2 arguments{target:"missing",start_pos:"1"}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""substring null and missing propagation 2 arguments{target:"''",start_pos:"missing"}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""substring null and missing propagation 2 arguments{target:"missing",start_pos:"missing"}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""substring null and missing propagation 2 arguments{target:"null",start_pos:"missing"}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""substring null and missing propagation 2 arguments{target:"missing",start_pos:"null"}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""substring null and missing propagation 3 arguments{target:"null",start_pos:"1",quantity:"missing"}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""substring null and missing propagation 3 arguments{target:"null",start_pos:"null",quantity:"missing"}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""substring null and missing propagation 3 arguments{target:"null",start_pos:"missing",quantity:"1"}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""substring null and missing propagation 3 arguments{target:"null",start_pos:"missing",quantity:"null"}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""substring null and missing propagation 3 arguments{target:"null",start_pos:"missing",quantity:"missing"}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""substring null and missing propagation 3 arguments{target:"missing",start_pos:"1",quantity:"1"}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""substring null and missing propagation 3 arguments{target:"missing",start_pos:"1",quantity:"null"}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""substring null and missing propagation 3 arguments{target:"missing",start_pos:"1",quantity:"missing"}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""substring null and missing propagation 3 arguments{target:"missing",start_pos:"null",quantity:"1"}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""substring null and missing propagation 3 arguments{target:"missing",start_pos:"null",quantity:"null"}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""substring null and missing propagation 3 arguments{target:"missing",start_pos:"null",quantity:"missing"}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""substring null and missing propagation 3 arguments{target:"missing",start_pos:"missing",quantity:"1"}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""substring null and missing propagation 3 arguments{target:"missing",start_pos:"missing",quantity:"null"}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""substring null and missing propagation 3 arguments{target:"missing",start_pos:"missing",quantity:"missing"}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""substring null and missing propagation 3 arguments{target:"''",start_pos:"1",quantity:"missing"}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""substring null and missing propagation 3 arguments{target:"''",start_pos:"null",quantity:"missing"}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""substring null and missing propagation 3 arguments{target:"''",start_pos:"missing",quantity:"1"}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""substring null and missing propagation 3 arguments{target:"''",start_pos:"missing",quantity:"null"}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""substring null and missing propagation 3 arguments{target:"''",start_pos:"missing",quantity:"missing"}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""upper null and missing propagation{param:"missing"}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""lower null and missing propagation{param:"missing"}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""extract null and missing propagation{time_part:"year",timestamp:"missing"}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""|| valid cases{lparam:"null",rparam:"missing",result:missing::null}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""|| valid cases{lparam:"missing",rparam:"null",result:missing::null}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""|| valid cases{lparam:"missing",rparam:"'b'",result:missing::null}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""|| valid cases{lparam:"'a'",rparam:"missing",result:missing::null}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""|| valid cases{lparam:"missing",rparam:"missing",result:missing::null}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""concatenation with null values{left:"MISSING",right:"MISSING"}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""concatenation with null values{left:"''",right:"MISSING"}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""concatenation with null values{left:"MISSING",right:"''"}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""concatenation with null values{left:"'a'",right:"MISSING"}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""concatenation with null values{left:"MISSING",right:"'b'"}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    // similar to above partiql-lang-kotlin doesn't have an STRICT typing mode/ERROR eval mode; its LEGACY mode
    // propagates NULL rather than MISSING
    Pair("""null comparison{sql:"MISSING = NULL",result:missing::null}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""null comparison{sql:"NULL = MISSING",result:missing::null}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""null comparison{sql:"`null.null` = MISSING",result:missing::null}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""null comparison{sql:"`null.bool` = MISSING",result:missing::null}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""null comparison{sql:"`null.int` = MISSING",result:missing::null}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""null comparison{sql:"`null.decimal` = MISSING",result:missing::null}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""null comparison{sql:"`null.string` = MISSING",result:missing::null}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""null comparison{sql:"`null.symbol` = MISSING",result:missing::null}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""null comparison{sql:"`null.clob` = MISSING",result:missing::null}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""null comparison{sql:"`null.blob` = MISSING",result:missing::null}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""null comparison{sql:"`null.list` = MISSING",result:missing::null}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""null comparison{sql:"`null.struct` = MISSING",result:missing::null}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""null comparison{sql:"`null.sexp` = MISSING",result:missing::null}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
)

private val LANG_KOTLIN_EVAL_EQUIV_FAIL_LIST = listOf(
    // partiql-lang-kotlin gives a parser error for tuple path navigation in which the path expression is a string
    // literal
    // e.g. { 'a': 1, 'b': 2}.'a' -> 1 (see section 4 of spec)
    Pair("equiv tuple path navigation with array notation", COERCE_EVAL_MODE_COMPILE_OPTIONS),
    Pair("equiv tuple path navigation with array notation", ERROR_EVAL_MODE_COMPILE_OPTIONS),

    // partiql-lang-kotlin doesn't support a STRICT typing mode/ERROR eval mode.
    Pair("equiv attribute value pair unpivot non-missing", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("equiv attribute value pair unpivot missing", ERROR_EVAL_MODE_COMPILE_OPTIONS),

    // partiql-lang-kotlin doesn't support `LATERAL` keyword which results in a parser error
    Pair("equiv of comma, cross join, and join", COERCE_EVAL_MODE_COMPILE_OPTIONS),
    Pair("equiv of comma, cross join, and join", ERROR_EVAL_MODE_COMPILE_OPTIONS),

    // partiql-lang-kotlin doesn't support `TUPLEUNION` function which results in an evaluation error
    Pair("equiv tupleunion with select list", COERCE_EVAL_MODE_COMPILE_OPTIONS),
    Pair("equiv tupleunion with select list", ERROR_EVAL_MODE_COMPILE_OPTIONS),

    // partiql-lang-kotlin doesn't support coercion of subqueries which results in different outputs
    Pair("equiv coercion of a SELECT subquery into a scalar", COERCE_EVAL_MODE_COMPILE_OPTIONS),
    Pair("equiv coercion of a SELECT subquery into a scalar", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("equiv coercion of a SELECT subquery into an array", COERCE_EVAL_MODE_COMPILE_OPTIONS),
    Pair("equiv coercion of a SELECT subquery into an array", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("equiv coercions with explicit literals", COERCE_EVAL_MODE_COMPILE_OPTIONS),
    Pair("equiv coercions with explicit literals", ERROR_EVAL_MODE_COMPILE_OPTIONS),

    // partiql-lang-kotlin doesn't support `GROUP ALL` and `COLL_*` aggregate functions. Currently, results in a parser
    // error
    Pair("equiv group_all", COERCE_EVAL_MODE_COMPILE_OPTIONS),
    Pair("equiv group_all", ERROR_EVAL_MODE_COMPILE_OPTIONS),

    // partiql-lang-kotlin doesn't support `COLL_*` aggregate functions. Currently, results in an evaluation error
    Pair("equiv group by with aggregates", COERCE_EVAL_MODE_COMPILE_OPTIONS),
    Pair("equiv group by with aggregates", ERROR_EVAL_MODE_COMPILE_OPTIONS),

    // partiql-lang-kotlin doesn't support using aliases created in select list in `GROUP BY` (and `ORDER BY`). GH
    // issue to track:
    // https://github.com/partiql/partiql-lang-kotlin/issues/571
    Pair("equiv aliases from select clause", COERCE_EVAL_MODE_COMPILE_OPTIONS),
    Pair("equiv aliases from select clause", ERROR_EVAL_MODE_COMPILE_OPTIONS),
)

/**
 * Checks all the PartiQL conformance test data in [PARTIQL_EVAL_TEST_DATA_DIR] conforms to the test data schema.
 */
class TestRunner {
    private fun parseTestFile(file: File): Namespace {
        val loadedData = file.readText()
        val dataInIon = ION.loader.load(loadedData)
        val emptyNamespace = Namespace(
            env = ION.newEmptyStruct(),
            namespaces = mutableListOf(),
            testCases = mutableListOf(),
            equivClasses = mutableMapOf()
        )
        dataInIon.forEach { d ->
            parseNamespace(emptyNamespace, d)
        }
        return emptyNamespace
    }

    private fun allTestsFromNamespace(ns: Namespace): List<TestCase> {
        return ns.testCases + ns.namespaces.fold(listOf()) { acc, subns ->
            acc + allTestsFromNamespace(subns)
        }
    }

    private fun loadTests(path: String): List<TestCase> {
        val allFiles = File(path).walk()
            .filter { it.isFile }
            .filter { it.path.endsWith(".ion") }
            .toList()
        val filesAsNamespaces = allFiles.map { file ->
            parseTestFile(file)
        }

        val allTestCases = filesAsNamespaces.flatMap { ns ->
            allTestsFromNamespace(ns)
        }
        return allTestCases
    }

    private fun runEvalTestCase(evalTC: EvalTestCase, expectedFailedTests: List<Pair<String, CompileOptions>>) {
        val compilerPipeline = CompilerPipeline.builder().compileOptions(evalTC.compileOptions).build()
        val globals = ExprValue.of(evalTC.env).bindings
        val session = EvaluationSession.build { globals(globals) }
        try {
            val expression = compilerPipeline.compile(evalTC.statement)
            val actualResult = expression.eval(session)
            when (evalTC.assertion) {
                is Assertion.EvaluationSuccess -> {
                    val actualResultAsIon = actualResult.toIonValue(ION)
                    if (!expectedFailedTests.contains(Pair(evalTC.name, evalTC.compileOptions)) && !PartiQLEqualityChecker().areEqual(evalTC.assertion.expectedResult, actualResultAsIon)) {
                        val testName = evalTC.name
                        val evalMode = when (evalTC.compileOptions.typingMode) {
                            TypingMode.PERMISSIVE -> "COERCE_EVAL_MODE_COMPILE_OPTIONS"
                            TypingMode.LEGACY -> "ERROR_EVAL_MODE_COMPILE_OPTIONS"
                        }
                        error("Pair(\"\"\"$testName\"\"\", $evalMode),\nExpected and actual results differ:\nExpected: ${evalTC.assertion.expectedResult}\nActual:   $actualResultAsIon\nMode: ${evalTC.compileOptions.typingMode}")
                    }
                }
                is Assertion.EvaluationFailure -> {
                    if (!expectedFailedTests.contains(Pair(evalTC.name, evalTC.compileOptions))) {
                        error("Expected error to be thrown but none was thrown.\n${evalTC.name}\nActual result: ${actualResult.toIonValue(ION)}")
                    }
                }
            }
        } catch (e: SqlException) {
            when (evalTC.assertion) {
                is Assertion.EvaluationSuccess -> {
                    if (!expectedFailedTests.contains(Pair(evalTC.name, evalTC.compileOptions))) {
                        error("Expected success but exception thrown: $e")
                    }
                }
                is Assertion.EvaluationFailure -> {
                    // Expected failure and test threw when evaluated
                }
            }
        }
    }

    private fun runEvalEquivTestCase(evalEquivTestCase: EvalEquivTestCase, expectedFailedTests: List<Pair<String, CompileOptions>>) {
        val compilerPipeline = CompilerPipeline.builder().compileOptions(evalEquivTestCase.compileOptions).build()
        val globals = ExprValue.of(evalEquivTestCase.env).bindings
        val session = EvaluationSession.build { globals(globals) }
        val statements = evalEquivTestCase.statements

        statements.forEach { statement ->
            try {
                val expression = compilerPipeline.compile(statement)
                val actualResult = expression.eval(session)
                when (evalEquivTestCase.assertion) {
                    is Assertion.EvaluationSuccess -> {
                        val actualResultAsIon = actualResult.toIonValue(ION)
                        if (!expectedFailedTests.contains(Pair(evalEquivTestCase.name, evalEquivTestCase.compileOptions)) && !PartiQLEqualityChecker().areEqual(evalEquivTestCase.assertion.expectedResult, actualResultAsIon)) {
                            error("Expected and actual results differ:\nExpected: ${evalEquivTestCase.assertion.expectedResult}\nActual:   $actualResultAsIon\nMode: ${evalEquivTestCase.compileOptions.typingMode}")
                        }
                    }
                    is Assertion.EvaluationFailure -> {
                        if (!expectedFailedTests.contains(Pair(evalEquivTestCase.name, evalEquivTestCase.compileOptions))) {
                            error("Expected error to be thrown but none was thrown.\n${evalEquivTestCase.name}\nActual result: ${actualResult.toIonValue(ION)}")
                        }
                    }
                }
            } catch (e: SqlException) {
                when (evalEquivTestCase.assertion) {
                    is Assertion.EvaluationSuccess -> {
                        if (!expectedFailedTests.contains(Pair(evalEquivTestCase.name, evalEquivTestCase.compileOptions))) {
                            error("Expected success but exception thrown: $e")
                        }
                    }
                    is Assertion.EvaluationFailure -> {
                        // Expected failure and test threw when evaluated
                    }
                }
            }
        }
    }

    /**
     * Runs the conformance tests with an expected list of failing tests. Ensures that tests not in the failing list
     * succeed with the expected result. Ensures that tests included in the failing list fail.
     *
     * These tests are included in the normal test/building.
     */
    class DefaultConformanceTestRunner {
        // Tests the eval tests with the Kotlin implementation
        @ParameterizedTest(name = "{arguments}")
        @ArgumentsSource(EvalTestCases::class)
        fun validatePartiQLEvalTestData(tc: TestCase) {
            when (tc) {
                is EvalTestCase -> TestRunner().runEvalTestCase(tc, LANG_KOTLIN_EVAL_FAIL_LIST)
                else -> error("Unsupported test case category")
            }
        }

        // Tests the eval equivalence tests with the Kotlin implementation
        @ParameterizedTest(name = "{arguments}")
        @ArgumentsSource(EvalEquivTestCases::class)
        fun validatePartiQLEvalEquivTestData(tc: TestCase) {
            when (tc) {
                is EvalEquivTestCase -> TestRunner().runEvalEquivTestCase(tc, LANG_KOTLIN_EVAL_EQUIV_FAIL_LIST)
                else -> error("Unsupported test case category")
            }
        }
    }

    /**
     * Runs the conformance tests without a fail list, so we can document the passing/failing tests in the conformance
     * report.
     *
     * These tests are excluded from normal testing/building unless the `conformanceReport` gradle property is
     * specified (i.e. `gradle test ... -PconformanceReport`)
     */
    @ExtendWith(ConformanceTestReportGenerator::class)
    class ConformanceTestsReportRunner {
        // Tests the eval tests with the Kotlin implementation without a fail list
        @ParameterizedTest(name = "{arguments}")
        @ArgumentsSource(EvalTestCases::class)
        fun validatePartiQLEvalTestData(tc: TestCase) {
            when (tc) {
                is EvalTestCase -> TestRunner().runEvalTestCase(tc, emptyList())
                else -> error("Unsupported test case category")
            }
        }

        // Tests the eval equivalence tests with the Kotlin implementation without a fail list
        @ParameterizedTest(name = "{arguments}")
        @ArgumentsSource(EvalEquivTestCases::class)
        fun validatePartiQLEvalEquivTestData(tc: TestCase) {
            when (tc) {
                is EvalEquivTestCase -> TestRunner().runEvalEquivTestCase(tc, emptyList())
                else -> error("Unsupported test case category")
            }
        }
    }

    class EvalTestCases : ArgumentsProviderBase() {
        override fun getParameters(): List<Any> {
            return TestRunner().loadTests(PARTIQL_EVAL_TEST_DATA_DIR)
        }
    }

    class EvalEquivTestCases : ArgumentsProviderBase() {
        override fun getParameters(): List<Any> {
            return TestRunner().loadTests(PARTIQL_EVAL_EQUIV_TEST_DATA_DIR)
        }
    }
}
