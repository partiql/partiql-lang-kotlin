package org.partiql.lang.eval.physical.operators

import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.eval.address
import org.partiql.lang.eval.name
import org.partiql.lang.eval.physical.EvaluatorState
import org.partiql.lang.eval.physical.SetVariableFunc
import org.partiql.lang.eval.relation.RelationIterator
import org.partiql.lang.eval.relation.RelationType
import org.partiql.lang.eval.relation.relation
import org.partiql.lang.eval.unnamedValue
import org.partiql.lang.planner.transforms.DEFAULT_IMPL_NAME

/**
 * Provides an implementation of the [PartiqlPhysical.Bexpr.Scan] operator.
 *
 * @constructor
 *
 * @param name
 */
abstract class ScanRelationalOperatorFactoryAsync(name: String) : RelationalOperatorFactory {

    final override val key = RelationalOperatorFactoryKey(RelationalOperatorKind.SCAN, name)

    /**
     * Creates a [RelationExpressionAsync] instance for [PartiqlPhysical.Bexpr.Scan].
     *
     * @param impl static arguments
     * @param expr invoked to obtain an iterable value
     * @param setAsVar AS variable binding
     * @param setAtVar AT variable binding, if non-null
     * @param setByVar BY variable binding, if non-null
     * @return
     */
    abstract fun create(
        impl: PartiqlPhysical.Impl,
        expr: ValueExpressionAsync,
        setAsVar: SetVariableFunc,
        setAtVar: SetVariableFunc?,
        setByVar: SetVariableFunc?
    ): RelationExpressionAsync
}

internal object ScanRelationalOperatorFactoryDefaultAsync : ScanRelationalOperatorFactoryAsync(DEFAULT_IMPL_NAME) {
    override fun create(
        impl: PartiqlPhysical.Impl,
        expr: ValueExpressionAsync,
        setAsVar: SetVariableFunc,
        setAtVar: SetVariableFunc?,
        setByVar: SetVariableFunc?
    ) = ScanOperatorAsync(expr, setAsVar, setAtVar, setByVar)
}

internal class ScanOperatorAsync(
    private val expr: ValueExpressionAsync,
    private val setAsVar: SetVariableFunc,
    private val setAtVar: SetVariableFunc?,
    private val setByVar: SetVariableFunc?
) : RelationExpressionAsync {

    override suspend fun evaluate(state: EvaluatorState): RelationIterator {
        val value = expr(state)
        val sequence: Sequence<ExprValue> = when (value.type) {
            ExprValueType.LIST,
            ExprValueType.BAG -> value.asSequence()
            else -> sequenceOf(value)
        }
        return relation(RelationType.BAG) {
            val rows: Iterator<ExprValue> = sequence.iterator()
            while (rows.hasNext()) {
                val item = rows.next()
                // .unnamedValue() removes any ordinal that might exist on item
                setAsVar(state, item.unnamedValue())
                setAtVar?.let { it(state, item.name ?: ExprValue.missingValue) }
                setByVar?.let { it(state, item.address ?: ExprValue.missingValue) }
                yield()
            }
        }
    }
}
