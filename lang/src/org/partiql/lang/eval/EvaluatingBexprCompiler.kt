package org.partiql.lang.eval

import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.util.toIntExact

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
}