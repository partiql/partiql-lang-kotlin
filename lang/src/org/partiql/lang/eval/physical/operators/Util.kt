import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.booleanValue
import org.partiql.lang.eval.isUnknown
import org.partiql.lang.eval.physical.EvaluatorState
import org.partiql.lang.eval.physical.ExprThunkEnv
import org.partiql.lang.eval.relation.RelationIterator
import org.partiql.lang.eval.relation.RelationType
import org.partiql.lang.eval.relation.relation

internal fun createFilterRelItr(
    relItr: RelationIterator,
    predicateThunk: ExprThunkEnv,
    env: EvaluatorState
) = relation(RelationType.BAG) {
    while (true) {
        if (!relItr.nextRow()) {
            break
        } else {
            val matches = predicateThunk(env)
            if (coercePredicateResult(matches)) {
                yield()
            }
        }
    }
}

internal fun coercePredicateResult(value: ExprValue): Boolean =
    when {
        value.isUnknown() -> false
        else -> value.booleanValue() // <-- throws if [value] is not a boolean.
    }
