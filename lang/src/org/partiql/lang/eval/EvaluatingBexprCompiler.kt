package org.partiql.lang.eval

import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.util.toIntExact

// DL TODO: consider a different name and package for this.
internal class EvaluatingBexprCompiler(
    private val exprCompiler: ExprCompiler,
    private val thunkFactory: ThunkFactory
) : PartiqlPhysical.Bexpr.Converter<BindingsThunkEnv> {
    override fun convertProject(node: PartiqlPhysical.Bexpr.Project): BindingsThunkEnv {
        TODO("not implemented")
    }

    override fun convertScan(node: PartiqlPhysical.Bexpr.Scan): BindingsThunkEnv {
        val exprThunk = exprCompiler.compile(node.expr)
        val asIndex = node.asDecl.index.value.toIntExact()
        val atIndex = node.atDecl?.index?.value?.toIntExact() ?: -1
        val byIndex = node.byDecl?.index?.value?.toIntExact() ?: -1

        return thunkFactory.bindingsThunk(node.metas) { env ->
            val valueToScan = exprThunk.invoke(env)
            // DL TODO: verify that this .map is *not* eager.

            // determines the BindingsCollectionType and coerces non-collection types to a singleton Sequence<>.
            val (bcType, rows)  = when(valueToScan.type) {
                ExprValueType.SEXP, ExprValueType.LIST -> BindingsCollectionType.LIST to valueToScan.asSequence()
                ExprValueType.BAG -> BindingsCollectionType.BAG to valueToScan.asSequence()
                else -> BindingsCollectionType.LIST to sequenceOf(valueToScan)
            }
            BindingsCollection(
                bcType,
                rows.asSequence().map {
                    newBindingsMap().also { bindingsMap ->
                        bindingsMap[asIndex] = it

                        if (atIndex >= 0) {
                            bindingsMap[asIndex] = it
                        }

                        if (byIndex >= 0) {
                            // DL TODO: consider putting a ValueFactory on the EvaluationSession so we don't have
                            // DL TODO: to use thunkFactory's
                            bindingsMap[byIndex] = it.address ?: thunkFactory.valueFactory.missingValue
                        }
                    }
                }
            )
        }
    }

    override fun convertFilter(node: PartiqlPhysical.Bexpr.Filter): BindingsThunkEnv {
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
}