package org.partiql.lang.planner

import org.partiql.lang.ION
import org.partiql.lang.eval.BindingCase
import org.partiql.lang.eval.BindingName
import org.partiql.lang.eval.Bindings
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueFactory
import org.partiql.lang.eval.StructOrdering
import org.partiql.lang.eval.namedValue

/**
 * This is an extremely simple in-memory "database" for the purposes of demonstrating PartiQL's DML functionality
 * in the simplest manner possible.
 *
 * This database supports basic SFW and DML operations.
 */
class InMemoryDatabase(
    /**
     * A list of tables that will be in the in-memory database.
     * Names of the tables should be unique.
     */
    theTables: List<InMemoryTable>
) {
    val valueFactory = ExprValueFactory.standard(ION)
    val tables = theTables.associateBy { it.name }

    // planner needs to resolve global variables (i.e. tables)
    private val resolver = object : MetadataResolver {
        override fun resolveVariable(bindingName: BindingName): ResolutionResult {
            return when (bindingName.bindingCase) {
                BindingCase.SENSITIVE -> tables[bindingName.name]
                BindingCase.INSENSITIVE -> tables.entries.firstOrNull { it.key.compareTo(bindingName.name) == 0 }?.value
            }?.let {
                // Note that the name we pass here becomes the case-sensitive lookup into global bindings, below.
                // Therefore it is important to use the defined name of the table and *not* the name of the
                // variable, which might vary by letter case.
                // DL TODO: the above may not be true (see dl todo comments in the `binding` declaration below
                ResolutionResult.GlobalVariable(it.name)
            } ?: ResolutionResult.Undefined
        }
    }

    val bindings = object : Bindings<ExprValue> {
        override fun get(bindingName: BindingName): ExprValue? {
            // The need to assert this is one of the reasons why we should consider using something other
            // than Bindings<ExprValue>
            require(bindingName.bindingCase == BindingCase.SENSITIVE) {
                "the planner should never set bindingName.bindingCase to INSENSITIVE"
            }
            val table: InMemoryTable = tables[bindingName.name] ?: return null
            return valueFactory.newBag(table)
        }
    }

    private val planner = PlannerPipeline.build(valueFactory) {
        metadataResolver(resolver)
    }

    // planner API changes:
    // need a way to indicate if the query planned is DML or not
    fun executeQuery(sql: String): ExprValue {
        when (val plan = planner.planAndCompile(sql)) {
            is PassResult.Error -> error(plan)
            is PassResult.Success -> {
                val session = EvaluationSession.build {
                    globals(bindings)
                }

                return when (val queryResult = plan.result.eval(session)) {
                    is QueryResult.Value -> queryResult.value
                    is QueryResult.DmlCommand -> {
                        // find the target table
                        val targetTable = tables[queryResult.targetUniqueId] ?: error(
                            "Planned query for some reason allowed a reference to a " +
                                "non-existing table '$queryResult.target.uniqueId' to pass unnoticed!"
                        )

                        var rowsEffected = 0L
                        // now perform the action.
                        when (queryResult.action) {
                            DmlAction.INSERT -> queryResult.rows.forEach {
                                targetTable.insert(it)
                                rowsEffected++
                            }
                            DmlAction.DELETE -> queryResult.rows.forEach {
                                targetTable.delete(it)
                                rowsEffected++
                            }
                        }

                        // returns `{ rows_effected: $rowsEffected }`
                        valueFactory.newStruct(
                            listOf(valueFactory.newInt(rowsEffected).namedValue(valueFactory.newString("rows_effected"))),
                            StructOrdering.UNORDERED
                        )
                    }
                }
            }
        }
    }
}
