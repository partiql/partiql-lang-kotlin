package org.partiql.lang.ots_work.stscore

import org.partiql.lang.ast.SourceLocationMeta
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.ots_work.interfaces.CompileTimeType
import org.partiql.lang.ots_work.interfaces.Plugin
import org.partiql.lang.ots_work.plugins.standard.operators.StandardScalarCast

/**
 * [plugin] is the plugin that a PartiQL scalar type system uses. For now, let's assume there is only one plugin existed in the type system.
 */
class ScalarTypeSystem(
    // TODO remove dependencies of the following field from `org.partiql.lang` package and make it private
    val plugin: Plugin
) {
    internal fun inferReturnTypeOfScalarCastOp(sourceType: CompileTimeType, targetType: CompileTimeType) =
        plugin.scalarCast.inferType(sourceType, targetType)

    internal fun invokeCastOp(value: ExprValue, targetType: CompileTimeType, locationMeta: SourceLocationMeta?): ExprValue? {
        // TODO: remove the hard-coded cast below and properly catch the error and add location meta to that error and throw it again.
        (plugin.scalarCast as StandardScalarCast).currentLocationMeta = locationMeta

        return plugin.scalarCast.invoke(value, targetType)
    }

    internal fun invokeIsOp(value: ExprValue, targetType: CompileTimeType): Boolean {
        return plugin.scalarIs.invoke(value, targetType)
    }
}
