package org.partiql.runner

import org.junit.jupiter.api.extension.RegisterExtension
import org.partiql.eval.PartiQLResult
import org.partiql.eval.PartiQLStatement
import org.partiql.lang.eval.CompileOptions
import org.partiql.lang.eval.TypingMode
import org.partiql.runner.executor.EvalExecutor
import org.partiql.runner.report.ReportGenerator
import org.partiql.runner.test.TestRunner

class ConformanceTestEval : ConformanceTestBase<PartiQLStatement<*>, PartiQLResult>() {
    companion object {
        @JvmStatic
        @RegisterExtension
        val reporter = ReportGenerator("eval")

        private val COERCE_EVAL_MODE_COMPILE_OPTIONS = CompileOptions.build { typingMode(TypingMode.PERMISSIVE) }
        private val ERROR_EVAL_MODE_COMPILE_OPTIONS = CompileOptions.build { typingMode(TypingMode.LEGACY) }
    }

    private val factory = EvalExecutor.Factory
    override val runner = TestRunner(factory)

    /**
     * Currently, the [ConformanceTestEval] only skips GPML-related tests.
     */
    override val skipListForEvaluation: List<Pair<String, CompileOptions>>
        get() = gpmlTests + testsToFix

    override val skipListForEquivalence: List<Pair<String, CompileOptions>> = emptyList()

    /**
     * TODO: ADD SUPPORT FOR THESE
     * Variable does not exist.
     */
    private val aliasTests: List<Pair<String, CompileOptions>> = listOf(
        Pair("testing alias support", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("testing alias support", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("testing nested alias support", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("testing nested alias support", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("group and order by count", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("group and order by count", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    )

    /**
     * TODO: ADD SUPPORT FOR THESE
     * Wrong precision/scale
     */
    private val arithmeticCases = listOf(
        Pair("division with mixed StaticType", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("division with mixed StaticType", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("repeatingDecimal", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("repeatingDecimal", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("repeatingDecimalHigherPrecision", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("repeatingDecimalHigherPrecision", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("divDecimalInt", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("divDecimalInt", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("subtractionOutOfAllowedPrecision", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("subtractionOutOfAllowedPrecision", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    )

    /**
     * TODO: Fix these once https://github.com/partiql/partiql-tests/pull/124 is decided upon.
     */
    private val undefinedVariableCases = listOf(
        Pair("GROUP BY binding referenced in FROM clause", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("GROUP BY binding referenced in WHERE clause", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("GROUP AS binding referenced in FROM clause", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("GROUP AS binding referenced in WHERE clause", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair(
            "GROUP BY with JOIN : SELECT supplierName, COUNT(*) as the_count FROM suppliers AS s INNER JOIN products AS p ON s.supplierId = p.supplierId GROUP BY supplierName",
            COERCE_EVAL_MODE_COMPILE_OPTIONS
        ),
        Pair(
            "GROUP BY with JOIN : SELECT supplierName, COUNT(*) as the_count FROM suppliers AS s INNER JOIN products AS p ON s.supplierId = p.supplierId GROUP BY supplierName",
            ERROR_EVAL_MODE_COMPILE_OPTIONS
        ),
        Pair(
            "SELECT col1, g FROM simple_1_col_1_group, join_me GROUP BY col1 GROUP AS g",
            COERCE_EVAL_MODE_COMPILE_OPTIONS
        ),
        Pair(
            "SELECT col1, g FROM simple_1_col_1_group, join_me GROUP BY col1 GROUP AS g",
            ERROR_EVAL_MODE_COMPILE_OPTIONS
        ),
        Pair(
            "SELECT VALUE { 'col1': col1, 'g': g } FROM simple_1_col_1_group, join_me GROUP BY col1 GROUP AS g",
            COERCE_EVAL_MODE_COMPILE_OPTIONS
        ),
        Pair(
            "SELECT VALUE { 'col1': col1, 'g': g } FROM simple_1_col_1_group, join_me GROUP BY col1 GROUP AS g",
            ERROR_EVAL_MODE_COMPILE_OPTIONS
        ),
        Pair(
            "SELECT col1, g FROM simple_1_col_1_group, different_types_per_row GROUP BY col1 GROUP AS g",
            COERCE_EVAL_MODE_COMPILE_OPTIONS
        ),
        Pair(
            "SELECT col1, g FROM simple_1_col_1_group, different_types_per_row GROUP BY col1 GROUP AS g",
            ERROR_EVAL_MODE_COMPILE_OPTIONS
        ),
        Pair(
            "SELECT VALUE { 'col1': col1, 'g': g } FROM simple_1_col_1_group, different_types_per_row GROUP BY col1 GROUP AS g",
            COERCE_EVAL_MODE_COMPILE_OPTIONS
        ),
        Pair(
            "SELECT VALUE { 'col1': col1, 'g': g } FROM simple_1_col_1_group, different_types_per_row GROUP BY col1 GROUP AS g",
            ERROR_EVAL_MODE_COMPILE_OPTIONS
        ),
        Pair("select fld3,period from t1,t2 where fld1 = 011401", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("select fld3,period from t2,t1 where companynr*10 = 37*10", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("undefinedUnqualifiedVariableWithUndefinedVariableBehaviorMissing", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair(
            "undefinedUnqualifiedVariableIsNullExprWithUndefinedVariableBehaviorMissing",
            COERCE_EVAL_MODE_COMPILE_OPTIONS
        ),
        Pair(
            "undefinedUnqualifiedVariableIsMissingExprWithUndefinedVariableBehaviorMissing",
            COERCE_EVAL_MODE_COMPILE_OPTIONS
        ),
        Pair("MYSQL_SELECT_20", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("MYSQL_SELECT_20", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("MYSQL_SELECT_21", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("MYSQL_SELECT_21", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    )

    /**
     * TODO
     */
    private val negativeOffset = listOf(
        Pair("offset -1", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("offset -1", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("offset 1-2", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    )

    /**
     * TODO
     */
    private val mistypedCollAggs = listOf(
        Pair("COLL_MAX non-collection", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("COLL_AVG non-collection", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("COLL_COUNT non-collection", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("COLL_SUM non-collection", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("COLL_MIN non-collection", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("COLL_ANY non-collection", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("COLL_SOME non-collection", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("COLL_EVERY non-collection", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    )

    /**
     * TODO
     */
    private val miscellaneousCases = listOf(
        Pair("subscript with non-existent variable in lowercase", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("subscript with non-existent variable in uppercase", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair(
            "repeated field on struct is ambiguous{identifier:\"REPEATED\",cn:9,bn:\"REPEATED\"}",
            ERROR_EVAL_MODE_COMPILE_OPTIONS
        ),
        Pair(
            "repeated field on struct is ambiguous{identifier:\" \\\"repeated\\\" \",cn:10,bn:\"repeated\"}",
            ERROR_EVAL_MODE_COMPILE_OPTIONS
        ),
        Pair("invalid extract year from time", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("invalid extract month from time", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("invalid extract day from time", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("invalid extract month from time with time zone", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("invalid extract day from time with time zone", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    )

    /**
     * TODO
     */
    private val mistypedFunctions = listOf(
        Pair("MOD(MISSING, 'some string')", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("MOD('some string', MISSING)", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("MOD(NULL, 'some string')", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("MOD('some string', NULL)", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("MOD(3, 'some string')", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("MOD('some string', 3)", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("CARDINALITY('foo') type mismatch", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("OCTET_LENGTH invalid type", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("CHARACTER_LENGTH invalid type", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("ABS('foo')", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("POSITION invalid type in string", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("POSITION string in invalid type", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("BIT_LENGTH invalid type", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    )

    /**
     * TODO
     */
    private val invalidCasts = listOf(
        Pair(
            "cast to int invalid target type{value:\"`2017T`\",target:\"TIMESTAMP\"}", ERROR_EVAL_MODE_COMPILE_OPTIONS
        ),
        Pair(
            "cast to int invalid target type{value:\" `{{\\\"\\\"}}` \",target:\"CLOB\"}",
            ERROR_EVAL_MODE_COMPILE_OPTIONS
        ),
        Pair(
            "cast to int invalid target type{value:\" `{{\\\"1\\\"}}` \",target:\"CLOB\"}",
            ERROR_EVAL_MODE_COMPILE_OPTIONS
        ),
        Pair("cast to int invalid target type{value:\"`{{}}`\",target:\"BLOB\"}", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("cast to int invalid target type{value:\"[1, 2]\",target:\"LIST\"}", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("cast to int invalid target type{value:\"[1]\",target:\"LIST\"}", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("cast to int invalid target type{value:\"[]\",target:\"LIST\"}", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("cast to int invalid target type{value:\"`(1 2)`\",target:\"SEXP\"}", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("cast to int invalid target type{value:\"`(1)`\",target:\"SEXP\"}", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("cast to int invalid target type{value:\"`()`\",target:\"SEXP\"}", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("cast to int invalid target type{value:\"{'a': 1}\",target:\"STRUCT\"}", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair(
            "cast to int invalid target type{value:\"{'a': '12'}\",target:\"STRUCT\"}", ERROR_EVAL_MODE_COMPILE_OPTIONS
        ),
        Pair("cast to int invalid target type{value:\"{}\",target:\"STRUCT\"}", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("cast to int invalid target type{value:\"<<1, 2>>\",target:\"BAG\"}", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("cast to int invalid target type{value:\"<<1>>\",target:\"BAG\"}", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("cast to int invalid target type{value:\"<<>>\",target:\"BAG\"}", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    )

    /**
     * TODO
     */
    private val specialForms = listOf(
        Pair("More than one character given for ESCAPE", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("LIKE bad value type", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("LIKE bad pattern type", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("LIKE bad escape type", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    )

    /**
     * TODO
     */
    private val operators = listOf(
        Pair("array navigation with wrongly typed array index", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("data type mismatch in comparison expression", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("data type mismatch in logical expression", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    )

    /**
     * TODO
     */
    private val bagOps = listOf(
        Pair("Example 6 - Value Coercion not union-compatible", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("Example 6 - Value Coercion not union-compatible", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    )

    /**
     * TODO: Determine whether SEXP should be treated as a first-class collection.
     *  See: https://github.com/partiql/partiql-tests/pull/126#discussion_r1710271573
     */
    private val sexpTests = listOf(
        Pair("projectOfSexp", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("projectOfSexp", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    )

    /**
     * TODO
     */
    private val testsToFix: List<Pair<String, CompileOptions>> =
        aliasTests + arithmeticCases + undefinedVariableCases + sexpTests + negativeOffset + mistypedFunctions + invalidCasts + miscellaneousCases + mistypedCollAggs + specialForms + operators + bagOps

    /**
     * This holds all of the Graph Pattern Matching Language conformance tests. The new evaluator does not yet support
     * their evaluation.
     */
    private val gpmlTests: List<Pair<String, CompileOptions>> = listOf(
        Pair("Right with variables", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("Right with variables", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("Right with variables", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("Right with variables", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("Right with spots", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("Right with spots", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("Right with spots", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("Right with spots", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("Right shorthand", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("Right shorthand", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("Right shorthand", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("Right shorthand", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("Left with variables", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("Left with variables", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("Left with variables", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("Left with variables", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("Left with spots", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("Left with spots", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("Left with spots", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("Left with spots", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("Left shorthand", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("Left shorthand", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("Left shorthand", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("Left shorthand", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("Left+right with variables", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("Left+right with variables", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("Left+right with variables", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("Left+right with variables", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("Left+right with spots", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("Left+right with spots", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("Left+right with spots", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("Left+right with spots", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("Left+right shorthand", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("Left+right shorthand", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("Left+right shorthand", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("Left+right shorthand", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("Left+right with variables and label", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("Left+right with variables and label", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("Left+right with variables and label", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("Left+right with variables and label", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("Undirected with variables", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("Undirected with variables", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("Undirected with variables", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("Undirected with variables", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("Undirected with spots", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("Undirected with spots", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("Undirected with spots", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("Undirected with spots", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("Undirected shorthand", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("Undirected shorthand", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("Undirected shorthand", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("Undirected shorthand", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("Undirected with variables and label", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("Undirected with variables and label", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("Undirected with variables and label", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("Undirected with variables and label", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("Right+undirected with variables", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("Right+undirected with variables", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("Right+undirected with variables", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("Right+undirected with variables", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("Right+undirected with spots", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("Right+undirected with spots", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("Right+undirected with spots", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("Right+undirected with spots", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("Right+undirected shorthand", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("Right+undirected shorthand", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("Right+undirected shorthand", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("Right+undirected shorthand", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("Right+undirected with variables and labels", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("Right+undirected with variables and labels", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("Right+undirected with variables and labels", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("Right+undirected with variables and labels", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("Left+undirected with variables", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("Left+undirected with variables", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("Left+undirected with variables", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("Left+undirected with variables", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("Left+undirected with spots", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("Left+undirected with spots", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("Left+undirected with spots", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("Left+undirected with spots", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("Left+undirected shorthand", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("Left+undirected shorthand", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("Left+undirected shorthand", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("Left+undirected shorthand", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("Left+undirected with variables and label", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("Left+undirected with variables and label", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("Left+undirected with variables and label", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("Left+undirected with variables and label", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("Left+right+undirected with variables", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("Left+right+undirected with variables", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("Left+right+undirected with variables", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("Left+right+undirected with variables", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("Left+right+undirected with spots", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("Left+right+undirected with spots", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("Left+right+undirected with spots", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("Left+right+undirected with spots", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("Left+right+undirected shorthand", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("Left+right+undirected shorthand", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("Left+right+undirected shorthand", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("Left+right+undirected shorthand", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N0E0 MATCH (x))", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N0E0 MATCH (x))", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N0E0 MATCH (x))", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N0E0 MATCH (x))", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N0E0 MATCH -[y]-> )", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N0E0 MATCH -[y]-> )", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N0E0 MATCH -[y]-> )", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N0E0 MATCH -[y]-> )", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N0E0 MATCH (x)-[y]->(z) )", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N0E0 MATCH (x)-[y]->(z) )", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N0E0 MATCH (x)-[y]->(z) )", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N0E0 MATCH (x)-[y]->(z) )", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N1E0 MATCH (x))", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N1E0 MATCH (x))", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N1E0 MATCH (x))", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N1E0 MATCH (x))", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N1E0 MATCH -[y]-> )", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N1E0 MATCH -[y]-> )", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N1E0 MATCH -[y]-> )", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N1E0 MATCH -[y]-> )", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N1E0 MATCH (x)-[y]->(z) )", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N1E0 MATCH (x)-[y]->(z) )", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N1E0 MATCH (x)-[y]->(z) )", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N1E0 MATCH (x)-[y]->(z) )", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N1E0 MATCH (x)-[y]->(x) )", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N1E0 MATCH (x)-[y]->(x) )", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N1E0 MATCH (x)-[y]->(x) )", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N1E0 MATCH (x)-[y]->(x) )", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N1U1 MATCH (x))", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N1U1 MATCH (x))", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N1U1 MATCH (x))", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N1U1 MATCH (x))", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N1U1 MATCH ~[y]~ )", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N1U1 MATCH ~[y]~ )", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N1U1 MATCH ~[y]~ )", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N1U1 MATCH ~[y]~ )", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N1U1 MATCH (x)~[y]~(z) )", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N1U1 MATCH (x)~[y]~(z) )", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N1U1 MATCH (x)~[y]~(z) )", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N1U1 MATCH (x)~[y]~(z) )", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N1U1 MATCH (x)~[y]~(x) )", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N1U1 MATCH (x)~[y]~(x) )", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N1U1 MATCH (x)~[y]~(x) )", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N1U1 MATCH (x)~[y]~(x) )", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N1U1 MATCH (x1)~[y1]~(x2)~[y2]~(x3) )", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N1U1 MATCH (x1)~[y1]~(x2)~[y2]~(x3) )", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N1U1 MATCH (x1)~[y1]~(x2)~[y2]~(x3) )", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N1U1 MATCH (x1)~[y1]~(x2)~[y2]~(x3) )", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N1D2 MATCH (x))", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N1D2 MATCH (x))", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N1D2 MATCH (x))", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N1D2 MATCH (x))", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N1D2 MATCH -[y]-> )", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N1D2 MATCH -[y]-> )", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N1D2 MATCH -[y]-> )", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N1D2 MATCH -[y]-> )", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N1D2 MATCH (x)-[y]->(z) )", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N1D2 MATCH (x)-[y]->(z) )", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N1D2 MATCH (x)-[y]->(z) )", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N1D2 MATCH (x)-[y]->(z) )", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N1D2 MATCH (x)-[y]->(x) )", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N1D2 MATCH (x)-[y]->(x) )", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N1D2 MATCH (x)-[y]->(x) )", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N1D2 MATCH (x)-[y]->(x) )", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N1D2 MATCH (x1)-[y1]->(x2)-[y2]->(x3) )", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N1D2 MATCH (x1)-[y1]->(x2)-[y2]->(x3) )", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N1D2 MATCH (x1)-[y1]->(x2)-[y2]->(x3) )", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N1D2 MATCH (x1)-[y1]->(x2)-[y2]->(x3) )", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2E0 MATCH (x))", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2E0 MATCH (x))", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2E0 MATCH (x))", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2E0 MATCH (x))", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2E0 MATCH -[y]-> )", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2E0 MATCH -[y]-> )", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2E0 MATCH -[y]-> )", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2E0 MATCH -[y]-> )", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2E0 MATCH (x)-[y]->(z) )", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2E0 MATCH (x)-[y]->(z) )", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2E0 MATCH (x)-[y]->(z) )", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2E0 MATCH (x)-[y]->(z) )", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2E0 MATCH (x)-[y]->(x) )", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2E0 MATCH (x)-[y]->(x) )", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2E0 MATCH (x)-[y]->(x) )", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2E0 MATCH (x)-[y]->(x) )", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2D1 MATCH (x))", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2D1 MATCH (x))", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2D1 MATCH (x))", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2D1 MATCH (x))", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2D1 MATCH -[y]-> )", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2D1 MATCH -[y]-> )", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2D1 MATCH -[y]-> )", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2D1 MATCH -[y]-> )", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2D1 MATCH (x)-[y]->(z) )", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2D1 MATCH (x)-[y]->(z) )", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2D1 MATCH (x)-[y]->(z) )", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2D1 MATCH (x)-[y]->(z) )", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2D1 MATCH (x)-[y]->(x) )", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2D1 MATCH (x)-[y]->(x) )", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2D1 MATCH (x)-[y]->(x) )", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2D1 MATCH (x)-[y]->(x) )", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2D1 MATCH (x1)-[y1]->(x2)-[y2]->(x3) )", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2D1 MATCH (x1)-[y1]->(x2)-[y2]->(x3) )", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2D1 MATCH (x1)-[y1]->(x2)-[y2]->(x3) )", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2D1 MATCH (x1)-[y1]->(x2)-[y2]->(x3) )", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2D1 MATCH (x1)-[y1]->(x2)-[y2]-(x3) )", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2D1 MATCH (x1)-[y1]->(x2)-[y2]-(x3) )", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2D1 MATCH (x1)-[y1]->(x2)-[y2]-(x3) )", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2D1 MATCH (x1)-[y1]->(x2)-[y2]-(x3) )", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2D1 MATCH (x1)-[y1]-(x2)-[y2]->(x3) )", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2D1 MATCH (x1)-[y1]-(x2)-[y2]->(x3) )", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2D1 MATCH (x1)-[y1]-(x2)-[y2]->(x3) )", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2D1 MATCH (x1)-[y1]-(x2)-[y2]->(x3) )", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2D1 MATCH (x1)-[y1]-(x2)-[y2]-(x3) )", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2D1 MATCH (x1)-[y1]-(x2)-[y2]-(x3) )", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2D1 MATCH (x1)-[y1]-(x2)-[y2]-(x3) )", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2D1 MATCH (x1)-[y1]-(x2)-[y2]-(x3) )", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2U1 MATCH (x))", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2U1 MATCH (x))", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2U1 MATCH (x))", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2U1 MATCH (x))", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2U1 MATCH ~[y]~ )", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2U1 MATCH ~[y]~ )", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2U1 MATCH ~[y]~ )", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2U1 MATCH ~[y]~ )", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2U1 MATCH (x)~[y]~(z) )", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2U1 MATCH (x)~[y]~(z) )", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2U1 MATCH (x)~[y]~(z) )", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2U1 MATCH (x)~[y]~(z) )", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2U1 MATCH (x)~[y]~(x) )", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2U1 MATCH (x)~[y]~(x) )", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2U1 MATCH (x)~[y]~(x) )", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2U1 MATCH (x)~[y]~(x) )", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2U1 MATCH (x1)~[y1]~(x2)~[y2]~(x3) )", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2U1 MATCH (x1)~[y1]~(x2)~[y2]~(x3) )", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2U1 MATCH (x1)~[y1]~(x2)~[y2]~(x3) )", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2U1 MATCH (x1)~[y1]~(x2)~[y2]~(x3) )", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2U1 MATCH (x1)~[y1]~(x2)-[y2]-(x3) )", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2U1 MATCH (x1)~[y1]~(x2)-[y2]-(x3) )", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2U1 MATCH (x1)~[y1]~(x2)-[y2]-(x3) )", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2U1 MATCH (x1)~[y1]~(x2)-[y2]-(x3) )", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2U1 MATCH (x1)-[y1]-(x2)~[y2]~(x3) )", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2U1 MATCH (x1)-[y1]-(x2)~[y2]~(x3) )", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2U1 MATCH (x1)-[y1]-(x2)~[y2]~(x3) )", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2U1 MATCH (x1)-[y1]-(x2)~[y2]~(x3) )", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2U1 MATCH (x1)-[y1]-(x2)-[y2]-(x3) )", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2U1 MATCH (x1)-[y1]-(x2)-[y2]-(x3) )", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2U1 MATCH (x1)-[y1]-(x2)-[y2]-(x3) )", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2U1 MATCH (x1)-[y1]-(x2)-[y2]-(x3) )", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2D2 MATCH (x))", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2D2 MATCH (x))", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2D2 MATCH (x))", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2D2 MATCH (x))", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2D2 MATCH -[y]-> )", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2D2 MATCH -[y]-> )", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2D2 MATCH -[y]-> )", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2D2 MATCH -[y]-> )", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2D2 MATCH (x)-[y]->(z) )", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2D2 MATCH (x)-[y]->(z) )", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2D2 MATCH (x)-[y]->(z) )", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2D2 MATCH (x)-[y]->(z) )", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2D2 MATCH (x)-[y]->(x) )", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2D2 MATCH (x)-[y]->(x) )", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2D2 MATCH (x)-[y]->(x) )", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2D2 MATCH (x)-[y]->(x) )", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2D2 MATCH (x1)-[y1]->(x2)-[y2]->(x3) )", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2D2 MATCH (x1)-[y1]->(x2)-[y2]->(x3) )", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2D2 MATCH (x1)-[y1]->(x2)-[y2]->(x3) )", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2D2 MATCH (x1)-[y1]->(x2)-[y2]->(x3) )", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2D2 MATCH (x1)-[y1]->(x2)-[y2]-(x3) )", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2D2 MATCH (x1)-[y1]->(x2)-[y2]-(x3) )", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2D2 MATCH (x1)-[y1]->(x2)-[y2]-(x3) )", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2D2 MATCH (x1)-[y1]->(x2)-[y2]-(x3) )", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2D2 MATCH (x1)-[y1]-(x2)-[y2]->(x3) )", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2D2 MATCH (x1)-[y1]-(x2)-[y2]->(x3) )", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2D2 MATCH (x1)-[y1]-(x2)-[y2]->(x3) )", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2D2 MATCH (x1)-[y1]-(x2)-[y2]->(x3) )", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2D2 MATCH (x1)-[y1]-(x2)-[y2]-(x3) )", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2D2 MATCH (x1)-[y1]-(x2)-[y2]-(x3) )", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2D2 MATCH (x1)-[y1]-(x2)-[y2]-(x3) )", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2D2 MATCH (x1)-[y1]-(x2)-[y2]-(x3) )", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2D2c MATCH (x))", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2D2c MATCH (x))", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2D2c MATCH (x))", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2D2c MATCH (x))", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2D2c MATCH -[y]-> )", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2D2c MATCH -[y]-> )", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2D2c MATCH -[y]-> )", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2D2c MATCH -[y]-> )", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2D2c MATCH (x)-[y]->(z) )", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2D2c MATCH (x)-[y]->(z) )", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2D2c MATCH (x)-[y]->(z) )", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2D2c MATCH (x)-[y]->(z) )", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2D2c MATCH (x)-[y]->(x) )", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2D2c MATCH (x)-[y]->(x) )", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2D2c MATCH (x)-[y]->(x) )", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2D2c MATCH (x)-[y]->(x) )", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2D2c MATCH (x1)-[y1]->(x2)-[y2]->(x3) )", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2D2c MATCH (x1)-[y1]->(x2)-[y2]->(x3) )", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2D2c MATCH (x1)-[y1]->(x2)-[y2]->(x3) )", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2D2c MATCH (x1)-[y1]->(x2)-[y2]->(x3) )", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2D2c MATCH (x1)-[y1]->(x2)-[y2]->(x1) )", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2D2c MATCH (x1)-[y1]->(x2)-[y2]->(x1) )", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2D2c MATCH (x1)-[y1]->(x2)-[y2]->(x1) )", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2D2c MATCH (x1)-[y1]->(x2)-[y2]->(x1) )", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2D2c MATCH (x1)-[y1]->(x2)-[y2]-(x3) )", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2D2c MATCH (x1)-[y1]->(x2)-[y2]-(x3) )", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2D2c MATCH (x1)-[y1]->(x2)-[y2]-(x3) )", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2D2c MATCH (x1)-[y1]->(x2)-[y2]-(x3) )", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2D2c MATCH (x1)-[y1]-(x2)-[y2]->(x3) )", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2D2c MATCH (x1)-[y1]-(x2)-[y2]->(x3) )", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2D2c MATCH (x1)-[y1]-(x2)-[y2]->(x3) )", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2D2c MATCH (x1)-[y1]-(x2)-[y2]->(x3) )", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2D2c MATCH (x1)-[y1]-(x2)-[y2]-(x3) )", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2D2c MATCH (x1)-[y1]-(x2)-[y2]-(x3) )", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2D2c MATCH (x1)-[y1]-(x2)-[y2]-(x3) )", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2D2c MATCH (x1)-[y1]-(x2)-[y2]-(x3) )", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2U2 MATCH (x))", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2U2 MATCH (x))", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2U2 MATCH (x))", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2U2 MATCH (x))", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2U2 MATCH ~[y]~ )", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2U2 MATCH ~[y]~ )", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2U2 MATCH ~[y]~ )", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2U2 MATCH ~[y]~ )", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2U2 MATCH (x)~[y]~(z) )", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2U2 MATCH (x)~[y]~(z) )", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2U2 MATCH (x)~[y]~(z) )", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2U2 MATCH (x)~[y]~(z) )", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2U2 MATCH (x)~[y]~(x) )", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2U2 MATCH (x)~[y]~(x) )", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2U2 MATCH (x)~[y]~(x) )", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2U2 MATCH (x)~[y]~(x) )", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2U2 MATCH (x1)~[y1]~(x2)~[y2]~(x3) )", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2U2 MATCH (x1)~[y1]~(x2)~[y2]~(x3) )", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2U2 MATCH (x1)~[y1]~(x2)~[y2]~(x3) )", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2U2 MATCH (x1)~[y1]~(x2)~[y2]~(x3) )", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2U2 MATCH (x1)~[y1]~(x2)~[y2]~(x1) )", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2U2 MATCH (x1)~[y1]~(x2)~[y2]~(x1) )", ERROR_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2U2 MATCH (x1)~[y1]~(x2)~[y2]~(x1) )", COERCE_EVAL_MODE_COMPILE_OPTIONS),
        Pair("(N2U2 MATCH (x1)~[y1]~(x2)~[y2]~(x1) )", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    )
}
