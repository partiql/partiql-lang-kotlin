package org.partiql.lang.util

import com.amazon.ion.IonSystem
import com.amazon.ionschema.IonSchemaSystemBuilder
import org.partiql.lang.util.impl.ResourceAuthority

fun createPartiqlIonSchemaSystem(ion: IonSystem) = IonSchemaSystemBuilder.standard()
    .addAuthority(ResourceAuthority.getResourceAuthority(ion))
    .withIonSystem(ion)
    .build()
