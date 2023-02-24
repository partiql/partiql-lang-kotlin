package org.partiql.lang.planner.transforms.optimizations

import com.amazon.ionelement.api.ionSymbol
import org.junit.jupiter.api.fail
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.annotations.ExperimentalPartiQLCompilerPipeline
import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.errors.Problem
import org.partiql.lang.errors.ProblemHandler
import org.partiql.lang.planner.GlobalResolutionResult
import org.partiql.lang.planner.GlobalVariableResolver
import org.partiql.lang.planner.PartiQLPlanner
import org.partiql.lang.planner.PartiQLPlannerBuilder
import org.partiql.lang.planner.assertSexpEquals
import org.partiql.lang.planner.litInt
import org.partiql.lang.planner.litTrue
import org.partiql.lang.planner.transforms.DEFAULT_IMPL
import org.partiql.lang.planner.transforms.PLAN_VERSION_NUMBER
import org.partiql.lang.syntax.PartiQLParserBuilder
import org.partiql.lang.util.ArgumentsProviderBase
import org.partiql.spi.types.BagType
import org.partiql.spi.types.StaticType
import org.partiql.spi.types.StructType

private const val FAKE_GET_BY_KEY_PROJECT_OPERATOR_NAME = "foo"
private const val TABLE_WITH_1_FIELD_PK = "table_with_1_field_pk"
private const val TABLE_OF_TYPE_LIST = "table_of_type_list"
private const val TABLE_OF_TYPE_LIST_UUID = "uuid_for_table_of_type_list"
private const val TABLE_WITH_1_FIELD_PK_UUID = "uuid_for_table_with_1_field_pk"
private const val TABLE_WITH_3_FIELD_PK = "table_with_3_field_pk"
private const val TABLE_WITH_3_FIELD_PK_UUID = "uuid_for_table_with_3_field_pk"

@OptIn(ExperimentalPartiQLCompilerPipeline::class)
class FilterScanToKeyLookupTests {
    /** A test case for [PartiQLPhysicalPass] implementations that work on expressions in the [PartiqlPhysical] domain. */
    data class TestCase(
        /** The input SQL. */
        val inputSql: String,
        /**
         * The expected output Bexpr.  If `null`, we will assume that the pass should be a no-op and assert
         * that the plan remains unchanged.
         */
        val expectedOutputBexpr: PartiqlPhysical.Bexpr?
    )

    @ParameterizedTest
    @ArgumentsSource(Arguments::class)
    fun runTestCase(tc: TestCase) {

        val parser = PartiQLParserBuilder.standard().build()

        val statement = parser.parseAstStatement(tc.inputSql)

        val physicalPlan = when (val planningResult = pipeline.plan(statement)) {
            is PartiQLPlanner.Result.Success -> planningResult.plan
            is PartiQLPlanner.Result.Error -> fail("Expected no errors but found ${planningResult.problems}")
        }

        val fakeProblemHandler = object : ProblemHandler {
            override fun handleProblem(problem: Problem): Unit = error("didn't expect any problems")
        }

        if (tc.expectedOutputBexpr != null) {
            val expectedOutputPlan = makeFakePlan(tc.expectedOutputBexpr)
            val actualOutputPlan = x.apply(physicalPlan, fakeProblemHandler)
            assertSexpEquals(
                expectedOutputPlan.toIonElement(),
                actualOutputPlan.toIonElement(),
                "expected the rewrite to change the input plan"
            )
        } else {
            val actualOutputPlan = x.apply(physicalPlan, fakeProblemHandler)
            assertSexpEquals(
                physicalPlan.toIonElement(),
                actualOutputPlan.toIonElement(),
                "did NOT expect the rewrite to change the input plan"
            )
        }
    }

    class Arguments : ArgumentsProviderBase() {
        override fun getParameters() = listOf(
            // single-field primary key (field-reference on LHS)
            TestCase(
                "SELECT * FROM $TABLE_WITH_1_FIELD_PK AS f WHERE f.id = 42",
                PartiqlPhysical.build {
                    filter(
                        DEFAULT_IMPL,
                        // NOTE: this useless filter will be removed in production by [RemoveUselessFiltersPass].
                        litTrue(),
                        project(
                            impl(FAKE_GET_BY_KEY_PROJECT_OPERATOR_NAME, listOf(ionSymbol(TABLE_WITH_1_FIELD_PK_UUID))),
                            varDecl(0),
                            list(litInt(42)),
                        )
                    )
                }
            ),
            // single-field primary key (table is list instead of bag)
            TestCase(
                "SELECT * FROM $TABLE_OF_TYPE_LIST AS f WHERE f.id = 42",
                PartiqlPhysical.build {
                    filter(
                        DEFAULT_IMPL,
                        // NOTE: this useless filter will be removed in production by [RemoveUselessFiltersPass].
                        litTrue(),
                        project(
                            impl(FAKE_GET_BY_KEY_PROJECT_OPERATOR_NAME, listOf(ionSymbol(TABLE_OF_TYPE_LIST_UUID))),
                            varDecl(0),
                            list(litInt(42)),
                        )
                    )
                }
            ),
            // single-field primary key (field-reference on RHS)
            TestCase(
                "SELECT * FROM $TABLE_WITH_1_FIELD_PK AS f WHERE 42 = f.id",
                PartiqlPhysical.build {
                    filter(
                        DEFAULT_IMPL,
                        // NOTE: this useless filter will be removed in production by [RemoveUselessFiltersPass].
                        litTrue(),
                        project(
                            impl(FAKE_GET_BY_KEY_PROJECT_OPERATOR_NAME, listOf(ionSymbol(TABLE_WITH_1_FIELD_PK_UUID))),
                            varDecl(0),
                            list(litInt(42)),
                        )
                    )
                }
            ),
            // compound primary key. note: WHERE predicate lists fields in different order than
            // specified in primary key, but they still appear in the correct order in the primary key constructor.
            TestCase(
                "SELECT * FROM $TABLE_WITH_3_FIELD_PK AS f WHERE f.marketplaceId = 43 AND f.fulfillmentCenterId = 44 AND f.customerId = 42",
                PartiqlPhysical.build {
                    filter(
                        DEFAULT_IMPL,
                        // NOTE: this useless `and` expr and filter will be removed in production by
                        // [RemoveUselessAndsPass] and [RemoveUselessFiltersPass], respectively.
                        and(
                            and(litTrue(), litTrue()),
                            litTrue()
                        ),
                        project(
                            impl(FAKE_GET_BY_KEY_PROJECT_OPERATOR_NAME, listOf(ionSymbol(TABLE_WITH_3_FIELD_PK_UUID))),
                            varDecl(0),
                            list(litInt(42), litInt(43), litInt(44)),
                        )
                    )
                }
            ),
            // same as previous but LHS and RHS reversed
            TestCase(
                "SELECT * FROM $TABLE_WITH_3_FIELD_PK AS f WHERE 43 = f.marketplaceId AND 44 = f.fulfillmentCenterId AND 42 = f.customerId",
                PartiqlPhysical.build {
                    filter(
                        DEFAULT_IMPL,
                        // NOTE: this useless `and` expr and filter will be removed in production by
                        // [RemoveUselessAndsPass] and [RemoveUselessFiltersPass], respectively.
                        and(
                            and(litTrue(), litTrue()),
                            litTrue()
                        ),
                        project(
                            impl(FAKE_GET_BY_KEY_PROJECT_OPERATOR_NAME, listOf(ionSymbol(TABLE_WITH_3_FIELD_PK_UUID))),
                            varDecl(0),
                            list(litInt(42), litInt(43), litInt(44)),
                        )
                    )
                }
            ),
            // compound primary key with missing keys missing key
            TestCase(
                "SELECT * FROM $TABLE_WITH_3_FIELD_PK AS f WHERE f.fulfillmentCenterId = 42",
                expectedOutputBexpr = null
            ),
            // compound primary key with mixed case, case-insensitive fields
            TestCase(
                "SELECT * FROM $TABLE_WITH_3_FIELD_PK AS f WHERE f.mArKeTpLaCeId = 43 AND f.FuLfIlLmEnTCEnTerId = 44 AND f.cUsToMeRID = 42",
                PartiqlPhysical.build {
                    filter(
                        DEFAULT_IMPL,
                        // NOTE: this useless `and` expr and filter will be removed in production by
                        // [RemoveUselessAndsPass] and [RemoveUselessFiltersPass], respectively.
                        and(
                            and(litTrue(), litTrue()),
                            litTrue()
                        ),
                        project(
                            impl(FAKE_GET_BY_KEY_PROJECT_OPERATOR_NAME, listOf(ionSymbol(TABLE_WITH_3_FIELD_PK_UUID))),
                            varDecl(0),
                            list(litInt(42), litInt(43), litInt(44)),
                        )
                    )
                }
            ),
            // single-field primary key with mixed-case, case-sensitive field
            // (normally this would cause a semantic check would fail before we get to this point, but we don't have
            // such a semantic check yet).
            TestCase(
                "SELECT * FROM $TABLE_WITH_1_FIELD_PK AS f WHERE f.\"ID\" = 42",
                expectedOutputBexpr = null
            ),
            // non-global variables don't apply
            TestCase(
                "SELECT * FROM << { 'id': 42  } >> AS f WHERE f.id = 42",
                expectedOutputBexpr = null
            ),
            // key-field references inside non-equals expressions are ignored.
            TestCase(
                "SELECT * FROM $TABLE_WITH_1_FIELD_PK AS f WHERE f.id + 2",
                expectedOutputBexpr = null
            ),
            TestCase(
                "SELECT * FROM $TABLE_WITH_1_FIELD_PK AS f WHERE f.id % 2",
                expectedOutputBexpr = null
            ),

            // key-field equality expressions inside other expression types are ignored.
            // We should *only* be looking at `<pk-field> = <expr>` (and the reverse) at the top level of a
            // predicate or within any nesting of `and` expressions that starts at the top level of the predicate.
            // If the rewrite applied to other expression types it would change the semantic meaning of the query and
            // be a huge bug. There are dozens if different types of expressions to skip here--we test only a sampling.

            // OR expressions
            TestCase(
                "SELECT * FROM $TABLE_WITH_1_FIELD_PK AS f WHERE f.id = 42 OR f.id = 43",
                expectedOutputBexpr = null
            ),
            // + expressions
            TestCase(
                "SELECT * FROM $TABLE_WITH_1_FIELD_PK AS f WHERE (f.id = 42) + (f.id = 43)",
                expectedOutputBexpr = null
            ),
            // cast expressions
            TestCase(
                "SELECT * FROM $TABLE_WITH_1_FIELD_PK AS f WHERE CAST(f.id = 42 AS STRING)",
                expectedOutputBexpr = null
            ),
            // Function call arguments
            TestCase(
                "SELECT * FROM $TABLE_WITH_1_FIELD_PK AS f WHERE to_string(f.id = 42)",
                expectedOutputBexpr = null
            ),
        )
    }

    data class FakeTableMetadata(
        val tableName: String,
        val uniqueId: String,
        val staticType: StaticType
    )

    private val tables = listOf(
        FakeTableMetadata(
            tableName = TABLE_WITH_1_FIELD_PK,
            uniqueId = TABLE_WITH_1_FIELD_PK_UUID,
            staticType = BagType(
                StructType(
                    fields = emptyMap(), // currently, fields are unused by the pass under test.
                    primaryKeyFields = listOf("id"), // this list of primary key fields is used, however.
                )
            )
        ),
        FakeTableMetadata(
            tableName = TABLE_WITH_3_FIELD_PK,
            uniqueId = TABLE_WITH_3_FIELD_PK_UUID,
            staticType = BagType(
                StructType(
                    fields = emptyMap(),
                    primaryKeyFields = listOf("customerId", "marketplaceId", "fulfillmentCenterId")
                )
            )
        ),
        FakeTableMetadata(
            tableName = TABLE_OF_TYPE_LIST,
            uniqueId = TABLE_OF_TYPE_LIST_UUID,
            staticType = BagType(
                StructType(
                    fields = emptyMap(),
                    primaryKeyFields = listOf("id"),
                )
            )
        )
    )

    // planner needs to resolve global variables (i.e. tables). By "resolve" we mean to look up the
    // uniqueId of the table.
    val globalVariableResolver = GlobalVariableResolver { bindingName ->
        tables.firstOrNull { bindingName.isEquivalentTo(it.tableName) }
            ?.let { GlobalResolutionResult.GlobalVariable(it.uniqueId) }
            ?: GlobalResolutionResult.Undefined
    }

    /**
     * We need a minimal PlannerPipeline to go from [TestCase.inputSql] to a physical plan, without the
     * pass under test applied.  We will invoke the pass separately.
     */
    private val pipeline = PartiQLPlannerBuilder
        .standard()
        .globalVariableResolver(globalVariableResolver)
        .build()

    val x = createFilterScanToKeyLookupPass(
        customProjectOperatorName = FAKE_GET_BY_KEY_PROJECT_OPERATOR_NAME,
        staticTypeResolver = { uniqueId ->
            // The uniqueId here was returned from the global variable resolver, above, so we should be able
            // to safely assume it's valid.
            tables.single { it.uniqueId == uniqueId }.staticType
        }
    ) { recordType, keyFieldEqualityPredicates ->
        PartiqlPhysical.build {
            list(
                // Key values are expressed to the imaginary storage layer as ordered list. Therefore, we need
                // to ensure that the list we pass in as an argument to the custom_get_by_key project operator
                // impl is in the right order.
                recordType.primaryKeyFields.map { keyFieldName ->
                    keyFieldEqualityPredicates.single { it.keyFieldName == keyFieldName }.equivalentValue
                }
            )
        }
    }

    /** Reduces boilerplate when specifying expected plans. */
    private fun makeFakePlan(bexpr: PartiqlPhysical.Bexpr) =
        PartiqlPhysical.build {
            plan(
                stmt = query(
                    expr = bindingsToValues(
                        struct(structFields(localId(0))),
                        bexpr
                    )
                ),
                locals = listOf(localVariable("f", 0)),
                version = PLAN_VERSION_NUMBER
            )
        }
}
