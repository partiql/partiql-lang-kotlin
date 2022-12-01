package org.partiql.lang.compiler.memorydb

import com.amazon.ionelement.api.toIonValue
import org.partiql.lang.ION
import org.partiql.lang.compiler.PartiQLCompilerPipeline
import org.partiql.lang.compiler.memorydb.operators.GetByKeyProjectRelationalOperatorFactory
import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.eval.BindingCase
import org.partiql.lang.eval.BindingName
import org.partiql.lang.eval.Bindings
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.PartiQLResult
import org.partiql.lang.eval.StructOrdering
import org.partiql.lang.eval.namedValue
import org.partiql.lang.planner.GlobalResolutionResult
import org.partiql.lang.planner.GlobalVariableResolver
import org.partiql.lang.planner.PartiQLPhysicalPass
import org.partiql.lang.planner.StaticTypeResolver
import org.partiql.lang.planner.transforms.optimizations.createConcatWindowFunctionPass
import org.partiql.lang.planner.transforms.optimizations.createFilterScanToKeyLookupPass
import org.partiql.lang.planner.transforms.optimizations.createRemoveUselessAndsPass
import org.partiql.lang.planner.transforms.optimizations.createRemoveUselessFiltersPass
import org.partiql.lang.types.BagType
import org.partiql.lang.types.StructType
import org.partiql.lang.util.PartiQLExperimental
import org.partiql.lang.util.SexpAstPrettyPrinter
import org.partiql.pig.runtime.DomainNode
import java.util.UUID

// The name of the database in the context variable.
internal const val DB_CONTEXT_VAR = "in-memory-database"

/**
 * This class is a demonstration of how to integrate a storage layer with the experimental PartiQLCompilerPipeline.
 */
@PartiQLExperimental
class QueryEngine(val db: MemoryDatabase) {
    var enableDebugOutput = false

    /** Given a [BindingName], inform the planner the unique identifier of the global variable (usually a table). */
    private val globalVariableResolver = GlobalVariableResolver { bindingName ->
        // The planner has asked us to resolve a global variable named [bindingName]. let's do so and return the
        // UUID of the table.  This will get packaged into a (global_id <uuid>) node (a reference to an
        // unambiguously global variable).
        db.findTableMetadata(bindingName)?.let { tableMetadata ->
            GlobalResolutionResult.GlobalVariable(tableMetadata.tableId.toString())
        } ?: GlobalResolutionResult.Undefined
    }

    /** Given a global variable's unique id, informs the planner about the static type (schema) of the global variable. */
    private val staticTypeResolver = StaticTypeResolver { uniqueId ->
        val tableMetadata = db.getTableMetadata(UUID.fromString(uniqueId))
        // Tables are a bag of structs.
        // TODO: at some point we'll populate this with complete schema information.
        BagType(
            StructType(
                // TODO: nothing in the planner uses the fields property yet
                fields = emptyMap(),
                // TODO: nothing in the planner uses the contentClosed property yet, but "technically" do have open
                // content since nothing is constraining the fields in the table.
                contentClosed = false,
                // The FilterScanTokeyLookup pass does use this.
                primaryKeyFields = tableMetadata.primaryKeyFields
            )
        )
    }

    val bindings = object : Bindings<ExprValue> {
        /**
         * This function is called by the `(global_id <unique_id>)` expression to fetch an [ExprValue] for a resolved
         * global variable, which is almost always a database table.
         */
        override fun get(bindingName: BindingName): ExprValue {
            // TODO: PlannerPipeline may need some additional cleanup here because, perhaps very confusingly, the
            // bindingName passed here contains the UUID of the table and *not* its name.  It should also always
            // specify a case-sensitive binding name.  Really, we should reconsider if [PlannerPipeline] should use
            // [Bindings<T>] at all, perhaps another interface that's more narrow should be used instead.
            // Another difference in how PlannerPipeline uses Bindings<ExprValue> (and argument for using a new
            // interface) is that the bindingName here is guaranteed to be valid because otherwise planning would have
            // been aborted.  If the lookup fails for some reason it would mean the plan is invalid.  But with
            // the [CompilerPipeline] these checks don't exist and therefore the [Bindings<T>] implementation is
            // expected to throw if the variable does not exist.
            require(bindingName.bindingCase == BindingCase.SENSITIVE) {
                "It is assumed that the plan evaluator will always set bindingName.bindingCase to SENSITIVE"
            }

            val tableId = UUID.fromString(bindingName.name)
            return db.valueFactory.newBag(
                db.getFullScanSequence(tableId)
            )
        }
    }

    private val compilerPipeline = PartiQLCompilerPipeline.build {
        planner
            .callback {
                fun prettyPrint(label: String, data: Any) {
                    val padding = 10
                    when (data) {
                        is DomainNode -> {
                            println("$label:")
                            val sexpElement = data.toIonElement()
                            println(SexpAstPrettyPrinter.format(sexpElement.asAnyElement().toIonValue(ION)))
                        }
                        else ->
                            println("$label:".padEnd(padding) + data.toString())
                    }
                }
                if (this@QueryEngine.enableDebugOutput) {
                    prettyPrint("event", it.eventName)
                    prettyPrint("duration", it.duration)
                    if (it.eventName == "parse_sql") prettyPrint("input", it.input)
                    prettyPrint("output", it.output)
                }
            }
            .globalVariableResolver(globalVariableResolver)
            .physicalPlannerPasses(
                listOf(
                    // TODO: push-down filters on top of scans before this pass.
                    PartiQLPhysicalPass { plan, problemHandler ->
                        createFilterScanToKeyLookupPass(
                            customProjectOperatorName = GET_BY_KEY_PROJECT_IMPL_NAME,
                            staticTypeResolver = staticTypeResolver,
                            createKeyValueConstructor = { recordType, keyFieldEqualityPredicates ->
                                require(recordType.primaryKeyFields.size == keyFieldEqualityPredicates.size)
                                PartiqlPhysical.build {
                                    list(
                                        // Key values are expressed to the in-memory storage engine as ordered list. Therefore, we need
                                        // to ensure that the list we pass in as an argument to the custom_get_by_key project operator
                                        // impl is in the right order.
                                        recordType.primaryKeyFields.map { keyFieldName ->
                                            keyFieldEqualityPredicates.single { it.keyFieldName == keyFieldName }.equivalentValue
                                        }
                                    )
                                }
                            }
                        ).apply(plan, problemHandler)
                    },
                    // Note that the order of the following plans is relevant--the "remove useless filters" pass
                    // will not work correctly if "remove useless ands" pass is not executed first.

                    // After the filter-scan-to-key-lookup pass above, we may be left with some `(and ...)` expressions
                    // whose operands were replaced with `(lit true)`. This pass removes `(lit true)` operands from `and`
                    // expressions, and replaces any `and` expressions with only `(lit true)` operands with `(lit true)`.
                    // This happens recursively, so an entire tree of useless `(and ...)` expressions will be replaced
                    // with a single `(lit true)`.
                    // A constant folding pass might one day eliminate the need for this, but that is not within the current scope.
                    PartiQLPhysicalPass { plan, problemHandler ->
                        createRemoveUselessAndsPass().apply(plan, problemHandler)
                    },

                    // After the previous pass, we may have some `(filter ... )` nodes with `(lit true)` as a predicate.
                    // This pass removes these useless filter nodes.
                    PartiQLPhysicalPass { plan, problemHandler ->
                        createRemoveUselessFiltersPass().apply(plan, problemHandler)
                    },

                    PartiQLPhysicalPass { plan, problemHandler ->
                        createConcatWindowFunctionPass().apply(plan, problemHandler)
                    },
                )
            )
        compiler
            .ionSystem(ION)
            .customOperatorFactories(
                listOf(
                    GetByKeyProjectRelationalOperatorFactory()
                )
            )
    }

    fun executeQuery(sql: String): ExprValue {

        // session data
        val session = EvaluationSession.build {
            globals(bindings)
            // Please note that the context here is immutable once the call to .build above
            // returns, (Hopefully that will reduce the chances of it being abused.)
            withContextVariable("in-memory-database", db)
        }

        // compile query to statement
        val statement = compilerPipeline.compile(sql)

        // First step is to plan the query.
        // This parses the query and runs it through all the planner passes:
        // AST -> logical plan -> resolved logical plan -> default physical plan -> custom physical plan
        return when (val result = statement.eval(session)) {
            is PartiQLResult.Value -> result.value
            is PartiQLResult.Delete -> {
                val targetTableId = UUID.fromString(result.target)
                var rowsEffected = 0L
                result.rows.forEach {
                    db.delete(targetTableId, it)
                    rowsEffected ++
                }
                db.valueFactory.newStruct(
                    listOf(
                        db.valueFactory.newInt(rowsEffected)
                            .namedValue(db.valueFactory.newString("rows_effected"))
                    ),
                    StructOrdering.UNORDERED
                )
            }
            is PartiQLResult.Insert -> {
                val targetTableId = UUID.fromString(result.target)
                var rowsEffected = 0L
                result.rows.forEach {
                    db.insert(targetTableId, it)
                    rowsEffected ++
                }
                db.valueFactory.newStruct(
                    listOf(
                        db.valueFactory.newInt(rowsEffected)
                            .namedValue(db.valueFactory.newString("rows_effected"))
                    ),
                    StructOrdering.UNORDERED
                )
            }
            is PartiQLResult.Replace -> TODO("Not implemented yet")
            is PartiQLResult.Explain.Domain -> TODO("Not implemented yet")
        }
    }
}
