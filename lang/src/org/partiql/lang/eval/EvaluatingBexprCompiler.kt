package org.partiql.lang.eval

import com.amazon.ion.IntegerSize
import com.amazon.ion.IonInt
import org.partiql.lang.ast.SourceLocationMeta
import org.partiql.lang.ast.sourceLocation
import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.Property
import org.partiql.lang.util.toIntExact
import org.partiql.lang.util.drop
import org.partiql.lang.util.take

private val DEFAULT_IMPL = PartiqlPhysical.build { impl("default") }

// DL TODO: consider a different name and package for this.
internal class EvaluatingBexprCompiler(
    private val exprCompiler: ExprCompiler,
    private val thunkFactory: ThunkFactory
) : PartiqlPhysical.Bexpr.Converter<BindingsThunkEnv> {
    val valueFactory = thunkFactory.valueFactory

    private fun blockNonDefaultImpl(i: PartiqlPhysical.Impl) {
        if(i != DEFAULT_IMPL) {
            TODO("Support non-default operator implementations")
        }
    }

    override fun convertProject(node: PartiqlPhysical.Bexpr.Project): BindingsThunkEnv {
        TODO("not implemented")
    }

    override fun convertScan(node: PartiqlPhysical.Bexpr.Scan): BindingsThunkEnv {
        blockNonDefaultImpl(node.i)

        val exprThunk = exprCompiler.compile(node.expr)
        val asIndex = node.asDecl.index.value.toIntExact()
        val atIndex = node.atDecl?.index?.value?.toIntExact() ?: -1
        val byIndex = node.byDecl?.index?.value?.toIntExact() ?: -1

        return thunkFactory.bindingsThunk(node.metas) { env ->
            val valueToScan = exprThunk.invoke(env)
            // DL TODO: verify that this .map is *not* eager.

            // coerces non-collection types to a singleton Sequence<>.
            val rows = when(valueToScan.type) {
                ExprValueType.LIST, ExprValueType.BAG -> valueToScan.asSequence()
                else -> sequenceOf(valueToScan)
            }

            BindingsCollection(
                BindingsCollectionType.BAG,
                rows.map { item ->
                    val bindings = newBindingsMap().also { bindingsMap ->
                        bindingsMap[asIndex] = item

                        // DL TODO: consider putting a ValueFactory on the EvaluationSession so we don't have
                        // DL TODO: to use thunkFactory's

                        if (atIndex >= 0) {
                            bindingsMap[atIndex] = item.name ?: valueFactory.missingValue
                        }

                        if (byIndex >= 0) {
                            bindingsMap[byIndex] = item.address ?: valueFactory.missingValue
                        }
                    }
                    bindings
                }
            )
        }
    }

    override fun convertFilter(node: PartiqlPhysical.Bexpr.Filter): BindingsThunkEnv {
        blockNonDefaultImpl(node.i)

        val predicateThunk = exprCompiler.compile(node.predicate)
        val sourceThunk = this.convert(node.source)

        return thunkFactory.bindingsThunk(node.metas) { env ->
            val sourceToFilter = sourceThunk(env)

            BindingsCollection(
                sourceToFilter.seqType,
                sourceToFilter.asSequence().filter { item ->
                    val predicateResult = predicateThunk(env.copy(localBindingsMap = item))
                    when(predicateResult.type) {
                       ExprValueType.NULL, ExprValueType.MISSING -> false
                       ExprValueType.BOOL -> predicateResult.booleanValue()
                       else -> TODO("how to handle predicates that don't return missing, null or boolean?")
                    }
                }
            )
        }
    }

    override fun convertJoin(node: PartiqlPhysical.Bexpr.Join): BindingsThunkEnv {
        blockNonDefaultImpl(node.i)

        val leftThunk = this.convert(node.left)
        val rightThunk = this.convert(node.right)
        val predicateThunk = exprCompiler.compile(node.predicate)

//        if(!(node.predicate is PartiqlPhysical.Expr.Lit && node.predicate.value is BoolElement && node.predicate.value.booleanValue)) {
//            TODO("Support JOIN predicates other than TRUE")
//        }

        return when(node.joinType) {
            is PartiqlPhysical.JoinType.Inner -> thunkFactory.bindingsThunk(node.metas) { env ->
                // evaluate left thunk
                val leftRows = leftThunk(env)
                BindingsCollection(
                    BindingsCollectionType.BAG,
                    // DL TODO: verify that this is lazy and not eager.
                    sequence<BindingsMap> {
                        // DL TODO: can we reduce the need to build so many hashsets?
                        // DL TODO: create a BindingsMap.merge function which also checks for duplicate variable indexes, as this should indicate a bug.
                        // DL TODO: rename these variables for clarity (clearly indicate input vs output bindings)
                        leftRows.asSequence().forEach { leftOutputBindings: BindingsMap ->
                            val rightBindings = newBindingsMap().apply {
                                putAll(env.localBindingsMap)
                                putAll(leftOutputBindings)
                            }

                            val rightRows = rightThunk(env.copy(localBindingsMap = rightBindings))
                            rightRows.asSequence().forEach { rightOutputBindings ->
                                val joinOutputBindings = newBindingsMap().apply {
                                    putAll(leftOutputBindings)
                                    putAll(rightOutputBindings)
                                }

                                // Now we've got the final output scope, let's check the predicate and yield
                                // a row if it matches.
                                val matches = predicateThunk(env.copy(localBindingsMap = joinOutputBindings))
                                when(matches.type) {
                                    ExprValueType.MISSING, ExprValueType.NULL -> { }
                                    ExprValueType.BOOL -> {
                                        if (matches.booleanValue()) {
                                            yield(joinOutputBindings)
                                        }; Unit
                                    }
                                    else -> TODO("Handle mismatched where predicate type.")
                                }.let { }
                            }
                        }
                    }
                )
            }
            is PartiqlPhysical.JoinType.Full -> TODO("FULL JOIN")
            is PartiqlPhysical.JoinType.Left -> TODO("LEFT JOIN")
            is PartiqlPhysical.JoinType.Right -> TODO("RIGHT JOIN")
        }
    }

    override fun convertOffset(node: PartiqlPhysical.Bexpr.Offset): BindingsThunkEnv {
        val rowCountThunk = exprCompiler.compile(node.rowCount)
        val sourceThunk = this.convert(node.source)
        val rowCountLocation = node.rowCount.metas.sourceLocationMeta
        return thunkFactory.bindingsThunk(node.metas) { env ->
            val rowCount = evalOffsetRowCount(rowCountThunk, env, rowCountLocation)
            val rows = sourceThunk(env)
            BindingsCollection(
                rows.seqType,
                rows.drop(rowCount)
            )
        }
    }

    override fun convertLimit(node: PartiqlPhysical.Bexpr.Limit): BindingsThunkEnv {
        val rowCountThunk = exprCompiler.compile(node.rowCount)
        val sourceThunk = this.convert(node.source)
        val rowCountLocation = node.rowCount.metas.sourceLocationMeta
        return thunkFactory.bindingsThunk(node.metas) { env ->
            val rowCount = evalLimitRowCount(rowCountThunk, env, rowCountLocation)
            val rows = sourceThunk(env)
            BindingsCollection(
                rows.seqType,
                rows.take(rowCount)
            )
        }
    }
}


private fun evalLimitRowCount(rowCountThunk: ThunkEnv, env: Environment, limitLocationMeta: SourceLocationMeta?): Long {
    val limitExprValue = rowCountThunk(env)

    if (limitExprValue.type != ExprValueType.INT) {
        err(
            "LIMIT value was not an integer",
            ErrorCode.EVALUATOR_NON_INT_LIMIT_VALUE,
            errorContextFrom(limitLocationMeta).also {
                it[Property.ACTUAL_TYPE] = limitExprValue.type.toString()
            },
            internal = false
        )
    }

    // `Number.toLong()` (used below) does *not* cause an overflow exception if the underlying [Number]
    // implementation (i.e. Decimal or BigInteger) exceeds the range that can be represented by Longs.
    // This can cause very confusing behavior if the user specifies a LIMIT value that exceeds
    // Long.MAX_VALUE, because no results will be returned from their query.  That no overflow exception
    // is thrown is not a problem as long as PartiQL's restriction of integer values to +/- 2^63 remains.
    // We throw an exception here if the value exceeds the supported range (say if we change that
    // restriction or if a custom [ExprValue] is provided which exceeds that value).
    val limitIonValue = limitExprValue.ionValue as IonInt
    if (limitIonValue.integerSize == IntegerSize.BIG_INTEGER) {
        err(
            "IntegerSize.BIG_INTEGER not supported for LIMIT values",
            ErrorCode.INTERNAL_ERROR,
            errorContextFrom(limitLocationMeta),
            internal = true
        )
    }

    val limitValue = limitExprValue.numberValue().toLong()

    if (limitValue < 0) {
        err(
            "negative LIMIT",
            ErrorCode.EVALUATOR_NEGATIVE_LIMIT,
            errorContextFrom(limitLocationMeta),
            internal = false
        )
    }

    // we can't use the Kotlin's Sequence<T>.take(n) for this since it accepts only an integer.
    // this references [Sequence<T>.take(count: Long): Sequence<T>] defined in [org.partiql.util].
    return limitValue
}

private fun evalOffsetRowCount(rowCountThunk: ThunkEnv, env: Environment, offsetLocationMeta: SourceLocationMeta?): Long {
    val offsetExprValue = rowCountThunk(env)

    if (offsetExprValue.type != ExprValueType.INT) {
        err(
            "OFFSET value was not an integer",
            ErrorCode.EVALUATOR_NON_INT_OFFSET_VALUE,
            errorContextFrom(offsetLocationMeta).also {
                it[Property.ACTUAL_TYPE] = offsetExprValue.type.toString()
            },
            internal = false
        )
    }

    // `Number.toLong()` (used below) does *not* cause an overflow exception if the underlying [Number]
    // implementation (i.e. Decimal or BigInteger) exceeds the range that can be represented by Longs.
    // This can cause very confusing behavior if the user specifies a OFFSET value that exceeds
    // Long.MAX_VALUE, because no results will be returned from their query.  That no overflow exception
    // is thrown is not a problem as long as PartiQL's restriction of integer values to +/- 2^63 remains.
    // We throw an exception here if the value exceeds the supported range (say if we change that
    // restriction or if a custom [ExprValue] is provided which exceeds that value).
    val offsetIonValue = offsetExprValue.ionValue as IonInt
    if (offsetIonValue.integerSize == IntegerSize.BIG_INTEGER) {
        err(
            "IntegerSize.BIG_INTEGER not supported for OFFSET values",
            ErrorCode.INTERNAL_ERROR,
            errorContextFrom(offsetLocationMeta),
            internal = true
        )
    }

    val offsetValue = offsetExprValue.numberValue().toLong()

    if (offsetValue < 0) {
        err(
            "negative OFFSET",
            ErrorCode.EVALUATOR_NEGATIVE_OFFSET,
            errorContextFrom(offsetLocationMeta),
            internal = false
        )
    }

    return offsetValue
}
