
package org.partiql.lang.ast.passes

import com.amazon.ion.IonSexp
import com.amazon.ion.IonSystem
import org.partiql.lang.ast.AstSerializer
import org.partiql.lang.ast.AstVersion
import org.partiql.lang.ast.ExprNode

/**
 * This class is intended for backward compatibility.  It can serialize any [ExprNode] instance to a V0 s-expression
 * based AST.
 *
 * Instead of this class, please use [AstSerializer] instead.
 */
@Deprecated("Please use AstSerializer class instead")
class V0AstSerializer {

    companion object {
        /**
         * Converts an instance of [ExprNode] to the legacy s-expression based AST.
         */
        @JvmStatic
        fun serialize(expr: ExprNode, ion: IonSystem): IonSexp =
            AstSerializer.serialize(expr, AstVersion.V0, ion)
    }
}
