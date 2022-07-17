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

        // Sanity check the static and dynamic arguments of this operator.

        // If either of these checks fail, it would indicate a bug in the rewrite which created this (project ...)
        // operator.
        require(impl.staticArgs.size == 1) {
            "Expected one static argument to $GET_BY_KEY_PROJECT_IMPL_NAME but found ${args.size}"
        }
        require(args.size == 1) {
            "Expected one argument to $GET_BY_KEY_PROJECT_IMPL_NAME but found ${args.size}"
        }

        // Extract the key value constructor
        val keyValueExpression = args.single()

        // Parse the tableId so we don't have to at evaluation-time
        val tableId = UUID.fromString(impl.staticArgs.single().textValue)

        // Finally, return a RelationExpression which evaluates the key value expression and returns a
        // RelationIterator containing a single row corresponding to the key (or no rows if nothing matches)
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
                relation(RelationType.BAG) {
                    // this is an empty relation empty because there is no call to yield()
                }
            else {
                // Return the relation which is Kotlin-coroutine that simply projects the single record we
                // found above into the one variable allowed by the project operator, yields, and then returns.
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
