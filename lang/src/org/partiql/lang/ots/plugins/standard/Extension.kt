package org.partiql.lang.ots.plugins.standard

import com.amazon.ion.system.IonSystemBuilder
import org.partiql.lang.eval.ExprValueFactory

internal val ion = IonSystemBuilder.standard().build()
internal val valueFactory = ExprValueFactory.standard(ion)
