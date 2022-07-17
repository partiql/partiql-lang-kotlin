package org.partiql.lang.planner.transforms.optimizations

import com.amazon.ionelement.api.ionSymbol
import org.junit.jupiter.api.fail
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.ION
import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.eval.BindingCase
import org.partiql.lang.eval.ExprValueFactory
import org.partiql.lang.planner.GlobalResolutionResult
import org.partiql.lang.planner.PartiqlPhysicalPass
import org.partiql.lang.planner.PlannerPassResult
import org.partiql.lang.planner.PlannerPipeline
import org.partiql.lang.planner.assertSexpEquals
import org.partiql.lang.planner.litInt
import org.partiql.lang.planner.litString
import org.partiql.lang.planner.transforms.DEFAULT_IMPL
import org.partiql.lang.planner.transforms.PLAN_VERSION_NUMBER
import org.partiql.lang.types.BagType
import org.partiql.lang.types.StaticType
import org.partiql.lang.types.StructType
import org.partiql.lang.util.ArgumentsProviderBase

private const val FAKE_GET_BY_KEY_PROJECT_OPERATOR_NAME = "foo"
private const val TABLE_WITH_1_FIELD_PK = "table_with_1_field_pk"
private const val TABLE_WITH_1_FIELD_PK_UUID = "uuid_for_table_with_1_field_pk"
private const val TABLE_WITH_3_FIELD_PK = "table_with_3_field_pk"
private const val TABLE_WITH_3_FIELD_PK_UUID = "uuid_for_table_with_3_field_pk"

class FilterScanToKeyLookupTests {
    /** A test case for [PartiqlPhysicalPass] implementations that work on expressions in the [PartiqlPhysical] domain. */
    data class TestCase(
        val inputSql: String,
        val expectedOutputBexpr: PartiqlPhysical.Bexpr
    )

    @ParameterizedTest
    @ArgumentsSource(Arguments::class)
    fun runTestCase(tc: TestCase) {
        val planningResult = planner.plan(tc.inputSql)

        val actualOutputPlan = when(planningResult) {
            is PlannerPassResult.Success -> planningResult.output
            is PlannerPassResult.Error -> {
                planningResult.errors.forEach { System.err.println(it) }
                fail("Encountered one or more errors during planning.  See stderr.")
            }
        }

        val expectedOutputPlan = makeFakePlan(tc.expectedOutputBexpr)
        assertSexpEquals(expectedOutputPlan.toIonElement(), actualOutputPlan.toIonElement())
    }

    class Arguments : ArgumentsProviderBase() {
        // TODO: stuff to test:
        // x single-field primary keys
        // x compound primary keys (all key fields included)
        // x compound primary keys (some key fields omitted)
        // - path expressions with various case-sensitivity (i.e. `f.id` vs `f."id"`)
        // - tables of type list.
        // - Non-table variable in from-clause (i.e. a scalar)
        // - non container types, struct types (pass does not apply in that case)
        // - ORs and sub-queries are not recursed into into
        // todo: scour the code looking for more cases.
        // todo: should specify input trees values with SQL? (might necessitate setting up addl planner plumbing).
        // todo: should we consider running the 'remove useless *' passes to simplify expected values?
        // we gotta lotta test cases to include here and anything we can do to reduce the verbosity
        // should be considered.
        override fun getParameters() = listOf(
            // single-field primary key (field-reference on LHS)
            TestCase(
                "SELECT * FROM $TABLE_WITH_1_FIELD_PK AS f WHERE f.id = 42",
                PartiqlPhysical.build {
                    project(
                        impl(FAKE_GET_BY_KEY_PROJECT_OPERATOR_NAME, listOf(ionSymbol(TABLE_WITH_1_FIELD_PK_UUID))),
                        varDecl(0),
                        list(litInt(42)),
                    )
                }
            ),
            // single-field primary key (field-reference on RHS)
            TestCase(
                "SELECT * FROM $TABLE_WITH_1_FIELD_PK AS f WHERE 42 = f.id",
                PartiqlPhysical.build {
                    project(
                        impl(FAKE_GET_BY_KEY_PROJECT_OPERATOR_NAME, listOf(ionSymbol(TABLE_WITH_1_FIELD_PK_UUID))),
                        varDecl(0),
                        list(litInt(42)),
                    )
                }
            ),
            // compound primary key. note: WHERE predicate lists fields in different order than
            // specified in primary key, but they still appear in the correct order in the primary key constructor.
            TestCase(
                "SELECT * FROM $TABLE_WITH_3_FIELD_PK AS f WHERE f.marketplaceId = 43 AND f.fulfillmentCenterId = 44 AND f.customerId = 42",
                PartiqlPhysical.build {
                    project(
                        impl(FAKE_GET_BY_KEY_PROJECT_OPERATOR_NAME, listOf(ionSymbol(TABLE_WITH_3_FIELD_PK_UUID))),
                        varDecl(0),
                        list(litInt(42), litInt(43), litInt(44)),
                    )
                }
            ),
            // same as previous but LHS and RHS reversed
            TestCase(
                "SELECT * FROM $TABLE_WITH_3_FIELD_PK AS f WHERE 43 = f.marketplaceId AND 44 = f.fulfillmentCenterId AND 42 = f.customerId",
                PartiqlPhysical.build {
                    project(
                        impl(FAKE_GET_BY_KEY_PROJECT_OPERATOR_NAME, listOf(ionSymbol(TABLE_WITH_3_FIELD_PK_UUID))),
                        varDecl(0),
                        list(litInt(42), litInt(43), litInt(44)),
                    )
                }
            ),
            // compound primary key with missing keys missing key (rewrite does not apply--still needs full scan)
            TestCase(
                "SELECT * FROM $TABLE_WITH_3_FIELD_PK AS f WHERE f.fulfillmentCenterId = 42",
                PartiqlPhysical.build {
                    filter(
                        DEFAULT_IMPL,
                        eq(path(localId(0), pathExpr(litString("fulfillmentCenterId"), caseInsensitive())), litInt(42)),
                        scan(DEFAULT_IMPL, globalId(TABLE_WITH_3_FIELD_PK_UUID), varDecl(0))
                    )
                }
            )
        )
    }

    val valueFactory = ExprValueFactory.standard(ION)

    data class GenericTableMetadata(
        val tableName: String,
        val uniqueId: String,
        val staticType: StaticType
    )

    val tables = listOf(
        GenericTableMetadata(
            tableName = TABLE_WITH_1_FIELD_PK,
            uniqueId = TABLE_WITH_1_FIELD_PK_UUID,
            staticType = BagType(
                StructType(
                    fields = emptyMap(), // currently, fields are unused by the pass under test.
                    primaryKeyFields = listOf("id"), // this list of primary key fields is used, however.
                )
            )
        ),
        GenericTableMetadata(
            tableName = TABLE_WITH_3_FIELD_PK,
            uniqueId = TABLE_WITH_3_FIELD_PK_UUID,
            staticType = BagType(
                StructType(
                    fields = emptyMap(),
                    primaryKeyFields = listOf("customerId", "marketplaceId", "fulfillmentCenterId")
                )
            )
        )
    )

    @Suppress("DEPRECATION") // <-- PlannerPipeline is experimental, we are ok with it being deprecated.
    private val planner = PlannerPipeline.build(valueFactory) {
        // planner needs to resolve global variables (i.e. tables). By "resolve" we mean to look up the
        // uniqueId of the table.
        globalVariableResolver { bindingName ->
            tables.firstOrNull { bindingName.isEquivalentTo(it.tableName) }
                ?.let { GlobalResolutionResult.GlobalVariable(it.uniqueId) }
                ?: GlobalResolutionResult.Undefined
        }

        addPhysicalPlanPass(
            createFilterScanToKeyLookupPass(
                customOperatorName = FAKE_GET_BY_KEY_PROJECT_OPERATOR_NAME,
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
        )
        // We include these passes below to ensure they work correctly with "filter scan to key lookup" pass and
        // to reduce the syntactic overhead of the expected values.
        // DL TODO: reconsider this decision--this hides what the result coming out of the pass actually looks like.
        addPhysicalPlanPass(createRemoveUselessAndsPass())
        addPhysicalPlanPass(createRemoveUselessFiltersPass())
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