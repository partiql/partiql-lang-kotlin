package org.partiql.plugins.local

import org.partiql.eval.bindings.Binding
import org.partiql.eval.bindings.Bindings

internal object LocalBindings : Bindings {

    override fun getBindings(name: String): Bindings? = null

    override fun getBinding(name: String): Binding? = null
}
