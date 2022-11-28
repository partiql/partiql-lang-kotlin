package org.partiql.lang.util

import com.amazon.ion.IonSystem
import com.amazon.ionschema.IonSchemaSystemBuilder
import org.partiql.lang.partiqlisl.getResourceAuthority

fun createPartiqlIonSchemaSystem(ion: IonSystem) = IonSchemaSystemBuilder.standard()
    .addAuthority(getResourceAuthority(ion))
    .withIonSystem(ion)
    .build()
