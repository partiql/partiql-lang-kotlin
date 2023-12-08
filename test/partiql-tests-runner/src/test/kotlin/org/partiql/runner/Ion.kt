package org.partiql.runner

import com.amazon.ion.system.IonSystemBuilder

/**
 * IonSystem for legacy pipelines and value comparison.
 */
public val ION = IonSystemBuilder.standard().build()
