package org.partiql.lang.eval

import com.amazon.ion.IntegerSize
import com.amazon.ion.IonInt
import org.partiql.lang.ast.SourceLocationMeta
import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.Property
import org.partiql.lang.eval.relation.RelationIterator
import org.partiql.lang.eval.relation.RelationType
import org.partiql.lang.eval.relation.relation
import org.partiql.lang.util.toIntExact

private val DEFAULT_IMPL = PartiqlPhysical.build { impl("default") }

// DL TODO: consider a different name and package for this.
internal class EvaluatingBexprCompiler(
    private val exprCompiler: ExprCompiler,
    private val thunkFactory: ThunkFactory
) : PartiqlPhysical.Bexpr.Converter<RelationThunkEnv> {
    val valueFactory = thunkFactory.valueFactory

    private fun blockNonDefaultImpl(i: PartiqlPhysical.Impl) {
        if(i != DEFAULT_IMPL) {
            TODO("Support non-default operator implementations")
        }
    }

    override fun convertProject(node: PartiqlPhysical.Bexpr.Project): RelationThunkEnv {
        TODO("not implemented")
    }

    override fun convertScan(node: PartiqlPhysical.Bexpr.Scan): RelationThunkEnv {
        blockNonDefaultImpl(node.i)

        val exprThunk = exprCompiler.compile(node.expr)
        val asIndex = node.asDecl.index.value.toIntExact()
        val atIndex = node.atDecl?.index?.value?.toIntExact() ?: -1
        val byIndex = node.byDecl?.index?.value?.toIntExact() ?: -1

        return thunkFactory.bindingsThunk(node.metas) { env ->
            val valueToScan = exprThunk.invoke(env)

            // coerces non-collection types to a singleton Sequence<>.
            val rows: Sequence<ExprValue> = when(valueToScan.type) {
                ExprValueType.LIST, ExprValueType.BAG -> valueToScan.asSequence()
                else -> sequenceOf(valueToScan)
            }

            relation(RelationType.BAG) {
                var rowsIter: Iterator<ExprValue> = rows.iterator()
                while(rowsIter.hasNext()) {
                    val item = rowsIter.next()
                    env.registers[asIndex] = item.unnamedValue() // Remove any ordinal (output is a bag)

                    if (atIndex >= 0) {
                        env.registers[atIndex] = item.name ?: valueFactory.missingValue
                    }

                    if (byIndex >= 0) {
                        env.registers[byIndex] = item.address ?: valueFactory.missingValue
                    }

                    yield()
                }
            }
        }
    }

    override fun convertFilter(node: PartiqlPhysical.Bexpr.Filter): RelationThunkEnv {
        blockNonDefaultImpl(node.i)

        val predicateThunk = exprCompiler.compile(node.predicate)
        val sourceThunk = this.convert(node.source)

        return thunkFactory.bindingsThunk(node.metas) { env ->
            val sourceToFilter = sourceThunk.eval(env)
            createFilterRelItr(sourceToFilter, predicateThunk, env)
        }
    }

    private fun createCrossJoinRelItr(
        leftThunk: RelationThunkEnv,
        rightThunk: RelationThunkEnv,
        env: Environment
    ): RelationIterator {
        return relation(RelationType.BAG) {
            val leftItr = leftThunk.eval(env)
            while (leftItr.nextRow()) {
                val rightItr = rightThunk.eval(env)
                while (rightItr.nextRow()) {
                    yield()
                }
            }
        }
    }

    override fun convertJoin(node: PartiqlPhysical.Bexpr.Join): RelationThunkEnv {
        blockNonDefaultImpl(node.i)

        val leftThunk = this.convert(node.left)
        val rightThunk = this.convert(node.right)
        val predicateThunk = exprCompiler.compile(node.predicate)

        return when (node.joinType) {
            is PartiqlPhysical.JoinType.Inner -> thunkFactory.bindingsThunk(node.metas) { env ->
                val crossJoinRelItr = createCrossJoinRelItr(leftThunk, rightThunk, env)
                createFilterRelItr(crossJoinRelItr, predicateThunk, env)
            }
            is PartiqlPhysical.JoinType.Full -> TODO("Full join")
            is PartiqlPhysical.JoinType.Left -> TODO("left join")
            is PartiqlPhysical.JoinType.Right -> TODO("right join")
        }
    }

    private fun createFilterRelItr(
        relItr: RelationIterator,
        predicateThunk: ThunkEnv,
        env: Environment
    ) = relation(RelationType.BAG) {
        while(true) {
            if (!relItr.nextRow()) {
                break
            } else {
                val matches = predicateThunk(env)
                when (matches.type) {
                    ExprValueType.MISSING, ExprValueType.NULL -> { }
                    ExprValueType.BOOL -> {
                        if (matches.booleanValue()) {
                            yield()
                        };
                        Unit
                    }
                    else -> TODO("Handle msismatched where predicate type.")
                }
            }
        }
    }

    override fun convertOffset(node: PartiqlPhysical.Bexpr.Offset): RelationThunkEnv {
        val rowCountThunk = exprCompiler.compile(node.rowCount)
        val sourceThunk = this.convert(node.source)
        val rowCountLocation = node.rowCount.metas.sourceLocationMeta
        return thunkFactory.bindingsThunk(node.metas) { env ->
            val skipCount: Long = evalOffsetRowCount(rowCountThunk, env, rowCountLocation)
            relation(RelationType.BAG) {
                val sourceRel = sourceThunk.eval(env)
                var rowCount = 0L
                while(rowCount++ < skipCount) {
                    // stop iterating if we finish run out of rows before we hit the offset.
                    if(!sourceRel.nextRow()) {
                        return@relation
                    }
                }

                yieldAll(sourceRel)
            }
        }
    }

    override fun convertLimit(node: PartiqlPhysical.Bexpr.Limit): RelationThunkEnv {
        val rowCountThunk = exprCompiler.compile(node.rowCount)
        val sourceThunk = this.convert(node.source)
        val rowCountLocation = node.rowCount.metas.sourceLocationMeta
        return thunkFactory.bindingsThunk(node.metas) { env ->
            val limitCount = evalLimitRowCount(rowCountThunk, env, rowCountLocation)
            val rowIter = sourceThunk.eval(env)
            relation(RelationType.BAG) {
                var rowCount = 0L
                while(rowCount++ < limitCount && rowIter.nextRow()) {
                    yield()
                }
            }
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
