package org.partiql.lang.planner.e2e.operators

import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.eval.physical.SetVariableFunc
import org.partiql.lang.eval.physical.operators.ProjectRelationalOperatorFactory
import org.partiql.lang.eval.physical.operators.RelationExpression
import org.partiql.lang.eval.physical.operators.ValueExpression
import org.partiql.lang.eval.relation.RelationType
import org.partiql.lang.eval.relation.relation
import org.partiql.lang.planner.e2e.DB_CONTEXT_VAR
import org.partiql.lang.planner.e2e.GET_BY_KEY_PROJECT_IMPL_NAME
import org.partiql.lang.planner.e2e.InMemoryDatabase
import java.util.UUID

class GetByKeyProjectRelationalOperatorFactory : ProjectRelationalOperatorFactory(GET_BY_KEY_PROJECT_IMPL_NAME) {
    /**
     * This function is called at compile-time to create an instance of the operator [RelationExpression]
     * that will be invoked at evaluation-time.
     */
    override fun create(
        impl: PartiqlPhysical.Impl,
        setVar: SetVariableFunc,
        args: List<ValueExpression>
    ): RelationExpression {
        // this code runs at compile-time.
        require(args.size == 1) {
            "Expected one argument to $GET_BY_KEY_PROJECT_IMPL_NAME but found ${args.size}"
        }

        val keyValueExpression = args.single()

        val tableId = UUID.fromString(impl.staticArgs.single().textValue)

        // DL TODO: probably want to refactor this to a get-by-key or get-by-range operation because it
        // DL TODO: is closer to real-life.  The custom scan operation really provides no value over the
        // DL TODO: default scan
        return RelationExpression { state ->
            // this code runs at evaluation-time.

            // Get the current database from the EvaluationSession context.
            // Please note that the state.session.context map is immutable, therefore it is not possible
            // for custom operators or functions to put stuff in there. (Hopefully that will reduce the
            // chances of it being abused.)
            val db = state.session.context[DB_CONTEXT_VAR] as InMemoryDatabase

            // Compute the value of the key using the keyValueExpression
            val keyValue = keyValueExpression.invoke(state)

            // get the record requested.
            val record = db.getRecordByKey(tableId, keyValue)

            // if the record was not found, return an empty relation:
            if (record == null)
                relation(RelationType.BAG) { }
            else {
                // Return the relation which is Kotlin-coroutine that simply iterates every item in the table.
                // This is very naive and doesn't really provide any value over the default scan operator
                // that might be used in conjunction with this.bindings, above.
                relation(RelationType.BAG) {
                    // `state` is sacrosanct and should not be modified outside PartiQL.  PartiQL
                    // provides the setVar function so that embedders can safely set the value of the
                    // variable from within the relation without clobbering anything else.
                    // It is important to call setVar *before* the yield since otherwise the value
                    // of the variable will not be assigned before it is accessed.
                    setVar(state, record)
                    yield()
                }
            }
        }
    }
}
