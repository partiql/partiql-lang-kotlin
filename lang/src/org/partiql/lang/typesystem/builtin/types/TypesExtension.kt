package org.partiql.lang.typesystem.builtin.types

import com.amazon.ion.IonSystem
import com.amazon.ion.system.IonSystemBuilder
import org.partiql.lang.eval.ExprValueFactory

val ion: IonSystem = IonSystemBuilder.standard().build()
val valueFactory = ExprValueFactory.standard(ion)
